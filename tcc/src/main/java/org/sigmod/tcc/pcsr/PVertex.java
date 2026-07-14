package org.sigmod.tcc.pcsr;

import org.sigmod.tcc.rgraph.RVertex;

/**
 * Constructs a PCSR-based vertex.
 */
public class PVertex implements RVertex {
    private int id;
    private int beginning;
    private int end;
    private int neighborCount;

    public PVertex(int id, int beginning, int end, int neighborCount) {
        this.id = id;
        this.beginning = beginning;
        this.end = end;
        this.neighborCount = neighborCount;
    }

    @Override
    public int getId() {
        return 0;
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
        return "PVertex{" + "id=" + id + ", beginning=" + beginning + ", end=" + end + ", neighborCount=" + neighborCount + "}";
    }
}
