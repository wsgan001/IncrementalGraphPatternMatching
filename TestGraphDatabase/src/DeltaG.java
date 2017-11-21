import java.sql.Driver;
import java.util.ArrayList;
import java.util.List;

public class DeltaG {
    protected List<PatternEdge> deletedEdges;
    protected List<PatternEdge> insertedEdges;
    protected List<PatternNode> deletedNodes;
    protected List<PatternNode> insertedNodes;

    public DeltaG () {
        deletedEdges = new ArrayList<>();
        insertedEdges = new ArrayList<>();
    }

    public void addEdgeToInsert (PatternEdge toInsert) {
        insertedEdges.add(toInsert);
    }

    public void addEdgeToInsert (Object idFrom, Object idTo, org.neo4j.driver.v1.Driver driver) {
        PatternEdge toInsert = new PatternEdge(PatternNode
                .toPatternNode(idFrom,driver),PatternNode
                .toPatternNode(idTo,driver));
        insertedEdges.add(toInsert);
    }
    public void addEdgeToDelete (PatternEdge toDelete) {
        deletedEdges.add(toDelete);
    }
    public void addEdgeToDelete (Object idFrom, Object idTo, org.neo4j.driver.v1.Driver driver) {
        PatternEdge toDelete = new PatternEdge(PatternNode
                .toPatternNode(idFrom,driver),PatternNode
                .toPatternNode(idTo,driver));
        deletedEdges.add(toDelete);
    }
}
