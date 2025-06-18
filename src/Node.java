import java.io.Serializable;
import java.util.ArrayList;


/**
 *
 *
 * Public class {@link Node} representing an internal or leaf node of the {@link RStarTree}.
 *
 * <p>Each internal node except the root can have
 * up to M children and have to have at least m = M/2 children.
 *
 * <p>Each leaf node can have up to M {@link LeafEntry} and have to have at least m = M/2 {@link LeafEntry}.
 *
 * <p>Multiple nodes can fit in a single {@link IndexBlock}, which is implemented by keeping each
 * the {@code blockId} that the {@link Node} is saved into, and its {@code nodeIndex} inside the {@link IndexBlock}, aka the position
 * in the Block.
 *
 * <p>In this class, there is also the implementation of the {@link #chooseSplitAxis} and {@link #chooseSplitIndex} algorithms
 * described in the R*Tree paper.
 *
 *
 */


class Node implements Serializable {
    private static final int MAX_ENTRIES = 4; // The maximum entries that a Node can fit based on the file parameters
    private static final int MIN_ENTRIES = (int)(0.5 * MAX_ENTRIES); // Setting m to 50%
    private int level; // The level of the tree that this Node is located
    private long blockId = -1; // The unique ID of the file block that this Node refers to
    private ArrayList<Entry> entries; // The ArrayList with the Entries of the Node
    private int nodeIndexInBlock = -1;


    /**
     * Root constructor, which takes the root level as parameter, and sets the {@link Node} into {@code blockId} = 1 and {@code nodeIndex} = 0,
     * which is always the toot
     *
     * @param level Always 1 when called with {@code ROOT_LEVEL}
     */


    Node(int level) {
        this.level = level;
        this.entries = new ArrayList<>();
        this.blockId = RStarTree.getRootNodeBlockId();
        this.nodeIndexInBlock = 0;
    }


    /**
     * Normal {@link Node} constructor with level and {@link ArrayList} of {@link Entry}as parameters. Entries can be either pointers to other Nodes or
     * leaf entries, which point to blocks in the datafile.
     *
     * @param level The {@link Node}'s level in the tree.
     * @param entries {@link ArrayList} of {@link Entry}.
     */


    Node(int level, ArrayList<Entry> entries) {
        this.level = level;
        this.entries = entries;
    }


    /**
     * Setter for {@code NodeBlockId}, aka the id in the {@link IndexBlock} that the node is saved in.
     *
     * @param blockId The {@link IndexBlock} id
     */


    void setNodeBlockId(long blockId) {
        this.blockId = blockId;
    }


    /**
     * Setter for the {@link Node}'s entries.
     *
     * @param entries {@link ArrayList} of {@link Entry} to be set.
     */


    void setEntries(ArrayList<Entry> entries) {
        this.entries = entries;
    }


    /**
     * Getter for M.
     *
     * @return {@code MAX_ENTRIES}
     */


    static int getMaxEntriesInNode() {
        return MAX_ENTRIES;
    }


    /**
     * Getter for m = M/2.
     *
     * @return {@code MIN_ENTRIES}
     */

    static int getMinEntriesInNode() {return MIN_ENTRIES;}


    /**
     * Getter for the {@link Node}'s {@link IndexBlock} id
     *
     * @return The {@link IndexBlock} id
     */

    long getNodeBlockId() {
        return blockId;
    }


    /**
     * Getter for the {@link Node}'s level in the tree.
     *
     * @return The {@link Node}'s level.
     */


    int getNodeLevelInTree() {
        return level;
    }


    /**
     * Setter for the {@link Node}'s level in the tree
     *
     * @param level The {@link Node}'s level in the tree.
     */


    public void setNodeLevelInTree(int level) {
        this.level = level;
    }


    /**
     * Getter for the {@link Node}'s entries.
     *
     * @return {@link ArrayList} of {@link Entry}.
     */

    ArrayList<Entry> getEntries() {
        return entries;
    }


    /**
     * Setter for the {@code nodeIndex} in the {@link IndexBlock}
     *
     * @param index The {@code nodeIndex} in the {@link IndexBlock}.
     */


    public void setNodeIndexInBlock(int index) { this.nodeIndexInBlock = index; }


    /**
     * Getter for the {@code nodeIndex} in the {@link IndexBlock}
     *
     * @return The {@code nodeIndex} in the {@link IndexBlock}.
     */
    public int getNodeIndexInBlock() { return nodeIndexInBlock; }


    /**
     * Getter for the {@link Node}'s combined entries' {@link MBR}.
     *
     * @return The {@link MBR} of the entries' combined {@link Bounds}
     */

    public MBR getMBR() {
        if (entries == null || entries.isEmpty()) return null;

        ArrayList<Bounds> combinedBounds = new ArrayList<>();
        int dimensions = FilesHandler.getDataDimensions();

        MBR firstMBR = entries.get(0).getMBR();
        for (int d = 0; d < dimensions; d++) {
            double lower = firstMBR.getBounds().get(d).getLower();
            double upper = firstMBR.getBounds().get(d).getUpper();
            combinedBounds.add(new Bounds(lower, upper));
        }

        for (int i = 1; i < entries.size(); i++) {
            MBR current = entries.get(i).getMBR();
            for (int d = 0; d < dimensions; d++) {
                Bounds existing = combinedBounds.get(d);
                double lower = Math.min(existing.getLower(), current.getBounds().get(d).getLower());
                double upper = Math.max(existing.getUpper(), current.getBounds().get(d).getUpper());
                combinedBounds.set(d, new Bounds(lower, upper));
            }
        }

        return new MBR(combinedBounds);
    }


    /**
     * Adds given {@link Entry} in the {@link ArrayList} of entries.
     *
     * @param entry The {@link Entry} to be added
     */


    void insertEntry(Entry entry)
    {
        entries.add(entry);
    }


    /**
     * Splits the {@link Node} into two nodes.
     *
     * @return {@link ArrayList} of the split nodes.
     */


    ArrayList<Node> splitNode() {
        ArrayList<Distribution> splitAxisDistributions = chooseSplitAxis();
        return chooseSplitIndex(splitAxisDistributions);
    }


    /**
     *
     * {@code chooseSplitAxis }method returns the distribution of the best axis based on the R*Tree paper algorithm.
     * <p>
     * For each axis it sorts the entries by the lower and then by the upper value of their rectangles and
     * determines all the distributions by the sum of all margin-values of the different distributions
     *
     * @return The {@code bestAxisDistributions}
     *
     */


    private ArrayList<Distribution> chooseSplitAxis() {
        ArrayList<Distribution> bestAxisDistributions = new ArrayList<>();
        double minTotalMarginSum = Double.MAX_VALUE;
        for (int d = 0; d < FilesHandler.getDataDimensions(); d++)
        {
            ArrayList<Entry> entriesSortedByUpper = new ArrayList<>();
            ArrayList<Entry> entriesSortedByLower = new ArrayList<>();

            for (Entry entry : entries)
            {
                entriesSortedByLower.add(entry);
                entriesSortedByUpper.add(entry);
            }

            entriesSortedByLower.sort(new EntryComparator.EntryBoundByAxisComparator(entriesSortedByLower,d,false));
            entriesSortedByUpper.sort(new EntryComparator.EntryBoundByAxisComparator(entriesSortedByUpper,d,true));

            ArrayList<ArrayList<Entry>> sortedEntries = new ArrayList<>();
            sortedEntries.add(entriesSortedByLower);
            sortedEntries.add(entriesSortedByUpper);

            double currentAxisMarginSum = 0; // S for Current Axis
            ArrayList<Distribution>  axisDistributions = new ArrayList<>();
            for (ArrayList<Entry> sortedEntryList: sortedEntries)
            {
                for (int k = 1; k <= MAX_ENTRIES - 2* MIN_ENTRIES +2; k++)
                {
                    ArrayList<Entry> firstGroup = new ArrayList<>();
                    ArrayList<Entry> secondGroup = new ArrayList<>();
                    for (int j = 0; j < (MIN_ENTRIES -1)+k; j++)
                        firstGroup.add(sortedEntryList.get(j));
                    for (int j = (MIN_ENTRIES -1)+k; j < entries.size(); j++)
                        secondGroup.add(sortedEntryList.get(j));

                    MBR bbFirstGroup = new MBR(Bounds.findMinimumBounds(firstGroup));
                    MBR bbSecondGroup = new MBR(Bounds.findMinimumBounds(secondGroup));

                    Distribution distribution = new Distribution(new DistributionGroup(firstGroup,bbFirstGroup), new DistributionGroup(secondGroup,bbSecondGroup));
                    axisDistributions.add(distribution);
                    currentAxisMarginSum += bbFirstGroup.getMargin() + bbSecondGroup.getMargin();
                }

                if (minTotalMarginSum > currentAxisMarginSum)
                {
                    minTotalMarginSum = currentAxisMarginSum;
                    bestAxisDistributions = axisDistributions;
                }
            }
        }
        return bestAxisDistributions;
    }


    /**
     * {@code ChooseSplitIndex} method as described in the R*Tree paper.
     * After choosing the best split axis with {@link #chooseSplitAxis}, choose the {@link Distribution}
     * with the minimum overlap-value and Rrsolve ties by choosing
     * the {@link Distribution} with the minimum area-value
     *
     * @param splitAxisDistributions The candidate distributions along the chosen axis
     * @return The resulting split nodes
     *
     */


    private ArrayList<Node> chooseSplitIndex(ArrayList<Distribution> splitAxisDistributions) {

        if (splitAxisDistributions.isEmpty())
            throw new IllegalArgumentException("Wrong distributions group size. Given 0");

        double minOverlapValue = Double.MAX_VALUE;
        double minAreaValue = Double.MAX_VALUE;
        int bestSplitIndex = 0;

        for (int i = 0; i < splitAxisDistributions.size(); i++)
        {
            DistributionGroup distributionFirstGroup = splitAxisDistributions.get(i).getFirstGroup();
            DistributionGroup distributionSecondGroup = splitAxisDistributions.get(i).getSecondGroup();

            double overlap = MBR.calculateOverlapValue(distributionFirstGroup.getMBR(), distributionSecondGroup.getMBR());
            if(minOverlapValue > overlap)
            {
                minOverlapValue = overlap;
                minAreaValue = distributionFirstGroup.getMBR().getArea() + distributionSecondGroup.getMBR().getArea();
                bestSplitIndex = i;
            }

            else if (minOverlapValue == overlap)
            {
                double area = distributionFirstGroup.getMBR().getArea() + distributionSecondGroup.getMBR().getArea() ;
                if(minAreaValue > area)
                {
                    minAreaValue = area;
                    bestSplitIndex = i;
                }
            }
        }
        ArrayList<Node> resultingSplitNodes = new ArrayList<>();
        DistributionGroup firstGroup = splitAxisDistributions.get(bestSplitIndex).getFirstGroup();
        DistributionGroup secondGroup = splitAxisDistributions.get(bestSplitIndex).getSecondGroup();
        resultingSplitNodes.add(new Node(level,firstGroup.getEntries()));
        resultingSplitNodes.add(new Node(level,secondGroup.getEntries()));
        return resultingSplitNodes;
    }

}


/**
 *
 *
 * Helper class {@link Distribution} that keeps two split {@link DistributionGroup} objects.
 *
 *
 */


class Distribution {
    private DistributionGroup firstGroup;
    private DistributionGroup secondGroup;

    Distribution(DistributionGroup firstGroup, DistributionGroup secondGroup) {
        this.firstGroup = firstGroup;
        this.secondGroup = secondGroup;
    }

    DistributionGroup getFirstGroup(){
        return firstGroup;
    }

    DistributionGroup getSecondGroup(){
        return secondGroup;
    }
}


/**
 *
 *
 * {@link DistributionGroup} helper class that keeps an {@link ArrayList} of the group's entries and their {@link MBR}.
 *
 *
 */

class DistributionGroup {
    private ArrayList<Entry> entries;
    private MBR MBR;

    DistributionGroup(ArrayList<Entry> entries, MBR MBR) {
        this.entries = entries;
        this.MBR = MBR;
    }

    ArrayList<Entry> getEntries() {
        return entries;
    }

    MBR getMBR(){
        return MBR;
    }
}