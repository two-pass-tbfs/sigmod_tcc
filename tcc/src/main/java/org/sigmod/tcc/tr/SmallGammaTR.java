package org.sigmod.tcc.tr;

import com.tinkerpop.blueprints.Vertex;
import org.sigmod.chronograph.common.Time;
import org.sigmod.chronograph.memstore.ChronoVertex;

import java.util.Arrays;
import java.util.BitSet;
import java.util.function.IntConsumer;

/**
 * Contains the visit times of the reachable vertices.
 */
public class SmallGammaTR {
    public static final Time NULL_TIME = null;

    private Time[] times;
    private int neighborCount = 0;

    public SmallGammaTR(int size) {
        this.times = new Time[size];
        Arrays.fill(this.times, NULL_TIME);
    }

    public void put(Vertex inVertex, Time visitTime) {
        int inIndex = ((ChronoVertex) inVertex).getIntId();

        this.put(inIndex, visitTime);
    }

    /**
     * Increases the list size if the index is greater than the current size </br>
     * Adds the index to the set of reachable vertices </br>
     * Sets the visit time at the index </br>
     *
     * @param inIndex   the index of the reachable vertex
     * @param visitTime the visit time of the reachable vertex
     */
    public void put(int inIndex, Time visitTime) {
        if (inIndex >= times.length) {
            times = Arrays.copyOf(times, inIndex + 1);
            times[inIndex] = NULL_TIME;
        }
        if (times[inIndex] == NULL_TIME && visitTime != NULL_TIME) neighborCount++;
        else if (times[inIndex] != NULL_TIME && visitTime == NULL_TIME)
            neighborCount--;

        this.times[inIndex] = visitTime;
    }

    /**
     * Gets the integer value of the reachable vertex
     *
     * @return the visit time of the reachableVertex or {@code null} if not reachable
     */
    public Time getTime(Vertex reachableVertex) {
        int index = ((ChronoVertex) reachableVertex).getIntId();

        if (index < 0 || index >= times.length)
            return NULL_TIME;

        return times[index];
    }

    public Time getTime(int index) {
        if (index < 0 || index >= times.length)
            return NULL_TIME;

        return times[index];
    }


    /**
     * Returns the number of reachable vertices
     *
     * @return the number of reachable vertices
     */
    public int getNeighborCount() {
        return this.neighborCount;
    }

    public void print() {
        for (Time time : times) {
            System.out.print(time + " ");
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Time time : times) {
            if (time != NULL_TIME) {
                sb.append(time).append(" ");
            } else {
                sb.append("null " + " ");
            }
        }
        return sb.toString();
    }

    public void forEachNeighbor(IntConsumer action) {
        for (int i = 0; i < times.length; i++) {
            if (times[i] != NULL_TIME) {
                action.accept(i);
            }
        }
    }

    public BitSet reachableVertices() {
        BitSet neighbors = new BitSet(times.length);
        for (int i = 0; i < times.length; i++) {
            if (times[i] != NULL_TIME) {
                neighbors.set(i);
            }
        }
        return neighbors;
    }

    public int size() {
        return times.length;
    }

    public Time get(int jId) {
        return times[jId];
    }

    public SmallGammaTR clone() {
        SmallGammaTR clone = new SmallGammaTR(this.size());
        clone.neighborCount = this.neighborCount;
        for (int i = 0; i < this.size(); i++) {
            clone.put(i, this.get(i));
        }
        return clone;
    }

    public void clear() {
        Arrays.fill(this.times, NULL_TIME);
        this.neighborCount = 0;
    }
}
