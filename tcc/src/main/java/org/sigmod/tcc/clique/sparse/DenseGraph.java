package org.sigmod.tcc.clique.sparse;

import org.sigmod.tcc.rgraph.REdge;
import org.sigmod.tcc.rgraph.RGraph;
import org.sigmod.tcc.rgraph.RVertex;

import java.util.*;
import java.util.logging.Logger;

 /**
 * Constructs a dense graph representation of the original graph.
 */
public class DenseGraph {

    int[] newIdToOldIdMapper;
    HashMap<Integer, Integer> oldIdToNewIdMapper;

    BitSet[] graph;

    ArrayList<Integer> ids;
    public static Logger log = Logger.getLogger(DenseGraph.class.getName());

    private int maxNewId = 0;

    DenseGraph(List<Integer> originalIds) {
        this.ids = new ArrayList<>(originalIds.size());
        for (int i = 0; i < originalIds.size(); i++) this.ids.add(i);

        // Convert the IDs
        newIdToOldIdMapper = new int[ids.size()];
        oldIdToNewIdMapper = new HashMap<>(ids.size());

        int newId = 0;
        for (Integer id : originalIds) {
            oldIdToNewIdMapper.put(id, newId);
            newIdToOldIdMapper[newId++] = id;
            if(newId > maxNewId) maxNewId = newId;
        }

        // Initialize adjacency row per vertex
        graph = new BitSet[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            graph[i] = new BitSet(maxNewId);
        }
    }

    public int getMaxNewId() {
        return maxNewId;
    }

    public <V extends RVertex, E extends REdge<?>> void convert(RGraph<V, E> rGraph) {
        for (int newIId = 0; newIId < ids.size(); newIId++) {
            int ogIId = newIdToOldIdMapper[newIId];

            Iterator<E> edgeIterator = rGraph.edgeIterator(ogIId);
            while (edgeIterator.hasNext()) {
                E edge = edgeIterator.next();
                int vId = edge.getDest();

                if (!oldIdToNewIdMapper.containsKey(vId)) continue;

                int newVId = oldIdToNewIdMapper.get(vId);
                if (newIId < newVId) {
                    graph[newIId].set(newVId);
                    graph[newVId].set(newIId);
                }
            }
            graph[newIId].set(newIId);
        }
    }

    public BitSet[] getGraph() {
        return graph;
    }

    public int getSize() {
        return graph.length;
    }

    public boolean isAdjacent(int uId, int vId) {
        return graph[uId].get(vId);
    }

    public void flip(int uId, int vId) {
        graph[uId].flip(vId);
    }

    public ArrayList<Integer> getIdList() {
        return ids;
    }

    public BitSet getNeighbors(int id) {
        return graph[id];
    }


    public int getOldId(int id) {
        return newIdToOldIdMapper[id];
    }

    public int getNewId(int id) {
        boolean test = oldIdToNewIdMapper.get(id) == null;
        if (test)
            return -1;
        return oldIdToNewIdMapper.get(id);
    }

    public int[] getNewIdToOldIdMapper() {
        return newIdToOldIdMapper;
    }

    public int getId(int idx) {
        return ids.get(idx);
    }

    public HashMap<Integer, Integer> getOldIdToNewIdMapper() {
        return oldIdToNewIdMapper;
    }
}
