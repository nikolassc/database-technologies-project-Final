import java.util.ArrayList;
import java.util.List;

public class RangeQuery {

    public static List<Record> rangeQuery(Node node, double[] minCoor, double[] maxCoor) {
        List<Record> results = new ArrayList<>();

        // Creats BoundixBox for the query
        BoundingBox queryBox = new BoundingBox(getBoundsFromMinMax(minCoor, maxCoor));

        for (Entry entry : node.getEntries()) {
            if (BoundingBox.checkOverlap(entry.getBoundingBox(), queryBox)) {
                if (node.getNodeLevelInTree() == 0) {
                    // If we are on a leaf, read the records from the data block
                    ArrayList<Record> records = FilesHandler.readDataFileBlock(entry.getChildNodeBlockId());
                    if (records != null) {
                        for (Record record : records) {
                            if (isRecordInRange(record, minCoor, maxCoor)) {
                                results.add(record);
                            }
                        }
                    }
                } else {
                    // We are o a node, we go to the child node
                    Node childNode = FilesHandler.readIndexFileBlock(entry.getChildNodeBlockId());
                    if (childNode != null) {
                        results.addAll(rangeQuery(childNode, minCoor, maxCoor));
                    }
                }
            }
        }

        return results;
    }

    // Creates BoundingBox for the query range
    private static ArrayList<Bounds> getBoundsFromMinMax(double[] minCoor, double[] maxCoor) {
        ArrayList<Bounds> bounds = new ArrayList<>();
        for (int i = 0; i < minCoor.length; i++) {
            bounds.add(new Bounds(minCoor[i], maxCoor[i]));
        }
        return bounds;
    }

    // Checks if a Record is in boundaries
    private static boolean isRecordInRange(Record record, double[] minCoor, double[] maxCoor) {
        ArrayList<Double> coords = record.getCoordinates();
        for (int i = 0; i < coords.size(); i++) {
            if (coords.get(i) < minCoor[i] || coords.get(i) > maxCoor[i]) {
                return false;
            }
        }
        return true;
    }

}

