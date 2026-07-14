package org.sigmod.tcc.adjlist;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import junit.framework.TestCase;
import org.sigmod.chronograph.common.EdgeEvent;
import org.sigmod.chronograph.common.TimeInstant;
import org.sigmod.chronograph.memstore.ChronoEdgeEvent;
import org.sigmod.chronograph.memstore.ChronoGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ConnectedComponentTest extends TestCase {

    public void testConnectedComponent() {
        String edgeLabel = "label";
        List<EdgeEvent> edgeEvents = new ArrayList<>();

        HashMap<String, Integer> vertexIdMap = new HashMap<>();
        vertexIdMap.put("a", 0);
        vertexIdMap.put("b", 1);
        vertexIdMap.put("c", 2);
        vertexIdMap.put("d", 3);
        vertexIdMap.put("e", 4);
        vertexIdMap.put("f", 5);


        Graph graph = new ChronoGraph(vertexIdMap);
        Vertex a = graph.addVertex("a");
        Vertex b = graph.addVertex("b");
        Vertex c = graph.addVertex("c");
        Vertex d = graph.addVertex("d");
        Vertex e = graph.addVertex("e");
        Vertex f = graph.addVertex("f");

        Edge edge;
        EdgeEvent edgeEvent;

        edge = graph.addEdge(a, e, edgeLabel);
        edgeEvent = new ChronoEdgeEvent(edge, new TimeInstant(2));
        edgeEvents.add(edgeEvent);

        edge = graph.addEdge(e, a, edgeLabel);
        edgeEvent = new ChronoEdgeEvent(edge, new TimeInstant(2));
        edgeEvents.add(edgeEvent);

        edge = graph.addEdge(e, f, edgeLabel);
        edgeEvent = new ChronoEdgeEvent(edge, new TimeInstant(2));
        edgeEvents.add(edgeEvent);

        edge = graph.addEdge(f, e, edgeLabel);
        edgeEvent = new ChronoEdgeEvent(edge, new TimeInstant(2));
        edgeEvents.add(edgeEvent);

        edge = graph.addEdge(b, c, edgeLabel);
        edgeEvent = new ChronoEdgeEvent(edge, new TimeInstant(2));
        edgeEvents.add(edgeEvent);

        edge = graph.addEdge(c, b, edgeLabel);
        edgeEvent = new ChronoEdgeEvent(edge, new TimeInstant(2));
        edgeEvents.add(edgeEvent);

        ConnectedComponent.compute(edgeEvents).forEach((vertex, component) -> {
            System.out.print(vertex + "\t");
            System.out.println(component);
        });
    }
}
