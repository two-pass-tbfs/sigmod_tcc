package org.sigmod.tcc.pcsr;

import org.sigmod.tcc.rgraph.REdge;

/**
 * Constructs a PCSR-based edge.
 */
public class PEdge implements REdge<Byte> {
    public static final byte NULL_MARKER = 0;
    public static final byte EDGE_MARKER = 1;
    public static final byte SENTINEL_MARKER = 2;

    public static final PEdge NULL_EDGE = new PEdge(NULL_MARKER, NULL_MARKER);

    private int dst;
    private byte val;

    public PEdge(int dst, byte val) {
        this.dst = dst;
        this.val = val;
    }

    public PEdge(PEdge edge) {
        this.dst = edge.getDest();
        this.val = edge.getValue();
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
        return dst;
    }

    @Override
    public void setDest(int dst) {
        this.dst = dst;
    }

    @Override
    public boolean isNull() {
        return val == NULL_MARKER;
    }

    public boolean isSentinel() {
        return val == SENTINEL_MARKER;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PEdge otherEdge)) return false;

        return this.getDest() == otherEdge.getDest() && this.getValue() == otherEdge.getValue();
    }

    @Override
    public String toString() {
        return "Edge{" + "dest=" + dst + ", value=" + val + '}';
    }

    public void copy(PEdge source){
        if (source.isNull()){
            this.val = NULL_MARKER;
            this.dst = NULL_MARKER;
            return;
        }
        this.dst = source.dst;
        this.val = source.val;
    }

    public void setNull(){
        this.val = NULL_MARKER;
        this.dst = NULL_MARKER;
    }
}
