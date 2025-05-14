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

    static int getLeafLevel(){
        return LEAF_LEVEL;
    }

    private void insertRecord(Record record, long blockId){
        ArrayList<Bounds> boundsForDimensions = new ArrayList<>();

        for (int i = 0; i < FilesHandler.getDataDimensions(); i++) {
            boundsForDimensions.add(new Bounds(record.getCoordinate(i), record.getCoordinate(i) ));
        }
    }


}