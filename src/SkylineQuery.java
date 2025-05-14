import java.util.*;

public class SkylineQuery {
    static boolean dominates(Record r1, Record r2){
        boolean strictlyLess = false;

        ArrayList<Double> coords1 = r1.getCoordinates();
        ArrayList<Double> coords2 = r2.getCoordinates();

        for (int i=0; i<coords1.size(); i++){
            if (coords1.get(i) > coords2.get(i)){
                return false;
            }
            else if (coords1.get(i) < coords2.get(i)) {
                strictlyLess = true;
            }
        }

        return strictlyLess;
    }

    static ArrayList<Record> merge(ArrayList<Record> left, ArrayList<Record> right){
        ArrayList<Record> result = new ArrayList<>(left);

        for (Record r: right){
            boolean dominated = false;
            for (Record l: left){
                if (dominates(l,r)){
                    dominated = true;
                    break;
                }
            }
            if (!dominated){
                result.add(r);
            }
        }

        return result;
    }

    static ArrayList<Record> divideAndConquer(ArrayList<Record> records){
        if (records.size() <= 1){
            return records;
        }

        int mid = records.size()/2;
        ArrayList<Record> leftHalf = divideAndConquer((ArrayList<Record>) records.subList(0,mid));
        ArrayList<Record> rightHalf = divideAndConquer((ArrayList<Record>) records.subList(mid,records.size()));
        return merge(leftHalf, rightHalf);
    }
}
