import  java.util.ArrayList;


/**
 * Executes a linear (brute-force) range query over the records stored in the datafile.
 * <p>
 * It scans all records and returns those whose coordinates lie within a specified {@link MBR} (Minimum Bounding Rectangle).
 * <p>
 * This implementation does not use any index structure (like an R*-Tree); it performs a full scan of the data blocks.
 */


public class LinearRangeQuery {


    /**
     * Checks if the given coordinates are within the provided minimum and maximum {@link Bounds} for all dimensions.
     *
     * @param coords The coordinates of a {@link Record}.
     * @param minCoor An array of minimum {@link Bounds} for each dimension.
     * @param maxCoor An array of maximum {@link Bounds} for each dimension.
     * @return {@code true} if the coordinates lie within the {@link Bounds} in all dimensions; {@code false} otherwise.
     */


    private static boolean inRange(ArrayList<Double> coords, double[] minCoor, double[] maxCoor) {
        for (int i = 0; i < coords.size(); i++) {
            if (coords.get(i) < minCoor[i] || coords.get(i) > maxCoor[i]) {
                return false;
            }
        }
        return true;
    }


    /**
     * Executes a linear range query by scanning all records in the datafile.
     * <p>
     * Returns all {@link Record}s whose coordinates fall within the given {@link MBR}.
     *
     * @param queryMBR The query {@link MBR} that defines the range of interest.
     * @return A list of {@link Record}s that lie within the specified range.
     */


    public static ArrayList<Record> runLinearRangeQuery(MBR queryMBR){
        ArrayList<Record> results = new ArrayList<>();

        int totalBlocks = FilesHandler.getTotalBlocksInDataFile();
        ArrayList<Bounds> boundsList = queryMBR.getBounds();

        int dimensions = FilesHandler.getDataDimensions();
        double[] minCoor = new double[dimensions];
        double[] maxCoor = new double[dimensions];

        for (int i = 0; i < dimensions; i++) {
            Bounds b = boundsList.get(i);
            minCoor[i]= b.getLower();
            maxCoor[i]= b.getUpper();
        }

        for(int blockId=1; blockId<totalBlocks; blockId++){
            ArrayList<Record> records = FilesHandler.readDataFileBlock(blockId);
            if(records == null) continue;

            for(Record rec : records){
                if(inRange(rec.getCoordinates(), minCoor, maxCoor)){
                    results.add(rec);
                }
            }
        }
        return results;
    }
}
