import java.io.Serializable;
import java.util.ArrayList;


/**
 *
 *
 * Public class {@link IndexBlock} that refers to a block object in the index file.
 * <p></p>
 * The {@link IndexBlock} keeps an {@link ArrayList} of {@link Node} that it contains, so multiple nodes fit in one block.
 *
 *
 */
public class IndexBlock implements Serializable {
    private static final int MAX_NODES_PER_BLOCK = 48;
    private ArrayList<Node> nodes;


    /**
     * {@link IndexBlock} constructor that initializes an index block with an empty {@link Node} {@link ArrayList}
     */


    IndexBlock() {
        this.nodes = new ArrayList<>();
    }


    /**
     * Getter for the {@link Node} {@link ArrayList}
     * @return The {@link Node} {@link ArrayList}
     */


    ArrayList<Node> getNodes() {
        return nodes;
    }


    /**
     * {@code hasSpace} method checks if the {@link IndexBlock} has space left
     * @return {@code true} if there is space left, else {@code false}
     */


    boolean hasSpace() {
        return nodes.size() < MAX_NODES_PER_BLOCK;
    }


    /**
     * {@code addNode} method that adds a new {@link Node} in the {@link IndexBlock}
     * @param node The {@link Node} to be added
     */


    void addNode(Node node) {
        if (!nodes.contains(node)) nodes.add(node);
    }

}