import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

class FilesHandler {
    private static final String DELIMITER = ",";
    private static final String PATH_TO_CSV = "src/resources/data.csv";
    static final String PATH_TO_DATAFILE = "src/resources/datafile.dat";
    static final String PATH_TO_INDEXFILE = "src/resources/indexfile.dat";
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
    private static ArrayList<Integer> readMetaDataBlock(String pathToFile) {
        try {
            RandomAccessFile raf = new RandomAccessFile(new File(pathToFile), "r");
            byte[] block = new byte[BLOCK_SIZE];

            // Διαβάζει το πρώτο block (block 0) όπου βρίσκονται τα μεταδεδομένα
            raf.seek(0);
            int bytesRead = raf.read(block);
            if (bytesRead != BLOCK_SIZE) {
                throw new IOException("Could not read full metadata block (expected " + BLOCK_SIZE + ", got " + bytesRead + ")");
            }

            // Δημιουργεί stream για ανάγνωση αντικειμένων από το block
            ByteArrayInputStream bais = new ByteArrayInputStream(block);
            ObjectInputStream ois = new ObjectInputStream(bais);

            // Πρώτα διαβάζουμε το μέγεθος των serialized metadata (π.χ. πόσα bytes έχουν τα metadata)
            int metaDataSize = (Integer) ois.readObject();

            // Διαβάζουμε το πραγματικό metadata array (π.χ. [dataDimensions, BLOCK_SIZE, totalBlocks...])
            byte[] metadataBytes = new byte[metaDataSize];
            int actuallyRead = bais.read(metadataBytes);
            if (actuallyRead != metaDataSize) {
                throw new IOException("Could not read full metadata content (expected " + metaDataSize + " bytes, got " + actuallyRead + ")");
            }

            // Απο-σειριοποιούμε σε ArrayList<Integer>
            ObjectInputStream metadataStream = new ObjectInputStream(new ByteArrayInputStream(metadataBytes));
            ArrayList<Integer> metadata = (ArrayList<Integer>) metadataStream.readObject();

            // Επιστροφή των μεταδεδομένων
            return metadata;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
        for (i = 0; i < 10000; i++) { // Αυθαίρετο upper bound
            ArrayList<Double> coords = new ArrayList<>();
            for (int d = 0; d < dataDimensions; d++)
                coords.add(0.0);

            // Πρόσεξε: βάλε και non-empty string για name
            Record record = new Record(0, "default_name", coords);
            blockRecords.add(record);

            byte[] recordInBytes = serializeOrEmpty(blockRecords);
            byte[] lengthInBytes = serializeOrEmpty(recordInBytes.length);

            if (lengthInBytes.length + recordInBytes.length > BLOCK_SIZE)
                break;
        }
        return i - 1;
    }

    private static byte[] serializeOrEmpty(Object obj) {
        try {
            return serialize(obj);
        } catch (IOException e) {
            return new byte[0];
        }
    }

    private static void writeDataFileBlock(ArrayList<Record> records) {
        try{
            byte[] recordInBytes = serialize(records);
            byte[] metaDataLengthInBytes = serialize(recordInBytes.length);
            byte[] block = new byte[BLOCK_SIZE];

            if (metaDataLengthInBytes.length + recordInBytes.length > BLOCK_SIZE) {
                throw new IllegalStateException("Block too large to fit in one data block");
            }

            System.arraycopy(metaDataLengthInBytes, 0, block, 0, metaDataLengthInBytes.length);
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
        try {
            RandomAccessFile raf = new RandomAccessFile(new File(PATH_TO_DATAFILE), "r");
            raf.seek(blockID * BLOCK_SIZE);
            byte[] block = new byte[BLOCK_SIZE];

            int bytesRead = raf.read(block);
            if (bytesRead != BLOCK_SIZE)
                throw new IOException("Block size read was not " + BLOCK_SIZE + " bytes");

            // 1. Read the length of the actual serialized record data
            ByteArrayInputStream bais = new ByteArrayInputStream(block);
            ObjectInputStream ois = new ObjectInputStream(bais);
            int recordDataLength = (Integer) ois.readObject();

            // 2. Read the record data itself
            byte[] recordBytes = new byte[recordDataLength];
            int actuallyRead = bais.read(recordBytes);
            if (actuallyRead != recordDataLength)
                throw new IOException("Could not read full record data");

            // 3. Deserialize to get ArrayList<Record>
            ObjectInputStream recordOis = new ObjectInputStream(new ByteArrayInputStream(recordBytes));
            ArrayList<Record> records = (ArrayList<Record>) recordOis.readObject();

            return records;

        } catch (Exception e) {
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
                System.out.println("dataFileMetaData = " + dataFileMetaData);
                if(dataFileMetaData == null)
                    throw new Exception("Could not read datafile's MetaData block");
                FilesHandler.dataDimensions = dataFileMetaData.get(0);
                if(FilesHandler.dataDimensions <= 0)
                    throw new Exception("Data dimensions must be greater than 0");
                if(dataFileMetaData.get(1) != BLOCK_SIZE)
                    throw new Exception("Block size read was not " + BLOCK_SIZE + " bytes");
                totalBlocksInDataFile = dataFileMetaData.get(2);
                if (totalBlocksInDataFile < 0)
                    throw new Exception("Total blocks in data file must be greater than 0");
            }
            // Else initializes a new datafile
            else {
                Files.deleteIfExists(Paths.get(PATH_TO_DATAFILE));
                FilesHandler.dataDimensions = dataDimensions;
                if (FilesHandler.dataDimensions <= 0)
                    throw new Exception("Data dimensions must be greater than 0");
                updateMetaDataBlock(PATH_TO_DATAFILE);
                ArrayList<Record> blockRecords = new ArrayList<>();
                BufferedReader csvReader = new BufferedReader(new FileReader(PATH_TO_CSV));
                String line = csvReader.readLine();
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


    //--------------------------------------------- INDEX FILE METHODS -------------------------------------------------

    static int getTotalBlocksInIndexFile() {
        return totalBlocksInIndexFile;
    }

    static int getTotalLevelsFile() {
        return totalLevelsOfTreeIndex;
    }

    static int calculateMaxEntriesInNode() {
        ArrayList<Entry> entriesInNode = new ArrayList<>();
        int i = 0;
        final int SAFETY_THRESHOLD = 5000;

        while(i < SAFETY_THRESHOLD){
            ArrayList<Bounds> boundsForEachDimension = new ArrayList<>();
            for(int j=0; j < FilesHandler.dataDimensions; j++){
                boundsForEachDimension.add(new Bounds(0.0, 0.0));
            }

            Entry entry = new LeafEntry(0L, 0L, boundsForEachDimension);
            entry.setChildNodeBlockId(0L);
            entriesInNode.add(entry);
            i++;

            try{
                byte[] nodeInBytes = serialize(new Node(0, entriesInNode));
                byte[] metaDataLengthInBytes = serialize(entriesInNode.size());
                if (metaDataLengthInBytes.length + nodeInBytes.length > BLOCK_SIZE){
                    break;
                }
            } catch (IOException e){
                e.printStackTrace();
                break;
            }
        }
        return i-1;
    }

    //updates the metadata block of the indexfile with increased level of tree and other details

    private static void updateLevelsOfTreeIndexFile() {
        try{
            ArrayList<Integer> indexFileMetaData = new ArrayList<>();
            indexFileMetaData.add(dataDimensions);
            indexFileMetaData.add(BLOCK_SIZE);
            indexFileMetaData.add(totalBlocksInIndexFile);
            indexFileMetaData.add(++totalLevelsOfTreeIndex);
            byte[] metaDataInBytes = serialize(indexFileMetaData);
            byte[] metaDataLengthInBytes = serialize(metaDataInBytes.length);
            byte[] block = new byte[BLOCK_SIZE];
            System.arraycopy(metaDataInBytes, 0, block, 0, metaDataLengthInBytes.length);
            System.arraycopy(metaDataInBytes, metaDataLengthInBytes.length, block, 0, block.length);

            RandomAccessFile raf = new RandomAccessFile(new File(PATH_TO_DATAFILE), "rw");
            raf.write(block);
            raf.close();

        } catch (Exception e){
            e.printStackTrace();
        }
    }


    //Reads data from csv and adds it to IndexFile

    static void initializeIndexFile(int dataDimensions, boolean newFile){
        try{
            if(!newFile && Files.exists(Paths.get(PATH_TO_INDEXFILE))){
                ArrayList<Integer> indexFileMetaData = readMetaDataBlock(PATH_TO_INDEXFILE);
                if(indexFileMetaData == null) {
                    throw new Exception("Could not read index file's MetaData block");
                }
                FilesHandler.dataDimensions = indexFileMetaData.get(0);
                if(FilesHandler.dataDimensions <= 0){
                    throw new Exception("Data dimensions must be greater than 0");
                }
                if(indexFileMetaData.get(1) != BLOCK_SIZE){
                    throw new Exception("Block size read was not " + BLOCK_SIZE + " bytes");
                }
                totalBlocksInIndexFile = indexFileMetaData.get(2);
                if(totalBlocksInIndexFile < 0){
                    throw new Exception("Total blocks in data file must be greater than 0");
                }
                totalLevelsOfTreeIndex = indexFileMetaData.get(3);
                if(totalLevelsOfTreeIndex < 0){
                    throw new Exception("Total levels of tree index must be greater than 0");
                }
            }
            // if it doesnt exist
            else {
                Files.deleteIfExists(Paths.get(PATH_TO_INDEXFILE));
                FilesHandler.dataDimensions = dataDimensions;
                totalLevelsOfTreeIndex = 1;
                if(FilesHandler.dataDimensions <= 0){
                    throw new Exception("Data dimensions must be greater than 0");
                }
                updateMetaDataBlock(PATH_TO_INDEXFILE);
            }
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    // Appends given Node as a new block in indexFiles
    static void writeNewIndexFileBlock(Node node){
        try {
            byte[] nodeInBytes = serialize(node);
            byte[] metaDataLengthInBytes = serialize(nodeInBytes.length);
            byte[] block = new byte[BLOCK_SIZE];

            System.arraycopy(metaDataLengthInBytes, 0, block, 0, metaDataLengthInBytes.length);
            System.arraycopy(nodeInBytes, metaDataLengthInBytes.length, block, 0, block.length);

            FileOutputStream fos = new FileOutputStream(PATH_TO_INDEXFILE,true);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bos.write(block);
            updateMetaDataBlock(PATH_TO_INDEXFILE);
        } catch (Exception e){
            e.printStackTrace();
        }
    }


    // Updates IndexFile block with the Node
    static void updateIndexFileBlock(Node node, int totalLevelsOfTreeIndex) {
        try {
            byte[] nodeInBytes = serialize(node);
            byte[] goodPutLengthInBytes = serialize(nodeInBytes.length);
            byte[] block = new byte[BLOCK_SIZE];
            System.arraycopy(goodPutLengthInBytes, 0, block, 0, goodPutLengthInBytes.length);
            System.arraycopy(nodeInBytes, 0, block, goodPutLengthInBytes.length, nodeInBytes.length);

            RandomAccessFile f = new RandomAccessFile(new File(PATH_TO_INDEXFILE), "rw");
            f.seek(node.getNodeBlockId()*BLOCK_SIZE); // this basically reads n bytes in the file
            f.write(block);
            f.close();

            if (node.getNodeBlockId() == RStarTree.getRootNodeBlockId() && FilesHandler.totalLevelsOfTreeIndex != totalLevelsOfTreeIndex)
                updateLevelsOfTreeIndexFile();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static Node readIndexFileBlock(long blockId) {
        try{
            RandomAccessFile raf = new RandomAccessFile(new File(PATH_TO_INDEXFILE), "rw");
            FileInputStream fis = new FileInputStream(raf.getFD());
            BufferedInputStream bis = new BufferedInputStream(fis);
            raf.seek(blockId*BLOCK_SIZE);
            byte[] block = new byte[BLOCK_SIZE];
            if(bis.read(block,0,BLOCK_SIZE) != BLOCK_SIZE){
                throw new Exception("Block size read was not " + BLOCK_SIZE + " bytes");
            }

            byte[] metaDataLengthInBytes = serialize(block.length);
            System.arraycopy(metaDataLengthInBytes, 0, block, 0, metaDataLengthInBytes.length);

            byte[] nodeInBytes = new byte[(Integer)deserialize(metaDataLengthInBytes)];
            System.arraycopy(block, metaDataLengthInBytes.length, nodeInBytes, 0, nodeInBytes.length);

            return (Node) deserialize(nodeInBytes);

        } catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }


}
