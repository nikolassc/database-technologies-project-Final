package rstartree.geometry;

import rstartree.geometry.Point;

public class MBR {
    public double[] min;
    public double[] max;

    public MBR(double[] min, double[] max) {
        this.min = min.clone();
        this.max = max.clone();
    }

    public double mindist(Point p) {
        double dist = 0;
        for (int i = 0; i < p.getDimensions(); i++) {
            double r;
            if (p.getCoord(i) < min[i]) r = min[i];
            else if (p.getCoord(i) > max[i]) r = max[i];
            else r = p.getCoord(i);
            dist += Math.pow(p.getCoord(i) - r, 2);
        }
        return Math.sqrt(dist);
    }

    public boolean intersects(MBR other) {
        for (int i = 0; i < min.length; i++) {
            if (this.max[i] < other.min[i] || this.min[i] > other.max[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("MBR(");
        for (int i = 0; i < min.length; i++) {
            sb.append("[").append(min[i]).append(", ").append(max[i]).append("]");
            if (i < min.length - 1) sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }
}
