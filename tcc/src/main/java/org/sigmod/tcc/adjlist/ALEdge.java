package org.sigmod.tcc.adjlist;

import org.sigmod.tcc.rgraph.REdge;

/**
 * Constructs an adjacency-list-based edge.
 */
public class ALEdge implements REdge<Integer> {
    public static final int NULL_MARKER = 0;

    private int dst;
    private int val;

    public ALEdge(int dst, int val) {
        this.dst = dst;
        this.val = val;
    }

    @Override
    public void setValue(Integer val) {
        this.val = val;
    }

    @Override
    public Integer getValue() {
        return this.val;
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
        return this.val == NULL_MARKER;
    }

    @Override
    public String toString() {
        return "ALEdge{" + "dst=" + dst + ", val=" + val + "}";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ALEdge) return ((ALEdge) obj).dst == dst;
        return super.equals(obj);
    }
}
