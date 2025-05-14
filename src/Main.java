import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Block block = null; // ➊ Δηλώνεται εκτός try, ώστε να είναι ορατή παντού

        try {
            // 1. Δημιουργία εγγραφών
            ArrayList<Double> cafeCoords = new ArrayList<>();
            ArrayList<Double> museumCoords = new ArrayList<>();
            cafeCoords.add(37.9838);
            cafeCoords.add(23.7275);
            museumCoords.add(37.9681);
            museumCoords.add(23.7286);
            Record cafe = new Record(1, "Cafe Central", cafeCoords);
            Record museum = new Record(2, "Acropolis Museum", museumCoords);

            // 2. Create blocks and insert data
            block = new Block(1);
            block.addRecord(cafe);
            block.addRecord(museum);

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

            List<Record> result = LinearRangeQuery.runLinearQuery(block, min, max);

            System.out.println("Range query found " + result.size() + " results:");
            for (Record rec : result) {
                System.out.println(rec);
            }
        } else {
            System.out.println("Block was not initialized.");
        }
    }
}
