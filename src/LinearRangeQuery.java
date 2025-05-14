import  java.util.ArrayList;
import java.util.List;

public class LinearRangeQuery {
    //Checks if the coordinates are in between the min and max in any dimension
    private static boolean inRange(ArrayList<Double> coords, double[] minCoor, double[] maxCoor){
        for(int i=0; i<coords.size(); i++){
            if(coords.get(i) <minCoor[i] || coords.get(i) >maxCoor[i]){
                return false;
            }
        }
        return true;
    }

    //Range Query on a Block
    public static List<Record> runLinearQuery(Block block, double[] minCoor, double[] maxCoor){
        List<Record> results = new ArrayList<>();

        for(Record rec : block.getRecordlist()){
            ArrayList<Double> coords = rec.getCoordinates();
            if(inRange(coords, minCoor, maxCoor)){
                results.add(rec);
            }
        }
        return results;
    }
}
