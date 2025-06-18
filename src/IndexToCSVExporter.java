import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;


/**
 *
 * Public class IndexToCSVExporter exports the R*Tree Index Node's MBRS into a CSV file, used for python visualization.
 */


public class IndexToCSVExporter {

    public static void exportMBRsToCSV(String outputFilePath) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFilePath))) {
            writer.println("nodeBlockId,nodeLevelInTree,isLeaf,dataBlockId,boundingBox");

            Node root = FilesHandler.readNode(RStarTree.getRootNodeBlockId(), 0);
            traverseAndExport(root, writer);
            System.out.println("✅ Export complete: " + outputFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void traverseAndExport(Node node, PrintWriter writer) {
        if (node == null) return;

        boolean isLeaf = node.getNodeLevelInTree() == RStarTree.getLeafLevel();
        int nodeLevel = node.getNodeLevelInTree();

        for (Entry entry : node.getEntries()) {
            StringBuilder line = new StringBuilder();
            line.append(node.getNodeBlockId()).append(",");   // node id
            line.append(nodeLevel).append(",");               // nodeLevelInTree
            line.append(isLeaf).append(",");                   // isLeaf

            if (entry instanceof LeafEntry leafEntry) {
                line.append(leafEntry.getDataBlockId());       // datafile block id
            } else {
                line.append("");                               // empty for non-leaf
            }
            line.append(",");

            MBR mbr = entry.getMBR();
            List<Bounds> bounds = mbr.getBounds();
            for (int i = 0; i < bounds.size(); i++) {
                Bounds b = bounds.get(i);
                line.append(b.getLower()).append("-").append(b.getUpper());
                if (i < bounds.size() - 1) {
                    line.append(";");
                }
            }
            writer.println(line);
            writer.flush();

            // recursive traversal
            if (!isLeaf && entry.getChildNodeBlockId() != -1) {
                Node child = FilesHandler.readNode(entry.getChildNodeBlockId(), entry.getChildNodeIndexInBlock());
                traverseAndExport(child, writer);
            }
        }
    }

    public static void main(String[] args) {
        exportMBRsToCSV("index_mbrs.csv");
    }
}
