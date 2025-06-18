import java.util.ArrayList;


/**
 * Public class {@link LeafEntry} extends {@link Entry} and represents the entries at the bottom of the {@link RStarTree}
 * <p> The {@link LeafEntry} {@link MBR} corresponds to the {@link MBR} of the spatial records that it points to in the datafile
 *
 */


public class LeafEntry extends Entry {
    private long datafileBlockId;


    /**
     * {@link LeafEntry} constructor with {@code datafileBlockId} and {@link MBR} as parameters.
     * @param datafileBlockId Data block id that the {@link LeafEntry} refers to
     * @param mbr The {@link LeafEntry}'s records' {@link MBR}
     */


    public LeafEntry(long datafileBlockId, MBR mbr) {
        super(mbr);  // sets bounding box
        this.datafileBlockId = datafileBlockId;  // sets block pointer
    }


    /**
     * Getter method for the {@code datafileBlockId} that is referenced.
     * @return The {@code datafileBlockId}.
     */

    public long getDataBlockId() {
        return datafileBlockId;
    }
}
