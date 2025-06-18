import java.util.ArrayList;


/**
 * {@code RangeQuery} class implements a range query algorithm over an {@link RStarTree} structure.
 * It recursively traverses only the branches of the tree whose {@link MBR} (Minimum Bounding Rectangles)
 * overlap with the given {@code queryMBR}, ensuring efficient pruning of irrelevant branches.
 *
 * <p>This approach significantly reduces the number of node accesses compared to a linear scan.
 * At the leaf level, it verifies each {@link Record} against the query MBR to ensure correctness.</p>
 */


public class RangeQuery {


    /**
     * Executes a range query starting from the given {@code node} in the {@link RStarTree}.
     * It recursively explores only those branches where the {@link MBR} of the {@link Entry}
     * overlaps with the {@code queryMBR}.
     *
     * @param node The current {@link Node} to explore.
     * @param queryMBR The {@link MBR} defining the query range (lower and upper bounds for each dimension).
     * @return A list of {@link Record} objects that fall within the query range.
     */


    public static ArrayList<Record> rangeQuery(Node node, MBR queryMBR) {
        ArrayList<Record> results = new ArrayList<>();

        for (Entry entry : node.getEntries()) {
            MBR entryMBR = entry.getMBR();

            if (MBR.checkOverlap(entryMBR, queryMBR)) {
                if (node.getNodeLevelInTree() == RStarTree.getLeafLevel()) {
                    LeafEntry leafEntry = (LeafEntry) entry;
                    long dataBlockId = leafEntry.getDataBlockId();
                    ArrayList<Record> records = FilesHandler.readDataFileBlock(dataBlockId);

                    if (records != null) {
                        for (Record record : records) {
                            if (isRecordInRange(record, queryMBR)) {
                                results.add(record);
                            }
                        }
                    }

                } else {
                    long childBlockId = entry.getChildNodeBlockId();
                    int childNodeIndex = entry.getChildNodeIndexInBlock();

                    Node childNode = FilesHandler.readNode(childBlockId, childNodeIndex);
                    if (childNode != null) {
                        results.addAll(rangeQuery(childNode, queryMBR));
                    }
                }
            }
        }

        return results;
    }


    /**
     * Checks whether a given {@link Record} lies entirely within the specified {@link MBR}.
     *
     * @param record The {@link Record} to check.
     * @param queryMBR The query {@link MBR} defining the valid range for each dimension.
     * @return {@code true} if the record's coordinates fall within the query MBR, {@code false} otherwise.
     */


    private static boolean isRecordInRange(Record record, MBR queryMBR) {
        ArrayList<Double> coords = record.getCoordinates();
        ArrayList<Bounds> bounds = queryMBR.getBounds();

        for (int i = 0; i < coords.size(); i++) {
            double val = coords.get(i);
            Bounds b = bounds.get(i);
            if (val < b.getLower() || val > b.getUpper()) {
                return false;
            }
        }
        return true;
    }
}


