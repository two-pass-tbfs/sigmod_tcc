package org.sigmod.tcc.csr;

import org.sigmod.tcc.rgraph.RGraph;

import java.util.*;
import java.util.function.IntConsumer;

/**
 * Constructs a CSR-based graph.
 */
public class CGraph implements RGraph<CVertex, CEdge> {
    private List<CVertex> vertices; // row pointers
    private List<CEdge> edges; // values

    private final int[] ranks;
    private final int[] cores;
    private final int[] upperBounds;
    private final int[] colors;

    public CGraph(int numVertices, int numEdges) {
        this.vertices = new ArrayList<>(numVertices);
        this.edges = new ArrayList<>(numEdges);
//        for (int i = 0; i < numEdges; i++)
//            this.edges.add(null);

        for (int id = 0; id < numVertices; id++) {
            addVertex(id);
        }

        this.ranks = new int[numVertices];
        this.cores = new int[numVertices];
        this.upperBounds = new int[numVertices];
        this.colors = new int[numVertices];
    }

    @Override
    public void addVertex(int id) {
        vertices.add(new CVertex(id));
    }

    @Override
    public CVertex getVertex(int id) {
        return this.vertices.get(id);
    }

    @Override
    public void removeVertex(int id) {
        CVertex vertex = this.vertices.get(id);
        vertex.setNeighborCount(0);
        vertex.setBeginning(CVertex.NULL_MARKER);
        vertex.setEnd(CVertex.NULL_MARKER);
    }

    @Override
    public List<CVertex> getVertices() {
        return this.vertices;
    }

    @Override
    public void addEdge(int src, int dst, long val) {
        addDirectedEdge(src, dst, (int) val);
    }

    private void addDirectedEdge(int src, int dst, int val) {
        CVertex srcVertex = this.vertices.get(src);
        if (srcVertex.getNeighborCount() == 0)
            srcVertex.setBeginning(edges.size());
        edges.add(new CEdge(dst, val));
        srcVertex.setNeighborCount(srcVertex.getNeighborCount() + 1);
        srcVertex.setEnd(srcVertex.getBeginning() + srcVertex.getNeighborCount());
    }

    @Override
    public void updateEdge(int src, int dst, long val) {
        byte bVal = (byte) val;
        updateDirectedEdge(src, dst, bVal);
        updateDirectedEdge(dst, src, bVal);
    }

    private void updateDirectedEdge(int src, int dst, byte val) {
        CEdge edge = this.getEdge(src, dst);
        edge.setValue(val);
    }

    @Override
    public CEdge getEdge(int idx) {
        return this.edges.get(idx);
    }

    @Override
    public CEdge getEdge(int src, int dst) {
        CVertex srcVertex = this.vertices.get(src);
        for (int i = srcVertex.getBeginning(); i < srcVertex.getEnd(); i++) {
            CEdge edge = edges.get(i);
            if (edge.getDest() == dst) return edge;
        }
        return null;
    }

    @Override
    public void addEdges(int src, Collection<CEdge> newEdges) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<CEdge> getEdges(int id) {
        CVertex vertex = this.vertices.get(id);
        List<CEdge> edges = new ArrayList<>(vertex.getNeighborCount());
        for (int i = vertex.getBeginning(); i < vertex.getEnd(); i++) {
            edges.add(edges.get(i));
        }
        return edges;
    }

    @Override
    public void removeEdge(int src, int dst) {
        removeDirectedEdge(src, dst);
        removeDirectedEdge(dst, src);

        this.edges.subList(this.edges.size() - 2, this.edges.size()).clear();
    }

    private void removeDirectedEdge(int src, int dst) {
        CVertex srcVertex = this.vertices.get(src);

        // Find the edge to remove
        int dstIdx;
        for (dstIdx = srcVertex.getBeginning(); dstIdx < srcVertex.getEnd(); dstIdx++) {
            if (edges.get(dstIdx).getDest() == dst) {
                break;
            }
        }

        // Shift the edges to the left
        for (int i = dstIdx; i < srcVertex.getEnd(); i++) {
            edges.set(i, edges.get(i + 1));
        }

        srcVertex.setEnd(srcVertex.getEnd() - 1);
        srcVertex.setNeighborCount(srcVertex.getNeighborCount() - 1);

        for (int id = src + 1; id < vertices.size(); id++) {
            CVertex vertex = vertices.get(id);
            for (int edgeIdx = vertex.getBeginning(); edgeIdx < vertex.getEnd(); edgeIdx++) {
                edges.set(edgeIdx - 1, edges.get(edgeIdx));
            }
            vertex.setBeginning(vertex.getBeginning() - 1);
            vertex.setEnd(vertex.getEnd() - 1);
        }
    }

    @Override
    public int getEdgeCount() {
        return this.edges.size();
    }

    @Override
    public int getCore(int id) {
        return this.cores[id];
    }

    @Override
    public void setCore(int id, int core) {
        this.cores[id] = core;
    }

    @Override
    public int getUpperBound(int id) {
        return this.upperBounds[id];
    }

    @Override
    public int getRank(int id) {
        return this.ranks[id];
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
        CVertex vertex = this.vertices.get(id);
        HashSet<Integer> output = new HashSet<>(vertex.getNeighborCount());
        for (int i = vertex.getBeginning(); i < vertex.getEnd(); i++) {
            output.add(edges.get(i).getDest());
        }
        return output;
    }

    @Override
    public void forEachNeighbor(int id, IntConsumer consumer) {
        CVertex vertex = this.vertices.get(id);
        for (int i = vertex.getBeginning(); i < vertex.getEnd(); i++) {
            consumer.accept(edges.get(i).getDest());
        }
    }

    @Override
    public Iterator<CEdge> edgeIterator(int id) {
        CVertex vertex = this.vertices.get(id);
        return this.edges.subList(vertex.getBeginning(), vertex.getEnd()).iterator();
    }

    @Override
    public double getDensity() {
        return getEdgeCount() / ((double) getVertices().size() * getVertices().size() - 1);
    }

    public String debugEdges() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < edges.size(); i++) {
            sb.append(edges.get(i).toString()).append("\n");
        }
        return sb.toString();
    }
}
