import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;


/**
 * The {@code EntryComparator} class provides various comparators for sorting {@link Entry} objects
 * based on specific spatial criteria, as required by the R*-Tree algorithms.
 * <p>
 * These comparators are used during node splitting, reinsertion, and other R*-Tree operations.
 */


class EntryComparator {


    /**
     * Comparator for sorting entries by their lower or upper bound along a specific axis (dimension).
     * <p>
     * Used in the R*-Tree split algorithm (ChooseSplitAxis) to order entries by spatial position.
     */


    static class EntryBoundByAxisComparator implements Comparator<Entry> {
        private HashMap<Entry,Double> boundsMap;


        /**
         * Constructs the comparator for a specific axis and bound type.
         *
         * @param entries The entries to compare.
         * @param dimension The axis (dimension) to sort by.
         * @param byUpper If true, compare by upper bounds; otherwise, by lower bounds.
         */


        EntryBoundByAxisComparator(List<Entry> entries, int dimension, boolean byUpper) {
            this.boundsMap = new HashMap<>();
            if (byUpper) {
                for (Entry entry : entries)
                    boundsMap.put(entry, entry.getMBR().getBounds().get(dimension).getUpper());
            } else {
                for (Entry entry : entries)
                    boundsMap.put(entry,entry.getMBR().getBounds().get(dimension).getLower());
            }
        }


        /**
         * Compares two entries based on their bound value along the specified axis.
         *
         * @param a The first entry.
         * @param b The second entry.
         * @return A negative integer, zero, or a positive integer as the first entry is less than,
         * equal to, or greater than the second.
         */


        public int compare(Entry a, Entry b) {
            return Double.compare(boundsMap.get(a),boundsMap.get(b));
        }
    }


    /**
     * Comparator for sorting entries based on their area enlargement when including a given MBR.
     * <p>
     * Used when choosing a leaf to insert a new entry.
     */


    static class AreaEnlargementComparator implements Comparator<Entry> {
        private HashMap<Entry,ArrayList<Double>> enlargementMap;


        /**
         * Constructs the comparator for a given set of entries and a new MBR to include.
         *
         * @param entries The entries to compare.
         * @param mbrToAdd The MBR that will be added to each entry.
         */


        AreaEnlargementComparator(List<Entry> entries, MBR mbrToAdd) {
            this.enlargementMap = new HashMap<>();
            for (Entry entry : entries) {
                MBR entryNewBB = new MBR(Bounds.findMinimumBounds(entry.getMBR(), mbrToAdd));
                ArrayList<Double> values = new ArrayList<>();
                values.add(entry.getMBR().getArea());
                double areaEnlargement = entryNewBB.getArea() - entry.getMBR().getArea();
                if (areaEnlargement < 0)
                    throw new IllegalStateException("Enlargement cannot be a negative number");
                values.add(areaEnlargement);
                enlargementMap.put(entry,values);
            }

        }


        /**
         * Compares two entries based on area enlargement. Ties are broken by comparing current area.
         *
         * @param a The first entry.
         * @param b The second entry.
         * @return A negative integer, zero, or a positive integer based on the comparison.
         */


        @Override
        public int compare(Entry a, Entry b) {
            double areaEnlargementA = enlargementMap.get(a).get(1);
            double areaEnlargementB = enlargementMap.get(b).get(1);
            if (areaEnlargementA == areaEnlargementB)
                return Double.compare(enlargementMap.get(a).get(0),enlargementMap.get(b).get(0));
            else
                return Double.compare(areaEnlargementA,areaEnlargementB);
        }
    }


    /**
     * Comparator for sorting entries based on overlap enlargement when including a new MBR.
     * <p>
     * Used in R*-Tree insertion to reduce overlap between entries.
     */


    static class OverlapEnlargementComparator implements Comparator<Entry> {
        private MBR mbrToAdd;
        private ArrayList<Entry> entries;
        private HashMap<Entry,Double> overlapMap;


        /**
         * Constructs the comparator for a given set of entries, a new MBR, and existing node entries.
         *
         * @param entriesToCompare The entries to compare.
         * @param mbrToAdd The MBR to add.
         * @param entries The current entries in the node (for overlap calculations).
         */


        OverlapEnlargementComparator(List<Entry> entriesToCompare, MBR mbrToAdd, ArrayList<Entry> entries) {
            this.mbrToAdd = mbrToAdd;
            this.entries = entries;
            this.overlapMap = new HashMap<>();

            for (Entry entry : entriesToCompare) {
                double overlapEntry = calculateEntryOverlapValue(entry, entry.getMBR());
                Entry newEntry = new Entry(new MBR(Bounds.findMinimumBounds(entry.getMBR(), mbrToAdd))); // The entry's bounding box after it includes the new bounding box
                double overlapNewEntry = calculateEntryOverlapValue(entry, newEntry.getMBR()); // Using the previous entry signature in order to check for equality
                double overlapEnlargementEntry = overlapNewEntry - overlapEntry ;

                if (overlapEnlargementEntry < 0)
                    throw new IllegalStateException("The enlargement cannot be a negative number");

                overlapMap.put(entry,overlapEnlargementEntry);
            }
        }


        /**
         * Compares two entries based on overlap enlargement. Ties are broken by area enlargement.
         *
         * @param a The first entry.
         * @param b The second entry.
         * @return A negative integer, zero, or a positive integer based on the comparison.
         */



        @Override
        public int compare(Entry a, Entry b) {
            double overlapEnlargementA = overlapMap.get(a);
            double overlapEnlargementB = overlapMap.get(b);

            if (overlapEnlargementA == overlapEnlargementB) {
                ArrayList<Entry> entriesToCompare = new ArrayList<>();
                entriesToCompare.add(a);
                entriesToCompare.add(b);
                return new AreaEnlargementComparator(entriesToCompare, mbrToAdd).compare(a,b);
            } else
                return Double.compare(overlapEnlargementA,overlapEnlargementB);
        }


        /**
         * Calculates the total overlap value of the given entry with all other entries in the node.
         *
         * @param entry The entry for which to calculate overlap.
         * @param mbr The MBR of the entry.
         * @return The total overlap value.
         */


        double calculateEntryOverlapValue(Entry entry, MBR mbr){
            double sum = 0;
            for (Entry nodeEntry : entries) {
                if (nodeEntry != entry)
                    sum += MBR.calculateOverlapValue(mbr,nodeEntry.getMBR());
            }
            return sum;
        }
    }


    /**
     * Comparator for sorting entries based on their distance from the center of a given MBR or point.
     * <p>
     * Used in the reinsertion step of R*-Tree to sort entries by proximity to the node center.
     */


    static class DistanceFromCenterComparator implements Comparator<Entry> {
        private HashMap<Entry,Double> distanceMap;


        /**
         * Constructs the comparator using an MBR as the center.
         *
         * @param entries The entries to compare.
         * @param mbr The reference MBR for distance calculation.
         */


        DistanceFromCenterComparator(List<Entry>entries, MBR mbr) {
            this.distanceMap = new HashMap<>();

            for (Entry entry : entries)
                distanceMap.put(entry, MBR.findDistanceBetweenMBRs(entry.getMBR(), mbr));
        }

        public int compare(Entry a, Entry b) {
            return Double.compare(distanceMap.get(a),distanceMap.get(b));
        }
    }


}