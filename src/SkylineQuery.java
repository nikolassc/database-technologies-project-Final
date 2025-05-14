import java.util.*;

public class SkylineQuery {
    static boolean dominates(Records r1, Records r2){
        boolean strictlyLess = false;

        double[] coords1 = r1.getCoordinates();
        double[] coords2 = r2.getCoordinates();

        for (int i=0; i<coords1.length; i++){
            if (coords1[i] > coords2[i]){
                return false;
            }
            else if (coords1[i] < coords2[i]) {
                strictlyLess = true;
            }
        }

        return strictlyLess;
    }

    static ArrayList<Records> merge(ArrayList<Records> left, ArrayList<Records> right){
        ArrayList<Records> result = new ArrayList<>(left);

        for (Records r: right){
            boolean dominated = false;
            for (Records l: left){
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

    static ArrayList<Records> divideAndConquer(ArrayList<Records> records){
        if (records.size() <= 1){
            return records;
        }

        int mid = records.size()/2;
        ArrayList<Records> leftHalf = divideAndConquer((ArrayList<Records>) records.subList(0,mid));
        ArrayList<Records> rightHalf = divideAndConquer((ArrayList<Records>) records.subList(mid,records.size()));
        return merge(leftHalf, rightHalf);
    }
}
