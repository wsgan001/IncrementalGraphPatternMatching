import org.neo4j.driver.v1.*;
import org.neo4j.driver.v1.types.Node;
import java.util.*;
import java.util.stream.Collectors;

import static org.neo4j.driver.v1.Values.parameters;

public class Helper {

    static List<Node> findChildrenAsNode(Object id,Driver driver) {
        List<Node> resList = new ArrayList<Node>();
        try (Session session= driver.session()) {
            StatementResult result = session.run( "MATCH (b:Person ) -[a:friend]->(d:Person ) where ID(b)={x}" +
                    "RETURN d as res",
                    parameters("x",id));
            while (result.hasNext()) {
                Record record = result.next();
                resList.add(record.get("res").asNode());
            }
            return resList;
        }
    }

    static List<PatternNode> findChildrenAsPatternNode(Object id,Driver driver) {
        List<PatternNode> resList = new ArrayList<PatternNode>();
        try (Session session= driver.session()) {
            StatementResult result = session.run( "MATCH (b:Person ) -[a:friend]->(d:Person ) where ID(b)={x}" +
                            "RETURN ID(d) as res",
                    parameters("x",id));
            while (result.hasNext()) {
                Record record = result.next();
                resList.add(PatternNode.toPatternNode(record.get("res").asLong(),driver));
            }
            return resList;
        }
    }

    static List<PatternNode> findChildrenInPattern(PatternNode node,Pattern pattern) {
      return pattern.vertices.stream()
                .filter(p->pattern.edges.contains(new PatternEdge(node,p)))
                .collect(Collectors.toList());

    }

    static List<PatternNode> findMatch (PatternNode u,Driver driver) {
        List<PatternNode> matU= new ArrayList<>();
        Set<String> keys = u.node.keySet();
            String str = "MATCH (a:Person) WHERE ";
            String bunch = "";
            for (Iterator<String> it = keys.iterator(); it.hasNext(); ) {
                str += bunch;
                String x = it.next();
                Object y = u.node.get(x);
                String param = " a." + x + "= \"" + y + "\"";
                str += param;
                bunch = " AND";
            }
            str += " RETURN a as res";
            try (Session session = driver.session()) {
                StatementResult result = session.run(str);
                while (result.hasNext()) {
                    Record record = result.next();
                    matU.add(PatternNode.toPatternNode(record.get("res").asNode().id(),driver));
                }
               return  matU;
        }
    }

    static List<Object> convertListPatternNodeToObject (List<PatternNode> nodes) {
        List<Object> res=new ArrayList<>();
        for (PatternNode node: nodes) {
            res.add(node.id);
        }
        return res;
    }

    static boolean hasChild (Node node,Driver driver) {
        try (Session session = driver.session()) {
            StatementResult result = session.run("MATCH (a: Person)-[b:friend] ->(c:Person) WHERE ID(a)={x} RETURN ID(c) AS res",
                    parameters("x", node.id()));
            while (result.hasNext()) {
                return true;
            }
            return false;
        }
    }

    static boolean insertEdge (PatternEdge toBeInserted,Driver driver) {
        try (Session session = driver.session()) {
            StatementResult result = session.run("MATCH (a:Person),(b:Person) \n" +
                    "WHERE ID(a)={x}  AND ID(b)= {y}\n" +
                    "CREATE UNIQUE (a)-[:friend]->(b)",
                    parameters("x", toBeInserted.leftNode.id,
                            "y", toBeInserted.rightNode.id));
            while (result.hasNext()) {
                return true;
            }
        }
        return false;
    }

    static boolean deleteEdge (PatternEdge toBeDeleted,Driver driver) {

        try (Session session = driver.session()) {
            StatementResult result = session.run("MATCH (a:Person)-[b:friend]->(c:Person)\n" +
                            "        WHERE ID(a)={x}  AND ID(c)= {y}\n" +
                            "        DELETE b",
                    parameters("x", toBeDeleted.leftNode.id,
                            "y", toBeDeleted.rightNode.id));
            while (result.hasNext()) {
                return true;
            }
        }
        return false;
    }

}
