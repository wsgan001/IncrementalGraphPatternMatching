import org.neo4j.driver.v1.*;
import org.neo4j.driver.v1.types.Node;

import java.util.*;

import static org.neo4j.driver.v1.Values.parameters;

public class PatternNode {

    protected Map<String, Object> node;
    protected Long id;

    public PatternNode(Map<String, Object> node, Long id) {
        this.node = node;
        this.id = id;
    }

    public static PatternNode toPatternNode(Object node,Driver driver) {
        Map<String,Object> atributes = new HashMap<>();
        try (Session session= driver.session()){
            StatementResult result = session.run("MATCH (a:Person) WHERE ID(a)={x} " +
                            "RETURN a as res",
                    parameters("x", node));
            while (result.hasNext()) {
                Record record = result.next();
                atributes.putAll(record.get("res").asMap());
//                Map<String,Object> atributes1 = record.get("res").asMap();
//                for (Map.Entry entry: atributes1.entrySet()) {
//                    String key = (String)entry.getKey();
//                    Long value = (Long)entry.getValue();
//                    atributes.put(key, value);
//                }
            }
        }
        return new PatternNode(atributes, Long.decode(node.toString()));
    }

    @Override
    public boolean equals(Object other){
        if (other == null) return false;
        if (other == this) return true;
        if (!(other instanceof PatternNode))return false;
        PatternNode otherMyClass = (PatternNode)other;
        return otherMyClass.id == id;
    }
}