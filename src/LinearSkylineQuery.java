import java.util.ArrayList;

public class LinearSkylineQuery {

    public static ArrayList<Record> computeSkyline() {
        ArrayList<Record> skyline = new ArrayList<>();

        // Φόρτωσε όλα τα records από το datafile
        ArrayList<Record> allRecords = new ArrayList<>();
        int totalBlocks = FilesHandler.getTotalBlocksInDataFile();
        for (int i = 1; i < totalBlocks; i++) { // skip block 0 (metadata)
            ArrayList<Record> blockRecords = FilesHandler.readDataFileBlock(i);
            if (blockRecords != null)
                allRecords.addAll(blockRecords);
        }

        // Υπολόγισε το skyline
        for (Record candidate : allRecords) {
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
        }

        return skyline;
    }

    // Επιστρέφει true αν το A κυριαρχεί το B
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
