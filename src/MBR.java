import java.io.Serializable;
import java.util.ArrayList;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;


/**
 * Public class {@link MBR} represents a minimum bounding rectangle in a n-dimensional space.
 * <P></P> Works for n dimensions because an {@link ArrayList} of {@link Bounds} for each dimension is stored in the class
 */


class MBR implements Serializable {
    private ArrayList<Bounds> bounds; // The bounds for each dimension
    private Double area; // The total area that the MBR covers
    private Double margin; // The MBR's total margin
    private ArrayList<Double> center; // Represents the coordinates of the center of the MBR


    /**
     * {@link MBR} constructor that takes an {@link ArrayList} of {@link Bounds} as parameter.
     * @param bounds {@link ArrayList} of {@link Bounds}
     */


    MBR(ArrayList<Bounds> bounds) {
        this.bounds = bounds;
        this.area = calculateArea();
        this.margin = calculateMargin();
        this.center = getCenter();
    }


    /**
     * Getter for the {@link ArrayList} of {@link Bounds}
     * @return {@link ArrayList} of {@link Bounds}
     */


    ArrayList<Bounds> getBounds() {
        return bounds;
    }


    /**
     * Getter for the {@link MBR}'s {@code area}
     * @return {@link MBR}'s {@code area}
     */


    double getArea() {
        if (area == null)
            area = calculateArea();

        return area;
    }


    /**
     * Getter for the {@link MBR}'s {@code margin}
     * @return {@link MBR}'s {@code margin}
     */


    double getMargin() {
        if (margin == null)
            margin = calculateMargin();

        return margin;
    }


    /**
     * Getter for the {@link MBR}'s {@code center}
     * @return {@link MBR}'s {@code center}
     */


    public ArrayList<Double> getCenter() {
        if (center == null)
        {
            center = new ArrayList<>();

            for (int d = 0; d < FilesHandler.getDataDimensions(); d++)
                center.add((bounds.get(d).getUpper()+bounds.get(d).getLower())/2);
        }
        return center;
    }


    /**
     * {@code findMinDistanceFromPoint} method returns the minimum distance between the {@link MBR} and the given {@code point}
     * @param point The given point
     * @return The minimum distance
     */


    double findMinDistanceFromPoint(ArrayList<Double> point){
        double minDistance = 0;
        double rd;
        for (int d = 0; d < FilesHandler.getDataDimensions(); d++)
        {
            if(getBounds().get(d).getLower() > point.get(d))
                rd = getBounds().get(d).getLower();
            else if (getBounds().get(d).getUpper() < point.get(d))
                rd = getBounds().get(d).getUpper();
            else
                rd = point.get(d);

            minDistance += Math.pow(point.get(d) - rd,2);
        }
        return sqrt(minDistance);
    }


    /**
     * Calculates and returns the {@link MBR}'s {@code margin}
     * @return The {@link MBR}'s {@code margin}
     */


    private double calculateMargin() {
        double sum = 0;
        for (int d = 0; d < FilesHandler.getDataDimensions(); d++)
            sum += abs(bounds.get(d).getUpper() - bounds.get(d).getLower());
        return sum;
    }


    /**
     * Calculates and returns the {@link MBR}'s {@code area}
     * @return The {@link MBR}'s {@code area}
     */


    private double calculateArea() {
        double productOfEdges = 1;
        for (int d = 0; d < FilesHandler.getDataDimensions(); d++)
            productOfEdges = productOfEdges * (bounds.get(d).getUpper() - bounds.get(d).getLower());
        return abs(productOfEdges);
    }


    /**
     * Checks if two given MBRs overlap
     * @param MBRA The first {@link MBR}
     * @param MBRB The second {@link MBR}
     * @return {@code true} if MBRs overlap, else {@code false}
     */


    static boolean checkOverlap(MBR MBRA, MBR MBRB) {
        for (int d = 0; d < FilesHandler.getDataDimensions(); d++)
        {
            double overlapD = Math.min(MBRA.getBounds().get(d).getUpper(), MBRB.getBounds().get(d).getUpper())
                    - Math.max(MBRA.getBounds().get(d).getLower(), MBRB.getBounds().get(d).getLower());

            if (overlapD < 0)
                return false;
        }
        return true;
    }


    /**
     * Calculates and returns the given MBRs {@code overlapValue}
     * @param MBRA The first {@link MBR}
     * @param MBRB The second {@link MBR}
     * @return The two MBRs {@code overlapValue}
     */


    static double calculateOverlapValue(MBR MBRA, MBR MBRB) {
        double overlapValue = 1;
        for (int d = 0; d < FilesHandler.getDataDimensions(); d++)
        {
            double overlapD = Math.min(MBRA.getBounds().get(d).getUpper(), MBRB.getBounds().get(d).getUpper())
                    - Math.max(MBRA.getBounds().get(d).getLower(), MBRB.getBounds().get(d).getLower());

            if (overlapD <= 0)
                return 0;
            else
                overlapValue = overlapD*overlapValue;
        }
        return overlapValue;
    }


    /**
     * Calculates and returns the distance between the centers of the two given MBRs
     * @param MBRA The first {@link MBR}
     * @param MBRB The second {@link MBR}
     * @return The distance between the two centers
     */


    static double findDistanceBetweenMBRs(MBR MBRA, MBR MBRB) {
        double distance = 0;
        // For every dimension find the intersection point
        for (int d = 0; d < FilesHandler.getDataDimensions(); d++)
        {
            distance += Math.pow(MBRA.getCenter().get(d) - MBRB.getCenter().get(d),2);
        }
        return sqrt(distance);
    }


    /**
     * Calculates the sum of the lower {@link Bounds} in each dimension
     * <p></p> Used as priority metric for {@link OptimalSkylineQuery}
     * @return The minimum sum
     */


    public double minSum(){
        double sum = 0.0;
        for (Bounds b: this.bounds){
            sum += b.getLower();
        }
        return sum;
    }
}