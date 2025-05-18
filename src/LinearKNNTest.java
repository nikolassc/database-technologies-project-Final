import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class LinearKNNTest {

    public static void main(String[] args) {
        FilesHandler.initializeDataFile(2, false);
        System.out.println("🔎 Executing Sequential Nearest Neighbours Query...");

        // Example query point (μπορεί να τροποποιηθεί από τον χρήστη)
        ArrayList<Double> queryPoint = new ArrayList<>(Arrays.asList(40.378, 23.006));
        int k = 20;

        // Εκκίνηση χρονομέτρησης
        long start = System.nanoTime();

        // Εκτέλεση k-NN query
        LinearNearestNeighboursQuery query = new LinearNearestNeighboursQuery(queryPoint, k);
        ArrayList<Record> results = query.getNearestRecords();

        long end = System.nanoTime();
        double durationMs = (end - start) / 1_000_000.0;

        // Συλλογή όλων των records για αντιστοίχιση ID → Record
        Map<Long, Record> allRecordsById = new HashMap<>();
        for (int blockId = 1; blockId < FilesHandler.getTotalBlocksInDataFile(); blockId++) {
            ArrayList<Record> records = FilesHandler.readDataFileBlock(blockId);
            if (records != null) {
                for (Record r : records) {
                    allRecordsById.put(r.getRecordID(), r);
                }
            }
        }

        // Εκτύπωση αποτελεσμάτων
        System.out.println("✅ k-NN Query completed in " + durationMs + " ms");
        System.out.println("📦 Number of neighbours found: " + results.size());
        System.out.println("📍 Nearest neighbour record IDs (sorted):");

        for(Record r : results){
            System.out.println(r.toString());
        }
    }
}
