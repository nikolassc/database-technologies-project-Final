import java.util.ArrayList;
import java.util.Collections;

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

    private void insertRecord(Record record, long blockId){
        ArrayList<Bounds> boundsForDimensions = new ArrayList<>();

        for (int i = 0; i < FilesHandler.getDataDimensions(); i++) {
            boundsForDimensions.add(new Bounds(record.getCoordinateFromDimension(i), record.getCoordinateFromDimension(i)));
        }
        levelsInserted = new boolean[totalLevels];
        insert(null, null, new LeafEntry(record.getId(), datafileBlockId, bonusForDimensions), LEAF_LEVEL);

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




}