import java.util.*;
import rstartree.geometry.Point;
import rstartree.geometry.MBR;

public class KNNCLIExample {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // 1. Ζήτα διαστάσεις
        System.out.print("Δώσε αριθμό διαστάσεων: ");
        int d = scanner.nextInt();

        ArrayList<Double> coords = new ArrayList<>();
        for (int i = 0; i < d; i++) {
            System.out.print("Δώσε τιμή για διάσταση " + (i + 1) + ": ");
            coords.add(scanner.nextDouble());
        }

        Point query = new Point(coords.stream().mapToDouble(Double::doubleValue).toArray());

        System.out.print("Δώσε πόσα k-nearest θέλεις: ");
        int k = scanner.nextInt();

        // 2. Φτιάξε φύλλο Node με Record εγγραφές
        KNNQuery.Node root = new KNNQuery.Node(true);

        root.entries.add(new KNNQuery.Entry(new Record(1, "A", new ArrayList<>(List.of(2.0, 3.0)))));
        root.entries.add(new KNNQuery.Entry(new Record(2, "B", new ArrayList<>(List.of(5.0, 4.0)))));
        root.entries.add(new KNNQuery.Entry(new Record(3, "C", new ArrayList<>(List.of(1.0, 7.0)))));
        root.entries.add(new KNNQuery.Entry(new Record(4, "D", new ArrayList<>(List.of(4.0, 6.0)))));

        root.mbr = computeMBRFromEntries(root.entries);

        // 3. Εκτέλεση ερωτήματος
        List<Record> results = KNNQuery.kNearestNeighbors(root, query, k);

        System.out.println("\nΚοντινότερες εγγραφές στο " + query + ":");
        for (Record rec : results) {
            System.out.println(rec);
        }
    }

    private static MBR computeMBRFromEntries(List<KNNQuery.Entry> entries) {
        int d = entries.get(0).point.getDimensions();
        double[] min = new double[d];
        double[] max = new double[d];
        Arrays.fill(min, Double.POSITIVE_INFINITY);
        Arrays.fill(max, Double.NEGATIVE_INFINITY);

        for (KNNQuery.Entry e : entries) {
            for (int i = 0; i < d; i++) {
                double val = e.point.getCoord(i);
                if (val < min[i]) min[i] = val;
                if (val > max[i]) max[i] = val;
            }
        }
        return new MBR(min, max);
    }
}
