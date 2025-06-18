import java.io.Serializable;
import java.util.ArrayList;


/**
 *
 * Public class {@link Entry} that points to the address of a child {@link Node} in the R*Tree and to its minimum bounding rectangle that covers
 * all the child node's entries
 *
 */


class Entry implements Serializable {
    private MBR MBR; // The Minimum Bounding Rectangle that closes all the Objects in each dimension
    private Long childNodeBlockId; // The Block Id of a child Node in indexFile
    private int childNodeIndexInBlock; // The Index in the Block of a child Node in the IndexFile


    /**
     * Constructor with {@code childNode} as parameter. The {@link MBR} is adjusted to fit the new child node
     *
     * @param childNode The lower {@link Node} that the {@link Entry} points to.
     */


    Entry(Node childNode) {
        this.childNodeBlockId = childNode.getNodeBlockId();
        this.childNodeIndexInBlock = childNode.getNodeIndexInBlock();
        adjustMBRToFitEntries(childNode.getEntries());
    }


    /**
     * Constructor with the {@link Entry}'s {@link MBR} as parameter.
     *
     * @param MBR The entry's {@link MBR}.
     */


    Entry(MBR MBR)
    {
        this.MBR = MBR;
    }


    /**
     * Getter for the {@link Entry}'s {@link MBR}
     *
     * @return The {@link Entry}'s {@link MBR}
     */


    MBR getMBR() {
        return MBR;
    }


    /**
     * Getter for the address of the child {@link Node}'s {@code blockId}.
     *
     * @return The child node's {@code blockId}
     */


    Long getChildNodeBlockId() {
        return childNodeBlockId;
    }


    /**
     * Getter for the child's {@code nodeIndex} in its index file block
     *
      * @return The child's {@code nodeIndex}
     */


    int getChildNodeIndexInBlock() { return childNodeIndexInBlock; }


    /**
     * Setter for the child's {@code nodeIndex} in its index file block
     *
     * @param nodeIndex The {@code nodeIndex} to set.
     */


    void setChildNodeIndexInBlock(int nodeIndex) { this.childNodeIndexInBlock = nodeIndex; }


    /**
     * {@code adjustMBRToFitEntries} method adjusts the {@link MBR} of the {@link Entry} by assigning a new {@link MBR} based on the new minimum {@link Bounds}
     * of the {@link ArrayList} parameter entries
     *
     * @param entries {@link ArrayList} of {@link Entry}
     */


    void adjustMBRToFitEntries(ArrayList<Entry> entries){
        MBR = new MBR(Bounds.findMinimumBounds(entries));
    }


    /**
     * {@code adjustMBRToFitEntry} method adjusts the {@link MBR} of the {@link Entry} by assigning a new {@link MBR} with the extended minimum {@link Bounds}
     * that contain the new given entry parameter
     *
     * @param entryToInclude The given {@link Entry} to contain in the {@link MBR}
     */


    void adjustMBRToFitEntry(Entry entryToInclude){
        MBR = new MBR(Bounds.findMinimumBounds(MBR,entryToInclude.getMBR()));
    }


    /**
     * {@code adjustMBRToFitMBR} method adjusts the {@link MBR} of the {@link Entry} by assinging a new {@link MBR} with the combined minimum {@link Bounds} of
     * the given parameter mbr
     *
     * @param otherMBR The parameter {@link MBR} to be combined.
     */


    void adjustMBRToFitMBR(MBR otherMBR) {
        if (this.MBR == null) {
            this.MBR = new MBR(otherMBR.getBounds());
        } else {
            ArrayList<Bounds> combinedBounds = Bounds.findMinimumBounds(this.MBR, otherMBR);
            this.MBR = new MBR(combinedBounds);
        }
    }
}
