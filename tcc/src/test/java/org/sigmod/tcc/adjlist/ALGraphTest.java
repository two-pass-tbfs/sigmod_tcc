package org.sigmod.tcc.adjlist;

import junit.framework.TestCase;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class ALGraphTest extends TestCase {
    Logger log = Logger.getLogger(ALGraphTest.class.getName());

    public void testALGraphSetup() {
        Set<Integer> sources = new HashSet<>();

        sources.add(0);
        sources.add(1);
        sources.add(2);

        ALGraph graph = new ALGraph(sources);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);

        graph.debugPrint();
    }
}
