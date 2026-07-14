package org.sigmod.tcc.csr;

import org.sigmod.tcc.rgraph.RVertex;

/**
 * Constructs a CSR-based vertex.
 */
public class CVertex implements RVertex {
    public static final int NULL_MARKER = -1;

    private int id;
    private int neighborCount = 0;
    private int beginning;
    private int end;

    public CVertex(int id) {
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getBeginning() {
        return beginning;
    }

    @Override
    public int getEnd() {
        return end;
    }

    @Override
    public int getNeighborCount() {
        return neighborCount;
    }

    @Override
    public void setBeginning(int idx) {
        this.beginning = idx;
    }

    @Override
    public void setEnd(int idx) {
        this.end = idx;
    }

    @Override
    public void setNeighborCount(int count) {
        this.neighborCount = count;
    }

    @Override
    public String toString() {
        return "CVertex{" + "id=" + id + ", neighborCount=" + neighborCount + ", beginning=" + beginning + ", end=" + end + '}';
    }
}
