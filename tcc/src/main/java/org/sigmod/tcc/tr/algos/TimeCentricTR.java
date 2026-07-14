package org.sigmod.tcc.tr.algos;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import org.sigmod.chronograph.common.EdgeEvent;
import org.sigmod.chronograph.common.TemporalRelation;
import org.sigmod.chronograph.common.Time;
import org.sigmod.chronograph.memstore.ChronoVertex;
import org.sigmod.tcc.tr.BigGammaTR;
import org.sigmod.tcc.tr.SmallGammaTR;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Computes temporal reachability using time-centric computation.
 */
public class TimeCentricTR {
    private static final Logger log = Logger.getLogger(TimeCentricTR.class.getName());
    private final BigGammaTR bigGamma;
    private final Graph graph;
    private long computationTime;
    Logger logger;

    /**
     * Constructs time-centric temporal reachability algorithm
     *
     * @param graph    the graph to traverse
     * @param bigGamma the big gamma to update
     */
    public TimeCentricTR(Graph graph, BigGammaTR bigGamma) {
        this.bigGamma = bigGamma;
        this.graph = graph;
        this.logger = Logger.getLogger(TimeCentricTR.class.getName());
    }

    /**
     * Returns the computation time of this implementation.
     *
     * @return the computation time
     */
    public long getComputationTime() {
        return computationTime;
    }

    /**
     * Computes temporal reachability using time-centric computation with fixed-time point iteration.
     * It considers edge events with a temporal relation to specified base time.
     *
     * @param sources          the sources to compute
     * @param baseTime         the time reference
     * @param temporalRelation the temporal relation to match
     * @param includeBaseTime  a flag to include base time in the computation
     * @param edgeLabel        the edge label to match
     */
    public void computeWithIteration(Set<Vertex> sources, Time baseTime, TemporalRelation temporalRelation, Boolean includeBaseTime,
                                     String edgeLabel) {
        long pre = System.currentTimeMillis();

        NavigableMap<Time, List<EdgeEvent>> eventsByTime = graph.getEdgeEvents().stream().distinct()
                .filter(edgeEvent -> edgeEvent.getLabel().equals(edgeLabel))
                .filter(edgeEvent -> {
                    Time time = edgeEvent.getTime();

                    if (baseTime.equals(time) && includeBaseTime)
                        return true;
                    else if (temporalRelation == TemporalRelation.isAfter)
                        return baseTime.getStartTime() < time.getStartTime();
                    else if (temporalRelation == TemporalRelation.isBefore)
                        return baseTime.getStartTime() > time.getFinishTime();
                    else
                        throw new IllegalArgumentException("Temporal relation not supported");
                })
                .collect(Collectors.groupingBy(EdgeEvent::getTime, TreeMap::new, Collectors.toList()));

        eventsByTime.forEach((time, edgeEvents) -> {
            boolean hasChanged = true;
            int prevCount;
            int newCount;

            while (hasChanged) {
                hasChanged = false;
                for (EdgeEvent edgeEvent : edgeEvents) {
                    ChronoVertex outVertex = (ChronoVertex) edgeEvent.getVertex(Direction.OUT);
                    ChronoVertex inVertex = (ChronoVertex) edgeEvent.getVertex(Direction.IN);

                    for (Vertex source : sources) {
                        if (inVertex == source) continue;

                        ChronoVertex chronoSource = (ChronoVertex) source;

                        Time reachableTime = bigGamma.get(chronoSource.getId(), outVertex.getId());
                        if (reachableTime != SmallGammaTR.NULL_TIME && reachableTime.compareTo(time) <= 0) {
                            prevCount = bigGamma.getGamma(chronoSource.getIntId()).getNeighborCount();
                            bigGamma.set(chronoSource.getIntId(), inVertex.getIntId(), time);
                            newCount = bigGamma.getGamma(chronoSource.getIntId()).getNeighborCount();

                            if (newCount > prevCount) {
                                hasChanged = true;
                            }
                        }
                    }
                }
            }

        });

        computationTime = System.currentTimeMillis() - pre;
    }
}
