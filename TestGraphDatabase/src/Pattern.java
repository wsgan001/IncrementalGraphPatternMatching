import org.neo4j.driver.v1.*;
import org.neo4j.driver.v1.types.Node;
import java.util.*;
import java.util.stream.Collectors;
import static org.neo4j.driver.v1.Values.parameters;


public class Pattern {
    protected List<PatternNode> vertices = new ArrayList<PatternNode>();
    protected List<PatternEdge> edges = new ArrayList<PatternEdge>();

    public void addVertex(Map<String, Object> node) {
        PatternNode newNode =new PatternNode(node, ( Integer.toUnsignedLong(vertices.size())));
        vertices.add(newNode);
    }

    public  void addEdge (PatternNode leftNode, PatternNode righrNode) {
        PatternEdge newEdge = new PatternEdge(leftNode,righrNode);
        edges.add(newEdge);
    }

    public void addVertex(Map<String, Object> node, Long id) {
        PatternNode newNode =new PatternNode(node, id);
        vertices.add(newNode);
    }
}
