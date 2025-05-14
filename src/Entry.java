import java.io.Serializable;
import java.util.ArrayList;

// An Entry refers to the address of a lower Node (child) in the RStarTree and to it's BoundingBox (it's covering rectangle),
// which covers all the bounding boxes in the lower Node's Entries
class Entry implements Serializable {
    private MBR MBR; // The closed bounded intervals describing the extent of the object along each dimension
    private Long childNodeBlockId; // The address (block ID) of a lower Node (child) in the RStarTree

    // Constructor which takes parameters the lower Node which represents the child node of the entry
    Entry(Node childNode) {
        this.childNodeBlockId = childNode.getNodeBlockId();
        adjustBBToFitEntries(childNode.getEntries()); // Adjusting the BoundingBox of the Entry to fit the objects of the childNode
    }

    // Constructor which takes parameters the lower Node which represents the child node of the entry
    Entry(MBR MBR)
    {
        this.MBR = MBR;
    }

    void setChildNodeBlockId(Long childNodeBlockId) {
        this.childNodeBlockId = childNodeBlockId;
    }

    MBR getBoundingBox() {
        return MBR;
    }

    Long getChildNodeBlockId() {
        return childNodeBlockId;
    }

    // Adjusting the Bouncing Box of the entry by assigning a new bounding box to it with the new minimum bounds
    // based on the ArrayList parameter entries
    void adjustBBToFitEntries(ArrayList<Entry> entries){
        MBR = new MBR(Bounds.findMinimumBounds(entries));
    }

    // Adjusting the Bouncing Box of the entry by assigning a new bounding box to it with the extended minimum bounds
    // that also enclose the given Entry parameter entryToInclude
    void adjustBBToFitEntry(Entry entryToInclude){
        MBR = new MBR(Bounds.findMinimumBounds(MBR,entryToInclude.getBoundingBox()));
    }
}
