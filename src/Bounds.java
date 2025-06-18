import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;


/**
 *
 *
 * Public class {@link Bounds} that references the lower and upper bounds of a geographical object in a single dimension.
 *
 *
 */


class Bounds implements Serializable {
    private double lower;
    private double upper;


    /**
     * Constructor method of {@link Bounds}. The lower bound cannot be bigger than the upper.
     *
     * @param lower The lower Bound
     * @param upper The upper Bound
     */


    Bounds(double lower, double upper) {
        if (lower <= upper)
        {
            this.lower = lower;
            this.upper = upper;
        }
        else
            throw new IllegalArgumentException( "The lower value of the bounds cannot be bigger than the upper");
    }


    /**
     * Getter for the lower bound
     * @return The lower bound
     */


    double getLower() {
        return lower;
    }


    /**
     * Getter for the upper bound
     * @return The upper bound
     */


    double getUpper() {
        return upper;
    }


    /**
     * {@code findMinimumBounds} method finds the minimum bounds for each dimension that fit the given entries.
     *
     * @param entries The entries to fit in the {@link Bounds}
     * @return {@link ArrayList} of the minimum bounds
     */


    static ArrayList<Bounds> findMinimumBounds(ArrayList<Entry> entries) {
        ArrayList<Bounds> minimumBounds = new ArrayList<>();
        for (int d = 0; d < FilesHandler.getDataDimensions(); d++)
        {
            Entry lowerEntry = Collections.min(entries, new EntryComparator.EntryBoundByAxisComparator(entries,d,false));
            Entry upperEntry = Collections.max(entries, new EntryComparator.EntryBoundByAxisComparator(entries,d,true));
            minimumBounds.add(new Bounds(lowerEntry.getMBR().getBounds().get(d).getLower(),upperEntry.getMBR().getBounds().get(d).getUpper()));
        }
        return minimumBounds;
    }


    /**
     * {@code findMinimumBounds} method finds the minimum bounds for each dimension that fit the given minimum bounding rectangles.
     *
     * @param MBRA The first {@link MBR}
     * @param MBRB The second {@link MBR}
     * @return {@link ArrayList} of the minimum {@link Bounds}
     */


    static ArrayList<Bounds> findMinimumBounds(MBR MBRA, MBR MBRB) {
        ArrayList<Bounds> minimumBounds = new ArrayList<>();
        // For each dimension finds the minimum interval needed for the entries to fit
        for (int d = 0; d < FilesHandler.getDataDimensions(); d++)
        {
            double lower = Math.min(MBRA.getBounds().get(d).getLower(), MBRB.getBounds().get(d).getLower());
            double upper = Math.max(MBRA.getBounds().get(d).getUpper(), MBRB.getBounds().get(d).getUpper());
            minimumBounds.add(new Bounds(lower,upper));
        }
        return minimumBounds;
    }


    /**
     * {@code findMinimumBoundsFromRecords} method finds the minimum bounds for each dimension that fit the given records
     *
     * @param records The records to fit in the bounds
     * @return {@link ArrayList} of the minimum {@link Bounds}
     */


    public static ArrayList<Bounds> findMinimumBoundsFromRecords(ArrayList<Record> records) {
        int dimensions = FilesHandler.getDataDimensions();
        double[] min = new double[dimensions];
        double[] max = new double[dimensions];
        Arrays.fill(min, Double.POSITIVE_INFINITY);
        Arrays.fill(max, Double.NEGATIVE_INFINITY);

        for (Record r : records) {
            for (int i = 0; i < dimensions; i++) {
                double val = r.getCoordinateFromDimension(i);
                if (val < min[i]) min[i] = val;
                if (val > max[i]) max[i] = val;
            }
        }

        ArrayList<Bounds> bounds = new ArrayList<>();
        for (int i = 0; i < dimensions; i++) {
            bounds.add(new Bounds(min[i], max[i]));
        }
        return bounds;
    }


    /**
     * {@code findMimimumBoundsFromRecord} method finds the minimum bounds for each dimension of a single {@link Record}, used in single insert method
     * @param record The {@link Record} to find the bounds from
     * @return {@link ArrayList} of the {@link Bounds}
     */


    public static ArrayList<Bounds> findMinimumBoundsFromRecord(Record record) {
        ArrayList<Bounds> boundsList = new ArrayList<>();
        for (int i = 0; i < record.getCoordinates().size(); i++) {
            double coord = record.getCoordinateFromDimension(i);
            boundsList.add(new Bounds(coord, coord));
        }
        return boundsList;
    }


}
