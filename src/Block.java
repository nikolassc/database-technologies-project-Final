import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Block {
    private int blockID;
    private List<Record> recordlist;

    // Constructor
    public Block(int blockID){
        this.blockID = blockID;
        recordlist = new ArrayList<>();
    }

    // Getters
    public int getBlockID(){
        return blockID;
    }

    public List<Record> getRecordlist(){
        return recordlist;
    }

    // Add a record to the block
    public void addRecord(Record rec){
        recordlist.add(rec);
    }

    // Get a record by index
    public Record getRecord(int i){
        return recordlist.get(i);
    }

    // The number of records in a Block
    public int recordCount(){
        return recordlist.size();
    }

    // Convert Blocks to Bytes
    public byte[] toBytes() throws IOException{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(blockID); // writes the blockId
        dos.writeInt(recordlist.size()); // writes the list size

        for(Record rec : recordlist){
            dos.writeLong(rec.getRecordID()); // writes the recordId
            dos.writeUTF(rec.getName()); // writes the record name

            ArrayList<Double> coords = rec.getCoordinates();
            dos.writeInt(coords.size());
            for(double c : coords){
                dos.writeDouble(c); // writes the coordinates for any dimension
            }
        }

        dos.flush();
        byte[] result  = baos.toByteArray();

        if(result.length > 32*1024){ // 32KB
            throw new IOException("Block size exceeds the limit");
        }
        return result;
    }

    // Convert Bytes to Blocks
    public static Block fromBytes(byte[] bytes) throws IOException{
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        DataInputStream dis = new DataInputStream(bais);

        int blockId = dis.readInt();
        Block block = new Block(blockId);
        int numRecords = dis.readInt();

        for(int i=0; i<numRecords; i++){
            long id = dis.readLong();
            String name = dis.readUTF();

            int dim = dis.readInt();
            ArrayList<Double> coords = new ArrayList<>(dim);
            while(dis.available()>0){
                coords.add(dis.readDouble());
            }

            block.addRecord(new Record(id, name, coords));
        }
        return block;
    }
}
