package rstartree.geometry;

import java.util.ArrayList;

public class Point {
    private final ArrayList<Double> coords;

    public Point(ArrayList<Double> coords) {
        this.coords = (ArrayList<Double>) coords.clone();
    }

    public int getDimensions() {
        return coords.size();
    }

    public double getCoord(int i) {
        return coords.get(i);
    }

    public ArrayList<Double> getCoords() {
        return coords;
    }

    public double distanceTo(Point other) {
        if (other.getDimensions() != this.getDimensions()) {
            throw new IllegalArgumentException("Dimension mismatch");
        }
        double sum = 0;
        for (int i = 0; i < coords.size(); i++) {
            double diff = coords.get(i) - other.getCoord(i);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Point(");
        for (int i = 0; i < coords.size(); i++) {
            sb.append(coords.get(i));
            if (i < coords.size() - 1) sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }
}
