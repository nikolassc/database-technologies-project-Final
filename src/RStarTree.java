import java.util.*;
import java.util.stream.Collectors;

public class RStarTree {

    private int totalLevels;
    private boolean[] levelsInserted;
    private static final int ROOT_NODE_BLOCK_ID = 1;
    private static final int LEAF_LEVEL = 1;
    private static final int CHOOSE_SUBTREE_LEVEL = 32;
    private static final int REINSERT_TREE_ENTRIES = (int) (0.3 * Node.getMaxEntriesInNode());

    RStarTree(boolean insertFromDataFile) {
        this.totalLevels = FilesHandler.getTotalLevelsFile();
        if (insertFromDataFile) {
            Node root = new Node(1);
            FilesHandler.writeNewIndexFileBlock(root);
            for (int i = 1; i < FilesHandler.getTotalBlocksInDataFile(); i++) {
                ArrayList<Record> records = FilesHandler.readDataFileBlock(i);
                if (records != null) {
                    insertDataBlock(records,i);
                    printTreeStats();
                } else {
                    throw new IllegalStateException("Error reading records from datafile");
                }
            }
            printTreeStats();
            FilesHandler.flushIndexBufferToDisk();

            System.out.println("✅ Total levels after insertion: " + totalLevels);
            printTreeStats();
        }
    }

    Node getRootNode() {
        return FilesHandler.readIndexFileBlock(ROOT_NODE_BLOCK_ID);
    }

    static int getRootNodeBlockId() {
        return ROOT_NODE_BLOCK_ID;
    }

    static int getLeafLevel() {
        return LEAF_LEVEL;
    }

    private void insertDataBlock(ArrayList<Record> records, long datafileBlockId) {
        ArrayList<Bounds> boundsList = Bounds.findMinimumBoundsFromRecords(records);
        MBR blockMBR = new MBR(boundsList);
        LeafEntry entry = new LeafEntry(datafileBlockId, blockMBR);
        this.levelsInserted = new boolean[totalLevels];
        insert(null, null, entry, LEAF_LEVEL);
    }


    private Entry insert(Node parentNode, Entry parentEntry, Entry dataEntry, int levelToAdd) {
        long nodeBlockId = (parentEntry == null) ? ROOT_NODE_BLOCK_ID : parentEntry.getChildNodeBlockId();

        if (parentEntry != null) {
            parentEntry.adjustBBToFitEntry(dataEntry);
            FilesHandler.updateIndexFileBlock(parentNode, totalLevels);
        }

        Node childNode = FilesHandler.readIndexFileBlock(nodeBlockId);
        if (childNode == null) {
            throw new IllegalStateException("Node-block is null");
        }

        if (levelToAdd > totalLevels) {
            totalLevels = levelToAdd;
            boolean[] newLevelsInserted = new boolean[totalLevels];
            if (levelsInserted != null)
                System.arraycopy(levelsInserted, 0, newLevelsInserted, 0, levelsInserted.length);
            levelsInserted = newLevelsInserted;
        }

        if (childNode.getNodeLevelInTree() == levelToAdd) {
            childNode.insertEntry(dataEntry);
            FilesHandler.updateIndexFileBlock(childNode, totalLevels);
        } else {
            Entry bestEntry = chooseSubTree(childNode, dataEntry.getBoundingBox(), levelToAdd);
            Entry newEntry = insert(childNode, bestEntry, dataEntry, levelToAdd);

            if (newEntry != null) {
                childNode.insertEntry(newEntry);
            }

            FilesHandler.updateIndexFileBlock(childNode, totalLevels);

            if (childNode.getEntries().size() <= Node.getMaxEntriesInNode()) {
                return null;
            }

            return overFlowTreatment(parentNode, parentEntry, childNode);
        }

        if (childNode.getEntries().size() > Node.getMaxEntriesInNode()) {
            return overFlowTreatment(parentNode, parentEntry, childNode);
        }

        return null;
    }

    private Entry chooseSubTree(Node node, MBR MBRToAdd, int levelToAdd) {
        ArrayList<Entry> entries = node.getEntries();
        if (node.getNodeLevelInTree() == levelToAdd + 1) {
            if (Node.getMaxEntriesInNode() > (CHOOSE_SUBTREE_LEVEL * 2) / 3 && entries.size() > CHOOSE_SUBTREE_LEVEL) {
                ArrayList<Entry> topEntries = getTopAreaEnlargementEntries(entries, MBRToAdd, CHOOSE_SUBTREE_LEVEL);
                return Collections.min(topEntries, new EntryComparator.EntryOverlapEnlargementComparator(topEntries, MBRToAdd, entries));
            }
            return Collections.min(entries, new EntryComparator.EntryOverlapEnlargementComparator(entries, MBRToAdd, entries));
        }
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
        if (levelIndex >= levelsInserted.length) {
            boolean[] newLevelsInserted = new boolean[totalLevels];
            System.arraycopy(levelsInserted, 0, newLevelsInserted, 0, levelsInserted.length);
            levelsInserted = newLevelsInserted;
        }

        if (childNode.getNodeBlockId() != ROOT_NODE_BLOCK_ID && !levelsInserted[levelIndex]) {
            levelsInserted[levelIndex] = true;
            reInsert(parentNode, parentEntry, childNode);
            return null;
        }

        ArrayList<Node> splitNodes = childNode.splitNode();
        if (splitNodes.size() != 2) {
            throw new IllegalStateException("Split must produce exactly two nodes.");
        }

        Node leftNode = splitNodes.get(0);
        Node rightNode = splitNodes.get(1);
        childNode.setEntries(leftNode.getEntries());

        if (childNode.getNodeBlockId() != ROOT_NODE_BLOCK_ID) {
            FilesHandler.updateIndexFileBlock(childNode, totalLevels);
            rightNode.setNodeBlockId(FilesHandler.getTotalBlocksInIndexFile());
            FilesHandler.writeNewIndexFileBlock(rightNode);
            parentEntry.adjustBBToFitEntries(childNode.getEntries());
            FilesHandler.updateIndexFileBlock(parentNode, totalLevels);
            return new Entry(rightNode);
        }

        childNode.setNodeBlockId(FilesHandler.getTotalBlocksInIndexFile());
        FilesHandler.writeNewIndexFileBlock(childNode);

        rightNode.setNodeBlockId(FilesHandler.getTotalBlocksInIndexFile());
        FilesHandler.writeNewIndexFileBlock(rightNode);

        ArrayList<Entry> newRootEntries = new ArrayList<>();
        newRootEntries.add(new Entry(childNode));
        newRootEntries.add(new Entry(rightNode));

        Node newRoot = new Node(childNode.getNodeLevelInTree()+1, newRootEntries);
        newRoot.setNodeBlockId(ROOT_NODE_BLOCK_ID);
        FilesHandler.setLevelsOfTreeIndex(++totalLevels);
        FilesHandler.updateIndexFileBlock(newRoot, totalLevels);
        System.out.println("newRootCreated at level: " + totalLevels);

        return null;
    }

    private void reInsert(Node parentNode, Entry parentEntry, Node childNode) {
        int totalEntries = childNode.getEntries().size();
        int expectedEntries = Node.getMaxEntriesInNode() + 1;

        if (totalEntries != expectedEntries) {
            throw new IllegalStateException("Reinsert requires exactly M+1 entries.");
        }

        childNode.getEntries().sort(
                new EntryComparator.EntryDistanceFromCenterComparator(childNode.getEntries(), parentEntry.getBoundingBox())
        );

        int start = totalEntries - REINSERT_TREE_ENTRIES;
        ArrayList<Entry> removedEntries = new ArrayList<>(childNode.getEntries().subList(start, totalEntries));
        childNode.getEntries().subList(start, totalEntries).clear();

        parentEntry.adjustBBToFitEntries(childNode.getEntries());
        FilesHandler.updateIndexFileBlock(parentNode, totalLevels);
        FilesHandler.updateIndexFileBlock(childNode, totalLevels);

        Queue<Entry> reinsertQueue = new LinkedList<>(removedEntries);
        while (!reinsertQueue.isEmpty()) {
            insert(null, null, reinsertQueue.poll(), childNode.getNodeLevelInTree());
        }
    }

    public static void printTreeStats() {
        Node root = FilesHandler.readIndexFileBlock(RStarTree.getRootNodeBlockId());
        Map<Integer, Integer> levelNodeCounts = new HashMap<>();
        traverseAndCount(root, levelNodeCounts);

        System.out.println("\n📊 R*-Tree Structure:");
        levelNodeCounts.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByKey().reversed())
                .forEach(entry -> {
                    int level = entry.getKey();
                    int count = entry.getValue();
                    String label = (level == RStarTree.getLeafLevel()) ? "Leaf" :
                            (level == FilesHandler.getTotalLevelsFile()) ? "Root" : "Internal";
                    System.out.printf("Level %d (%s): %d node(s)%n", level, label, count);
                });
    }

    private static void traverseAndCount(Node node, Map<Integer, Integer> levelNodeCounts) {
        int level = node.getNodeLevelInTree();
        levelNodeCounts.put(level, levelNodeCounts.getOrDefault(level, 0) + 1);

        // Αν δεν είναι φύλλο, συνέχισε προς τα κάτω
        if (level > RStarTree.getLeafLevel()) {
            for (Entry entry : node.getEntries()) {
                Node child = FilesHandler.readIndexFileBlock(entry.getChildNodeBlockId());
                if (child != null) {
                    traverseAndCount(child, levelNodeCounts);
                }
            }
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
