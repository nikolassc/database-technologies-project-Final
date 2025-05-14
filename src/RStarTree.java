import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

public class RStarTree {

    private int totalLevels;
    private boolean[] levelsInserted;
    private static final int ROOT_NODE_BLOCK_ID = 1;
    private static final int LEAF_LEVEL = 1;
    private static final int CHOOSE_SUBTREE_LEVEL = 32;
    private static final int REINSERT_TREE_ENTRIES = (int) (0.5* Node.getMaxEntriesInNode());


    //Constructor

    RStarTree(boolean insertFromDataFile){
        this.totalLevels = FilesHandler.getTotalLevelsFile();
        if(insertFromDataFile) {
            FilesHandler.writeNewIndexFileBlock(new Node(1));

            for (int i = 1; i <= FilesHandler.getTotalBlocksInDataFile(); i++) {
                ArrayList<Record> records = FilesHandler.readDataFileBlock(i);
                if(records != null){
                    for (Record record : records) {
                        insertRecord(record,i);
                    }
                } else {
                    throw new IllegalStateException("Error reading records from datafile");
                }
            }
        }
    }

    Node getRootNode(){
        return FilesHandler.readIndexFileBlock(ROOT_NODE_BLOCK_ID);
    }

    static int getRootNodeBlockId(){
        return ROOT_NODE_BLOCK_ID;
    }

    static int getLeafLevel(){
        return LEAF_LEVEL;
    }

    private void insertRecord(Record record, long datafileBlockId){
        ArrayList<Bounds> boundsForDimensions = new ArrayList<>();

        for (int i = 0; i < FilesHandler.getDataDimensions(); i++) {
            boundsForDimensions.add(new Bounds(record.getCoordinateFromDimension(i), record.getCoordinateFromDimension(i)));
        }
        levelsInserted = new boolean[totalLevels];
        insert(null, null, new LeafEntry(record.getRecordID(), datafileBlockId, boundsForDimensions), LEAF_LEVEL);

    }

    private Entry insert(Node parentNode, Entry parentEntry, Entry dataEntry, int levelToAdd) {
        long nodeBlockId = (parentEntry == null) ? ROOT_NODE_BLOCK_ID : parentEntry.getChildNodeBlockId();

        if (parentEntry != null) {
            parentEntry.adjustBBToFitEntry(dataEntry);
            FilesHandler.updateIndexFileBlock(parentNode, totalLevels);
        }

        Node childNode = FilesHandler.readIndexFileBlock(nodeBlockId);
        if (childNode == null) {
            throw new IllegalStateException("The Node-block read from file is null");
        }

        // Case: Target level reached -> insert directly into this node
        if (childNode.getNodeLevelInTree() == levelToAdd) {
            childNode.insertEntry(dataEntry);
            FilesHandler.updateIndexFileBlock(childNode, totalLevels);
        }
        // Case: Recurse further down the tree
        else {
            Entry bestEntry = chooseSubTree(childNode, dataEntry.getBoundingBox(), levelToAdd);
            Entry newEntry = insert(childNode, bestEntry, dataEntry, levelToAdd);

            // Read updated child node again in case it was modified during recursion
            childNode = FilesHandler.readIndexFileBlock(nodeBlockId);
            if (childNode == null) {
                throw new IllegalStateException("The Node-block read from file is null after recursion");
            }

            if (newEntry != null) {
                childNode.insertEntry(newEntry);
            }

            FilesHandler.updateIndexFileBlock(childNode, totalLevels);

            // No further overflow, return nothing
            if (childNode.getEntries().size() <= Node.getMaxEntriesInNode()) {
                return null;
            }

            // Overflow treatment needed
            return overFlowTreatment(parentNode, parentEntry, childNode);
        }

        // Final overflow check (leaf case)
        if (childNode.getEntries().size() > Node.getMaxEntriesInNode()) {
            return overFlowTreatment(parentNode, parentEntry, childNode);
        }

        return null;
    }

    private Entry chooseSubTree(Node node, MBR MBRToAdd, int levelToAdd) {
        ArrayList<Entry> entries = node.getEntries();

        // Case 1: Child points to leaves
        if(node.getNodeLevelInTree() == levelToAdd + 1){
            if (Node.getMaxEntriesInNode() > (CHOOSE_SUBTREE_LEVEL * 2) / 3 && entries.size() >CHOOSE_SUBTREE_LEVEL) {
                ArrayList<Entry> topEntries = getTopAreaEnlargementEntries(entries, MBRToAdd, CHOOSE_SUBTREE_LEVEL);
                return Collections.min(topEntries, new EntryComparator.EntryOverlapEnlargementComparator(topEntries, MBRToAdd, entries));
            }
            return Collections.min(entries, new EntryComparator.EntryOverlapEnlargementComparator(entries, MBRToAdd, entries));
        }

        // Case 2: Internal node -- pick based only on area enlargement
        return getEntryWithMinAreaEnlargement(entries, MBRToAdd);

    }

    private Entry getEntryWithMinAreaEnlargement(ArrayList<Entry> entries, MBR mbr) {
        return Collections.min(
                entries.stream()
                        .map(e -> new EntryAreaEnlargementPair(e, computeAreaEnlargement(e, mbr)))
                        .toList(),
                EntryAreaEnlargementPair::compareTo
        ).getEntry();
    }

    private ArrayList<Entry> getTopAreaEnlargementEntries(ArrayList<Entry> entries, MBR MBRToAdd, int p) {
        return entries.stream()
                .map(e -> new EntryAreaEnlargementPair(e, computeAreaEnlargement(e, MBRToAdd)))
                .sorted()
                .limit(p)
                .map(EntryAreaEnlargementPair::getEntry)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private double computeAreaEnlargement(Entry entry, MBR toAdd) {
        MBR enlarged = new MBR(Bounds.findMinimumBounds(entry.getBoundingBox(), toAdd));
        return enlarged.getArea() - entry.getBoundingBox().getArea();
    }

    private Entry overFlowTreatment(Node parentNode, Entry parentEntry, Node childNode) {
        int levelIndex = childNode.getNodeLevelInTree() - 1;

        // Case 1: Reinsertion (only once per level during this insertion)
        if (childNode.getNodeBlockId() != ROOT_NODE_BLOCK_ID && !levelsInserted[levelIndex]) {
            levelsInserted[levelIndex] = true;
            reInsert(parentNode, parentEntry, childNode);
            return null;
        }

        // Case 2: Split the overflowing node
        ArrayList<Node> splitNodes = childNode.splitNode();
        if (splitNodes.size() != 2) {
            throw new IllegalStateException("Split must produce exactly two nodes.");
        }

        Node leftNode = splitNodes.get(0);
        Node rightNode = splitNodes.get(1);
        childNode.setEntries(leftNode.getEntries()); // update current node with left part

        if (childNode.getNodeBlockId() != ROOT_NODE_BLOCK_ID) {
            // Regular internal/leaf node split
            FilesHandler.updateIndexFileBlock(childNode, totalLevels);

            rightNode.setNodeBlockId(FilesHandler.getTotalBlocksInIndexFile());
            FilesHandler.writeNewIndexFileBlock(rightNode);

            parentEntry.adjustBBToFitEntries(childNode.getEntries());
            FilesHandler.updateIndexFileBlock(parentNode, totalLevels);

            return new Entry(rightNode);
        }

        // Case 3: Split occurred at root — create a new root
        childNode.setNodeBlockId(FilesHandler.getTotalBlocksInIndexFile());
        FilesHandler.writeNewIndexFileBlock(childNode);

        rightNode.setNodeBlockId(FilesHandler.getTotalBlocksInIndexFile());
        FilesHandler.writeNewIndexFileBlock(rightNode);

        ArrayList<Entry> newRootEntries = new ArrayList<>();
        newRootEntries.add(new Entry(childNode));
        newRootEntries.add(new Entry(rightNode));

        Node newRoot = new Node(++totalLevels, newRootEntries);
        newRoot.setNodeBlockId(ROOT_NODE_BLOCK_ID);
        FilesHandler.updateIndexFileBlock(newRoot, totalLevels);

        return null;
    }

    private void reInsert(Node parentNode, Entry parentEntry, Node childNode) {
        int totalEntries = childNode.getEntries().size();
        int expectedEntries = Node.getMaxEntriesInNode() + 1;

        if (totalEntries != expectedEntries) {
            throw new IllegalStateException("Reinsert requires exactly M+1 entries.");
        }

        // Sort entries by distance to the center of the parent entry's bounding box (close reinsertion)
        childNode.getEntries().sort(
                new EntryComparator.EntryDistanceFromCenterComparator(childNode.getEntries(), parentEntry.getBoundingBox())
        );

        // Separate entries to remove
        int start = totalEntries - REINSERT_TREE_ENTRIES;
        ArrayList<Entry> removedEntries = new ArrayList<>(childNode.getEntries().subList(start, totalEntries));

        // Remove the last p entries
        childNode.getEntries().subList(start, totalEntries).clear();

        // Update bounding box and persist changes
        parentEntry.adjustBBToFitEntries(childNode.getEntries());
        FilesHandler.updateIndexFileBlock(parentNode, totalLevels);
        FilesHandler.updateIndexFileBlock(childNode, totalLevels);

        // Reinsert removed entries (close reinsertion)
        for (Entry entry : removedEntries) {
            insert(null, null, entry, childNode.getNodeLevelInTree());
        }
    }
}





class EntryAreaEnlargementPair implements Comparable {
    private Entry entry; // The Entry object
    private double areaEnlargement; // It's area enlargement assigned

    EntryAreaEnlargementPair(Entry entry, double areaEnlargement){
        this.entry = entry;
        this.areaEnlargement = areaEnlargement;
    }

    Entry getEntry() {
        return entry;
    }

    private double getAreaEnlargement() {
        return areaEnlargement;
    }

    // Comparing the pairs by area enlargement
    @Override
    public int compareTo(Object obj) {
        EntryAreaEnlargementPair pairB = (EntryAreaEnlargementPair)obj;
        // Resolve ties by choosing the entry with the rectangle of smallest area
        if (this.getAreaEnlargement() == pairB.getAreaEnlargement())
            return Double.compare(this.getEntry().getBoundingBox().getArea(),pairB.getEntry().getBoundingBox().getArea());
        else
            return Double.compare(this.getAreaEnlargement(),pairB.getAreaEnlargement());
    }
}
