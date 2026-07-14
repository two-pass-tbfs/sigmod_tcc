package org.sigmod.tcc.tr;

import com.tinkerpop.blueprints.Vertex;
import org.sigmod.chronograph.common.Time;
import org.sigmod.chronograph.memstore.ChronoGraph;
import org.sigmod.chronograph.memstore.IdManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Logger;

/**
 * Contains the gamma for each source vertex
 */
public class BigGammaTR {

    private static final Logger log = Logger.getLogger(BigGammaTR.class.getName());
    private final IdManager idManager;

    // The number of vertices
    private final int size;
    private HashSet<Vertex> sources;

    private HashMap<Integer, SmallGammaTR> gammaTable;

    private final Time srcTime;


    /**
     * The constructor for the Big Gamma
     *
     * @param graph   the reference graph
     * @param sources the source vertex events
     */
    public BigGammaTR(ChronoGraph graph, HashSet<Vertex> sources, Time srcTime) {
        this.idManager = graph.getVertexIdManager();
        this.size = idManager.size();
        this.sources = sources;
        this.srcTime = srcTime;

        this.gammaTable = new HashMap<>(sources.size());
        sources.forEach(source -> {
            int srcIdx = idManager.get(source.getId());
            SmallGammaTR gamma = new SmallGammaTR(size);
            gamma.put(srcIdx, srcTime);
            gammaTable.put(srcIdx, gamma);
        });
    }

    public int getSourceSize() {
        return sources.size();
    }

    public HashSet<Vertex> getSources() {
        return sources;
    }

    /**
     * Returns the gamma value of the reachable vertex at the source
     *
     * @param srcId the source vertex id
     * @param inId  the reachable vertex id
     * @return the gamma value
     */
    public Time get(String srcId, String inId) {
        int srcIdx = idManager.get(srcId);
        int inIdx = idManager.get(inId);

        return gammaTable.get(srcIdx).get(inIdx);
    }

    public Time get(Integer srcIdx, Integer inIdx) {
        return gammaTable.get(srcIdx).get(inIdx);
    }

    /**
     * Returns gamma of the source
     *
     * @param srcId the vertex id of the source
     * @return gamma
     */
    public SmallGammaTR getGamma(String srcId) {
        int srcIdx = idManager.get(srcId);

        return gammaTable.get(srcIdx);
    }

    public SmallGammaTR getGamma(int srcIdx) {
        return gammaTable.get(srcIdx);
    }


    /**
     * Sets the value of the reachable vertex at the given source
     *
     * @param srcIdx the integer id of the source
     * @param inIdx  the integer id of the reachable vertex
     * @param value  the value to set
     */
    public void set(Integer srcIdx, Integer inIdx, Time value) {
        if (srcIdx == null || inIdx == null) throw new NullPointerException();

        gammaTable.get(srcIdx).put(inIdx, value);
    }

    /**
     * Sets the gamma value of the reachable vertex at the source
     *
     * @param srcId the id of the source vertex event
     * @param inId  the id of the reachable vertex
     * @param value the visit time
     */
    public void set(String srcId, String inId, Time value) {
        Integer srcIdx = idManager.get(srcId);
        Integer inIdx = idManager.get(inId);

        if (srcIdx == null || inIdx == null) {
            log.severe("sourceId: " + srcId + " inId: " + inId + " not found");
        }

        this.set(srcIdx, inIdx, value);
    }

    /**
     * Sets the gamma value of the {@code reachableVertex} at the {@code sourceVertex}
     *
     * @param srcVertex the source vertex
     * @param dstVertex the reachable vertex
     * @param value     the visit time
     */
    public void set(Vertex srcVertex, Vertex dstVertex, Time value) {
        this.set(srcVertex.getId(), dstVertex.getId(), value);
    }

    /**
     * Returns the source time
     * @return the source time
     */
    public Time getSrcTime() {
        return srcTime;
    }

    public void print() {
        for (Vertex source : sources) {
            System.out.print(source.getId() + ": ");

            this.getGamma(source.getId()).print();
            System.out.println();
        }
    }

}
