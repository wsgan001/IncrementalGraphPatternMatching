import org.neo4j.driver.v1.*;

import java.util.*;


public class Main {

    Driver driver;

    public static void main(String[] args) {
        Driver driver = GraphDatabase.driver("bolt://localhost:7687",
                AuthTokens.basic("neo4j", "ne skazhu vam svoy parol"));
        Session session = driver.session();

        //создаю паттерн
        Pattern P2 = new Pattern();
        Map<String, Object> temp = new HashMap<>();
        temp.put("company", "CTO");
        PatternNode node = new PatternNode(temp, Integer.toUnsignedLong(0));
        P2.addVertex(temp);
        temp = new HashMap<>();
        temp.put("company", "Bio");
        node = new PatternNode(temp, Integer.toUnsignedLong(1));
        P2.addVertex(temp);
        temp = new HashMap<>();
        temp.put("company", "DB");
        P2.addVertex(temp);
        P2.edges.add(new PatternEdge(P2.vertices.get(2), P2.vertices.get(1)));
        P2.edges.add(new PatternEdge(P2.vertices.get(0), P2.vertices.get(1)));
        P2.edges.add(new PatternEdge(P2.vertices.get(2), P2.vertices.get(0)));
        P2.edges.add(new PatternEdge(P2.vertices.get(0), P2.vertices.get(2)));
      /*  Pattern pattern = new Pattern();
        Map<String, Object> temp = new HashMap<>();
        temp.put("company", "Med");
        PatternNode node = new PatternNode(temp,0);
        pattern.vertices.add(node);
        temp = new HashMap<>();
        temp.put("company", "Bio");
        node = new PatternNode(temp,1);
        pattern.vertices.add(node);
        temp = new HashMap<>();
        temp.put("company", "Soc");
        node = new PatternNode(temp,2);
        pattern.vertices.add(node);
        temp = new HashMap<>();
        temp.put("company","CS");
        node= new PatternNode(temp,3);  pattern.edges.add(new PatternEdge(pattern.vertices.get(1), pattern.vertices.get(1)));
        pattern.edges.add(new PatternEdge(pattern.vertices.get(0),pattern.vertices.get(1)));
        pattern.edges.add(new PatternEdge(pattern.vertices.get(2), pattern.vertices.get(0)));
        pattern.edges.add(new PatternEdge(pattern.vertices.get(0), pattern.vertices.get(2)));*/
       Helper.insertEdge(new PatternEdge(PatternNode
               .toPatternNode(26,driver),PatternNode
             .toPatternNode(30,driver)),driver);
        Helper.deleteEdge(new PatternEdge(PatternNode
               .toPatternNode(28,driver),PatternNode
              .toPatternNode(27,driver)),driver);
        Helper.deleteEdge(new PatternEdge(PatternNode
                .toPatternNode(27,driver),PatternNode
                .toPatternNode(28,driver)),driver);
        Helper.deleteEdge(new PatternEdge(PatternNode
                .toPatternNode(28,driver),PatternNode
                .toPatternNode(32,driver)),driver);
        Helper.deleteEdge(new PatternEdge(PatternNode
                .toPatternNode(27,driver),PatternNode
                .toPatternNode(32,driver)),driver);
        Helper.deleteEdge(new PatternEdge(PatternNode
                .toPatternNode(31,driver),PatternNode
                .toPatternNode(28,driver)),driver);
        Helper.deleteEdge(new PatternEdge(PatternNode
                .toPatternNode(26,driver),PatternNode
                .toPatternNode(29,driver)),driver);

        GraphPatternMatching work = new GraphPatternMatching(P2,driver);
        work.initMat();
        work.initPremv();
        work.initCan();
        //work.initL();
        //work.initMatrix();


        List<Pair<PatternNode,PatternNode>> relationS = work.algorithm(work.listMat,work.listPremv);
        Pattern resultGraph =work.makeResultGraph(relationS,driver);

        /*relationS= work
                .incMatchMinus(P2,relationS,new PatternEdge(PatternNode
                        .toPatternNode(26,driver),PatternNode
                        .toPatternNode(30,driver)));
        Pattern resultGraph1 =work.makeResultGraph(relationS,driver);
        work.initCan();
        relationS= work
                .incMatchPlus(P2,relationS,new PatternEdge(PatternNode
                        .toPatternNode(26,driver),PatternNode
                        .toPatternNode(29,driver)));
        Pattern resultGraph2 =work.makeResultGraph(relationS,driver);*/

        DeltaG deltaGraph = new DeltaG();
        deltaGraph.addEdgeToInsert(31,28,driver);
        deltaGraph.addEdgeToInsert(28,32,driver);
        deltaGraph.addEdgeToInsert(28,27,driver);
        deltaGraph.addEdgeToInsert(27,28,driver);
        deltaGraph.addEdgeToInsert(27,32,driver);
        deltaGraph.addEdgeToInsert(26,29,driver);
        deltaGraph.addEdgeToDelete(26,30,driver);
        relationS= work.incAlgorithm(P2,relationS,deltaGraph);
        Pattern resultGraph1 =work.makeResultGraph(relationS,driver);
        session.close();
        driver.close();
    }
}
