package org.sigmod.tcc.clique;

import org.sigmod.tcc.rgraph.REdge;
import org.sigmod.tcc.rgraph.RGraph;
import org.sigmod.tcc.rgraph.RVertex;

import java.util.*;
import java.util.logging.Logger;

/**
 * Reduces the graph to its k-core
 */
public class Core {
    public static Logger log = Logger.getLogger(Core.class.getName());

    public static <V extends RVertex, E extends REdge<?>> void reduce(RGraph<V, E> rGraph, List<Integer> ids, boolean[] active, int[] degrees, int K) {
        // Count the degrees of the vertices in the subgraph
        for (int id : ids) {
            rGraph.forEachNeighbor(id, vId -> {
                if (active[vId]) degrees[vId]++;
            });
        }

        int[] queue = new int[ids.size()];
        int queueN = 0;
        for (int id : ids) {
            if (degrees[id] < K) {
                queue[queueN++] = id;
                active[id] = false;
            }
        }

        for (int i = 0; i < queueN; i++) {
            int uId = queue[i];

            Iterator<E> edgeIterator = rGraph.edgeIterator(uId);

            while (edgeIterator.hasNext()) {
                E edge = edgeIterator.next();

                int vId = edge.getDest();
                if (!active[vId]) continue;

                if (--degrees[vId] == K - 1) {
                    queue[queueN++] = vId;
                    active[vId] = false;
                }
            }
        }

        int newSize = 0;
        for (int id : ids) {
            if (active[id]) {
                ids.set(newSize++, id);
            }
        }

        ids.subList(newSize, ids.size()).clear();
    }
}
