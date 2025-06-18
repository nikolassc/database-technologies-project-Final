import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


/**
 *
 *
 * Public class {@link RStarTree} that implements the R*Tree index on top of the datafile. Uses an {@code indexBuffer} to limit I/O's in memory
 * and does all the job regarding the index. <p>
 * Mostly uses {@link FilesHandler} Index File methods. Each {@link LeafEntry} points to a different
 * Datafile Block, and multiple Nodes can be saved to a single IndexFile Block, making the indexfile several times smaller
 * than the datafile (e.g for a 50MB datafile with 1721 blocks of 32KB each, the indexfile was only 500KB).
 * <p>Model is based
 * on the paper:<p>
 *                The R*-tree:
 *     An Efficient and Robust AccessMethod
 *        for Points and Rectangles+
 * <p></p></?>By:    Norbert Beckmann, Hans-Peterbegel
 *        Ralf Schneider,Bernhard Seeger
 * Praktuche Informatlk, Umversltaet Bremen, D-2800 Bremen 33, West Germany
 *
 *
 */


public class RStarTree {
    /** Total levels of the R*Tree */
    private int totalLevels;

    /** {@link Boolean} {@link ArrayList} to keep track of which levels where reinserted */
    private boolean[] levelsInserted;

    /** The root node's block id, always remains the same*/
    private static final int ROOT_NODE_BLOCK_ID = 1;

    /** The level of leaf entries in the R*Tree, always remains the same */
    private static final int LEAF_LEVEL = 1;

    /** P = 32 for {@link #chooseSubTree} as described in paper, to limit cpu usage*/
    private static final int CHOOSE_SUBTREE_LEVEL = 32;

    /** p entries that will be reinserted in Tree */
    private static final int REINSERT_TREE_ENTRIES = (int) (0.3 * Node.getMaxEntriesInNode());

    /** {@link Map} that keeps {@code RecordIds} and their corresponding leaf {@link Node}s, used in {@link #deleteRecord} */
    private static Map<Long, Long> recordToLeafMap = new HashMap<>(); //


    /**
     * {@code RStarTree} constructor method. <p>
     * Depending on user selection, the tree will be bulk-Loaded or every {@link Entry} will be inserted
     * one by one. Uses an {@code indexBuffer} to limit I/O speeds.
     *
     * @param doBulkLoad {@code boolean} to check whether to bulkLoad or not
     * @throws IOException to catch any IOException errors
     */


    RStarTree(boolean doBulkLoad) throws IOException {
        this.totalLevels = FilesHandler.getTotalLevelsFile();
        if (doBulkLoad) {
            ArrayList<RecordBlockPairID> allRecordsPairs = new ArrayList<>();
            int totalBlocks = FilesHandler.getTotalBlocksInDataFile();

            for (int i = 1; i < totalBlocks; i++) {
                ArrayList<Record> blockRecords = FilesHandler.readDataFileBlock(i);
                if (blockRecords != null) {
                    for (Record record : blockRecords) {
                        allRecordsPairs.add(new RecordBlockPairID(record, i));
                    }
                } else {
                    throw new IllegalStateException("Error reading records from datafile");
                }
            }

            bulkLoadFromRecords(allRecordsPairs);
        } else {
            Node root = new Node(ROOT_NODE_BLOCK_ID);
            FilesHandler.writeNewIndexFileBlock(root);
            FilesHandler.indexBuffer.put(FilesHandler.currentBlockId, FilesHandler.currentIndexBlock);
            for (int i = 1; i < FilesHandler.getTotalBlocksInDataFile(); i++) {
                ArrayList<Record> records = FilesHandler.readDataFileBlock(i);
                if (records != null) {
                    insertData(records,i);
                } else {
                    throw new IllegalStateException("Error reading records from datafile");
                }
            }
            printTreeStats();
            FilesHandler.flushIndexBufferToDisk();

            System.out.println("✅ Total levels after insertion: " + totalLevels);
        }
    }


    /**
     * Constructor for reading already existing {@link RStarTree} from indexfile
     */


    public RStarTree(ArrayList<Integer> metadata) throws IOException {
        if (metadata == null || metadata.size() < 4) {
            throw new IllegalStateException("Index metadata is missing or incomplete. Cannot load existing R*-Tree.");
        }

        this.totalLevels = metadata.get(3);
        buildRecordToLeafMap();
    }


    /**
     * Method that builds {@code recordToLeafMap} from existing indexfile and datafile
     */


    public void buildRecordToLeafMap() throws IOException {
        this.recordToLeafMap = new HashMap<>();

        Queue<Node> queue = new LinkedList<>();
        Node root = FilesHandler.readNode(ROOT_NODE_BLOCK_ID, 0);
        if (root == null) {
            throw new IllegalStateException("Root node is null. Cannot build recordToLeafMap.");
        }
        queue.add(root);

        while (!queue.isEmpty()) {
            Node currentNode = queue.poll();

            if (currentNode.getNodeLevelInTree() == RStarTree.getLeafLevel()) {
                for (Entry e : currentNode.getEntries()) {
                    if (e instanceof LeafEntry leafEntry) {
                        long dataBlockId = leafEntry.getDataBlockId();
                        ArrayList<Record> records = FilesHandler.readDataFileBlock(dataBlockId);
                        if (records != null) {
                            for (Record r : records) {
                                recordToLeafMap.put(r.getRecordID(), dataBlockId);
                            }
                        }
                    }
                }
            }
            else {
                for (Entry e : currentNode.getEntries()) {
                    long childBlockId = e.getChildNodeBlockId();
                    int childNodeIndex = e.getChildNodeIndexInBlock();
                    Node childNode = FilesHandler.readNode(childBlockId, childNodeIndex);
                    if (childNode != null) {
                        queue.add(childNode);
                    }
                }
            }
        }

        System.out.println("RecordToLeafMap built with " + recordToLeafMap.size() + " entries.");
    }


    /**
     * Getter method that returns the tree's Root Node
     *
     * @return The tree's Root Node
     */


    Node getRootNode() {
        return FilesHandler.readNode(ROOT_NODE_BLOCK_ID, 0);
    }


    /**
     * Getter method that returns {@code ROOT_NODE_BLOCK_ID}
     *
     * @return Always 1.
     */


    static int getRootNodeBlockId() {
        return ROOT_NODE_BLOCK_ID;
    }


    /**
     * Getter method that returns {@code LEAF_LEVEL}
     *
     * @return Always 1.
     */

    static int getLeafLevel() {
        return LEAF_LEVEL;
    }


    /**
     * {@code insertData} method, written as explained in R*Tree paper.<p> Invokes insert with the leaf level as parameter
     * to insert a new data rectangle
     *
     * @param records The records in a block of Data from Datafile.
     * @param datafileBlockId The id of the block of Data in Datafile
     * @throws IOException to catch any IOExceptions during Insert/Split/reInsert
     */


    private void insertData(ArrayList<Record> records, long datafileBlockId) throws IOException {
        ArrayList<Bounds> boundsList = Bounds.findMinimumBoundsFromRecords(records);
        MBR blockMBR = new MBR(boundsList);
        LeafEntry entry = new LeafEntry(datafileBlockId, blockMBR);
        this.levelsInserted = new boolean[totalLevels];
        insert(null, null, entry, LEAF_LEVEL);
        for (Record r : records) {
            RStarTree.recordToLeafMap.put(r.getRecordID(), datafileBlockId);
        }
    }


    /**
     * {@code Insert} method, starts from the root, adjusting all covering rectangles in the insertion path, and recursively
     * traverses tree using {@link #chooseSubTree} to find appropriate node to insert the data entry. <p>
     *  If node already has M entries,
     * {@link #overflowTreatment} is called.
     *
     * @param parentNode The parent of a {@link Node} in the tree. If null, {@link Node} is root.
     * @param parentEntry The parent {@link Entry} that points to a {@link Node} in the tree. If null, {@link Node} is Root.
     * @param dataEntry The new data {@link Entry} to be added to an appropriate {@link Node}.
     * @param levelToAdd The {@code LEAF_LEVEL} in which to add the new data entry. In our code, {@code LEAF_LEVEL} is always 1, and
     *                    tree increases levels upwards.
     * @return        Returns null when dataEntry is successfully inserted to Leaf Node, or {@link #overflowTreatment} when Node
     *                has M entries.
     * @throws IOException-: to catch any IOExceptions
     */


    private Entry insert(Node parentNode, Entry parentEntry, Entry dataEntry, int levelToAdd) throws IOException {
        long nodeBlockId = (parentEntry == null) ? ROOT_NODE_BLOCK_ID : parentEntry.getChildNodeBlockId();
        int nodeBlockIndex = (parentEntry == null) ? 0 : parentEntry.getChildNodeIndexInBlock();

        if (parentEntry != null) {
            parentEntry.adjustMBRToFitEntry(dataEntry);
            FilesHandler.updateIndexFileBlock(parentNode, totalLevels);
        }

        Node currentNode = FilesHandler.readNode(nodeBlockId, nodeBlockIndex);
        if (currentNode == null) {
            throw new IllegalStateException("Node-block is null");
        }

        if (levelToAdd > totalLevels) {
            totalLevels = levelToAdd;
            boolean[] newLevelsInserted = new boolean[totalLevels];
            if (levelsInserted != null)
                System.arraycopy(levelsInserted, 0, newLevelsInserted, 0, levelsInserted.length);
            levelsInserted = newLevelsInserted;
        }

        if (currentNode.getNodeLevelInTree() == levelToAdd) {
            currentNode.insertEntry(dataEntry);
            FilesHandler.updateIndexFileBlock(currentNode, totalLevels);
        } else {
            Entry bestEntry = chooseSubTree(currentNode, dataEntry.getMBR(), levelToAdd);
            Entry newEntry = insert(currentNode, bestEntry, dataEntry, levelToAdd);

            if (newEntry != null) {
                currentNode.insertEntry(newEntry);
            }

            FilesHandler.updateIndexFileBlock(currentNode, totalLevels);

            if (currentNode.getEntries().size() <= Node.getMaxEntriesInNode()) {
                return null;
            }

            return overflowTreatment(parentNode, parentEntry, currentNode);
        }

        if (currentNode.getEntries().size() > Node.getMaxEntriesInNode()) {
            return overflowTreatment(parentNode, parentEntry, currentNode);
        }

        return null;
    }


    /**
     * {@code chooseSubTree} algorithm as described in R*Tree paper. It determines the entry in a {@link Node} which needs the least
     * area enlargement to include the new data.
     *
     * @param node The current {@link Node} that {@code chooseSubTree} is at.
     * @param MBRToAdd The new data to add.
     * @param levelToAdd The {@code LEAF_LEVEL}.
     * @return The {@link Entry} with the least area enlargement or the least overlap enlargement.
     */


    private Entry chooseSubTree(Node node, MBR MBRToAdd, int levelToAdd) {
        ArrayList<Entry> entries = node.getEntries();
        if (node.getNodeLevelInTree() == levelToAdd + 1) {
            if (Node.getMaxEntriesInNode() > (CHOOSE_SUBTREE_LEVEL * 2) / 3 && entries.size() > CHOOSE_SUBTREE_LEVEL) {
                ArrayList<Entry> topEntries = getEntriesWithMinimalAreaEnlargement(entries, MBRToAdd);
                return Collections.min(topEntries, new EntryComparator.OverlapEnlargementComparator(topEntries, MBRToAdd, entries));
            }
            return Collections.min(entries, new EntryComparator.OverlapEnlargementComparator(entries, MBRToAdd, entries));
        }
        return getEntryWithMinAreaEnlargement(entries, MBRToAdd);
    }


    /**
     * Helper method for chooseSubTree which returns the {@link Entry} with the least area enlargement.
     *
     * @param entries the entries of a node that it investigates
     * @param mbr mbr to be added to the {@link Entry}
     * @return the {@link Entry} with the Least area enlargement
     */


    private Entry getEntryWithMinAreaEnlargement(ArrayList<Entry> entries, MBR mbr) {
        return Collections.min(
                entries.stream()
                        .map(e -> new EntryAreaEnlargementPair(e, computeAreaEnlargement(e, mbr)))
                        .toList(),
                EntryAreaEnlargementPair::compareTo
        ).getEntry();
    }


    /**
     * Helper method for chooseSubTree that returns the top p entries with the smallest Area Enlargement.
     *
     * @param candidateEntries -: The entries of a node that are investigated
     * @param mbrToAdd         -: The new MBR to be added to an Entry.
     * @return an ArrayList of the Entries with the smallestAreaEnlargement.
     */


    private ArrayList<Entry> getEntriesWithMinimalAreaEnlargement(ArrayList<Entry> candidateEntries, MBR mbrToAdd) {
        return candidateEntries.stream()
                .map(entry -> new EntryAreaEnlargementPair(entry, computeAreaEnlargement(entry, mbrToAdd)))
                .sorted()  // Sort by Ascending area enlargement
                .limit(RStarTree.CHOOSE_SUBTREE_LEVEL)
                .map(EntryAreaEnlargementPair::getEntry)
                .collect(Collectors.toCollection(ArrayList::new));
    }


    /**
     * Helper method that computes the AreaEnlargement of an Entry
     * @param entry-: the entry which we compute the area Enlargement at.
     * @param mbrToAdd-: the MBR to be added to the Entry
     * @return the enlarged area of the Entry
     */


    private double computeAreaEnlargement(Entry entry, MBR mbrToAdd) {
        MBR enlarged = new MBR(Bounds.findMinimumBounds(entry.getMBR(), mbrToAdd));
        return enlarged.getArea() - entry.getMBR().getArea();
    }


    /**
     * {@code overflowTreatment} method as described in R*Tree paper. <p> When overflowing, first checks if level is root and if there
     * has already been a reinsertion in current level. If not, then calls {@link #handleReinsert}. else, calls {@link #handleSplit}
     *
     * @param parentNode The parent {@link Node} of the current Node.
     * @param parentEntry The parent {@link Entry} pointing to current Node.
     * @param currentNode The current {@link Node} being processed.
     * @return {@code null} when reinsert or root split. If split at any other level, returns split {@link Entry}.
     * @throws IOException to catch any IOException errors.
     */


    private Entry overflowTreatment(Node parentNode, Entry parentEntry, Node currentNode) throws IOException {
        int levelIndex = currentNode.getNodeLevelInTree() - 1;
        if (levelIndex >= levelsInserted.length) {
            boolean[] newLevelsInserted = new boolean[totalLevels];
            System.arraycopy(levelsInserted, 0, newLevelsInserted, 0, levelsInserted.length);
            levelsInserted = newLevelsInserted;
        }

        if (currentNode.getNodeBlockId() != ROOT_NODE_BLOCK_ID && !levelsInserted[levelIndex]) {
            levelsInserted[levelIndex] = true;
            return handleReinsert(parentNode, parentEntry, currentNode);
        } else {
            return handleSplit(parentNode, parentEntry, currentNode);
        }
    }


    /**
     * {@code handleReinsert} helper method that calls {@link #reInsert} method for current node.
     *
     * @param parentNode The parent {@link Node} of the current node.
     * @param parentEntry The parent {@link Entry} pointing to current Node.
     * @param currentNode The current Node being processed.
     * @return {@code null} when reinserting or root split. If Split at any level, returns split Entry.
     * @throws IOException-: to catch any IOException errors.
     */


    private Entry handleReinsert(Node parentNode, Entry parentEntry, Node currentNode) throws IOException {
        reInsert(parentNode, parentEntry, currentNode);
        return null;
    }


    /**
     * {@code handleSplit} method that splits a {@link Node} in two nodes using {@code chooseSplitIndex}, {@code chooseSplitAxis} as described in R*tree paper
     * (methods implemented in {@code Node} class)
     *
     * @param parentNode The parent {@link Node} of the current node.
     * @param parentEntry The parent {@link Entry} pointing to current node.
     * @param currentNode The current {@link Node} being processed.
     * @return the split {@link Entry} when splitting in internal or leaf level, or null when splitting the root.
     * @throws IOException to catch any IOexception errors
     */


    private Entry handleSplit(Node parentNode, Entry parentEntry, Node currentNode) throws IOException {
        ArrayList<Node> splitNodes = currentNode.splitNode();
        if (splitNodes.size() != 2) {
            throw new IllegalStateException("Split must produce exactly two nodes.");
        }

        Node leftNode = splitNodes.get(0);
        Node rightNode = splitNodes.get(1);
        currentNode.setEntries(leftNode.getEntries());

        if (currentNode.getNodeBlockId() != ROOT_NODE_BLOCK_ID || currentNode.getNodeIndexInBlock() != 0) {
            FilesHandler.updateIndexFileBlock(currentNode, totalLevels);
            rightNode.setNodeBlockId(FilesHandler.getTotalBlocksInIndexFile());
            rightNode.setNodeIndexInBlock(FilesHandler.currentIndexBlock.getNodes().size());
            FilesHandler.writeNewIndexFileBlock(rightNode);
            parentEntry.adjustMBRToFitEntries(currentNode.getEntries());
            FilesHandler.updateIndexFileBlock(parentNode, totalLevels);
            return new Entry(rightNode);
        }

        // Handle root split
        leftNode.setNodeBlockId(FilesHandler.getTotalBlocksInIndexFile());
        leftNode.setNodeIndexInBlock(FilesHandler.currentIndexBlock.getNodes().size());
        FilesHandler.writeNewIndexFileBlock(leftNode);

        rightNode.setNodeBlockId(FilesHandler.getTotalBlocksInIndexFile());
        rightNode.setNodeIndexInBlock(FilesHandler.currentIndexBlock.getNodes().size());
        FilesHandler.writeNewIndexFileBlock(rightNode);

        ArrayList<Entry> newRootEntries = new ArrayList<>();
        newRootEntries.add(new Entry(leftNode));
        newRootEntries.getFirst().setChildNodeIndexInBlock(leftNode.getNodeIndexInBlock());
        newRootEntries.add(new Entry(rightNode));
        newRootEntries.get(1).setChildNodeIndexInBlock(rightNode.getNodeIndexInBlock());

        currentNode.setNodeLevelInTree(currentNode.getNodeLevelInTree() + 1);
        currentNode.setEntries(newRootEntries);
        currentNode.setNodeBlockId(ROOT_NODE_BLOCK_ID);
        currentNode.setNodeIndexInBlock(0);
        FilesHandler.setLevelsOfTreeIndex(++totalLevels);
        FilesHandler.updateIndexFileBlock(currentNode, totalLevels);

        return null;

    }


    /**
     * {@code reInsert} as described in R*Tree paper. <p>For all M+1 entries in a {@link Node}, it calculates and sorts the distances
     * between the centers of their rectangles in decreasing order, removes the first p entries from the {@link Node} and adjusts its {@link MBR},
     * and invokes {@link #insert} starting from farthest reinsert to reinsert the entries.
     *
     * @param parentNode The parent {@link Node} of the current node.
     * @param parentEntry The parent {@link Entry} pointing to current node.
     * @param currentNode The current {@link Node} being processed.
     * @throws IOException to catch any IOException errors.
     */


    private void reInsert(Node parentNode, Entry parentEntry, Node currentNode) throws IOException {
        int totalEntries = currentNode.getEntries().size();
        int expectedEntries = Node.getMaxEntriesInNode() + 1;

        if (totalEntries != expectedEntries) {
            throw new IllegalStateException("Reinsert requires exactly M+1 entries.");
        }

        currentNode.getEntries().sort(
                Collections.reverseOrder( //Reverse to get Farthest distances first
                new EntryComparator.DistanceFromCenterComparator(currentNode.getEntries(), parentEntry.getMBR())
                )
        );

        ArrayList<Entry> removedEntries = new ArrayList<>(currentNode.getEntries().subList(0, REINSERT_TREE_ENTRIES));
        currentNode.getEntries().subList(0, REINSERT_TREE_ENTRIES).clear();

        parentEntry.adjustMBRToFitEntries(currentNode.getEntries());
        FilesHandler.updateIndexFileBlock(parentNode, totalLevels);
        FilesHandler.updateIndexFileBlock(currentNode, totalLevels);

        Queue<Entry> reinsertQueue = new LinkedList<>(removedEntries);
        while (!reinsertQueue.isEmpty()) {
            insert(null, null, reinsertQueue.poll(), currentNode.getNodeLevelInTree());
        }
    }


    /**
     * {@code insertSingleRecord} method that inserts a single {@link Record} into a datafile block and the R*Tree, reusing space by appending it to
     * a suitable block with space.
     *
     * @param record The record to be added.
     * @throws IOException To catch any IOException errors
     */


    public void insertSingleRecord(Record record) throws IOException {
        Long recordId = recordToLeafMap.get(record.getRecordID());
        if (recordId != null) {
            System.out.println("Record with ID " + recordId + " already exists in Index");
            return;
        }

        long dataBlockId = FilesHandler.appendRecordToDataBlock(record);

        // Build MBR for the new record
        ArrayList<Bounds> boundsList = Bounds.findMinimumBoundsFromRecord(record);
        MBR recordMBR = new MBR(boundsList);

        // If leaf Node exists
        Node leafNode = findLeafNodeContainingDataBlock(dataBlockId);
        if (leafNode != null) {
            for (Entry e : leafNode.getEntries()) {
                if (e instanceof LeafEntry leafEntry && leafEntry.getDataBlockId() == dataBlockId) {
                    // Adjust existing MBR to include the new record
                    leafEntry.adjustMBRToFitMBR(recordMBR);
                    FilesHandler.updateIndexFileBlock(leafNode, totalLevels);
                    System.out.println("Record added to existing data block and LeafEntry MBR updated.");
                    recordToLeafMap.put(record.getRecordID(), dataBlockId);
                    return;
                }
            }
        }

        // If no LeafEntry found, create a new one
        LeafEntry newEntry = new LeafEntry(dataBlockId, recordMBR);
        insert(null, null, newEntry, LEAF_LEVEL);
        recordToLeafMap.put(record.getRecordID(), dataBlockId);

        System.out.println("New LeafEntry created and Record added to R*-Tree.");
    }



    /**
     * {@code deleteRecord} method that deletes a single {@link Record} from R*Tree index using {@code recordToLeafMap} to map records to their blocks in datafile.
     *
     * <p>Firstly it tries to find the leaf node containing the {@link LeafEntry} with the record.
     * And deletes it if everything is handled correctly. If It detects underflow, it calls {@link #condenseTree} to remove underflowed nodes
     * and reinsert their entries.
     *
     * @param recordId the recordId of the {@link Record} to be deleted.
     * @throws IOException to catch any IOException errors
     */


    public void deleteRecord(long recordId) throws IOException {
        //Search for record in Map
        Long dataBlockId = recordToLeafMap.get(recordId);
        if (dataBlockId == null) {
            System.out.println("Record not found in index!");
            return;
        }

        ArrayList<Record> records = FilesHandler.readDataFileBlock(dataBlockId);
        if (records == null) {
            System.out.println("Data block not found.");
            return;
        }

        boolean removed = records.removeIf(r -> r.getRecordID() == recordId);
        if (!removed) {
            System.out.println("Record not found in block.");
            return;
        }

        if (records.isEmpty()) {
            Node leafNode = findLeafNodeContainingDataBlock(dataBlockId);
            if (leafNode == null) {
                System.out.println("Leaf Node not found.");
                return;
            }

            boolean leafEntryRemoved = leafNode.getEntries().removeIf(e ->
                    e instanceof LeafEntry && ((LeafEntry) e).getDataBlockId() == dataBlockId
            );

            if (leafEntryRemoved) {
                System.out.println("LeafEntry removed from index (block was empty");
                FilesHandler.updateIndexFileBlock(leafNode, totalLevels);
                condenseTree(leafNode);
            }

        } else {
            FilesHandler.overwriteDataFileBlock(dataBlockId, records);
            System.out.println("Record removed from data block, leafEntry remains");
        }

        recordToLeafMap.remove(recordId);
    }

    /**
     * {@code findLeafNodeContainingDataBlock} helper function that returns the leaf {@link Node} that contains the data block
     *
     * @param dataBlockId The data block Id
     * @return The leaf that contains the data block with current id
     */
    private Node findLeafNodeContainingDataBlock(Long dataBlockId) {
        Node root = getRootNode();
        return searchLeafRecursive(root, dataBlockId);
    }


    /**
     * {@code searchLeafRecursive} method that searches a leaf node recursively from a {@link Node}.
     *
     * @param node The {@link Node} given.
     * @param dataBlockId The {@code blockId} of the datafile in which the node is in.
     * @return The leaf node
     */


    private Node searchLeafRecursive(Node node, Long dataBlockId) {
        if (node.getNodeLevelInTree() == getLeafLevel()) {
            for (Entry entry : node.getEntries()) {
                if (entry instanceof LeafEntry && ((LeafEntry) entry).getDataBlockId() == dataBlockId) {
                    return node;
                }
            }
            return null;
        }

        for (Entry entry : node.getEntries()) {
            Node child = FilesHandler.readNode(entry.getChildNodeBlockId(), entry.getChildNodeIndexInBlock());
            if (child == null) continue;
            Node result = searchLeafRecursive(child, dataBlockId);
            if (result != null) return result;
        }

        return null;
    }


    /**
     * {@code condenseTree} method that handles underflow when deleting single {@link Record}
     *
     * @param node The node containing underflow.
     * @throws IOException for any IOException errors.
     */


    private void condenseTree(Node node) throws IOException {
        Map<Node, List<Entry>> eliminated = new HashMap<>();

        Node current = node;
        while (current.getNodeBlockId() != ROOT_NODE_BLOCK_ID) {
            Node parent = findParent(current);
            if (parent == null) break;

            Entry parentEntry = findParentEntry(parent, current);
            if (parentEntry == null) break;

            if (current.getEntries().size() < Node.getMinEntriesInNode()) {
                parent.getEntries().remove(parentEntry);
                eliminated.put(current, new ArrayList<>(current.getEntries()));
            } else {
                parentEntry.adjustMBRToFitEntries(current.getEntries());
            }

            FilesHandler.updateIndexFileBlock(current, totalLevels);
            FilesHandler.updateIndexFileBlock(parent, totalLevels);
            current = parent;
        }

        Node root = getRootNode();
        if (root.getEntries().size() == 1 && root.getNodeLevelInTree() > LEAF_LEVEL) {
            Entry onlyEntry = root.getEntries().getFirst();
            Node newRoot = FilesHandler.readNode(onlyEntry.getChildNodeBlockId(), onlyEntry.getChildNodeIndexInBlock());
            newRoot.setNodeBlockId(ROOT_NODE_BLOCK_ID);
            newRoot.setNodeIndexInBlock(0);
            FilesHandler.setLevelsOfTreeIndex(--totalLevels);
            FilesHandler.updateIndexFileBlock(newRoot, totalLevels);
            System.out.println("Root condense, new level: " + totalLevels);
        }

        for (List<Entry> entryList : eliminated.values()) {
            for (Entry e : entryList) {
                int level;
                if (e instanceof LeafEntry) {
                    level = LEAF_LEVEL;
                } else {
                    Node child = FilesHandler.readNode(e.getChildNodeBlockId(), e.getChildNodeIndexInBlock());
                    if (child == null) {
                        System.out.println("Couldn't reinsert entry: child node not found.");
                        continue;
                    }
                    level = child.getNodeLevelInTree();
                }
                insert(null, null, e, level);
            }

        }
    }


    /**
     * {@code findParent} method finds a child {@link Node}'s parent.
     *
     * @param child The child node.
     * @return The child node's parent.
     */


    private Node findParent(Node child) {
        Node root = getRootNode();
        return searchParentRecursive(root, child.getNodeBlockId());
    }


    /**
     * {@code searchParentRecursive} helper method for {@link #findParent} that searches the tree recursively for the parent.
     *
     * @param current The current node being traversed.
     * @param childId The node block id of the child {@link Node}
     * @return The child node's parent.
     */


    private Node searchParentRecursive(Node current, long childId) {
        if (current.getNodeLevelInTree() == LEAF_LEVEL) return null;

        for (Entry entry : current.getEntries()) {
            if (entry.getChildNodeBlockId() == childId) return current;
            Node next = FilesHandler.readNode(entry.getChildNodeBlockId(), entry.getChildNodeIndexInBlock());
            if (next != null) {
                Node result = searchParentRecursive(next, childId);
                if (result != null) return result;
            }
        }
        return null;
    }


    /**
     * {@code findParentEntry} helper method that finds a {@link Node}'s parent {@link Entry} that points to it.
     *
     * @param parent The parent node.
     * @param child The child node.
     * @return The parent entry
     */


    private Entry findParentEntry(Node parent, Node child) {
        for (Entry entry : parent.getEntries()) {
            if (entry.getChildNodeBlockId() == child.getNodeBlockId()) return entry;
        }
        return null;
    }


    /**
     * {@code printTreeStats} method that prints the Tree's structure
     *
     */

    public static void printTreeStats() {
        Node root = FilesHandler.readNode(RStarTree.getRootNodeBlockId(), 0);
        Map<Integer, Integer> levelNodeCounts = new HashMap<>();
        traverseAndCount(root, levelNodeCounts);

        System.out.println("\n  R*-Tree Structure:");
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


    /**
     * {@code traverseAndCount} helper method for {@link #printTreeStats} that recursively traverses the tree and keeps a {@link Node} count
     * for each level.
     * @param node The starting node.
     * @param levelNodeCounts A {@link Map} that keeps each level's total {@link Node} count.
     */


    private static void traverseAndCount(Node node, Map<Integer, Integer> levelNodeCounts) {
        int level = node.getNodeLevelInTree();
        levelNodeCounts.put(level, levelNodeCounts.getOrDefault(level, 0) + 1);

        // Αν δεν είναι φύλλο, συνέχισε προς τα κάτω
        if (level > RStarTree.getLeafLevel()) {
            for (Entry entry : node.getEntries()) {
                Node child = FilesHandler.readNode(entry.getChildNodeBlockId(), entry.getChildNodeIndexInBlock());
                if (child != null) {
                    traverseAndCount(child, levelNodeCounts);
                }
            }
        }
    }


    /**
     * {@code bulkLoadFromRecords} method handles bulk loading of the R*Tree
     *
     * @param recordBlockIdPairs A list of pairs of all {@link Record} and the ids of the blocks they are saved in the datafile.
     */


    public void bulkLoadFromRecords(List<RecordBlockPairID> recordBlockIdPairs) {
        // 1. Ομαδοποίηση RecordBlockPairID ανά Data Block
        Map<Long, List<Record>> recordsPerBlock = new HashMap<>();
        for (RecordBlockPairID pair : recordBlockIdPairs) {
            recordsPerBlock
                    .computeIfAbsent(pair.getBlockID(), k -> new ArrayList<>())
                    .add(pair.getRecord());
        }

        // 2. Δημιουργία LeafEntries (ένα per Data Block)
        List<LeafEntry> leafEntries = new ArrayList<>();
        for (Map.Entry<Long, List<Record>> entry : recordsPerBlock.entrySet()) {
            long dataBlockId = entry.getKey();
            List<Record> records = entry.getValue();

            ArrayList<Bounds> overallBounds = null;
            for (Record r : records) {
                ArrayList<Bounds> boundsForDimensions = new ArrayList<>();
                for (int i = 0; i < FilesHandler.getDataDimensions(); i++) {
                    double coord = r.getCoordinateFromDimension(i);
                    boundsForDimensions.add(new Bounds(coord, coord));
                }
                if (overallBounds == null) {
                    overallBounds = boundsForDimensions;
                } else {
                    // Expand το MBR με κάθε νέο Record
                    for (int i = 0; i < overallBounds.size(); i++) {
                        Bounds current = overallBounds.get(i);
                        Bounds newBound = boundsForDimensions.get(i);
                        overallBounds.set(i, new Bounds(
                                Math.min(current.getLower(), newBound.getLower()),
                                Math.max(current.getUpper(), newBound.getUpper())
                        ));
                    }
                }
            }

            MBR mbr = new MBR(overallBounds);
            leafEntries.add(new LeafEntry(dataBlockId, mbr));
        }

        System.out.println("Created " + leafEntries.size() + " LeafEntries for " + recordsPerBlock.size() + " data blocks.");

        // 3. Συνέχισε με τα υπόλοιπα βήματα bulk loading (split, δημιουργία internal nodes, κλπ.)


        // 2. STR Bulk Loading Leaf Nodes
        ArrayList<Node> leaves = buildLeafNodesSTR(leafEntries, Node.getMaxEntriesInNode());

        // 3. Bottom-Up Build
        Node root = buildTreeBottomUp(leaves, Node.getMaxEntriesInNode());

        // 4. Root info
        root.setNodeBlockId(ROOT_NODE_BLOCK_ID);
        root.setNodeIndexInBlock(0);

        // 5. Save root
        FilesHandler.updateIndexFileBlock(root, ROOT_NODE_BLOCK_ID);

        // 6. Flush buffer
        FilesHandler.flushIndexBufferToDisk();
    }


    /**
     * {@code buildLeafNodesSTR} method that creates the leaf nodes using STR logic.
     *
     * @param leafEntries {@link LeafEntry} list to be bulk-loaded.
     * @param M Max entries in a {@link Node}.
     * @return An {@link ArrayList} of all the leaf nodes.
     */


    private ArrayList<Node> buildLeafNodesSTR(List<LeafEntry> leafEntries, int M) {
        ArrayList<Node> leafNodes = new ArrayList<>();

        leafEntries.sort(Comparator.comparingDouble(e -> e.getMBR().getCenter().getFirst()));

        int sliceCount = 1; //Math.max(1, (int) Math.ceil(Math.pow((double) leafEntries.size() / M, 0.4)));
        int sliceSize = (int) Math.ceil((double) leafEntries.size() / sliceCount);

        for(int i = 0; i < leafEntries.size(); i+=sliceSize) {
            List<LeafEntry> slice = leafEntries.subList(i, Math.min(i + sliceSize, leafEntries.size()));
            slice.sort(Comparator.comparingDouble(e -> e.getMBR().getCenter().get(1)));

            for (int j = 0; j < slice.size(); j+= M/2){
                List<LeafEntry> group = slice.subList(j, Math.min(j + M/2, slice.size()));
                Node leaf = new Node(LEAF_LEVEL, new ArrayList<>(group));

                FilesHandler.writeNewIndexFileBlock(leaf);
                leafNodes.add(leaf);

            }

        }

        return leafNodes;
    }


    /**
     * {@code buildTreeBottomUp} method that builds the rest of the tree's nodes bottom Up.
     *
     * @param children The leaf nodes.
     * @param M Max entries in {@link Node}.
     * @return The root node.
     */


    private Node buildTreeBottomUp(ArrayList<Node> children, int M) {
        int currentLevel = children.getFirst().getNodeLevelInTree() + 1;

        while (children.size() > 1){
            if (children.size() <= M) {
                ArrayList<Entry> entries = new ArrayList<>();
                for (Node child : children) {
                    entries.add(new Entry(child));
                }
                Node parent = new Node(currentLevel, entries);
                FilesHandler.writeNewIndexFileBlock(parent);
                FilesHandler.setLevelsOfTreeIndex(currentLevel);
                return parent; // Root node
            }

            ArrayList<Node> newLevelNodes = new ArrayList<>();

            children.sort(Comparator.comparingDouble(n -> n.getMBR().getCenter().getFirst()));

            int sliceCount = Math.max(1, children.size() / (M * 20));
            int sliceSize = (int) Math.ceil((double) children.size() / sliceCount);

            for (int i = 0; i < children.size(); i+=sliceSize) {
                List<Node> slice = children.subList(i, Math.min(i + sliceSize, children.size()));

                slice.sort(Comparator.comparingDouble(e -> e.getMBR().getCenter().get(1)));

                for(int j = 0; j < slice.size(); j+= M/2){
                    List<Node> group = slice.subList(j, Math.min(j + M, slice.size()));

                    ArrayList<Entry> entries = new ArrayList<>();
                    for (Node child : group){
                        entries.add(new Entry(child));
                    }

                    Node parent = new Node (currentLevel, entries);

                    FilesHandler.writeNewIndexFileBlock(parent);

                    newLevelNodes.add(parent);
                }
            }

            children = newLevelNodes;
            currentLevel++;
        }
        FilesHandler.setLevelsOfTreeIndex(currentLevel);
        return children.getFirst();
    }


}


/**
 *
 *
 * Helper class {@code EntryAreaEnlargementPair} used to compare an {@link Entry}'s area enlargements while keeping the {@link Entry} object.
 *
 *
 */


class EntryAreaEnlargementPair implements Comparable {
    private Entry entry;
    private double areaEnlargement;

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
            return Double.compare(this.getEntry().getMBR().getArea(),pairB.getEntry().getMBR().getArea());
        else
            return Double.compare(this.getAreaEnlargement(),pairB.getAreaEnlargement());
    }
}
