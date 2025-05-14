import java.util.ArrayList;
import java.util.List;

public class RangeQuery {

    /**
     * Executes a range query using the R*-Tree structure, starting from the given node.
     * Traverses the index file (indexfile.dat) to find relevant data blocks in the data file.
     */
    public static List<Record> rangeQuery(Node node, double[] minCoor, double[] maxCoor) {
        List<Record> results = new ArrayList<>();

        // Create the query MBR (bounding box)
        MBR queryBox = new MBR(getBoundsFromMinMax(minCoor, maxCoor));

        for (Entry entry : node.getEntries()) {
            // If entry's bounding box overlaps with the query box
            if (MBR.checkOverlap(entry.getBoundingBox(), queryBox)) {

                if (node.getNodeLevelInTree() == 0) {
                    // Leaf node: retrieve records from the associated data block
                    ArrayList<Record> records = FilesHandler.readDataFileBlock(entry.getChildNodeBlockId());
                    if (records != null) {
                        for (Record record : records) {
                            if (isRecordInRange(record, minCoor, maxCoor)) {
                                results.add(record);
                            }
                        }
                    }
                } else {
                    // Internal node: go deeper into the index file
                    Node childNode = FilesHandler.readIndexFileBlock(entry.getChildNodeBlockId());
                    if (childNode != null) {
                        results.addAll(rangeQuery(childNode, minCoor, maxCoor));
                    }
                }
            }
        }

        return results;
    }

    /**
     * Builds a list of Bounds from given min/max arrays for each dimension.
     */
    private static ArrayList<Bounds> getBoundsFromMinMax(double[] minCoor, double[] maxCoor) {
        ArrayList<Bounds> bounds = new ArrayList<>();
        for (int i = 0; i < minCoor.length; i++) {
            bounds.add(new Bounds(minCoor[i], maxCoor[i]));
        }
        return bounds;
    }

    /**
     * Checks whether a record lies within the query range.
     */
    private static boolean isRecordInRange(Record record, double[] minCoor, double[] maxCoor) {
        ArrayList<Double> coords = record.getCoordinates();
        for (int i = 0; i < coords.size(); i++) {
            if (coords.get(i) < minCoor[i] || coords.get(i) > maxCoor[i]) {
                return false;
            }
        }
        return true;
    }
    public static void main(String[] args) {
        // Καθορισμός εύρους αναζήτησης (ανάλογα με το datafile σου)
        double[] min = {23.0, 40.0};  // π.χ. lon min / lat min
        double[] max = {24.0, 41.0};  // lon max / lat max

        // Εκτέλεση σειριακής range query
        System.out.println("🔍 Εκτέλεση Linear Range Query (χωρίς index)...");
        long start = System.currentTimeMillis();
        List<Record> results = LinearRangeQuery.runLinearQuery(min, max);
        long end = System.currentTimeMillis();

        // Εκτύπωση αποτελεσμάτων
        System.out.println("✅ Βρέθηκαν " + results.size() + " εγγραφές:");
        for (Record r : results) {
            System.out.println(r);
        }

        System.out.println("⏱ Χρόνος εκτέλεσης: " + (end - start) + " ms");
    }
}



