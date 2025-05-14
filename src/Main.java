import java.io.*;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Block block = null; // ➊ Δηλώνεται εκτός try, ώστε να είναι ορατή παντού

        try {
            // Create Records
            Records cafe = new Records("1", "Cafe Central", new double[]{10.0, 20.0, 5.0});
            Records museum = new Records("2", "Acropolis Museum", new double[]{11.0, 21.0, 5.5});
            Records university = new Records("3", "Aristotle University", new double[]{15.0, 25.0, 10.0});

            // 2. Create blocks and insert data
            block = new Block(1);
            block.addRecord(cafe);
            block.addRecord(museum);
            block.addRecord(university);

            // 3. Convert to bytes
            byte[] bytes = block.toBytes();
            System.out.println("Serialized block to bytes: " + bytes.length + " bytes");

            // 4. Read from bytes
            Block loadedBlock = Block.fromBytes(bytes);

            // 5. Print the records
            System.out.println("Deserialized Block:");
            System.out.println("Block ID: " + loadedBlock.getBlockID());
            System.out.println("Number of records: " + loadedBlock.recordCount());

            for (int i = 0; i < loadedBlock.recordCount(); i++) {
                System.out.println("Record " + i + ": " + loadedBlock.getRecord(i));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Range Query
        if (block != null) {
            double[] min = {9.5, 19.5, 4.5};
            double[] max = {11.5, 21.5, 6.0};

            List<Records> result = LinearRangeQuery.runLinearQuery(block, min, max);

            System.out.println("Range query found " + result.size() + " results:");
            for (Records rec : result) {
                System.out.println(rec);
            }
        } else {
            System.out.println("Block was not initialized.");
        }
    }
}

