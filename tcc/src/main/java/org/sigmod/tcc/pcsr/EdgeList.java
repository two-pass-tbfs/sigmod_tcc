package org.sigmod.tcc.pcsr;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

/**
 * Constructs a PCSR-based edge list.
 */
public class EdgeList {
    private int N;
    private int H;
    private int logN;
    private int min_N;
    private PEdge[] items;

    private Logger log = Logger.getLogger(EdgeList.class.getName());

    public EdgeList(int n) {
//        min_N = n * 2;
        min_N = n;
        N = 2 << bsrWord(min_N);
//        log.info(String.format("bsrWord(%d) = %d", min_N, N));
        logN = (1 << bsrWord(bsrWord(N) + 1));
        H = bsrWord(N / logN);

        items = new PEdge[N];
        for (int i = 0; i < N; i++) {
            items[i] = new PEdge(PEdge.NULL_MARKER, PEdge.NULL_MARKER);
        }
//        log.info(String.format("EdgeList of n = %d created with N = %d, H = %d, logN = %d", n, N, H, logN));
    }

    public int getN() {
        return N;
    }

    public int getMinN() {
        return min_N;
    }

    public int getH() {
        return H;
    }

    public int getLogN() {
        return logN;
    }

    public PEdge[] getItems() {
        return items;
    }

    public PEdge getItem(int index) {
        return items[index];
    }

    public void setH(int h) {
        H = h;
    }

    public void setN(int n) {
        N = n;
    }

    public void setItem(int index, PEdge elem) {
        items[index] = elem;
    }

    public void doubleList() {
        int oldN = N;

        N *= 2;
        logN = (1 << bsrWord(bsrWord(N) + 1));
        H = bsrWord(N / logN);

        PEdge[] newItems = new PEdge[N];
        System.arraycopy(items, 0, newItems, 0, oldN);


        Arrays.fill(newItems, oldN, N, PEdge.NULL_EDGE);
        items = newItems;
    }

    public void expandList(int factor) {
        if (factor % 2 != 0)
            throw new IllegalArgumentException("factor must be even");

        int oldN = N;
        N *= factor;

        logN = (1 << bsrWord(bsrWord(N) + 1));
        H = bsrWord(N / logN);
        PEdge[] newItems = new PEdge[N];
        System.arraycopy(items, 0, newItems, 0, oldN);

        Arrays.fill(newItems, oldN, N, PEdge.NULL_EDGE);

        items = newItems;
    }

    /**
     * Shrinks the list by a factor of 2
     *
     * @param factor the factor by which to shrink the list
     */
    public void shrinkList(int factor) {
        if (factor % 2 != 0)
            throw new IllegalArgumentException("shrinkFactor must be even");

        int oldN = N;
        N = Math.max(min_N, N / factor);

        logN = (1 << bsrWord(bsrWord(N) + 1));
        H = bsrWord(N / logN);
        PEdge[] newItems = new PEdge[N];
        Arrays.fill(newItems, PEdge.NULL_EDGE);

        int j = 0;
        for (int i = 0; i < oldN && j < N; i++) {
            if (items[i].isNull()) continue;

            newItems[j++] = items[i];
        }
        items = newItems;
    }

    /**
     * Return the number of non-null (sentinels + data) edges
     *
     * @return the number of non-null edges
     */
    public int count() {
        int count = 0;
        for (PEdge item : items) {
            if (!item.isNull()) count++;
        }
        return count;
    }

    /**
     * Find the index of the most significant set bit in a 32-bit integer
     *
     * @param word the integer to scan
     * @return -1 if the input word is 0
     */
    private static int bsrWord(int word) {
        if (word == 0) return -1;
        return 31 - Integer.numberOfLeadingZeros(word);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (PEdge item : items) {
            sb.append(item).append("   ");
        }
        return sb.toString();
    }

    public Iterator<PEdge> iterator(int start, int end) {
        return new EdgeIterator().reset(start, end);
    }

    private class EdgeIterator implements Iterator<PEdge> {
        private int index = 0;
        private int end;

        public EdgeIterator reset(int start, int end) {
            this.index = start;
            this.end = end;
            return this;
        }

        @Override
        public boolean hasNext() {
            while (index < end && items[index] == null) {
                index++;
            }
            return index < end;
        }

        @Override
        public PEdge next() {
            if (!hasNext()) throw new NoSuchElementException();

            return items[index++];
        }
    }
}
