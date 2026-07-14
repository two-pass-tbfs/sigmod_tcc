package org.sigmod.tcc.tr.algos;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import org.apache.commons.lang3.time.StopWatch;
import org.sigmod.chronograph.common.EdgeEvent;
import org.sigmod.chronograph.common.TemporalRelation;
import org.sigmod.chronograph.common.Time;
import org.sigmod.chronograph.common.VertexEvent;
import org.sigmod.chronograph.memstore.ChronoEdge;
import org.sigmod.chronograph.memstore.ChronoVertex;
import org.sigmod.chronograph.memstore.ChronoVertexEvent;
import org.sigmod.tcc.tr.SmallGammaTR;

import java.util.*;

public class IterativeTR {
    private final SmallGammaTR gamma;
    private final Queue<VertexEvent> queue = new LinkedList<>();
    private StopWatch watch = new StopWatch();

    /**
     * Constructs with a gamma having an initial capacity
     *
     * @param capacity the initial capacity of gamma
     */
    public IterativeTR(int capacity) {
        gamma = new SmallGammaTR(capacity);
    }

    /**
     * Constructs with a pre-initialized gamma
     *
     * @param gamma the gamma to set
     */
    public IterativeTR(SmallGammaTR gamma) {
        this.gamma = gamma;
    }

    /**
     * Returns the computation time of the run
     *
     * @return the computation time
     */
    public long getComputationTime() {
        return watch.getTime();
    }

    /**
     * Computes the forward traversal from the source event and filters edges to process based on the included set
     *
     * @param g         the graph to traverse
     * @param source    the source to start
     * @param edgeLabel the label to match
     * @param included  the vertices to include
     * @return the resulting gamma containing temporal reachability information
     */
    public SmallGammaTR traverseForward(Graph g, VertexEvent source, String edgeLabel, BitSet included) {
        if (included.cardinality() == 0)
            return gamma;
        TemporalRelation tr = TemporalRelation.isAfter;
        final Collection<String> labels = Set.of(edgeLabel);

        gamma.put((Vertex) source.getElement(), source.getTime());

        queue.clear();
        queue.add(source);

        watch.start();
        while(!queue.isEmpty()) {
            VertexEvent vertexEvent = queue.poll();
            ChronoVertex outVertex = (ChronoVertex) vertexEvent.getElement();
            final Time eventTime = vertexEvent.getTime();

            // Do not explore the vertex event if the out vertex is not reachable,  if it has already been reached at an earlier time, or
            // it is not part of the included set.
            Time currentReachableTime = gamma.getTime(outVertex.getIntId());
            if (currentReachableTime == SmallGammaTR.NULL_TIME || currentReachableTime.compareTo(eventTime) < 0 ||
                    included.cardinality() == 0) {
                continue;
            }

            List<EdgeEvent> edgeEvents = new ArrayList<>();
            Iterator<Edge> edgeIterator = outVertex.getEdgeIterator(Direction.OUT, labels);

            while (edgeIterator.hasNext()) {
                Edge edge = edgeIterator.next();
                ChronoVertex inVertex = (ChronoVertex) edge.getVertex(Direction.IN);

                Time reachableTime = gamma.getTime(inVertex.getIntId());

                // First filter: Prune vertices already reached at an earlier time.
                if (reachableTime != SmallGammaTR.NULL_TIME && reachableTime.compareTo(eventTime) <= 0) {
                    continue;
                }

                // Attempt to get the event for the edge.
                EdgeEvent edgeEvent = edge.getEvent(eventTime, tr);

                if (edgeEvent == null) {
                    continue;
                }

                // Second filter: Add if the vertex is new or this is a better path.
                if (reachableTime == SmallGammaTR.NULL_TIME || reachableTime.compareTo(edgeEvent.getTime()) > 0) {
                    edgeEvents.add(edgeEvent);
                    gamma.put(inVertex.getIntId(), edgeEvent.getTime());
                    included.clear(inVertex.getIntId());
                }
            }

            edgeEvents.sort(ChronoEdge.TIME_COMPARATOR);
            for (EdgeEvent edgeEvent : edgeEvents) {
                queue.add(new ChronoVertexEvent(edgeEvent.getVertex(Direction.IN), edgeEvent.getTime()));
            }
        }

        watch.stop();
        return gamma;
    }

    /**
     * Computes the backward traversal from the source event and filters edges to process based on the included set
     *
     * @param g         the graph to traverse
     * @param source    the source to start
     * @param edgeLabel the label to match
     * @param included  the vertices to include
     * @return the resulting gamma containing temporal reachability information
     */
    public SmallGammaTR traverseBackward(Graph g, VertexEvent source, String edgeLabel, BitSet included) {
        if (included.cardinality() == 0)
            return gamma;

        TemporalRelation tr = TemporalRelation.isBefore;
        final Collection<String> labels = Set.of(edgeLabel);

        gamma.put((Vertex) source.getElement(), source.getTime());

        queue.clear();
        queue.add(source);

        watch.start();
        while(!queue.isEmpty()) {
            VertexEvent vertexEvent = queue.poll();
            ChronoVertex inVertex = (ChronoVertex) vertexEvent.getElement();
            final Time eventTime = vertexEvent.getTime();
            Time currentReachableTime = gamma.getTime(inVertex.getIntId());

            // Do not explore the vertex event if the out vertex is not reachable,  if it has already been reached at an earlier time, or
            // it is not part of the included set.
            if (currentReachableTime == SmallGammaTR.NULL_TIME || currentReachableTime.compareTo(eventTime) > 0 ||
                    included.cardinality() == 0) {
                continue;
            }

            List<EdgeEvent> edgeEvents = new ArrayList<>();
            Iterator<Edge> edgeIterator = inVertex.getEdgeIterator(Direction.IN, labels);

            while (edgeIterator.hasNext()) {
                Edge edge = edgeIterator.next();
                ChronoVertex outVertex = (ChronoVertex) edge.getVertex(Direction.OUT);
                Time reachableTime = gamma.getTime(outVertex.getIntId());

                // First filter: Prune if already reached at a later or equal time.
                if (reachableTime != SmallGammaTR.NULL_TIME && reachableTime.compareTo(eventTime) >= 0) {
                    continue;
                }

                // Attempt to get the event for the edge.
                EdgeEvent edgeEvent = edge.getEvent(eventTime, tr);

                if (edgeEvent == null) {
                    continue;
                }

                // Second filter: Add if the vertex is new or this is a better path.
                if (reachableTime == SmallGammaTR.NULL_TIME || reachableTime.compareTo(edgeEvent.getTime()) < 0) {
                    edgeEvents.add(edgeEvent);
                    gamma.put(outVertex.getIntId(), edgeEvent.getTime());
                    included.clear(outVertex.getIntId());
                }
            }


            edgeEvents.sort(ChronoEdge.TIME_COMPARATOR.reversed());
            for (EdgeEvent edgeEvent : edgeEvents) {
                queue.add(new ChronoVertexEvent(edgeEvent.getVertex(Direction.OUT), edgeEvent.getTime()));
            }
        }

        watch.stop();
        return gamma;
    }
}

