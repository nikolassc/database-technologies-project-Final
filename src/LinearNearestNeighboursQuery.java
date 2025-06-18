import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;

// Class used for executing a k-nearest neighbours query of a specific search point without any use of an index
// Finds the k closest records of that search point
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;


/**
 * Executes a linear (brute-force) k-nearest neighbours query over the data stored in the datafile,
 * without using any index structure (like an R*-Tree). It scans all records and finds the k closest records
 * to a specified search point based on Euclidean distance.
 */


class LinearNearestNeighboursQuery {
    /** The query point for which nearest neighbours are searched. */
    private ArrayList<Double> searchPoint;

    /** The number of nearest neighbours to find. */
    private int k;

    /** Max-heap (PriorityQueue) to store the current best k neighbours. */
    private PriorityQueue<RecordDistancePair> nearestNeighbours;


    /**
     * Constructs a {@link LinearNearestNeighboursQuery} with the specified search point and number of neighbours ({@code k}).
     *
     * @param searchPoint The coordinates of the query point.
     * @param k The number of nearest neighbours to retrieve. Must be a positive integer.
     * @throws IllegalArgumentException If {@code k} is negative.
     */


    LinearNearestNeighboursQuery(ArrayList<Double> searchPoint, int k) {
        if (k < 0)
            throw new IllegalArgumentException("Parameter 'k' for the nearest neighbours must be a positive integer.");
        this.searchPoint = searchPoint;
        this.k = k;
        this.nearestNeighbours = new PriorityQueue<>(k, (a, b) -> Double.compare(b.getDistance(), a.getDistance()));
    }


    /**
     * Returns the {@link ArrayList} of the {@code k} nearest records to the {@code searchPoint}, ordered from closest to farthest.
     *
     * @return {@link ArrayList} of the {@code k} nearest records.
     */


    ArrayList<Record> getNearestRecords() {
        findNeighbours();
        ArrayList<Record> result = new ArrayList<>();
        while (!nearestNeighbours.isEmpty()) {
            result.add(nearestNeighbours.poll().getRecord());
        }
        Collections.reverse(result);
        return result;
    }


    /**
     * Finds the {@code k} nearest neighbours by scanning all records in the data file sequentially.
     * <p>
     * Uses a max-heap ({@link PriorityQueue}) to maintain the {@code k} closest records seen so far.
     */


    private void findNeighbours() {
        int totalBlocks = FilesHandler.getTotalBlocksInDataFile();
        for (int blockId = 1; blockId < totalBlocks; blockId++) {
            ArrayList<Record> recordsInBlock = FilesHandler.readDataFileBlock(blockId);
            if (recordsInBlock == null) continue;

            for (Record record : recordsInBlock) {
                double distance = calculateEuclideanDistance(record.getCoordinates(), searchPoint);

                if (nearestNeighbours.size() < k) {
                    nearestNeighbours.add(new RecordDistancePair(record, distance));
                } else if (distance < nearestNeighbours.peek().getDistance()) {
                    nearestNeighbours.poll();
                    nearestNeighbours.add(new RecordDistancePair(record, distance));
                }
            }
        }
    }


    /**
     * Calculates the Euclidean distance between two points in a n-dimensional space.
     *
     * @param a The first point.
     * @param b The second point.
     * @return The Euclidean distance between a and b.
     */


    private double calculateEuclideanDistance(ArrayList<Double> a, ArrayList<Double> b) {
        double sum = 0;
        for (int i = 0; i < a.size(); i++) {
            double diff = a.get(i) - b.get(i);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }
}
