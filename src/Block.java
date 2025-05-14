import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Block {
    private int blockID;
    private List<Records> recordlist;

    // Constructor
    public Block(int blockID){
        this.blockID = blockID;
        recordlist = new ArrayList<>();
    }

    // Getters
    public int getBlockID(){
        return blockID;
    }

    public List<Records> getRecordlist(){
        return recordlist;
    }

    // Add a record to the block
    public void addRecord(Records rec){
        recordlist.add(rec);
    }

    // Get a record by index
    public Records getRecord(int i){
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

        for(Records rec : recordlist){
            dos.writeUTF(rec.getRecordID()); // writes the recordId
            dos.writeUTF(rec.getName()); // writes the record name

            double[] coords = rec.getCoordinates();
            dos.writeInt(coords.length);
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
            String id = dis.readUTF();
            String name = dis.readUTF();

            int dim = dis.readInt();
            double[] coords = new double[dim];
            for(int j=0; j<dim; j++){
                coords[j] = dis.readDouble();
            }

            block.addRecord(new Records(id, name, coords));
        }
        return block;
    }
}
