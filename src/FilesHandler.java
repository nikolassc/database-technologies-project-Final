import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Random;

class FilesHandler {
    private static final String DELIMITER = ",";
    private static final String PATH_TO_CSV = "data.csv";
    static final String PATH_TO_DATAFILE = "datafile.dat";
    static final String PATH_TO_INDEXFILE = "indexfile.dat";
    private static final int BLOCK_SIZE = 32 * 1024;
    private static int dataDimensions;
    private static int totalBlocksInDataFile;
    private static int totalBlocksInIndexFile;
    private static int totalLevelsOfTreeIndex;

    static String getPathToCsv(){
        return PATH_TO_CSV;
    }

    static String getDelimiter(){
        return DELIMITER;
    }

    static int getDataDimensions(){
        return dataDimensions;
    }

    //This is used to Serialize a serializable Object to byte array
    private static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        return baos.toByteArray();
    }

    //This is used to Deserialize a byte array to a Serializable object
    private static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bais);
        return ois.readObject();
    }

    //Reads data from given file path
    //Reads block 0, for metadata
    // returns ArrayList<Integer> which includes the metadata in the given file
    private static ArrayList<Integer> readMetaDataBlock(String pathToFile){
        try {
            RandomAccessFile raf = new RandomAccessFile(new File(pathToFile), "rw");
            FileInputStream fis = new FileInputStream(raf.getFD());
            BufferedInputStream bis = new BufferedInputStream(fis);
            byte[] block = new byte[BLOCK_SIZE];
            if (bis.read(block) != BLOCK_SIZE) {
                throw new IllegalStateException("Block size read was not " + BLOCK_SIZE + " bytes");
            }

            byte[] metaDataSizeBytes = serialize(new Random().nextInt());
            System.arraycopy(metaDataSizeBytes, 0, block, 0, metaDataSizeBytes.length);

            byte[] dataInBlock = new byte[(Integer) deserialize(metaDataSizeBytes)];
            System.arraycopy(block, 0, dataInBlock, 0, dataInBlock.length);

            return (ArrayList<Integer>) deserialize(dataInBlock);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    //Updates metadata of given file
    //Saves current dataDimensions, block size and total blocks on metadata block of this file
    private static void updateMetaDataBlock(String pathToFile) {
        try {
            ArrayList<Integer> fileMetaData = new ArrayList<>();
            fileMetaData.add(dataDimensions);
            fileMetaData.add(BLOCK_SIZE);
            if(pathToFile.equals(PATH_TO_DATAFILE)){
                fileMetaData.add(++totalBlocksInDataFile);
            } else if(pathToFile.equals(PATH_TO_INDEXFILE)){
                fileMetaData.add(++totalBlocksInIndexFile);
                fileMetaData.add(totalLevelsOfTreeIndex);
            }
            byte[] metaDataInBytes = serialize(fileMetaData);
            byte[] metaDataSizeBytes = serialize(metaDataInBytes.length);
            byte[] block = new byte[BLOCK_SIZE];
            System.arraycopy(metaDataSizeBytes, 0, block, 0, metaDataSizeBytes.length);
            System.arraycopy(metaDataInBytes, 0, block, metaDataSizeBytes.length, metaDataInBytes.length);

            RandomAccessFile raf = new RandomAccessFile(new File(pathToFile), "rw");
            raf.write(block);
            raf.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    // ------------------------------------------- DATA FILE METHODS ---------------------------------------------------

    static int getTotalBlocksInDataFile(){
        return totalBlocksInDataFile;
    }

    // Calculates and return an integer which represents the maximum number of records a block of BLOCK_SIZE can have
    private static int calculateMaxRecordsInBlock() {
        ArrayList<Record> blockRecords = new ArrayList<>();
        int i;
        for (i = 0; i < Integer.MAX_VALUE; i++) {
            ArrayList<Double> coordinateForEachDimension = new ArrayList<>();
            for (int d = 0; d < FilesHandler.dataDimensions; d++)
                coordinateForEachDimension.add(0.0);
            Record record = new Record(0, " ",coordinateForEachDimension);
            blockRecords.add(record);
            byte[] recordInBytes = new byte[0];
            byte[] metaDataLengthInBytes = new byte[0];
            try {
                recordInBytes = serialize(blockRecords);
                metaDataLengthInBytes = serialize(recordInBytes.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (metaDataLengthInBytes.length + recordInBytes.length > BLOCK_SIZE)
                break;
        }
        return i;
    }

    private static void writeDataFileBlock(ArrayList<Record> records) {
        try{
            byte[] recordInBytes = serialize(records);
            byte[] metaDataLengthInBytes = serialize(recordInBytes.length);
            byte[] block = new byte[BLOCK_SIZE];
            System.arraycopy(recordInBytes, 0, block, 0, metaDataLengthInBytes.length);
            System.arraycopy(recordInBytes, 0, block, metaDataLengthInBytes.length, recordInBytes.length);

            FileOutputStream fos = new FileOutputStream(PATH_TO_DATAFILE, true);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bos.write(block);
            updateMetaDataBlock(PATH_TO_DATAFILE);
            bos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static ArrayList<Record> readDataFileBlock(long blockID) {
        try{
            RandomAccessFile raf = new RandomAccessFile(new File(PATH_TO_DATAFILE), "rw");
            FileInputStream fis = new FileInputStream(raf.getFD());
            BufferedInputStream bis = new BufferedInputStream(fis);
            raf.seek(blockID*BLOCK_SIZE);
            byte[] block = new byte[BLOCK_SIZE];

            if(bis.read(block,0,BLOCK_SIZE) != BLOCK_SIZE)
                throw new IllegalStateException("Block size read was not " + BLOCK_SIZE + " bytes");
            byte[] metaDataLengthInBytes = serialize(new Random().nextInt());
            System.arraycopy(block, 0, block, metaDataLengthInBytes.length, metaDataLengthInBytes.length);

            byte[] recordsInBlock = new byte[(Integer) deserialize(metaDataLengthInBytes)];
            System.arraycopy(block, metaDataLengthInBytes.length, recordsInBlock, 0, recordsInBlock.length);

            return (ArrayList<Record>) deserialize(recordsInBlock);

        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    // Initializes data file
    // Calculates total blocks
    // reads data from csv and adds to datafile
    static void initializeDataFile(int dataDimensions, boolean newDataFile){
        try{
            // Checks if datafile already exists, and creates metaData in metaData block
            if(!newDataFile && Files.exists(Paths.get(PATH_TO_DATAFILE))){
                ArrayList<Integer> dataFileMetaData = readMetaDataBlock(PATH_TO_DATAFILE);
                if(dataFileMetaData == null)
                    throw new IllegalStateException("Could not read datafile's MetaData block");
                FilesHandler.dataDimensions = dataFileMetaData.get(0);
                if(FilesHandler.dataDimensions <= 0)
                    throw new IllegalStateException("Data dimensions must be greater than 0");
                if(dataFileMetaData.get(1) != BLOCK_SIZE)
                    throw new IllegalStateException("Block size read was not " + BLOCK_SIZE + " bytes");
                totalBlocksInDataFile = dataFileMetaData.get(2);
                if (totalBlocksInDataFile < 0)
                    throw new IllegalStateException("Total blocks in data file must be greater than 0");
            }
            // Else initializes a new datafile
            else {
                Files.deleteIfExists(Paths.get(PATH_TO_DATAFILE));
                FilesHandler.dataDimensions = dataDimensions;
                if (FilesHandler.dataDimensions <= 0)
                    throw new IllegalStateException("Data dimensions must be greater than 0");
                updateMetaDataBlock(PATH_TO_DATAFILE);
                ArrayList<Record> blockRecords = new ArrayList<>();
                BufferedReader csvReader = new BufferedReader(new FileReader(PATH_TO_CSV));
                String line;
                int maxRecordsInBlock = calculateMaxRecordsInBlock();
                while ((line = csvReader.readLine()) != null) {
                    if (blockRecords.size() == maxRecordsInBlock) {
                        writeDataFileBlock(blockRecords);
                        blockRecords = new ArrayList<>();
                    }
                    blockRecords.add(new Record(line));
                }
                csvReader.close();
                if(blockRecords.size() > 0)
                    writeDataFileBlock(blockRecords);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }


}
