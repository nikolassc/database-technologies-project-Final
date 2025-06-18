import javax.management.Query;
import java.util.*;


/**
 * Class for executing a {@code k}-nearest neighbours query using the {@link RStarTree} structure.
 * <p>
 * Implements the branch-and-bound algorithm for efficiently finding the {@code k} closest records to a given search point.
 * The search point is compared against the Minimum Bounding Rectangles ({@link MBR}s) in the tree to prune unnecessary branches.
 * </p>
 * <p>
 * The nearest neighbours are maintained in a max-heap priority queue of size {@code k}, ensuring that the farthest among
 * the current neighbours is always at the top, ready for replacement when a closer point is found.
 * </p>
 */


class NearestNeighboursQuery extends Query {
    private ArrayList<Double> searchPoint;
    private double searchPointRadius;
    private int k;
    private PriorityQueue<RecordDistancePair> nearestNeighbours;


    /**
     * Constructs a nearest neighbours query for the given {@code searchPoint} and {@code k}.
     *
     * @param searchPoint The point for which the nearest neighbours are to be found.
     * @param k The number of nearest neighbours to retrieve.
     * @throws IllegalArgumentException if {@code k} is not positive.
     */


    NearestNeighboursQuery(ArrayList<Double> searchPoint, int k) {
        if (k < 0)
            throw new IllegalArgumentException("Parameter 'k' for the nearest neighbours must be a positive integer.");
        this.searchPoint = searchPoint;
        this.k = k;
        this.searchPointRadius = Double.MAX_VALUE;
        this.nearestNeighbours = new PriorityQueue<>(k, (recordDistancePairA, recordDistancePairB) -> Double.compare(recordDistancePairB.getDistance(), recordDistancePairA.getDistance()));
    }


    /**
     * Retrieves the {@code k} nearest records to the search point, starting from the given root {@link Node}.
     *
     * @param node The starting node for the search (usually the root of the {@link RStarTree}).
     * @return A list of {@link Record} objects representing the nearest neighbours, ordered by distance (nearest first).
     */


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


    /**
     * Static helper method to retrieve the {@code k} nearest neighbours for a {@code searchPoint},
     * starting from the root of the {@link RStarTree}.
     *
     * @param searchPoint The query point.
     * @param k The number of nearest neighbours to retrieve.
     * @return A list of {@link Record} objects representing the nearest neighbours.
     */


    static ArrayList<Record> getNearestNeighbours(ArrayList<Double> searchPoint, int k){
        NearestNeighboursQuery query = new NearestNeighboursQuery(searchPoint,k);
        return query.getQueryRecord(FilesHandler.readNode(RStarTree.getRootNodeBlockId(), 0));
    }


    /**
     * Performs the branch-and-bound nearest neighbour search by traversing the {@link RStarTree}.
     * <p>
     * At each step, the method compares the minimum possible distance from the query point to the MBR of entries
     * and prunes branches that cannot yield closer results than the current {@code k}-th neighbour.
     *
     * @param node The starting node for traversal.
     */


    private void findNeighbours(Node node) {
        PriorityQueue<NodeEntryPair> queue = new PriorityQueue<>(
                Comparator.comparingDouble(p -> p.entry.getMBR().findMinDistanceFromPoint(searchPoint))
        );

        for (Entry e : node.getEntries()) {
            queue.add(new NodeEntryPair(node, e));
        }

        while (!queue.isEmpty()) {
            NodeEntryPair pair = queue.poll();
            Entry entry = pair.entry;

            double minDistance = entry.getMBR().findMinDistanceFromPoint(searchPoint);

            if (nearestNeighbours.size() == k && minDistance >= searchPointRadius) continue;


            Node childNode = FilesHandler.readNode(entry.getChildNodeBlockId(), entry.getChildNodeIndexInBlock());
            if (childNode == null) continue;

            if (childNode.getNodeLevelInTree() == RStarTree.getLeafLevel()){
                ArrayList<Record> records = FilesHandler.readDataFileBlock(entry.getChildNodeBlockId());
                if (records != null){
                    for (Record record : records) {
                        double distance = calculateEuclideanDistance(record.getCoordinates(), searchPoint);
                        if (nearestNeighbours.size() < k){
                            nearestNeighbours.add(new RecordDistancePair(record, distance));
                        } else if (distance < nearestNeighbours.peek().getDistance()){
                            nearestNeighbours.poll();
                            nearestNeighbours.add(new RecordDistancePair(record, distance));
                            searchPointRadius = nearestNeighbours.peek().getDistance();
                        }

                        if (nearestNeighbours.size() == k) {
                            searchPointRadius = nearestNeighbours.peek().getDistance();
                        }
                    }
                }
            } else {
                for(Entry childEntry : childNode.getEntries()){
                    queue.add(new NodeEntryPair(childNode, childEntry));
                }
            }
        }

    }


    /**
     * Helper class to associate a {@link Node} and an {@link Entry} in the traversal queue.
     */


    private static class NodeEntryPair {
        Node node;
        Entry entry;
        NodeEntryPair(Node node, Entry entry) {
            this.node = node;
            this.entry = entry;
        }
    }


    /**
     * Calculates the Euclidean distance between two points in n-dimensional space.
     *
     * @param a The first point.
     * @param b The second point.
     * @return The Euclidean distance between {@code a} and {@code b}.
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



