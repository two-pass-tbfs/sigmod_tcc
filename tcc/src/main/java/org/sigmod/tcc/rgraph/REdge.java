package org.sigmod.tcc.rgraph;

/**
 * Represents an edge in an RGraph
 *
 * @param <T> the value type of the edge
 */
public interface REdge<T> {
    /**
     * Sets the value of the edge
     *
     * @param val the new value of the edge
     */
    void setValue(T val);

    /**
     * Returns the value of the edge
     *
     * @return the value of the edge
     */
    T getValue();

    /**
     * Returns the destination of the edge
     *
     * @return the destination of the edge
     */
    int getDest();

    /**
     * Sets the destination of the edge
     *
     * @param dst the destination of the edge
     */
    void setDest(int dst);

    /**
     * Returns true if the edge is null, false otherwise
     *
     * @return true if the edge is null, false otherwise
     */
    boolean isNull();
}
