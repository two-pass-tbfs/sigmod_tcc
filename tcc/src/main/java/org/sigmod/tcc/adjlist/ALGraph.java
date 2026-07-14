package org.sigmod.tcc.adjlist;

import org.apache.commons.lang3.NotImplementedException;
import org.sigmod.tcc.rgraph.RGraph;

import java.util.*;
import java.util.function.IntConsumer;

/**
 * Constructs an adjacency-list-based graph.
 */
public class ALGraph implements RGraph<ALVertex, ALEdge> {

    private final int[] ranks;
    private final int[] cores;
    private final int[] upperBounds;
    private final int[] colors;

    private final Map<Integer, ALVertex> vertices = new HashMap<>();
    private final Map<Integer, Set<ALEdge>> adjList = new HashMap<>();

    public ALGraph(Set<Integer> sources) {
        int n = sources.size();
        this.ranks = new int[n];
        this.cores = new int[n];
        this.upperBounds = new int[n];
        this.colors = new int[n];

        for (Integer id : sources) {
            addVertex(id);
        }
    }

    @Override
    public void addVertex(int id) {
        vertices.putIfAbsent(id, new ALVertex(id));
        adjList.putIfAbsent(id, new HashSet<>());
        addEdge(id, id, 1);
    }

    @Override
    public ALVertex getVertex(int id) {
        return vertices.get(id);
    }

    @Override
    public void removeVertex(int id) {
        // Remove in-going edges
        Set<ALEdge> outEdges = adjList.get(id);
        for (ALEdge outEdge : outEdges) {
            Set<ALEdge> inEdges = adjList.get(outEdge.getDest());
            boolean isFound = false;
            Iterator<ALEdge> it = inEdges.iterator();
            while (it.hasNext()) {
                ALEdge inEdge = it.next();
                if (inEdge.getDest() == id) {
                    it.remove();
                    isFound = true;
                    break;
                }
            }

            if (isFound) {
                ALVertex inVertex = vertices.get(outEdge.getDest());
                inVertex.setNeighborCount(inVertex.getNeighborCount() - 1);
            }
        }

        // Remove out-going edges
        adjList.get(id).clear();
//        adjList.remove(id);

        // Remove vertex
        vertices.get(id).setNeighborCount(0);
//        vertices.remove(id);
    }

    @Override
    public List<ALVertex> getVertices() {
        return vertices.values().stream().toList();
    }

    @Override
    public void addEdge(int src, int dst, long val) {
        // Add the vertices if they don't exist
        if (src != dst) {
            addVertex(src);
            addVertex(dst);
        }

        addDirectedEdge(src, dst, (int) val);
    }

    @Override
    public void updateEdge(int src, int dst, long val) {
        if (!vertices.containsKey(src)) addVertex(src);

        Set<ALEdge> edges = adjList.get(src);
        for (ALEdge edge : edges) {
            if (edge.getDest() == dst) {
                edge.setValue((int) val);
                return;
            }
        }
    }

    @Override
    public ALEdge getEdge(int idx) {
//        return null;
        throw new NotImplementedException();
    }

    @Override
    public ALEdge getEdge(int src, int dst) {
        Set<ALEdge> edges = adjList.getOrDefault(src, Set.of());

        for (ALEdge edge : edges) {
            if (edge.getDest() == dst) return edge;
        }
        return null;
    }

    @Override
    public void addEdges(int src, Collection<ALEdge> newEdges) {
        Set<ALEdge> edges = Set.copyOf(newEdges);
        adjList.getOrDefault(src, Set.of()).addAll(edges);

        ALVertex srcVertex = vertices.get(src);
        srcVertex.setNeighborCount(adjList.get(src).size());
    }

    @Override
    public List<ALEdge> getEdges(int id) {
        return adjList.getOrDefault(id, Set.of()).stream().toList();
    }

    @Override
    public void removeEdge(int src, int dst) {
        Set<ALEdge> edges = adjList.getOrDefault(src, Set.of());
        edges.removeIf(edge -> edge.getDest() == dst);

        ALVertex srcVertex = vertices.get(src);
        srcVertex.setNeighborCount(srcVertex.getNeighborCount() - 1);
    }

    @Override
    public int getEdgeCount() {
        return vertices.values().stream().mapToInt(ALVertex::getNeighborCount).sum();
//        return adjList.values().stream().mapToInt(List::size).sum();
    }

    @Override
    public int getCore(int id) {
        return cores[id];
    }

    @Override
    public void setCore(int id, int core) {
        this.cores[id] = core;
    }

    @Override
    public int getUpperBound(int id) {
        return upperBounds[id];
    }

    @Override
    public int getRank(int id) {
        return ranks[id];
    }

    @Override
    public int[] getColors() {
        return colors;
    }

    @Override
    public void setRank(int id, int rank) {
        this.ranks[id] = rank;
    }

    @Override
    public void setUpperBound(int id, int val) {
        this.upperBounds[id] = val;
    }

    @Override
    public Set<Integer> getNeighbors(int id) {
        return new HashSet<>(adjList.get(id).stream().map(ALEdge::getDest).toList());
    }

    @Override
    public void forEachNeighbor(int id, IntConsumer consumer) {
        Set<ALEdge> edges = adjList.getOrDefault(id, Set.of());
        edges.forEach(edge -> consumer.accept(edge.getDest()));
    }

    @Override
    public Iterator<ALEdge> edgeIterator(int id) {
        return adjList.get(id).iterator();
    }

    @Override
    public double getDensity() {
        return getEdgeCount() / ((double) getVertices().size() * getVertices().size() - 1);
    }

    /**
     * Adds a directed edge to the graph.
     *
     * @param src the source vertex id
     * @param dst the destination vertex id
     * @param val the edge value
     */
    private void addDirectedEdge(int src, int dst, int val) {
        ALEdge outEdge = null;
        for (ALEdge edge : adjList.get(src)) {
            if (edge.getDest() == dst) {
                edge.setValue(val);
                outEdge = edge;
                break;
            }
        }
        if (outEdge == null) {
            adjList.get(src).add(new ALEdge(dst, val));
            // Increase the neighbor count
            ALVertex sourceVertex = this.getVertex(src);
            sourceVertex.setNeighborCount(sourceVertex.getNeighborCount() + 1);
        }
    }

    public void debugPrint() {
        for (ALVertex v : vertices.values()) {
            System.out.println(v);
            System.out.println(adjList.get(v.getId()));
        }
    }
}
