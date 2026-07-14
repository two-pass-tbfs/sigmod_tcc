package org.sigmod.tcc.rgraph;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;

/**
 * Interfaces a reachability graph
 *
 * @param <V> extends RVertex
 * @param <E> extends REdge
 */
public interface RGraph<V extends RVertex, E extends REdge<?>> {
    int NULL_VALUE = -1;

    /**
     * Adds a vertex to the graph.
     *
     * @param id the id of the vertex to be added
     */
    void addVertex(int id);

    /**
     * Returns the vertex with the given id.
     *
     * @param id the id of the vertex to be retrieved
     * @return the vertex with the given id
     */
    V getVertex(int id);

    /**
     * Removes the vertex with the given id.
     *
     * @param id the id of the vertex to be removed
     */
    void removeVertex(int id);

    /**
     * Returns a list of all vertices in the graph. This creates a copy of the internal list.
     *
     * @return a list of all vertices in the graph
     */
    List<V> getVertices();

    /**
     * Adds an edge to the graph.
     *
     * @param src the source vertex id
     * @param dst the destination vertex id
     * @param val the edge value
     */
    void addEdge(int src, int dst, long val);

    /**
     * Updates the edge value.
     *
     * @param src the source vertex id
     * @param dst the destination vertex id
     * @param val the new edge value
     */
    void updateEdge(int src, int dst, long val);

    /**
     * Returns the edge with the given index.
     *
     * @param idx the index of the edge
     * @return the edge with the given index
     */
    E getEdge(int idx);

    /**
     * Returns the edge between the given vertices.
     *
     * @param src the source vertex id
     * @param dst the destination vertex id
     * @return the edge between the given vertices
     */
    E getEdge(int src, int dst);

    /**
     * Adds a collection of edges to the graph.
     *
     * @param src      the source vertex id
     * @param newEdges the collection of edges to be added
     */
    void addEdges(int src, Collection<E> newEdges);

    /**
     * Returns all edges of the given vertex. This creates a copy of the internal list.
     *
     * @param id the vertex id
     * @return the list of edges of the given vertex
     */
    List<E> getEdges(int id);

    /**
     * Removes the edge between the given vertices.
     *
     * @param src the source vertex id
     * @param dst the destination vertex id
     */
    void removeEdge(int src, int dst);

    /**
     * Returns the number of edges in the graph.
     *
     * @return the number of edges in the graph
     */
    int getEdgeCount();

    /**
     * Returns the core of the vertex
     *
     * @param id the vertex id
     * @return the core of the vertex
     */
    int getCore(int id);

    /**
     * Sets the core of the vertex
     *
     * @param id   the vertex id
     * @param core the core of the vertex
     */
    void setCore(int id, int core);

    /**
     * Returns the upper bound of the vertex
     *
     * @param id the vertex id
     * @return the upper bound of the vertex
     */
    int getUpperBound(int id);

    /**
     * Returns the rank of the vertex
     *
     * @param id the vertex id
     * @return the rank of the vertex
     */
    int getRank(int id);

    /**
     * Returns the colors of the vertices
     *
     * @return the colors of the vertices
     */
    int[] getColors();

    /**
     * Sets the rank of the vertex
     *
     * @param id   the vertex id
     * @param rank the rank of the vertex
     */
    void setRank(int id, int rank);

    /**
     * Sets the upper bound of the vertex
     *
     * @param id  the vertex id
     * @param val the upper bound of the vertex
     */
    void setUpperBound(int id, int val);

    /**
     * Returns the neighbors of the vertex. This creates a copy of the internal list.
     *
     * @param id the vertex id
     * @return the neighbors of the vertex
     */
    Set<Integer> getNeighbors(int id);

    /**
     * Performs the given action for each neighbor of the vertex.
     *
     * @param id       the vertex id
     * @param consumer the action to be performed for each neighbor
     */
    void forEachNeighbor(int id, IntConsumer consumer);

    /**
     * Returns an iterator over the edges of the vertex.
     *
     * @param id the vertex id
     * @return an iterator over the edges of the vertex
     */
    Iterator<E> edgeIterator(int id);

    /**
     * Returns the density of the graph
     *
     * @return the density of the graph
     */
    double getDensity();
}
