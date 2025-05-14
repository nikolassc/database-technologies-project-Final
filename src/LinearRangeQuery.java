
import  java.util.ArrayList;
import java.util.List;


public class LinearRangeQuery {

    //Checks if the coordinates are in between the min and max in any dimensions
    private static boolean inRange(ArrayList<Double> coords, double[] minCoor, double[] maxCoor){
        for(int i=0; i<coords.size(); i++){
            if(coords.get(i) < minCoor[i] || coords.get(i) > maxCoor[i]){
                return false;
            }
        }
        return true;
    }

    //Performs a linear scan of the entire data file
    public static List<Record> runLinearQuery(double[] minCoor, double[] maxCoor){
        List<Record> results = new ArrayList<>();

        int totalBlocks = FilesHandler.getTotalBlocksInDataFile();
        for(int blockId=0; blockId<totalBlocks; blockId++){
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
