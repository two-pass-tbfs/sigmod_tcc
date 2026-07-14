package org.sigmod.tcc.pcsr;

import org.apache.commons.lang3.tuple.Pair;
import org.sigmod.tcc.rgraph.RGraph;

import java.util.*;
import java.util.function.IntConsumer;
import java.util.logging.Logger;

/**
 * Constructs a PCSR-based graph.
 */
public class PCSR implements RGraph<PVertex, PEdge> {
    private static final Logger log = Logger.getLogger(PCSR.class.getName());

    public static final int NULL_VALUE = -1;

    private final List<PVertex> vertices;
    private final EdgeList edges;

    private int maxCore = 0;

    private final int[] ranks;
    private final int[] cores;
    private final int[] upperBounds;
    private final int[] colors;

    /**
     *
     * @param n the number of vertices
     */
    public PCSR(int n) {
        edges = new EdgeList(n * 2); // Multiply by 2 to consider a sentinel and a gap for each vertex

        vertices = new ArrayList<>(n);
        for (int id = 0; id < n; id++) {
            addVertex(id);
        }

        this.colors = new int[n];
        this.ranks = new int[n];
        this.cores = new int[n];
        this.upperBounds = new int[n];

        Arrays.fill(colors, PCSR.NULL_VALUE);
        Arrays.fill(ranks, PCSR.NULL_VALUE);
        Arrays.fill(cores, PCSR.NULL_VALUE);
        Arrays.fill(upperBounds, PCSR.NULL_VALUE);
    }

    /**
     *
     * @param v the number of vertices
     */
    public PCSR(int v, int e) {
        int min_edge_count = (e * 2) + v; // undirected edges + vertex sentinels
        edges = new EdgeList(min_edge_count);

        vertices = new ArrayList<>(v);
        for (int id = 0; id < v; id++) {
            addVertex(id);
        }

        this.colors = new int[v];
        this.ranks = new int[v];
        this.cores = new int[v];
        this.upperBounds = new int[v];

        Arrays.fill(colors, PCSR.NULL_VALUE);
        Arrays.fill(ranks, PCSR.NULL_VALUE);
        Arrays.fill(cores, PCSR.NULL_VALUE);
        Arrays.fill(upperBounds, PCSR.NULL_VALUE);
    }

    public int[] getColors() {
        return colors;
    }

    public int getColor(int id) {
        return colors[id];
    }

    public void setColor(int id, int color) {
        colors[id] = color;
    }

    public int getCore(int id) {
        return cores[id];
    }

    public void setCore(int id, int core) {
        cores[id] = core;
    }

    public int[] getCores() {
        return cores;
    }

    public int getMaxCore() {
        return maxCore;
    }

    public int getRank(int id) {
        return ranks[id];
    }

    public void setRank(int id, int rank) {
        ranks[id] = rank;
    }

    public int getUpperBound(int id) {
        return upperBounds[id];
    }

    public void setUpperBound(int id, int val) {
        upperBounds[id] = val;
    }

    /**
     * Adds a bidirectional edge
     *
     * @param src the source vertex
     * @param dst the destination vertex
     * @param val the value of the edge (1 if it exists, 0 if not)
     */
    @Override
    public void addEdge(int src, int dst, long val) {
        byte bVal = (byte) val;
        if (src == dst) throw new IllegalArgumentException("Cannot add self-loop edge");
        if (bVal == PEdge.SENTINEL_MARKER) throw new IllegalArgumentException("Cannot add sentinel edge");
        if (bVal == PEdge.NULL_MARKER) return;

        addDirectedEdge(dst, src, bVal);

        addDirectedEdge(src, dst, bVal);
    }

    private void addDirectedEdge(int src, int dest, byte value) {
        PVertex vertex;
        PEdge edge;
        int locToAdd;

        vertex = vertices.get(src);
        edge = new PEdge(dest, value);
        locToAdd = BinarySearch.compute(edges, edge, vertex.getBeginning() + 1, vertex.getEnd());
        insert(locToAdd, edge, src);
        vertex.setNeighborCount(vertex.getNeighborCount() + 1);
    }


    /**
     * Removes a bidirectional edge
     *
     * @param src  the source vertex
     * @param dest the edge to delete
     */
    public void removeEdge(int src, int dest) {
        if (src == dest) return;

        if (src >= vertices.size() || dest >= vertices.size()) {
            throw new IllegalArgumentException("Vertex does not exist");
        }

        deleteDirectedEdge(src, dest);
        deleteDirectedEdge(dest, src);
    }

    private void deleteDirectedEdge(int src, int dest) {
        PVertex vertex = vertices.get(src);
        PEdge edge = new PEdge(dest, PEdge.NULL_MARKER);

        int locToDelete = BinarySearch.compute(edges, edge, vertex.getBeginning() + 1, vertex.getEnd());

        // Verify the edge exists at this location
        if (locToDelete >= vertex.getEnd()) {
            return; // Outside vertex's range
        }

        PEdge edgeToDelete = edges.getItem(locToDelete);

        if (edgeToDelete.isNull() || edgeToDelete.isSentinel() || edgeToDelete.getDest() != dest) {
            return; // Edge doesn't exist or wrong destination
        }

        // Delete the edge
        delete(locToDelete, src);
        vertices.get(src).setNeighborCount(vertices.get(src).getNeighborCount() - 1);
    }

    @Override
    public int getEdgeCount() {
        int count = 0;
        for (PVertex vertex : vertices) {
            count += vertex.getNeighborCount();
        }
        return count;
    }

    public BitSet getNeighborBitSet(int src) {
        PVertex srcVertex = vertices.get(src);
        BitSet output = new BitSet(srcVertex.getNeighborCount());
        for (int j = srcVertex.getBeginning() + 1; j < srcVertex.getEnd(); j++) {
            PEdge edge = edges.getItem(j);
            if (!edge.isNull()) {
                output.set(edge.getDest());
            }
            if (output.cardinality() == srcVertex.getNeighborCount()) break;
        }

        return output;
    }

    @Override
    public Set<Integer> getNeighbors(int id) {
        PVertex srcVertex = vertices.get(id);

        Set<Integer> output = new HashSet<>(srcVertex.getNeighborCount());

        for (int j = srcVertex.getBeginning() + 1; j < srcVertex.getEnd(); j++) {
            PEdge edge = edges.getItem(j);
            if (!edge.isNull()) {
                output.add(edge.getDest());
            }
            if (output.size() == srcVertex.getNeighborCount()) break;
        }

        return output;
    }

    @Override
    public void forEachNeighbor(int id, IntConsumer consumer) {
        int processed = 0;
        PVertex vertex = vertices.get(id);
        for (int start = vertex.getBeginning() + 1; start < vertex.getEnd(); start++) {
            PEdge edge = edges.getItem(start);
            if (!edge.isNull()) {
                processed++;
                consumer.accept(edge.getDest());
            }
            if (processed == vertex.getNeighborCount()) break;
        }
    }

    @Override
    public Iterator<PEdge> edgeIterator(int id) {
        PVertex vertex = vertices.get(id);
        return edges.iterator(vertex.getBeginning() + 1, vertex.getEnd());
    }

    @Override
    public double getDensity() {
        return getEdgeCount() / ((double) getVertices().size() * getVertices().size() - 1);
    }

    public PEdge getEdge(int idx) {
        return edges.getItem(idx);
    }

    public List<PVertex> getVertices() {
        return vertices;
    }

    @Override
    public PVertex getVertex(int id) {
        return vertices.get(id);
    }

    @Override
    public void removeVertex(int id) {
        PVertex v = vertices.get(id);

        if (v.getNeighborCount() == 0) return;

        // Delete all edges pointing to this vertex
        for (int index = v.getBeginning() + 1; index < v.getEnd(); index++) {
            PEdge edge = edges.getItem(index);
            if (edge.isNull()) continue;

            deleteDirectedEdge(edge.getDest(), id);
        }

        // Delete all edges pointing from this vertex in bulk
        deleteBulk(id);
        v.setNeighborCount(0);
    }

    /**
     * Deletes all edges pointing from the vertex id in bulk.
     *
     * @param id the vertex id
     */
    private void deleteBulk(int id) {
        PVertex v = vertices.get(id);

        for (int index = v.getBeginning() + 1; index < v.getEnd(); index++) {
            edges.getItem(index).setDest(PEdge.NULL_MARKER);
            edges.getItem(index).setValue(PEdge.NULL_MARKER);
        }

        int vertexRange = v.getEnd() - v.getBeginning();
        int len = edges.getLogN();
        int level = edges.getH();
        while (len < vertexRange) {
            len *= 2;
            if (len <= edges.getN()) {
                level--;
            }
        }

        int vertexIndex = findLeaf(v.getBeginning() + 1, len);
        double density = getDensity(vertexIndex, len);
        Pair<Double, Double> densityBound = densityBound(level);

        while (density < densityBound.getLeft() && len < edges.getN()) {
            len *= 2;
            level--;
            vertexIndex = findLeaf(vertexIndex, len);
            densityBound = densityBound(level);
            density = getDensity(vertexIndex, len);
        }

        if (len >= edges.getN() && density < densityBound.getLeft()) {
            int shrinkFactor = 2;
            int currentN = edges.getN();
            int totalItems = this.getEdgeCount() + this.vertices.size();

            while (currentN > edges.getMinN()) {
                int halfN = currentN / 2;
                if ((double) totalItems / halfN < densityBound.getLeft()) {
                    currentN = halfN;
                    shrinkFactor *= 2;
                } else break;
            }

            edges.shrinkList(shrinkFactor);
            redistribute(0, edges.getN());
            return;
        }

        redistribute(vertexIndex, len);
    }

    @Override
    public void updateEdge(int src, int dst, long val) {
        PEdge edge = getEdge(src, dst);
        edge.setValue((byte) val);
    }

    public PEdge getEdge(int src, int dest) {
        if (src >= vertices.size() || dest >= vertices.size()) return PEdge.NULL_EDGE;

        int start = vertices.get(src).getBeginning();
        int end = vertices.get(src).getEnd();

        PEdge edge = new PEdge(dest, PEdge.NULL_MARKER);
        int loc = BinarySearch.compute(edges, edge, start + 1, end);

        edge = edges.getItem(loc);
        if ((!edge.isNull() && !edge.isSentinel()) && edge.getDest() == dest) {
            return edge;
        }

        return PEdge.NULL_EDGE;
    }

    /**
     * Returns the list of edges of the source vertex.
     *
     * @param src the vertex id
     * @return a list of edges
     */
    public List<PEdge> getEdges(int src) {
        List<PEdge> output = new LinkedList<>();
        int start = vertices.get(src).getBeginning();
        int end = vertices.get(src).getEnd();
        for (int j = start + 1; j < end; j++) {
            PEdge edge = edges.getItem(j);
            if (edge != PEdge.NULL_EDGE) {
                output.add(edge);
            }
        }
        return output;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < vertices.size(); i++) {
            int matrixIndex = 0;
            PVertex currVertex = vertices.get(i);
            for (int j = currVertex.getBeginning() + 1; j < currVertex.getEnd(); j++) {
                PEdge edge = edges.getItem(j);

                if (!edge.isNull()) {
                    while (matrixIndex < edge.getDest()) {
                        sb.append("000 ");
                        matrixIndex++;
                    }
                    sb.append(String.format("%03d ", edge.getValue()));
                    matrixIndex++;
                }
            }
            sb.append("000 ".repeat(Math.max(0, vertices.size() - matrixIndex)));
            sb.append("\n");
        }

        return sb.toString();
    }

    @Override
    public void addVertex(int id) {
        PVertex vertex;

        PEdge sentinel = new PEdge(id, PEdge.SENTINEL_MARKER);

        if (id > 0) {
            int beginning = vertices.get(id - 1).getEnd();
            vertex = new PVertex(id, beginning, beginning + 1, 0);
        } else {
            vertex = new PVertex(id, 0, 1, 0);
        }

        vertices.add(vertex);
        insert(vertex.getBeginning(), sentinel, vertices.size() - 1);
    }

    /**
     * Adds a collection of edges to the graph.
     *
     * @param src      the source vertex id
     * @param newEdges the collection of edges to be added
     */
    public void addEdges(int src, Collection<PEdge> newEdges) {
        if (newEdges.isEmpty()) return;

        // Check the global density and expand if necessary
        int totalNeeded = this.getEdgeCount() + this.vertices.size() + (newEdges.size() * 2);
        if ((double) totalNeeded / edges.getN() >= densityBound(0).getRight()) {
            int factor = 2;
            while ((double) totalNeeded / (edges.getN() * factor) >= densityBound(0).getRight()) {
                factor *= 2;
            }
            edges.expandList(factor);
            redistribute(0, edges.getN());
        }

        // Batch insert the edges from src to all other vertices
        insertBulk(src, newEdges);

        // Insert the edges from all other vertices to src
        for (PEdge newEdge : newEdges) {
            addDirectedEdge(newEdge.getDest(), src, newEdge.getValue());
        }
    }

    public void debugEdges() {
        StringBuilder sb = new StringBuilder((edges.getN() + vertices.size()) * 50);
        sb.append("N: ").append(edges.getN()).append(" LogN: ").append(edges.getLogN()).append(" H: ").append(edges.getH()).append("\n");
        for (int i = 0; i < edges.getN(); i++) {
            PEdge edge = edges.getItem(i);
            if (edge.isSentinel()) {
                var v = vertices.get(edge.getDest());
                sb.append("\nVertex ").append(edge.getDest()).append(": ").append(v).append("\n");
            }
            sb.append("Edge ").append(i).append(": ").append(edge).append("\n");
        }

        log.info(sb::toString);
    }

    /**
     * Deletes the edge at the index
     *
     * @param index the index of the edge to be deleted
     * @param src   the source vertex id
     */
    private void delete(int index, int src) {
        edges.setItem(index, PEdge.NULL_EDGE);

        int leaf = findLeaf(index);
        int level = edges.getH();
        int len = edges.getLogN();

        double density = getDensity(leaf, len);
        Pair<Double, Double> densityBound = densityBound(level);

        while (density < densityBound.getLeft()) {
            len *= 2;
            if (len <= edges.getN()) {
                level--;
                leaf = findLeaf(leaf, len);
                densityBound = densityBound(level);
                density = getDensity(leaf, len);
            } else {
                halfList();
                return;
            }
        }
        redistribute(leaf, len);
    }

    /**
     * Shrinks the edge list to half its size.
     */
    private void halfList() {
        edges.shrinkList(2);

        redistribute(0, edges.getN());
    }

    /**
     * Inserts the elem into the edge array at index.
     *
     * @param index the index at which to insert the edge
     * @param elem  the edge to be inserted
     * @param src   the source vertex id
     * @return the index of the inserted edge
     */
    private int insert(int index, PEdge elem, int src) {
        int leaf = findLeaf(index);
        int level = edges.getH();
        int len = edges.getLogN();

        // always deposit on the left
        if (edges.getItem(index).isNull()) {
            edges.setItem(index, elem);
        } else {
            PEdge edge = edges.getItem(index);
            // if the edge already exists in the graph, update its value
            if (!elem.isSentinel() && !edge.isSentinel() && edge.getDest() == elem.getDest()) {
                edge.setValue(elem.getValue());
                return index;
            }

            if (index == edges.getN() - 1) {
                // when adding to the end double, then add edge
                doubleList();
                PVertex vertex = vertices.get(src);
                int locToAdd = BinarySearch.compute(edges, elem, vertex.getBeginning() + 1, vertex.getEnd());
                return insert(locToAdd, elem, src);
            } else {
                if (slideRight(index) == -1) {
                    index -= 1;
                    slideLeft(index);
                }
            }

            edges.setItem(index, new PEdge(elem.getDest(), elem.getValue()));
        }

        double density = getDensity(leaf, len);

        // Spill over into the next level up, vertex is completely full
        if (density == 1) {
            leaf = findLeaf(leaf, len * 2);
            redistribute(leaf, len * 2);
        } else { // Make the last slot in a section empty so you can always slide right
            redistribute(leaf, len);
        }

        Pair<Double, Double> densityBound = densityBound(level);
        density = getDensity(leaf, len);

        // while density too high, go up the implicit tree
        while (density >= densityBound.getRight()) {
            len *= 2;
            if (len <= edges.getN()) {
                level--;
                leaf = findLeaf(leaf, len);
                densityBound = densityBound(level);
                density = getDensity(leaf, len);
            } else {
                doubleList();
                // Search from the beginning because the list was doubled
                return findElemPointer(0, elem);
            }
        }

        redistribute(leaf, len);

        return findElemPointer(leaf, elem);
    }

    /**
     * Inserts the edges of a source vertex id into the edge array
     *
     * @param id       the source vertex id
     * @param newEdges the edges to be inserted
     */
    private void insertBulk(int id, Collection<PEdge> newEdges) {
        PVertex v = vertices.get(id);

        // Rebalance the tree
        int vertexRange = v.getEnd() - v.getBeginning();
        int len = edges.getLogN();
        int level = edges.getH();
        while (len < vertexRange) {
            len *= 2;
            if (len <= edges.getN()) {
                level--;
            }
        }

        int leaf = findLeaf(v.getBeginning(), len);
        double density = getDensity(leaf, len, newEdges.size());
        Pair<Double, Double> densityBound = densityBound(level);

        while (density >= densityBound.getRight()) {
            if (len >= edges.getN()) break;

            len *= 2;
            level--;
            leaf = findLeaf(leaf, len);
            densityBound = densityBound(level);
            density = getDensity(leaf, len, newEdges.size());
        }

        redistribute2(id, leaf, len, newEdges);
        vertices.get(id).setNeighborCount(v.getNeighborCount() + newEdges.size());
    }

    /**
     * Returns the index of the edge elem in the array.
     *
     * @param index starting index
     * @param elem  edge to find
     * @return index of edge elem
     */
    private int findElemPointer(int index, PEdge elem) {
        PEdge item = edges.getItem(index);
        while (!item.equals(elem) && index < edges.getN() - 1) {
            item = edges.getItem(++index);
        }
        return index;
    }

    private Pair<Double, Double> densityBound(int depth) {
        double lVal = 1.0 / 4.0 - ((.125 * depth) / edges.getH());
        double rVal = 3.0 / 4.0 + ((.25 * depth) / edges.getH());
        return Pair.of(lVal, rVal);
    }

    /**
     * Same as findLeaf, but does it for any level in the tree
     *
     * @param index index in array
     * @param len   length of sub-level
     * @return index of leaf
     */
    private int findLeaf(int index, int len) {
        return (index / len) * len;
    }

    private double getDensity(int index, int len) {
        int full = 0;
        for (int i = index; i < index + len; i++) {
            full += (edges.getItem(i).isNull()) ? 0 : 1;
        }
        double fullDensity = full;
        return fullDensity / len;
    }

    private double getDensity(int index, int len, int offset) {
        int full = 0;
        for (int i = index; i < index + len; i++) {
            full += (edges.getItem(i).isNull()) ? 0 : 1;
        }
        double fullDensity = full + offset;
        return fullDensity / len;
    }

    private int slideRight(int index) {
        int rVal = 0;
        PEdge el = edges.getItem(index);
        edges.setItem(index, PEdge.NULL_EDGE);

        index++;
        while (index < edges.getN() && !edges.getItem(index).isNull()) {
            PEdge next = edges.getItem(index);
            edges.setItem(index, el);

            if (el.isSentinel()) {
                // Fix pointer of vertex that goes into sentinel
                int vertexIndex = el.getDest();
                fixSentinel(vertexIndex, index);
            }
            el = next;
            index++;
        }

        if (el.isSentinel() && index < edges.getN()) {
            int vertexIndex = el.getDest();
            fixSentinel(vertexIndex, index);
        }

        if (index == edges.getN()) {
            index--;
            slideLeft(index);
            rVal = -1;
        }

        edges.setItem(index, el);
        return rVal;
    }

    private void slideLeft(int index) {
        PEdge el = edges.getItem(index);
        edges.setItem(index, PEdge.NULL_EDGE);

        index--;
        while (index >= 0 && !edges.getItem(index).isNull()) {
            PEdge next = edges.getItem(index);
            edges.setItem(index, el);
            if (!el.isNull() && el.isSentinel()) {
                int vertexIndex = el.getDest();
                fixSentinel(vertexIndex, index);
            }
            el = next;
            index--;
        }
        if (index == -1) {
            doubleList();

            slideRight(0);
            index = 0;
        }
        if (!el.isNull() && el.isSentinel()) {
            int vertexIndex = el.getDest();
            fixSentinel(vertexIndex, index);
        }
        edges.setItem(index, el);
    }

    private void doubleList() {
        edges.doubleList();

        redistribute(0, edges.getN());
    }

    /**
     * Redistributes the edges from index to index+len
     *
     * @param index the start index
     * @param len   the length of the range
     */
    private void redistribute(int index, int len) {
        int j = 0;

        // 1. Compact non-null edges to the beginning of the range
        for (int i = 0; i < len; i++) {
            PEdge edge = edges.getItem(index + i);
            if (!edge.isNull()) {
                if (i != j) {
                    edges.setItem(index + j, edge);
                }
                j++;
            }
        }

        // 2. Clear out the trailing spaces left after compaction
        for (int i = j; i < len; i++) {
            edges.setItem(index + i, PEdge.NULL_EDGE);
        }

        if (j == 0) return;

        int writePos = j - 1;
        for (int i = index + len - 1; i >= index; i--) {
            PEdge edge = edges.getItem(i);
            if (edge.isNull()) continue;

            int in = index + (int) ((long) writePos * len / j);
            if (i != in) {
                edges.setItem(i, PEdge.NULL_EDGE);
                edges.setItem(in, edge);
            }

            if (edge.isSentinel()) {
                fixSentinel(edge.getDest(), in);
            }

            writePos--;
        }
    }

    /**
     * Redistributes the edges from index to index+len, with the addition of newEdges associated with the src
     *
     * @param src      the source vertex id
     * @param index    the start index
     * @param len      the length of the range
     * @param newEdges the edges to be inserted
     */
    private void redistribute2(int src, int index, int len, Collection<PEdge> newEdges) {
        int srcNeighborCount = this.vertices.get(src).getNeighborCount();
        int j = 0;

        // 1. Compact non-null edges to the beginning of the range
        int indexOfSource = -1;
        for (int i = 0; i < len; i++) {
            PEdge edge = edges.getItem(index + i);
            if (!edge.isNull()) {
                if (i != j) {
                    edges.setItem(index + j, edge);
                }
                if (edge.isSentinel() && edge.getDest() == src) {
                    indexOfSource = index + j;
                }
                j++;
            }
        }

        for (int i = j; i < len; i++) {
            edges.setItem(index + i, PEdge.NULL_EDGE);
        }

        int totalEdges = j + newEdges.size();

        for (int i = index + j - 1; i > indexOfSource + srcNeighborCount; i--) {
            edges.setItem(i + newEdges.size(), edges.getItem(i));
        }

        int writeIdx = indexOfSource + srcNeighborCount + 1;
        for (PEdge newEdge : newEdges) {
            edges.setItem(writeIdx++, new PEdge(newEdge));
        }
        Arrays.sort(edges.getItems(), indexOfSource + 1, writeIdx, Comparator.comparingInt(PEdge::getDest));

        int writePos = totalEdges - 1;
        for (int i = index + len - 1; i >= index; i--) {
            PEdge edge = edges.getItem(i);
            if (edge.isNull()) continue;

            int in = index + (int) ((long) writePos * len / totalEdges);

            if (i != in) {
                edges.setItem(i, PEdge.NULL_EDGE);
                edges.setItem(in, edge);
            }

            if (edge.isSentinel()) {
                fixSentinel(edge.getDest(), in);

            }

            writePos--;
        }
    }

    private void fixSentinel(int vertexIndex, int in) {
        vertices.get(vertexIndex).setBeginning(in);

        if (vertexIndex > 0) vertices.get(vertexIndex - 1).setEnd(in);

        if (vertexIndex == vertices.size() - 1) vertices.get(vertexIndex).setEnd(edges.getN() - 1);
    }

    /**
     * Returns the starting index of the leaf the index belongs to
     *
     * @param index index in the edge array
     * @return the starting index of the leaf the index belongs to
     */
    private int findLeaf(int index) {
        return (index / edges.getLogN()) * edges.getLogN();
    }
}
