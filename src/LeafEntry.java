import java.util.ArrayList;

// Represents the entries on the bottom of the RStarTree
// Extends the Entry Class where it's BoundingBox
// is the bounding box of the spatial object (the record) indexed
// also holds the recordId of the record and a pointer of the block which the record is saved in the datafile
public class LeafEntry extends Entry {
    public LeafEntry(long datafileBlockId, MBR mbr) {
        super(mbr);  // sets bounding box
        this.setChildNodeBlockId(datafileBlockId);  // sets block pointer
    }
}
