import java.util.*;
import rstartree.geometry.MBR;
import rstartree.geometry.Point;

public class KNNQuery {

    public static class Entry {
        public Point point;
        public Record record;

        public Entry(Record record) {
            this.record = record;
            this.point = new Point(record.getCoordinates());
        }
    }

    public static class Node {
        public boolean isLeaf;
        public List<Node> children;
        public List<Entry> entries;
        public MBR mbr;

        public Node(boolean isLeaf) {
            this.isLeaf = isLeaf;
            if (isLeaf) entries = new ArrayList<>();
            else children = new ArrayList<>();
        }
    }

    public static List<Record> kNearestNeighbors(Node root, Point query, int k) {
        PriorityQueue<Object[]> queue = new PriorityQueue<>(Comparator.comparingDouble(a -> (Double) a[0]));
        PriorityQueue<Object[]> results = new PriorityQueue<>(Comparator.comparingDouble(a -> -(Double) a[0]));

        queue.add(new Object[]{root.mbr.mindist(query), root});

        while (!queue.isEmpty()) {
            Object[] current = queue.poll();
            double dist = (Double) current[0];

            if (current[1] instanceof Node) {
                Node node = (Node) current[1];
                if (node.isLeaf) {
                    for (Entry e : node.entries) {
                        double d = e.point.distanceTo(query);
                        if (results.size() < k) {
                            results.add(new Object[]{d, e.record});
                        } else if (d < (Double) results.peek()[0]) {
                            results.poll();
                            results.add(new Object[]{d, e.record});
                        }
                    }
                } else {
                    for (Node child : node.children) {
                        queue.add(new Object[]{child.mbr.mindist(query), child});
                    }
                }
            }
        }

        List<Record> knn = new ArrayList<>();
        while (!results.isEmpty()) {
            knn.add((Record) results.poll()[1]);
        }
        Collections.reverse(knn);
        return knn;
    }
}
