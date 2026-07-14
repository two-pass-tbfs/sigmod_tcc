package org.sigmod.tcc.rgraph;

/**
 * Represents a vertex in an RGraph.
 */
public interface RVertex {
    /**
     * Returns the id of the vertex.
     *
     * @return the id of the vertex
     */
    int getId();

    /**
     * Returns the beginning index of the vertex
     *
     * @return the beginning index of the vertex
     */
    int getBeginning();

    /**
     * Returns the end index of the vertex
     *
     * @return the end index of the vertex
     */
    int getEnd();

    /**
     * Returns the number of neighbors of the vertex
     *
     * @return the number of neighbors of the vertex
     */
    int getNeighborCount();

    /**
     * Sets the beginning index of the vertex
     *
     * @param idx the beginning index of the vertex
     */
    void setBeginning(int idx);

    /**
     * Sets the end index of the vertex
     *
     * @param idx the end index of the vertex
     */
    void setEnd(int idx);

    /**
     * Sets the number of neighbors of the vertex
     *
     * @param count the number of neighbors of the vertex
     */
    void setNeighborCount(int count);
}
