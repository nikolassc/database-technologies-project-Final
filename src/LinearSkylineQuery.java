import java.util.ArrayList;



/**
 * Class for executing a linear (brute-force) Skyline Query over the records stored in the datafile.
 * <p>
 * This method loads all records into memory and compares them pairwise to determine the set of {@code Skyline} records.
 * A record is part of the Skyline if it is not dominated by any other record in the dataset.
 * <p>
 * <b>Definition of Domination:</b> A record {@code a} dominates another record {@code b} if {@code a} is no worse than {@code b}
 * in all dimensions and strictly better in at least one dimension.
 * <p>
 * The query scans the entire dataset, so it is not optimized for large datasets.
 */


public class LinearSkylineQuery {


    /**
     * Computes and returns the {@code Skyline} of all {@link Record}s stored in the datafile.
     * <p>
     * The {@code Skyline} is computed by comparing each {@link Record} to all others and retaining those that are not dominated.
     *
     * @return A list of {@link Record} objects representing the Skyline points.
     */


    public static ArrayList<Record> computeSkyline() {
        ArrayList<Record> skyline = new ArrayList<>();

        System.out.println("Calculating Linear Skyline...");
        long startTime = System.currentTimeMillis();

        ArrayList<Record> allRecords = new ArrayList<>();
        int totalBlocks = FilesHandler.getTotalBlocksInDataFile();
        for (int i = 1; i < totalBlocks; i++) {
            ArrayList<Record> blockRecords = FilesHandler.readDataFileBlock(i);
            if (blockRecords != null)
                allRecords.addAll(blockRecords);
        }

        int total = allRecords.size();
        System.out.println("Total records loaded: " + total);
        System.out.println();

        for (int i = 0; i < total; i++) {
            Record candidate = allRecords.get(i);
            boolean dominated = false;
            for (Record other : allRecords) {
                if (dominates(other, candidate)) {
                    dominated = true;
                    break;
                }
            }
            if (!dominated) {
                skyline.add(candidate);
            }

            // Progress Bar
            if ((i + 1) % 1000 == 0 || i + 1 == total) {
                long now = System.currentTimeMillis();
                long elapsed = now - startTime;
                double progress = (100.0 * (i + 1)) / total;
                System.out.printf("Checked %d/%d records (%.2f%%) - Elapsed: %d ms%n",
                        i + 1, total, progress, elapsed);
            }
        }

        return skyline;
    }


    /**
     * Determines whether {@link Record} {@code a} dominates {@link Record} {@code b}.
     * <p>
     * A record {@code a} dominates another record {@code b} if for all dimensions:
     * <ul>
     *     <li>{@code a}'s coordinate is less than or equal to {@code b}'s coordinate</li>
     *     <li>and {@code a} is strictly better (lower) in at least one dimension</li>
     * </ul>
     *
     * @param a The record that is tested as the potential dominator.
     * @param b The record that is tested as the potential dominated record.
     * @return {@code true} if {@code a} dominates {@code b}; {@code false} otherwise.
     */


    private static boolean dominates(Record a, Record b) {
        ArrayList<Double> coordsA = a.getCoordinates();
        ArrayList<Double> coordsB = b.getCoordinates();
        boolean strictlyBetterInOne = false;

        for (int i = 0; i < coordsA.size(); i++) {
            if (coordsA.get(i) > coordsB.get(i)) {
                return false;
            } else if (coordsA.get(i) < coordsB.get(i)) {
                strictlyBetterInOne = true;
            }
        }

        return strictlyBetterInOne;
    }
}
