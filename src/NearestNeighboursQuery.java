import javax.management.Query;
import java.util.*;


// Class used for executing a k-nearest neighbours query of a specific search point with the use of the RStarTree
// Finds the k closest records of that search point
class NearestNeighboursQuery extends Query {
    private ArrayList<Double> searchPoint; // The coordinates of point used for radius queries
    private double searchPointRadius; // The reference radius that is used as a bound
    private int k; // The number of nearest neighbours to be found
    private PriorityQueue<RecordDistancePair> nearestNeighbours; // Using a max heap for the nearest neighbours

    NearestNeighboursQuery(ArrayList<Double> searchPoint, int k) {
        if (k < 0)
            throw new IllegalArgumentException("Parameter 'k' for the nearest neighbours must be a positive integer.");
        this.searchPoint = searchPoint;
        this.k = k;
        this.searchPointRadius = Double.MAX_VALUE;
        this.nearestNeighbours = new PriorityQueue<>(k, (recordDistancePairA, recordDistancePairB) -> {
            return Double.compare(recordDistancePairB.getDistance(), recordDistancePairA.getDistance()); // In order to make a MAX heap
        });
    }

    // Returns the ids of the query's records
    ArrayList<Record> getQueryRecord(Node node) {
        ArrayList<Record> qualifyingRecord = new ArrayList<>();
        findNeighbours(node);
        while (nearestNeighbours.size() != 0)
        {
            RecordDistancePair recordDistancePair = nearestNeighbours.poll();
            qualifyingRecord.add(recordDistancePair.getRecord());
        }
        Collections.reverse(qualifyingRecord); // In order to return closest neighbours first instead of farthest
        return qualifyingRecord;
    }

    static ArrayList<Record> getNearestNeighbours(ArrayList<Double> searchPoint, int k){
        NearestNeighboursQuery query = new NearestNeighboursQuery(searchPoint,k);
        return query.getQueryRecord(FilesHandler.readIndexFileBlock(RStarTree.getRootNodeBlockId()));
    }


    // Finds the nearest neighbours by using a branch and bound algorithm
    // with the help of the RStarTree
    private void findNeighbours(Node node) {
        node.getEntries().sort(new EntryComparator.EntryDistanceFromCenterComparator(node.getEntries(), searchPoint));
        int i = 0;
        //if (node.getNodeLevelInTree() != RStarTree.getLeafLevel()) {
        //while (i < node.getEntries().size() && (nearestNeighbours.size() < k || node.getEntries().get(i).getBoundingBox().findMinDistanceFromPoint(searchPoint) <= searchPointRadius))
        while (node.getNodeLevelInTree() != RStarTree.getLeafLevel()) {
            findNeighbours(FilesHandler.readIndexFileBlock(node.getEntries().get(i).getChildNodeBlockId()));
            i++;
        }
        //}
        //else {
            while (i < node.getEntries().size() && (nearestNeighbours.size() < k || node.getEntries().get(i).getBoundingBox().findMinDistanceFromPoint(searchPoint) <= searchPointRadius))
            {
                LeafEntry leafEntry = (LeafEntry) node.getEntries().get(i);
                /*if (nearestNeighbours.size() >= k)
                    nearestNeighbours.poll();
                double minDistance = leafEntry.getBoundingBox().findMinDistanceFromPoint(searchPoint);
                nearestNeighbours.add(new RecordDistancePair(FilesHandler.readDataFileBlock(leafEntry.getDataBlockId()), minDistance));
                searchPointRadius = nearestNeighbours.peek().getDistance();
                i++;*/
                for (Record record : FilesHandler.readDataFileBlock(leafEntry.getDataBlockId())) {
                    double distance = calculateEuclideanDistance(record.getCoordinates(), searchPoint);

                    if (nearestNeighbours.size() < k) {
                        nearestNeighbours.add(new RecordDistancePair(record, distance));
                    } else if (distance < nearestNeighbours.peek().getDistance()) {
                        nearestNeighbours.poll();
                        nearestNeighbours.add(new RecordDistancePair(record, distance));
                    }
                }
            }
        //}
    }

    private double calculateEuclideanDistance(ArrayList<Double> a, ArrayList<Double> b) {
        double sum = 0;
        for (int i = 0; i < a.size(); i++) {
            double diff = a.get(i) - b.get(i);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }
}



// Class which is used to hold an id of a record it's distance from a specific item
class IdDistancePair {
    private long recordId; // The id of the record
    private double distanceFromItem; // The distance from an item

    IdDistancePair(long recordId, double distanceFromItem) {
        this.recordId = recordId;
        this.distanceFromItem = distanceFromItem;
    }

    long getRecordId() {
        return recordId;
    }


    double getDistanceFromItem() {
        return distanceFromItem;
    }
}