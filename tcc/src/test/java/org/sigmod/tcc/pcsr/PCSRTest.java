package org.sigmod.tcc.pcsr;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

public class PCSRTest extends TestCase {
    Logger log = Logger.getLogger(PCSRTest.class.getName());

    public void testPCSRSetup() {
        PCSR pcsr;
        int count;

        log.info("Initialize PCSR with size 5");
        pcsr = new PCSR(5);
        log.info("\n" + pcsr);
        pcsr.debugEdges();

        assertEquals(PEdge.NULL_EDGE, pcsr.getEdge(0, 1));

        // ----- Add edges -----
        log.info("\nAdding edges...");
        count = 1;
//        pcsr.addEdge(3, 4, count);
//        log.info("\n" + pcsr);
//        pcsr.debugEdges();

        for (int i = 0; i < 5; i++) {
            for (int j = i + 1; j < 5; j++) {
                if (i == j)
                    continue;

                pcsr.addEdge(i, j, PEdge.EDGE_MARKER);
                count++;
            }
            log.info("Adding v(" + i + ")");
            log.info("\n" + pcsr);
            pcsr.debugEdges();
        }
        assertEquals(new PEdge(4, PEdge.EDGE_MARKER), pcsr.getEdge(3, 4));
        assertEquals(new PEdge(3, PEdge.EDGE_MARKER), pcsr.getEdge(4, 3));

        log.info("\n" + pcsr);
        pcsr.debugEdges();

//        pcsr.addEdge(3, 4, 100);
//        log.info("\n" + pcsr);
//        assertEquals(new Edge(4, 100), pcsr.getEdge(3, 4));
//        assertEquals(new Edge(3, 100), pcsr.getEdge(4, 3));
//        pcsr.debugEdges();

        // ----- Remove edges -----
        log.info("\nRemoving edges...");
//        for (int i = 0; i < 5; i++) {
//            for (int j = i+1; j < 5; j++) {
//                if (i == j)
//                    continue;
//                log.info(String.format("Removing edge %d-%d", i, j));
//                pcsr.removeEdge(i, j);
//                pcsr.debugEdges();
//            }
//        }

        // ----- Remove edges of v(0) ------
        log.info("\nRemoving edges of v(0)...");
        pcsr.removeEdge(0, 1);
        pcsr.removeEdge(0, 2);
        pcsr.removeEdge(0, 3);
        pcsr.removeEdge(0, 4);
        assertEquals(0, pcsr.getVertex(0).getNeighborCount());
        log.info("\n" + pcsr);
        pcsr.debugEdges();

        // ----- Remove edges of v(1) ------
        log.info("\nRemoving edges of v(1)...");
        pcsr.removeEdge(1, 2);
        pcsr.removeEdge(1, 3);
        pcsr.removeEdge(1, 4);
        assertEquals(0, pcsr.getVertex(1).getNeighborCount());
        pcsr.debugEdges();
        log.info("\n" + pcsr);

        // ----- Remove edges of v(2) ------
        log.info("\nRemoving edges of v(2)...");
        pcsr.removeEdge(2, 3);
        pcsr.removeEdge(2, 4);
        assertEquals(0, pcsr.getVertex(2).getNeighborCount());
        pcsr.debugEdges();
        log.info("\n" + pcsr);

        // ----- Remove edges of v(3) ------
        log.info("\nRemoving edges of v(3)...");
        pcsr.removeEdge(3, 4);
        pcsr.debugEdges();
        assertEquals(0, pcsr.getVertex(3).getNeighborCount());
        assertEquals(0, pcsr.getVertex(4).getNeighborCount());
        log.info("\n" + pcsr);

        for (int i = 0; i < 5; i++) {
            assertEquals(0, pcsr.getVertex(i).getNeighborCount());
        }
    }

    public void testRemoveVertex() {
        PCSR pcsr;
        int count;

        log.info("Initialize PCSR with size 5");
        pcsr = new PCSR(5);
        log.info("\n" + pcsr);
        pcsr.debugEdges();

        assertEquals(PEdge.NULL_EDGE, pcsr.getEdge(0, 1));

        // ----- Add edges -----
        log.info("\nAdding edges...");
        count = 1;

        for (int i = 0; i < 5; i++) {
            for (int j = i + 1; j < 5; j++) {
                if (i == j)
                    continue;

                pcsr.addEdge(i, j,PEdge.EDGE_MARKER);
                count++;
            }
        }
        assertEquals(new PEdge(4, PEdge.EDGE_MARKER), pcsr.getEdge(3, 4));
        assertEquals(new PEdge(3, PEdge.EDGE_MARKER), pcsr.getEdge(4, 3));

        log.info("\n" + pcsr);
        pcsr.debugEdges();


        // ----- Remove edges -----
        log.info("\nRemoving vertices...");
        log.info("\nRemoving v(0)");
        pcsr.removeVertex(0);
        assertEquals(0, pcsr.getVertex(0).getNeighborCount());
        assertEquals(3, pcsr.getVertex(1).getNeighborCount());
        assertEquals(3, pcsr.getVertex(2).getNeighborCount());
        assertEquals(3, pcsr.getVertex(3).getNeighborCount());
        assertEquals(3, pcsr.getVertex(4).getNeighborCount());

        log.info("\nRemoving v(1)");
        pcsr.removeVertex(1);
        assertEquals(0, pcsr.getVertex(0).getNeighborCount());
        assertEquals(0, pcsr.getVertex(1).getNeighborCount());
        assertEquals(2, pcsr.getVertex(2).getNeighborCount());
        assertEquals(2, pcsr.getVertex(3).getNeighborCount());
        assertEquals(2, pcsr.getVertex(4).getNeighborCount());

        log.info("\nRemoving v(2)");
        pcsr.removeVertex(2);
        assertEquals(0, pcsr.getVertex(0).getNeighborCount());
        assertEquals(0, pcsr.getVertex(1).getNeighborCount());
        assertEquals(0, pcsr.getVertex(2).getNeighborCount());
        assertEquals(1, pcsr.getVertex(3).getNeighborCount());
        assertEquals(1, pcsr.getVertex(4).getNeighborCount());

        log.info("\nRemoving v(3)");
        pcsr.removeVertex(3);
        assertEquals(0, pcsr.getVertex(0).getNeighborCount());
        assertEquals(0, pcsr.getVertex(1).getNeighborCount());
        assertEquals(0, pcsr.getVertex(2).getNeighborCount());
        assertEquals(0, pcsr.getVertex(3).getNeighborCount());
        assertEquals(0, pcsr.getVertex(4).getNeighborCount());

        log.info("\n" + pcsr);
        pcsr.debugEdges();
    }

    public void testAddBulkEdges() {
        PCSR pcsr;
        int count;

        log.info("Initialize PCSR with size 5");
        pcsr = new PCSR(5);
        log.info("\n" + pcsr);
        pcsr.debugEdges();

        assertEquals(new PEdge(0, PEdge.NULL_MARKER), pcsr.getEdge(0, 1));

        // ----- Add edges -----
        log.info("\nAdding edges...");
        count = 1;

        Collection<PEdge> edges = new ArrayList<>(5);
        for (int i = 0; i < 5; i++) {
            for (int j = i + 1; j < 5; j++) {
                if (i == j)
                    continue;
                edges.add(new PEdge(j, PEdge.EDGE_MARKER));
                count++;
            }

            log.info("Adding edges for v(" + i + ")");
            log.info("Edges " + edges);
            pcsr.addEdges(i, edges);

            log.info("\n" + pcsr);
            pcsr.debugEdges();

            assertEquals(4, pcsr.getVertex(i).getNeighborCount());
            edges.clear();
        }

        assertEquals(4, pcsr.getVertex(0).getNeighborCount());
        assertEquals(4, pcsr.getVertex(1).getNeighborCount());
        assertEquals(4, pcsr.getVertex(2).getNeighborCount());
        assertEquals(4, pcsr.getVertex(3).getNeighborCount());
        assertEquals(4, pcsr.getVertex(4).getNeighborCount());
    }

    public void testPrint() {
        PCSR pcsr;
        int count;

        log.info("Initialize PCSR with size 5");
        pcsr = new PCSR(8);
        log.info("\n" + pcsr);
        pcsr.debugEdges();

        assertEquals(PEdge.NULL_EDGE, pcsr.getEdge(0, 1));

        // ----- Add edges -----
        log.info("\nAdding edges...");
        count = 1;

        Collection<PEdge> edges = new ArrayList<>(5);
        int vId = 4;
        edges.add(new PEdge(1, PEdge.EDGE_MARKER));
        edges.add(new PEdge(2, PEdge.EDGE_MARKER));
        edges.add(new PEdge(5, PEdge.EDGE_MARKER));
        edges.add(new PEdge(6, PEdge.EDGE_MARKER));
        pcsr.addEdges(vId, edges);

        for(int i = 0; i < 8; i++) {
            log.info(pcsr.getVertex(i).toString());
        }
        pcsr.debugEdges();
        assertEquals(4, pcsr.getVertex(vId).getNeighborCount());
    }
}