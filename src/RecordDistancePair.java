

/**
 * {@code RecordDistancePair} is a utility class representing a pair of a {@link Record} object
 * and its corresponding distance from a query point.
 *
 * <p>This class is primarily used in nearest-neighbor search algorithms such as
 * {@link NearestNeighboursQuery} or {@link LinearNearestNeighboursQuery} to associate records
 * with their computed distance and facilitate efficient sorting and comparison.</p>
 *
 */


public class RecordDistancePair {
    private final Record record;
    private final double distance;


    /**
     * Constructs a new {@code RecordDistancePair} with the given {@code record} and its corresponding distance.
     *
     * @param record The {@link Record} object.
     * @param distance The computed distance of the {@code record} from a query point.
     */


    public RecordDistancePair(Record record, double distance) {
        this.record = record;
        this.distance = distance;
    }


    /**
     * Getter for the {@link Record}
     * @return The record object
     */


    public Record getRecord() {
        return record;
    }


    /**
     * Getter for the {@code distance} associated with the {@link Record}
     * @return The distance
     */


    public double getDistance() {
        return distance;
    }
}