package org.sigmod.chronograph.memstore;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import org.sigmod.chronograph.common.*;
import org.sigmod.chronograph.common.Tokens.EventComparator;
import org.sigmod.chronograph.common.Tokens.TimeComparator;

import java.util.*;
import java.util.stream.Collectors;

public class ChronoVertex implements Vertex {

    private final ChronoGraph g;
    private final String id;
    private final Integer intId;
    private final HashMap<String, Object> properties;
    private final NavigableSet<VertexEvent> events;

    ChronoVertex(ChronoGraph g, String id) {
        this.id = id;
        this.g = g;
        this.properties = new HashMap<>();
        this.events = new TreeSet<>();
        this.intId = g.getVertexIdManager().get(id);
    }

    @Override
    public Collection<Edge> getEdges(Direction direction, List<String> labels) {
        Map<String, HashSet<Edge>> edgeMap = direction.equals(Direction.OUT) ? g.getOutEdges() : g.getInEdges();

        Set<Edge> edgesOfId = edgeMap.get(id);

        if (edgesOfId == null || edgesOfId.isEmpty()) {
            return Collections.emptySet();
        }

        if (labels == null || labels.isEmpty())
            return edgesOfId;
        Set<String> labelSet = new HashSet<>(labels);

        return edgesOfId.stream().filter(edge -> labelSet.contains(edge.getLabel())).collect(Collectors.toSet());
    }

    public Iterator<Edge> getEdgeIterator(Direction direction, Collection<String> labels) {

        if (direction == Direction.BOTH) {
            return g.getEdges().parallelStream().filter(e -> e.getVertex(Direction.IN) == this || e.getVertex(Direction.OUT) == this)
                    .filter(e -> labels.contains(e.getLabel()))
                    .iterator();
        }

        HashSet<Edge> edgesOfId = null;

        if (direction == Direction.IN)
            edgesOfId = g.getInEdges().get(id);
        else if (direction == Direction.OUT)
            edgesOfId = g.getOutEdges().get(id);

        if (edgesOfId == null || edgesOfId.isEmpty()) {
            return Collections.emptyIterator();
        }

        if (labels == null || labels.isEmpty())
            return edgesOfId.iterator();

        return edgesOfId.stream().filter(edge -> labels.contains(edge.getLabel())).iterator();
    }

    public Collection<Edge> getEdges(Direction direction, List<String> labels, HashSet<Vertex> vIds) {
        Map<String, HashSet<Edge>> edgeMap = direction.equals(Direction.OUT) ? g.getOutEdges() : g.getInEdges();

        Set<Edge> edgesOfId = edgeMap.getOrDefault(id, new HashSet<>());

        if (labels == null || labels.isEmpty())
            return edgesOfId;
        Set<String> labelSet = new HashSet<>(labels);

        return edgesOfId.stream()
                .filter(edge -> vIds.contains(((ChronoVertex) edge.getVertex(direction.opposite())).getIntId()))
                .filter(edge -> labelSet.contains(edge.getLabel())).collect(Collectors.toSet());
    }

    @Override
    public Collection<Vertex> getVertices(Direction direction, List<String> labels) {
        HashMap<String, HashSet<Edge>> edgeSet = null;
        if (direction.equals(Direction.OUT))
            edgeSet = g.getOutEdges();
        else if (direction.equals(Direction.IN))
            edgeSet = g.getInEdges();
        else
            return Collections.emptySet();

        if (edgeSet == null || !edgeSet.containsKey(id))
            return new HashSet<>();

        HashSet<String> labelSet = new HashSet<>(labels);
        return edgeSet.get(id).stream().filter(e -> (labels == null || labelSet.contains(e.getLabel())))
                .map(e -> e.getVertex(direction.opposite()))
                .collect(Collectors.toSet());
    }

    @Override
    public Edge addEdge(String label, Vertex inVertex) {
        return g.addEdge(this, inVertex, label);
    }

    @Override
    public void remove() {
        g.removeVertex(this);
    }

    /// ////////////////////////
    /// // Temporal Support ////
    /// ////////////////////////

    @Override
    public VertexEvent addEvent(Time time) {
        VertexEvent newVertexEvent = new ChronoVertexEvent(this, time);
        events.add(newVertexEvent);
        return newVertexEvent;
    }

    @Override
    public VertexEvent getEvent(Time time) {
        return new ChronoVertexEvent(this, time);
    }

    @Override
    public NavigableSet<VertexEvent> getEvents(Time time, TemporalRelation tr, boolean awareOutEvents,
                                               boolean awareInEvents, EventComparator eventComparator) {

        NavigableSet<VertexEvent> resultSet = new TreeSet<>(eventComparator);

        resultSet.addAll(this.getEvents(time, tr));

        List<Direction> directions = new LinkedList<>();
        if (awareOutEvents)
            directions.add(Direction.OUT);
        if (awareInEvents)
            directions.add(Direction.IN);

        for (Direction direction : directions) {
            for (Vertex v : this.getVertices(direction, null)) {
                if (v.equals(this))
                    continue;
                resultSet.addAll(v.getEvents(time, tr, false, false, eventComparator));
            }
        }

        return resultSet;
    }

    @Override
    public void removeEvents(Time time, TemporalRelation temporalRelation) {
        this.events.removeIf(event -> time.getTemporalRelation(event.getTime()).equals(temporalRelation));
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getProperty(String key) {
        return (T) properties.get(key);
    }

    @Override
    public Set<String> getPropertyKeys() {
        return this.properties.keySet();
    }

    @Override
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T removeProperty(String key) {
        return (T) properties.remove(key);
    }

    @Override
    public NavigableSet<Time> getTimes(TimeComparator timeComparator) {
        NavigableSet<Time> times = new TreeSet<>(timeComparator);
        times.addAll(this.events.stream().map(Event::getTime).toList());
        return times;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ChronoVertex))
            return false;
        return this.getId().equals(((ChronoVertex) obj).getId());
    }

    @Override
    public String toString() {
        return id;
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> NavigableSet<T> getEvents(Time time, TemporalRelation... temporalRelations) {
        HashSet<TemporalRelation> temporalRelationSet = new HashSet<>(Arrays.asList(temporalRelations));
        if (temporalRelations == null || temporalRelationSet.isEmpty()) {
            return Collections.emptyNavigableSet();
        }

        // Use a stream to filter and collect the events
        return (NavigableSet<T>) this.events.stream()
                .filter(event -> temporalRelationSet.stream()
                        .anyMatch(tr -> event.getTime().checkTemporalRelation(time, tr)))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> T getEvent(Time time, TemporalRelation tr) {
        for (Event event : this.events) {
            if (time.getTemporalRelation(event.getTime()).equals(tr))
                return (T) event;
        }

        return null;
    }

    public Integer getIntId() {
        return intId;
    }
}
