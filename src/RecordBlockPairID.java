public class RecordBlockPairID {
    private Record record;
    private long blockID;

    public RecordBlockPairID(Record record, long blockID) {
        this.record = record;
        this.blockID = blockID;
    }

    public Record getRecord() {
        return record;
    }

    public long getBlockID() {
        return blockID;
    }
}
