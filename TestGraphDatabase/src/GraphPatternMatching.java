import com.sun.org.apache.xerces.internal.xs.datatypes.ObjectList;
import org.neo4j.driver.v1.*;
import org.neo4j.driver.v1.types.Node;
import org.omg.CORBA.Object;

import javax.lang.model.type.NullType;
import java.util.*;
import java.util.stream.Collectors;

import static org.neo4j.driver.v1.Values.parameters;

public class GraphPatternMatching {
    protected Pattern pattern;
    protected Driver driver;
    List<ArrayList<PatternNode>> listMat = new ArrayList<>();
    List<List<PatternNode>> listPremv = new ArrayList<List<PatternNode>>();
    List<Map<Pair<PatternEdge, PatternNode>, Integer>> listMatrix = new ArrayList<>();
    List<Map<PatternNode, List<PatternNode>>> listL = new ArrayList<>();
    List<List<PatternNode>> listCan = new ArrayList<>();

    public GraphPatternMatching(Pattern pattern, Driver driver) {
        this.pattern = pattern;
        this.driver = driver;
    }

    public void initMat() {
        ArrayList<PatternNode> matU;
        Node temp;
        for (int count = 0; count < pattern.vertices.size(); count++) {
            boolean flag = false;
            int dumpCount = count;
            if (!pattern.edges.stream()
                    .filter(p -> p.leftNode == pattern.vertices.get(dumpCount))
                    .collect(Collectors.toList()).isEmpty())
                flag = true;
            matU = new ArrayList<PatternNode>();
            Set<String> keys = pattern.vertices.get(count).node.keySet();
            String str = "MATCH (a:Person) WHERE ";
            String bunch = "";
            for (Iterator<String> it = keys.iterator(); it.hasNext(); ) {
                str += bunch;
                String x = it.next();
                java.lang.Object y = pattern.vertices.get(count).node.get(x);
                String param = " a." + x + "= \"" + y + "\"";
                str += param;
                bunch = " AND";
            }
            str += " RETURN a as res";
            try (Session session = driver.session()) {
                StatementResult result = session.run(str);
                while (result.hasNext()) {
                    Record record = result.next();
                    temp = (record.get("res").asNode());
                    if ((flag && Helper.hasChild(temp, driver)) || ((!flag) && (!Helper.hasChild(temp, driver))))
                        matU.add(PatternNode.toPatternNode(temp.id(), driver));
                }
                listMat.add(matU);
            }
        }
    }

    public void initPremv() {
        List<PatternNode> vertexWithChild = new ArrayList<>();
        try (Session session = driver.session()) {
            StatementResult result = session.run("MATCH (a:Person)-[b: friend]->(c:Person) RETURN DISTINCT a AS res");
            while (result.hasNext()) {
                Record record = result.next();
                vertexWithChild.add(PatternNode.toPatternNode(record.get("res").asNode().id(), driver));
            }
        }
        for (int count = 0; count < pattern.vertices.size(); count++) {
            PatternNode u = pattern.vertices.get((count));
            List<PatternNode> matU = listMat.get(count);

            List<PatternNode> parentU = pattern.vertices.stream()
                    .filter(s -> pattern.edges.contains(new PatternEdge(s, u)))
                    .collect(Collectors.toList());
            List<PatternNode> matParentU = new ArrayList<>();
            for (int i = 0; i < parentU.size(); i++) {
                int index = pattern.vertices.indexOf(parentU.get(i));
                for (int j = 0; j < listMat.get(index).size(); j++)
                    matParentU.add(listMat.get(index).get(j));
            }
//            List<PatternNode> satU = Helper.findMatch(u, driver);
//            List<PatternNode> satParentU = new ArrayList<>();
//            for (PatternNode v : parentU)
//                //for (PatternNode w : Helper.findMatch(v, driver)) satParentU.add(w);
//                for (PatternNode w : listMat.get(pattern.vertices.indexOf(v))) satParentU.add(w);
//
//            List<java.lang.Object> res = new ArrayList<>();
//            try (Session session = driver.session()) {
//                StatementResult result = session.run("MATCH (a:Person)-[b:friend]->(c:Person) WHERE ID(a) IN {x} " +
//                                "AND ID(c) in {y} RETURN DISTINCT a AS res",
//                        parameters("x", Helper.convertListPatternNodeToObject(satParentU), "y", Helper.convertListPatternNodeToObject(satU)));
//                while (result.hasNext()) {
//                    Record record = result.next();
//                    res.add(record.get("res").asNode().id());
//                }
//            }
            List<PatternNode> premv = new ArrayList<>();
            for (PatternNode node: matParentU) {
                List<java.lang.Object> res = new ArrayList<>();
                try (Session session = driver.session()) {
                    StatementResult result = session.run("MATCH (a:Person)-[b:friend]->(c:Person) WHERE ID(a)={x} " +
                                    "AND ID(c) in {y} RETURN DISTINCT a AS res",
                            parameters("x", node.id, "y", Helper.convertListPatternNodeToObject(matU)));
                    while (result.hasNext()) {
                        Record record = result.next();
                        res.add(record.get("res").asNode().id());
                    }
                }
                if (res.isEmpty()) premv.add(node);
            }
            listPremv.add(premv);
        }
    }

    public void initCan() {
        for (int i = 0; i < pattern.vertices.size(); i++)
            listCan.add(i, new ArrayList<>());
        List<PatternNode> canU;

        for (int count = 0; count < pattern.vertices.size(); count++) {
            int dumpCount = count;
            canU = Helper.findMatch(pattern.vertices.get(count), driver);
            canU = canU.stream()
                    .filter(p -> !listMat.get(dumpCount).contains(p))
                    .collect(Collectors.toList());
            for (int i = 0; i < canU.size(); i++)
                if (!listCan.get(count).contains(canU.get(i))) listCan.get(count).add(canU.get(i));
        }
    }



    public Pattern makeResultGraph(List<Pair<PatternNode, PatternNode>> relationS, Driver driver) {
        Pattern resultGraph = new Pattern();
        Pair<PatternNode, PatternNode> maker = new Pair<>();
        for (Pair<PatternNode, PatternNode> pair : relationS)
            resultGraph.addVertex(pair.right.node, pair.right.id);
        for (PatternEdge patternEdge : pattern.edges) {
            List<Pair<PatternNode, PatternNode>> corrLeftNode = relationS.stream()
                    .filter(p -> p.left == patternEdge.leftNode)
                    .collect(Collectors.toList());
            List<Pair<PatternNode, PatternNode>> corrRightNode = relationS.stream()
                    .filter(p -> p.left == patternEdge.rightNode)
                    .collect(Collectors.toList());
            try (Session session = driver.session()) {
                StatementResult result = session.run(
                        "MATCH (a:Person)-[b:friend]->(c:Person) WHERE ID(a) IN {x} AND" +
                                " ID(c) in {y} RETURN ID(a) as v1,ID(c) AS v2",
                        parameters("x", Helper.convertListPatternNodeToObject(maker.getRightElements(corrLeftNode)),
                                "y", Helper.convertListPatternNodeToObject(maker.getRightElements(corrRightNode))));
                while (result.hasNext()) {
                    Record record = result.next();
                    resultGraph.addEdge(PatternNode.toPatternNode(record.get("v1"), driver), PatternNode.toPatternNode(record.get("v2"), driver));
                }
            }
        }
        return resultGraph;
    }

    private List<PatternNode> anc(Pattern pattern, PatternEdge edge, PatternNode node) {
        List<java.lang.Object> satNode = Helper
                .convertListPatternNodeToObject(Helper
                        .findMatch(edge.leftNode, driver));
        List<PatternNode> res = new ArrayList<>();
        try (Session session = driver.session()) {
            StatementResult result = session.run("MATCH (a:Person)-[b:friend]->(c:Person) WHERE ID(a) IN {x} " +
                            "AND ID(c)= {y} RETURN DISTINCT a AS res",
                    parameters("x", satNode, "y", node.id));
            while (result.hasNext()) {
                Record record = result.next();
                res.add(PatternNode.toPatternNode(record.get("res").asNode().id(), driver));
            }
        }
        return res;
    }

    private List<PatternNode> desc(Pattern pattern, PatternEdge edge, PatternNode node) {
        List<java.lang.Object> satNode = Helper
                .convertListPatternNodeToObject(Helper
                        .findMatch(edge.rightNode, driver));
        List<PatternNode> res = new ArrayList<>();
        try (Session session = driver.session()) {
            StatementResult result = session.run("MATCH (a:Person)-[b:friend]->(c:Person) WHERE ID(a) = {x} " +
                            "AND ID(c) IN {y} RETURN c AS res",
                    parameters("x", node.id, "y", satNode));
            while (result.hasNext()) {
                Record record = result.next();
                res.add(PatternNode.toPatternNode(record.get("res").asNode().id(), driver));
            }
        }
        return res;
    }

    public List<Pair<PatternNode, PatternNode>> algorithm(List<ArrayList<PatternNode>> listMat,
                                                          List<List<PatternNode>> listPremv) {

        List<PatternNode> notNullPremv = pattern.vertices.stream()
                .filter(p -> !listPremv.get(pattern.vertices.indexOf(p)).isEmpty())
                .collect(Collectors.toList());
        List<Pair<PatternNode, PatternNode>> relationS = new ArrayList<>();
        while (!notNullPremv.isEmpty()) {
            PatternNode u = notNullPremv.get(0);
            for (PatternEdge edgeToU : pattern.edges.stream()
                    .filter(p -> p.rightNode == u)
                    .collect(Collectors.toList())) {
                PatternNode parentU = edgeToU.leftNode;
                List<PatternNode> matParentU = listMat.get(pattern.vertices.indexOf(parentU));
                List<PatternNode> premvU = listPremv.get(pattern.vertices.indexOf(u));
                List<PatternNode> premvParentU = listPremv.get(pattern.vertices.indexOf(parentU));
                List<PatternNode> premvUCrossMatParentU = premvU.stream()
                        .filter(p -> matParentU.contains(p))
                        .collect(Collectors.toList());
                for (PatternNode z : premvUCrossMatParentU) {
                    matParentU.remove(z);
                    if (matParentU.isEmpty()) return relationS;
                    List<PatternEdge> edgesFromParentU = pattern.edges.stream()
                            .filter(p -> p.rightNode == parentU)
                            .collect(Collectors.toList());
                    for (PatternEdge edgeFromParentU : edgesFromParentU) {
                        List<PatternNode> tempSet = anc(pattern, edgeFromParentU, z);
                        tempSet.removeAll(premvParentU);
                        for (PatternNode l : tempSet) {
                            List<PatternNode> desc = desc(pattern, edgeFromParentU, l);
                            if (desc.stream()
                                    .filter(p -> matParentU.contains(p))
                                    .collect(Collectors.toList()).isEmpty()) {
                                listPremv.get(pattern.vertices.indexOf(parentU)).add(l);
                                if (!notNullPremv.contains(parentU)) notNullPremv.add(parentU);
                            }
                        }
                    }
                }
            }
            listPremv.get(pattern.vertices.indexOf(u)).removeAll(listPremv.get(pattern.vertices.indexOf(u)));
            notNullPremv.remove(pattern.vertices.get(pattern.vertices.indexOf(u)));
        }
        for (PatternNode u : pattern.vertices)
            for (PatternNode x : listMat.get(pattern.vertices.indexOf(u))) relationS.add(new Pair(u, x));
        return relationS;
    }

    private boolean isCS(PatternEdge e, PatternEdge ep) {
        List<PatternNode> canU1 = listCan.get(pattern.vertices.indexOf(ep.leftNode));
        List<PatternNode> matU = listMat.get(pattern.vertices.indexOf(ep.rightNode));
        if (canU1.contains(e.leftNode) && matU.contains(e.rightNode))
            return true;
        else return false;
    }

    private boolean isCC(PatternEdge e, PatternEdge ep) {
        List<PatternNode> canU1 = listCan.get(pattern.vertices.indexOf(ep.leftNode));
        List<PatternNode> canU = listCan.get(pattern.vertices.indexOf(ep.rightNode));
        if (canU1.contains(e.leftNode) && canU.contains(e.rightNode))
            return true;
        else return false;
    }

    private boolean isSS(PatternEdge e, PatternEdge ep) {
        List<PatternNode> matU1 = listMat.get(pattern.vertices.indexOf(ep.leftNode));
        List<PatternNode> matU = listMat.get(pattern.vertices.indexOf(ep.rightNode));
        if (matU1.contains(e.leftNode) && matU.contains(e.rightNode))
            return true;
        else return false;
    }

    private boolean isSC(PatternEdge e, PatternEdge ep) {
        List<PatternNode> matU1 = listMat.get(pattern.vertices.indexOf(ep.leftNode));
        List<PatternNode> canU = listCan.get(pattern.vertices.indexOf(ep.rightNode));
        if (matU1.contains(e.leftNode) && canU.contains(e.rightNode))
            return true;
        else return false;
    }

    public Pattern algorithmInc(Pattern resultGraph, DeltaG deltaGpaph) {
        //deltaGpaph = minDelta(deltaGpaph);

        return resultGraph;
    }

    private DeltaG minDelta(DeltaG deltaGraph) {
        List<PatternEdge> originInsert = new ArrayList<>();
        originInsert.addAll(deltaGraph.insertedEdges);
        List<PatternEdge> originDelete = new ArrayList<>();
        originDelete.addAll(deltaGraph.deletedEdges);
        int count = 0;
        while (count < deltaGraph.insertedEdges.size()) {
            PatternEdge e = deltaGraph.insertedEdges.get(count);
            boolean flag = true;
            for (PatternEdge ep : pattern.edges) {
                if (isCC(e, ep) || isCS(e, ep)) {
                    flag = false;
                    break;
                }
            }
            if (flag) deltaGraph.insertedEdges.remove(e);
            else count++;
        }
        count = 0;
        while (count < deltaGraph.deletedEdges.size()) {
            PatternEdge e = deltaGraph.deletedEdges.get(count);
            boolean flag = true;
            for (PatternEdge ep : pattern.edges) {
                if (isSS(e, ep)) {
                    flag = false;
                    break;
                }
            }
            if (flag) deltaGraph.deletedEdges.remove(e);
            else count++;
        }
        for (PatternEdge e : pattern.edges) {
            if (!originInsert.stream()
                    .filter(p -> isSS(p, e))
                    .collect(Collectors.toList()).isEmpty() &&
                    !originDelete.stream()
                            .filter(p -> isSS(p, e))
                            .collect(Collectors.toList()).isEmpty()) {
                deltaGraph.insertedEdges.removeAll(deltaGraph.insertedEdges.stream()
                        .filter(p -> isSS(p, e))
                        .collect(Collectors.toList()));
                deltaGraph.deletedEdges.removeAll(deltaGraph.deletedEdges.stream()
                        .filter(p -> isSS(p, e))
                        .collect(Collectors.toList()));
            }
        }
        for (PatternEdge e : pattern.edges) {
            if (!originInsert.stream()
                    .filter(p -> isCS(p, e))
                    .collect(Collectors.toList()).isEmpty() &&
                    !originDelete.stream()
                            .filter(p -> isCS(p, e))
                            .collect(Collectors.toList()).isEmpty()) {
                deltaGraph.insertedEdges.removeAll(deltaGraph.insertedEdges.stream()
                        .filter(p -> isCS(p, e))
                        .collect(Collectors.toList()));
                deltaGraph.deletedEdges.removeAll(deltaGraph.deletedEdges.stream()
                        .filter(p -> isCS(p, e))
                        .collect(Collectors.toList()));
            }
        }
        for (PatternEdge insertedEdge : deltaGraph.insertedEdges) {
            List<PatternEdge> toDelete = new ArrayList<>(deltaGraph.insertedEdges.size());
            toDelete.addAll(deltaGraph.insertedEdges);
            toDelete.remove(insertedEdge);
            toDelete = toDelete.stream()
                    .filter(p -> p == insertedEdge)
                    .collect(Collectors.toList());
            deltaGraph.insertedEdges.removeAll(toDelete);
        }
        for (PatternEdge deletedEdge : deltaGraph.deletedEdges) {
            List<PatternEdge> toDelete = new ArrayList<>();
            toDelete.addAll(deltaGraph.deletedEdges);
            toDelete.remove(deletedEdge);
            toDelete = toDelete.stream()
                    .filter(p -> p == deletedEdge)
                    .collect(Collectors.toList());
            deltaGraph.deletedEdges.removeAll(toDelete);
        }
        return deltaGraph;
    }


    private List<Pair<PatternNode, PatternNode>> incMatchMinus(Pattern pattern,
                                                               List<Pair<PatternNode, PatternNode>> relationS,
                                                               //  Map<Pair<PatternNode,PatternNode>,Integer> distanceMatrix,
                                                               PatternEdge toBeDeleted) {

        Stack<Pair<PatternNode, PatternNode>> wSet = new Stack<>();
        for (PatternEdge ep : pattern.edges.stream()
                .filter(p -> isSS(toBeDeleted, p))
                .collect(Collectors.toList())) {
            List<PatternNode> matU = listMat.get(pattern.vertices.indexOf(ep.rightNode));

            if (desc(pattern, ep, toBeDeleted.leftNode).stream()
                    .filter(p -> (//!p.equals(toBeDeleted.rightNode) &&
                            matU.contains(p)))
                    .collect(Collectors.toList()).isEmpty()) {
                wSet.push(new Pair(ep.leftNode, toBeDeleted.leftNode));
                while (!wSet.empty()) {
                    Pair<PatternNode, PatternNode> item = wSet.pop();
                    List<PatternNode> matU1 = listMat.get(pattern.vertices.indexOf(item.left));
                    matU1.remove(item.right);
                    relationS.remove(item);
                    for (PatternEdge ep1 : pattern.edges.stream()
                            .filter(p -> p.rightNode == item.left)
                            .collect(Collectors.toList())) {
                        List<PatternNode> matU11 = listMat.get(pattern.vertices.indexOf(ep1.leftNode));
                        for (PatternNode v11 : anc(pattern, ep1, item.right).stream()
                                .filter(p -> matU11.contains(p))
                                .collect(Collectors.toList())) {
                            if (desc(pattern, ep1, v11).stream().filter(p -> matU1.contains(p))
                                    .collect(Collectors.toList()).isEmpty())
                                wSet.push(new Pair(ep1.leftNode, v11));
                        }
                    }
                }
            }
        }
        boolean flag = false;
        for (int i = 0; i < pattern.vertices.size(); i++)
            if (listMat.get(i).isEmpty()) {
                flag = true;
                break;
            }
        if (flag) relationS.removeAll(relationS);
        return relationS;
    }

    private List<Pair<PatternNode, PatternNode>> incMatchPlus(Pattern pattern,
                                                              List<Pair<PatternNode, PatternNode>> relationS,
                                                              //  Map<Pair<PatternNode,PatternNode>,Integer> distanceMatrix,
                                                              PatternEdge toBeInserted) {

        Stack<Pair<PatternNode, PatternNode>> wSet = new Stack<>();
        for (PatternEdge ep : pattern.edges.stream()
                .filter(p -> isCS(toBeInserted, p))
                .collect(Collectors.toList())) {
            if (pattern.edges.stream().filter(p -> p.leftNode == ep.leftNode).allMatch(edge -> {
                List<PatternNode> matU = listMat.get(pattern.vertices.indexOf(edge.rightNode));
                return !desc(pattern, edge, toBeInserted.leftNode).stream()
                        .filter(p -> matU.contains(p))
                        .collect(Collectors.toList()).isEmpty();
            })) {
                wSet.push(new Pair(ep.leftNode, toBeInserted.leftNode));
                while (!wSet.empty()) {
                    Pair<PatternNode, PatternNode> item = wSet.pop();
                    List<PatternNode> matU1 = listMat.get(pattern.vertices.indexOf(item.left));
                    if (!matU1.contains((item.right))) matU1.add(item.right);
                    List<PatternNode> canU1 = listCan.get(pattern.vertices.indexOf(item.left));
                    if (!canU1.contains((item.right))) canU1.add(item.right);
                    if (!relationS.contains((item))) relationS.add(item);
                    for (PatternEdge edge : pattern.edges.stream()
                            .filter(p -> p.rightNode == item.left)
                            .collect(Collectors.toList())) {
                        List<PatternNode> matU11 = listMat.get(pattern.vertices.indexOf(edge.leftNode));
                        for (PatternNode node : anc(pattern, edge, item.right).stream()
                                .filter(p -> matU11.contains(p))
                                .collect(Collectors.toList())) {
                            if (pattern.edges.stream().filter(p -> p.leftNode == edge.leftNode).allMatch(edge1 -> {
                                List<PatternNode> canU = listCan.get(pattern.vertices.indexOf(edge1.rightNode));
                                return !desc(pattern, edge1, node).stream()
                                        .filter(p -> canU.contains(p))
                                        .collect(Collectors.toList()).isEmpty();
                            }))
                                wSet.push(new Pair(edge.leftNode, node));
                        }
                    }
                }
            }

        }

        return relationS;
    }

    public List<Pair<PatternNode, PatternNode>> incAlgorithm(Pattern pattern,
                                                             List<Pair<PatternNode, PatternNode>> relationS,
                                                             DeltaG deltaGraph) {

      initCan();
        for (PatternEdge e : deltaGraph.insertedEdges)
            Helper.insertEdge(e, driver);
        for (PatternEdge e : deltaGraph.deletedEdges)
            Helper.deleteEdge(e, driver);


        deltaGraph = minDelta(deltaGraph);


        for (PatternEdge deletedEdge : deltaGraph.deletedEdges) {
            relationS = incMatchMinus(pattern, relationS, deletedEdge);
        }
        for (PatternEdge insertedEdge : deltaGraph.insertedEdges) {
            relationS = incMatchPlus(pattern, relationS, insertedEdge);
        }


        return relationS;
    }
}




