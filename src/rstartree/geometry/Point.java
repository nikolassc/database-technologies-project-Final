package rstartree.geometry;

public class Point {
    private final double[] coords;

    public Point(double[] coords) {
        this.coords = coords.clone();
    }

    public int getDimensions() {
        return coords.length;
    }

    public double getCoord(int i) {
        return coords[i];
    }

    public double[] getCoords() {
        return coords.clone();
    }

    public double distanceTo(Point other) {
        if (other.getDimensions() != this.getDimensions()) {
            throw new IllegalArgumentException("Dimension mismatch");
        }
        double sum = 0;
        for (int i = 0; i < coords.length; i++) {
            double diff = coords[i] - other.getCoord(i);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Point(");
        for (int i = 0; i < coords.length; i++) {
            sb.append(coords[i]);
            if (i < coords.length - 1) sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }
}
