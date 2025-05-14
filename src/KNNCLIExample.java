import java.util.*;
import rstartree.geometry.Point;

public class KNNCLIExample {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // 1. Ρώτησε χρήστη για πλήθος διαστάσεων
        System.out.print("Δώσε αριθμό διαστάσεων: ");
        int d = scanner.nextInt();

        double[] coords = new double[d];
        for (int i = 0; i < d; i++) {
            System.out.print("Δώσε τιμή για διάσταση " + (i + 1) + ": ");
            coords[i] = scanner.nextDouble();
        }

        Point query = new Point(coords);

        System.out.print("Δώσε πόσα k-nearest θέλεις: ");
        int k = scanner.nextInt();

        // 2. Φτιάξε ένα απλό φύλλο Node με εγγραφές για δοκιμή
        KNNQuery.Node root = new KNNQuery.Node(true);

        root.entries.add(new KNNQuery.Entry(new Records("1", "A", new double[]{1.0, 2.0})));
        root.entries.add(new KNNQuery.Entry(new Records("2", "B", new double[]{4.0, 3.0})));
        root.entries.add(new KNNQuery.Entry(new Records("3", "C", new double[]{5.0, 5.0})));
        root.entries.add(new KNNQuery.Entry(new Records("4", "D", new double[]{7.0, 2.0})));

        root.mbr = computeMBRFromEntries(root.entries);

        // 3. Εκτέλεση k-NN
        List<Records> results = KNNQuery.kNearestNeighbors(root, query, k);

        // 4. Εκτύπωση αποτελεσμάτων
        System.out.println("\nΚοντινότερες εγγραφές στο " + Arrays.toString(coords) + ":");
        for (Records rec : results) {
            System.out.println(rec);
        }
    }

    private static rstartree.geometry.MBR computeMBRFromEntries(List<KNNQuery.Entry> entries) {
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
        return new rstartree.geometry.MBR(min, max);
    }
}
