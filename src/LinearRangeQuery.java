import  java.util.ArrayList;
import java.util.List;

public class LinearRangeQuery {
    //Checks if the coordinates are in between the min and max in any dimension
    private static boolean inRange(double[] coords, double[] minCoor, double[] maxCoor){
        for(int i=0; i<coords.length; i++){
            if(coords[i]<minCoor[i] || coords[i]>maxCoor[i]){
                return false;
            }
        }
        return true;
    }

    //Range Query on a Block
    public static List<Records> runLinearQuery(Block block, double[] minCoor, double[] maxCoor){
        List<Records> results = new ArrayList<>();

        for(Records rec : block.getRecordlist()){
            double[] coords = rec.getCoordinates();
            if(inRange(coords, minCoor, maxCoor)){
                results.add(rec);
            }
        }
        return results;
    }
}
