
/**
 *
 *
 * Public class {@link RecordBlockPairID} that refers to a {@link Record} and it's corresponding {@code blockID} in which it is saved
 *
 *
 */


public class RecordBlockPairID {
    private Record record;
    private long blockID;


    /**
     * {@link RecordBlockPairID} constructor that takes a {@link Record} and its corresponding {@code blockID}
     *
     * @param record The {@link Record}
     * @param blockID The {@code blockID}
     */


    public RecordBlockPairID(Record record, long blockID) {
        this.record = record;
        this.blockID = blockID;
    }


    /**
     * Getter for the {@link Record}
     * @return The record
     */


    public Record getRecord() {
        return record;
    }


    /**
     * Getter for the {@code blockID}
     * @return The record's block id
     */


    public long getBlockID() {
        return blockID;
    }
}
