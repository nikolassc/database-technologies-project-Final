import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class Node implements Serializable{
    private static final int MAX_ENTRIES = FilesHandler.calculateMaxEntriesInNode(); // Max entries that fit in a Node
    private static final int MIN_ENTRIES = (int)(0.5 * MAX_ENTRIES); // Every node fills to 50%
    private int nodeLevelInTree; // The level in tree that the nods is
    private long nodeBlockId; // The id of the file block that contains this node
    private ArrayList<Entry> entries; // The entries in the Node

    Node(int level) {
        nodeLevelInTree = level;
        this.entries = new ArrayList<>();
        this.nodeBlockId = RStarTree.getRootNodeBlockId();
    }

    Node(int level, ArrayList<Entry> entries) {
        nodeLevelInTree = level;
        this.entries = entries;
    }

    void setNodeBlockId(long nodeBlockId) {
        this.nodeBlockId = nodeBlockId;
    }

    void setEntries(ArrayList<Entry> entries) {
        this.entries = entries;
    }

    static int getMaxEntriesInNode() {
        return MAX_ENTRIES;
    }

    long getNodeBlockId() {
        return nodeBlockId;
    }

    int getNodeLevelInTree() {
        return nodeLevelInTree;
    }

    ArrayList<Entry> getEntries() {
        return entries;
    }

    // Inserts entry to node
    void insertEntry(Entry entry) {
        entries.add(entry);
    }

    ArrayList<Node> splitNode() {
        ArrayList<Distribution> splitDistributions = chooseSplitNodes();
        return chooseSplitIndex(splitDistributions);
    }

    private ArrayList<Distribution> chooseSplitNodes() {
        ArrayList<Distribution> bestDistributions = new ArrayList<>();
        double minMarginSum = Double.MAX_VALUE;

        for (int dimension = 0; dimension < FilesHandler.getDataDimensions(); dimension++) {
            ArrayList<ArrayList<Entry>> sortedLists = getSortedEntries(entries, dimension);
            double totalMarginSum = 0;
            ArrayList<Distribution> currentDistributions = new ArrayList<>();

            for (ArrayList<Entry> sortedEntries : sortedLists) {
                ArrayList<Distribution> distributions = computeDistributions(sortedEntries);
                for (Distribution dist : distributions) {
                    totalMarginSum += dist.getFirstGroup().getBoundingBox().getMargin();
                    totalMarginSum += dist.getSecondGroup().getBoundingBox().getMargin();
                }
                currentDistributions.addAll(distributions);
            }

            if (totalMarginSum < minMarginSum) {
                minMarginSum = totalMarginSum;
                bestDistributions = currentDistributions;
            }
        }

        return bestDistributions;
    }

    private ArrayList<ArrayList<Entry>> getSortedEntries(ArrayList<Entry> originalEntries, int dimension) {
        ArrayList<Entry> byLower = new ArrayList<>(originalEntries);
        ArrayList<Entry> byUpper = new ArrayList<>(originalEntries);

        byLower.sort(new EntryComparator.EntryBoundComparator(byLower, dimension, false));
        byUpper.sort(new EntryComparator.EntryBoundComparator(byUpper, dimension, true));

        ArrayList<ArrayList<Entry>> result = new ArrayList<>();
        result.add(byLower);
        result.add(byUpper);
        return result;
    }

    private ArrayList<Distribution> computeDistributions(ArrayList<Entry> sortedEntries) {
        ArrayList<Distribution> distributions = new ArrayList<>();
        int total = sortedEntries.size();

        int maxK = MAX_ENTRIES - 2 * MIN_ENTRIES + 2;
        for (int k = 1; k <= maxK; k++) {
            int splitIndex = (MIN_ENTRIES - 1) + k;
            if (splitIndex >= total) break; // prevent out-of-bounds

            List<Entry> group1 = sortedEntries.subList(0, splitIndex);
            List<Entry> group2 = sortedEntries.subList(splitIndex, total);

            BoundingBox bb1 = new BoundingBox(Bounds.findMinimumBounds((ArrayList<Entry>) group1));
            BoundingBox bb2 = new BoundingBox(Bounds.findMinimumBounds((ArrayList<Entry>) group2));

            distributions.add(new Distribution(
                    new DistributionGroup(new ArrayList<>(group1), bb1),
                    new DistributionGroup(new ArrayList<>(group2), bb2)
            ));
        }
        return distributions;
    }

    private ArrayList<Node> chooseSplitIndex(ArrayList<Distribution> splitAxisDistributions) {
        if (splitAxisDistributions.isEmpty()) {
            throw new IllegalArgumentException("No distributions provided for split.");
        }

        double minOverlap = Double.MAX_VALUE;
        double minArea = Double.MAX_VALUE;
        int bestIndex = -1;

        for (int i = 0; i < splitAxisDistributions.size(); i++) {
            Distribution dist = splitAxisDistributions.get(i);
            BoundingBox bb1 = dist.getFirstGroup().getBoundingBox();
            BoundingBox bb2 = dist.getSecondGroup().getBoundingBox();

            double overlap = BoundingBox.calculateOverlapValue(bb1, bb2);
            double areaSum = bb1.getArea() + bb2.getArea();

            if (overlap < minOverlap || (overlap == minOverlap && areaSum < minArea)) {
                minOverlap = overlap;
                minArea = areaSum;
                bestIndex = i;
            }
        }

        Distribution best = splitAxisDistributions.get(bestIndex);
        ArrayList<Node> result = new ArrayList<>();
        result.add(new Node(nodeLevelInTree, best.getFirstGroup().getEntries()));
        result.add(new Node(nodeLevelInTree, best.getSecondGroup().getEntries()));
        return result;
    }



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

class DistributionGroup {
    private ArrayList<Entry> entries;
    private BoundingBox boundingBox;

    DistributionGroup(ArrayList<Entry> entries, BoundingBox boundingBox) {
        this.entries = entries;
        this.boundingBox = boundingBox;
    }

    ArrayList<Entry> getEntries() {
        return entries;
    }

    BoundingBox getBoundingBox(){
        return boundingBox;
    }
}