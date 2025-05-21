import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class IndexToCSVExporter {

    public static void exportMBRsToCSV(String outputPath) {
        try {
            FileWriter writer = new FileWriter(outputPath);
            writer.write("nodeBlockId,nodeLevel,entryIndex,x_min,x_max,y_min,y_max\n");

            Map<Long, Boolean> visited = new HashMap<>();
            Node root = FilesHandler.readIndexFileBlock(RStarTree.getRootNodeBlockId());
            traverseAndExport(root, visited, writer);

            writer.close();
            System.out.println("✅ MBRs exported to: " + outputPath);
        } catch (Exception e) {
            System.err.println("❌ Error exporting MBRs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void traverseAndExport(Node node, Map<Long, Boolean> visited, FileWriter writer) throws Exception {
        if (node == null || visited.containsKey(node.getNodeBlockId())) return;
        visited.put(node.getNodeBlockId(), true);

        ArrayList<Entry> entries = node.getEntries();
        int level = node.getNodeLevelInTree();

        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            MBR mbr = entry.getBoundingBox();
            ArrayList<Bounds> bounds = mbr.getBounds();

            double xMin = bounds.get(0).getLower();
            double xMax = bounds.get(0).getUpper();
            double yMin = bounds.get(1).getLower();
            double yMax = bounds.get(1).getUpper();

            writer.write(String.format(
                    "%d,%d,%d,%.6f,%.6f,%.6f,%.6f\n",
                    node.getNodeBlockId(), level, i, xMin, xMax, yMin, yMax
            ));

            if (level > RStarTree.getLeafLevel()) {
                Node child = FilesHandler.readIndexFileBlock(entry.getChildNodeBlockId());
                traverseAndExport(child, visited, writer);
            }
        }
    }

    public static void main(String[] args) {
        exportMBRsToCSV("index_mbrs.csv");
    }
}
