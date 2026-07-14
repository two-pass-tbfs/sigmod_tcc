package org.sigmod.tcc.csr;

import org.sigmod.tcc.rgraph.REdge;

/**
 * Constructs a CSR-based edge.
 */
public class CEdge implements REdge<Byte> {
    private int dst;
    private byte val;

    public CEdge(int dst, int val) {
        this.dst = dst;
        this.val = (byte) val;
    }

    @Override
    public void setValue(Byte val) {
        this.val = val;
    }

    @Override
    public Byte getValue() {
        return val;
    }

    @Override
    public int getDest() {
        return this.dst;
    }

    @Override
    public void setDest(int dst) {
        this.dst = dst;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    public String toString() {
        return "CEdge{" + "dst=" + dst + ", val=" + val + '}';
    }
}
