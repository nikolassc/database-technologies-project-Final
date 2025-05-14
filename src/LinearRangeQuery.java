
import  java.util.ArrayList;
import java.util.List;


public class LinearRangeQuery {

    //Checks if the coordinates are in between the min and max in any dimensions
    private static boolean inRange(ArrayList<Double> coords, double[] minCoor, double[] maxCoor){
        for(int i=0; i<coords.size(); i++){
            if(coords.get(i) < minCoor[i] || coords.get(i) > maxCoor[i]){
                return false;
            }
        }
        return true;
    }

    //Performs a linear scan of the entire data file
    public static List<Record> runLinearQuery(double[] minCoor, double[] maxCoor) {
        List<Record> results = new ArrayList<>();

        int totalBlocks = FilesHandler.getTotalBlocksInDataFile();
        for (int blockId = 1; blockId < totalBlocks; blockId++) {
            ArrayList<?> blockData = FilesHandler.readDataFileBlock(blockId);
            if (blockData == null) continue;

            for (Object obj : blockData) {
                if (!(obj instanceof Record)) {
                    System.out.println("⚠️ Προειδοποίηση: Μη έγκυρο αντικείμενο στο block " + blockId + ": " + obj.getClass().getName());
                    continue;
                }

                Record rec = (Record) obj;
                ArrayList<Double> coords = rec.getCoordinates();

                boolean inRange = true;
                for (int i = 0; i < coords.size(); i++) {
                    if (coords.get(i) < minCoor[i] || coords.get(i) > maxCoor[i]) {
                        inRange = false;
                        break;
                    }
                }

                if (inRange) {
                    results.add(rec);
                }
            }
        }

        return results;
    }


    public static void main(String[] args) {
        FilesHandler.initializeDataFile(2,false);
        // Καθορισμός εύρους αναζήτησης (ανάλογα με το datafile σου)
        double[] min = {0.0, 0.0};  // π.χ. lon min / lat min
        double[] max = {100.0, 100.0};  // lon max / lat max

        // Εκτέλεση σειριακής range query
        System.out.println("🔍 Εκτέλεση Linear Range Query (χωρίς index)...");
        long start = System.currentTimeMillis();
        List<Record> results = LinearRangeQuery.runLinearQuery(min, max);
        long end = System.currentTimeMillis();

        // Εκτύπωση αποτελεσμάτων
        System.out.println("✅ Βρέθηκαν " + results.size() + " εγγραφές:");
        for (Record r : results) {
            System.out.println(" - ID: " + r.getRecordID() +
                    ", Name: " + r.getName() +
                    ", Coords: " + r.getCoordinates());
        }

        System.out.println("⏱ Χρόνος εκτέλεσης: " + (end - start) + " ms");
    }
}

