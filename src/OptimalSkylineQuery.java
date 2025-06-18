import java.io.File;
import java.nio.file.Files;
import java.util.*;


/**
 * {@code OptimalSkylineQuery} class implements a branch-and-bound algorithm for computing the {@code Skyline} of a dataset
 * stored in an {@link RStarTree} index. The algorithm traverses the R*-tree efficiently, pruning dominated entries
 * based on the Minimum Bounding Rectangle ({@link MBR}) sums.
 * <p>
 * The skyline consists of all records that are not dominated by any other record in the dataset. A point
 * dominates another if it is equal or better in all dimensions and strictly better in at least one.
 * </p>
 */


public class OptimalSkylineQuery {


    /**
     * Computes the skyline set from all records stored in the {@link RStarTree}.
     *
     * @return An {@link ArrayList} of {@link Record} objects that represent the skyline points.
     */


     public static ArrayList<Record> computeSkyline(){
         ArrayList<Record> skyline = new ArrayList<>();

         long rootBlockID = RStarTree.getRootNodeBlockId();
         Node root = FilesHandler.readNode(rootBlockID, 0);

         if (root==null) return skyline;

         PriorityQueue<Entry> queue = new PriorityQueue<>(
                 Comparator.comparingDouble(e -> e.getMBR().minSum())
         );

         queue.addAll(root.getEntries());

         while (!queue.isEmpty()){
             Entry e = queue.poll();

             if (e instanceof LeafEntry){
                 LeafEntry le = (LeafEntry) e;
                 long recordsID = le.getDataBlockId();
                 ArrayList<Record> records = FilesHandler.readDataFileBlock(recordsID);
                 for (Record r: records){
                     ArrayList<Double> coords = r.getCoordinates();
                     if (!isDominated(coords, skyline)){
                         skyline.removeIf(s -> dominates(coords, s.getCoordinates()));

                         skyline.add(r);
                     }
                 }
             }
             else {
                 long childBlockID = e.getChildNodeBlockId();
                 int childBlockIndex = e.getChildNodeIndexInBlock();
                 Node childNode = FilesHandler.readNode(childBlockID, childBlockIndex);
                 if (childNode==null) continue;
                 for (Entry child_entry: childNode.getEntries()){
                     queue.add(child_entry);
                 }
             }
         }
         return skyline;
     }


    /**
     * Checks if the candidate point is dominated by any point in the current skyline set.
     *
     * @param candidate The point to check.
     * @param skyline The current skyline set.
     * @return {@code true} if the candidate is dominated by any skyline point, {@code false} otherwise.
     */


     private static boolean isDominated(ArrayList<Double> candidate, ArrayList<Record> skyline){
         for (Record s : skyline) {
             if (dominates(s.getCoordinates(), candidate)) return true;
         }
         return false;
     }


    /**
     * Determines if one point dominates another in all dimensions.
     *
     * @param skylinePoint The potential dominating point.
     * @param candidate The candidate point.
     * @return {@code true} if {@code skylinePoint} dominates {@code candidate}, {@code false} otherwise.
     */


     private static boolean dominates(ArrayList<Double> skylinePoint, ArrayList<Double> candidate){
         boolean strictlyBetterInOne = false;
         for (int i=0; i<skylinePoint.size(); i++){
             if (skylinePoint.get(i)>candidate.get(i)){
                 return false;
             }
             else if (skylinePoint.get(i)<candidate.get(i)){
                 strictlyBetterInOne = true;
             }
         }
         return strictlyBetterInOne;
     }
}
