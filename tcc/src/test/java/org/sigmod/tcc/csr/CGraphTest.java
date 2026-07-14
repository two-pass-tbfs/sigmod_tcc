package org.sigmod.tcc.csr;

import junit.framework.TestCase;

public class CGraphTest extends TestCase {

    public void testCGraph() {
        CGraph graph = new CGraph(3, 4);

        graph.addEdge(2, 1, 2);

        for (int id = 0; id < graph.getVertices().size(); id++) {
            CVertex vertex = graph.getVertex(id);
            System.out.println(vertex);
        }
        for (int i = 0; i < graph.getEdgeCount(); i++) {
            System.out.println(graph.getEdge(i));
        }

        graph.addEdge(0, 1, 1);

        for (int id = 0; id < graph.getVertices().size(); id++) {
            CVertex vertex = graph.getVertex(id);
            System.out.println(vertex);
        }

        System.out.println(graph.getEdgeCount());
        for (int i = 0; i < graph.getEdgeCount(); i++) {
            System.out.println(graph.getEdge(i));
        }
    }
}
