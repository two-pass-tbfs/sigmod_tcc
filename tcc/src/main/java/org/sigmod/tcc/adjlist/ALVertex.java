package org.sigmod.tcc.adjlist;

import org.sigmod.tcc.rgraph.RVertex;

/**
 * Constructs an adjacency-list-based vertex.
 */
public class ALVertex implements RVertex {

    private final int id;
    private int neighborCount = 0;

    public ALVertex(int id) {
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getBeginning() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getEnd() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getNeighborCount() {
        return this.neighborCount;
    }

    @Override
    public void setBeginning(int idx) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setEnd(int idx) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setNeighborCount(int count) {
        this.neighborCount = count;
    }

    @Override
    public String toString() {
        return "ALVertex{" + "id=" + id + ", neighborCount=" + neighborCount + '}';
    }
}
