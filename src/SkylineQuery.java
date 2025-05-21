
import java.util.*;
import java.util.PriorityQueue;
import java.util.Comparator;


public class SkylineQuery {

    public static List<Entry> runSkyline(Node root) {
        List<Entry> skyline = new ArrayList<>();
        PriorityQueue<Entry> queue = new PriorityQueue<>(new EntryAreaComparator());

        queue.addAll(root.getEntries());

        while (!queue.isEmpty()) {
            Entry current = queue.poll();

            if (root.getNodeLevelInTree() == RStarTree.getLeafLevel()) {
                if (!isDominated(current, skyline)) {
                    skyline.add(current);
                }
            } else {
                Long childBlockId = current.getChildNodeBlockId();
                try {
                    Node child = FilesHandler.readIndexFileBlock(childBlockId);
                    if (child != null) {
                        queue.addAll(child.getEntries());
                    } else {
                        System.err.println("⚠️ Could not load node at blockId = " + childBlockId + " → skipping.");
                    }
                } catch (Exception e) {
                    System.err.println("❌ Error loading node at blockId = " + childBlockId + ": " + e.getMessage());
                }
            }
        }

        return skyline;
    }

    private static boolean isDominated(Entry candidate, List<Entry> skyline) {
        for (Entry other : skyline) {
            if (dominates(other.getBoundingBox(), candidate.getBoundingBox())) {
                return true;
            }
        }
        return false;
    }

    private static boolean dominates(MBR a, MBR b) {
        List<Bounds> boundsA = a.getBounds();
        List<Bounds> boundsB = b.getBounds();

        boolean atLeastOneBetter = false;

        for (int i = 0; i < boundsA.size(); i++) {
            double aVal = boundsA.get(i).getLower();
            double bVal = boundsB.get(i).getLower();

            if (aVal > bVal) return false;
            if (aVal < bVal) atLeastOneBetter = true;
        }

        return atLeastOneBetter;
    }
}
