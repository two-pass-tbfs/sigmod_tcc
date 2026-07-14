package org.sigmod.chronograph.memstore;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import org.sigmod.chronograph.common.EdgeEvent;
import org.sigmod.chronograph.common.Time;
import org.sigmod.chronograph.common.Tokens.TimeComparator;

import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ChronoGraph implements Graph {

    /**
     * Vertices queried by a vertex identifier
     */
    private final HashMap<String, Vertex> vertices;

    /**
     * Edges queried by an edge identifier
     */
    private final HashMap<String, Edge> edges;

    /**
     * simple index for out-going edges by a key of vertex identifier
     * <br/>
     * <vertexID, HashSet<out-going edges>
     */
    private HashMap<String, HashSet<Edge>> outEdges;

    /**
     * simple index for in-going edges by a key of vertex identifier
     * <br/>
     * <vertexID, HashSet<in-going edges>
     */
    private HashMap<String, HashSet<Edge>> inEdges;

    /**
     * an index for vertex IDs
     */
    private IdManager vertexIdManager;

    /**
     * an index for edge labels
     */
    private LabelManager labelIdManager;

    /**
     * Create an empty graph
     */
    public ChronoGraph(Path filePath, HashSet<String> idSet) {
        vertices = new HashMap<>();
        edges = new HashMap<>();
        outEdges = new HashMap<>();
        inEdges = new HashMap<>();
        vertexIdManager = new IdManager(idSet);
        labelIdManager = new LabelManager();
    }

    /**
     * Create an empty graph
     */
    public ChronoGraph(HashMap<String, Integer> vertexIdSet) {
        vertices = new HashMap<>();
        edges = new HashMap<>();
        outEdges = new HashMap<>();
        inEdges = new HashMap<>();
        vertexIdManager = new IdManager(vertexIdSet);
        labelIdManager = new LabelManager();

        for(String id : vertexIdSet.keySet()) {
            vertices.put(id, new ChronoVertex(ChronoGraph.this, id));
        }
    }

    /**
     * Create an empty graph
     */
    public ChronoGraph(IdManager vertexIdManager, LabelManager labelIdManager) {
        this.vertices = new HashMap<>();
        this.edges = new HashMap<>();
        this.outEdges = new HashMap<>();
        this.inEdges = new HashMap<>();
        this.vertexIdManager = vertexIdManager;
        this.labelIdManager = labelIdManager;
    }

    /**
     * Returns a vertex ID manager
     *
     * @return the vertex ID manager
     */
    public IdManager getVertexIdManager() {
        return this.vertexIdManager;
    }

    /**
     * Returns a label ID manager
     *
     * @return the label id manager
     */
    public LabelManager getLabelIdManager() {
        return this.labelIdManager;
    }

    /**
     * Create a new vertex, add it to the graph, and return the newly created
     * vertex. The provided object identifier is a recommendation for the identifier
     * to use. It is not required that the implementation use this identifier.
     *
     * @param id the recommended object identifier
     * @return the newly created vertex
     */
    @Override
    public Vertex addVertex(String id) {
        if (id.contains("|"))
            throw new IllegalArgumentException("Vertex ID cannot contain '|'");

        return vertices.compute(id, (String identifier, Vertex existingVertex) -> {
            if (existingVertex == null)
                return new ChronoVertex(ChronoGraph.this, identifier);
            return existingVertex;
        });
    }

    /**
     * Return the vertex referenced by the provided object identifier. If no vertex
     * is referenced by that identifier, then return null.
     *
     * @param id the identifier of the vertex to retrieve from the graph
     * @return the vertex referenced by the provided identifier or null when no such
     * vertex exists
     */
    @Override
    public Vertex getVertex(String id) {
        return vertices.get(id);
    }

    /**
     * Returns the vertex referenced by the ID
     *
     * @param id the integer index
     * @return the vertex
     */
    public Vertex getVertex(Integer id) {
        String stringId = vertexIdManager.get(id);
        return vertices.get(stringId);
    }

    /**
     * Return an iterable to all the vertices in the graph. If this is not possible
     * for the implementation, then an UnsupportedOperationException can be thrown.
     *
     * @return an iterable reference to all vertices in the graph
     */
    @Override
    public Collection<Vertex> getVertices() {
        return vertices.values();
    }

    /**
     * Return an iterable to all the vertices in the graph that have a particular
     * key/value property. If this is not possible for the implementation, then an
     * UnsupportedOperationException can be thrown. The graph implementation should
     * use indexing structures to make this efficient else a full vertex-filter scan
     * is required.
     *
     * @param key   the key of vertex
     * @param value the value of the vertex
     * @return an iterable of vertices with a provided key and value
     */
    @Override
    public Collection<Vertex> getVertices(String key, Object value) {
        return vertices.values().parallelStream().filter(v -> {
            Object val = v.getProperty(key);
            return val != null && (val.equals(value));
        }).collect(Collectors.toSet());
    }

    /**
     * Add an edge to the graph. The added edges require a recommended identifier,
     * a tail vertex, a head vertex, and a label. Like adding a vertex, the
     * implementation may ignore the provided object identifier.
     *
     * @param outVertex the vertex on the tail of the edge
     * @param inVertex  the vertex on the head of the edge
     * @param label     the label associated with the edge
     * @return the newly created edge
     */
    @Override
    public Edge addEdge(Vertex outVertex, Vertex inVertex, String label) {
        if (label.contains("|"))
            throw new IllegalArgumentException("An edge label cannot contain '|'");
        this.addVertex(outVertex.getId());
        this.addVertex(inVertex.getId());
        String edgeId = ChronoEdge.getEdgeID(outVertex, inVertex, label);

        if (edges.containsKey(edgeId))
            return edges.get(edgeId);

        final Edge edge = new ChronoEdge(ChronoGraph.this, outVertex, label, inVertex);
        edges.put(edgeId, edge);

        // update an index for out-going edges
        outEdges.compute(outVertex.getId(), (String _, HashSet<Edge> outEdges) -> {
            if (outEdges == null)
                outEdges = new HashSet<>();
            outEdges.add(edge);
            return outEdges;
        });

        // update an index for in-going edges
        inEdges.compute(inVertex.getId(), (String _, HashSet<Edge> inEdges) -> {
            if (inEdges == null)
                inEdges = new HashSet<>();
            inEdges.add(edge);
            return inEdges;
        });

        return edge;
    }

    /**
     * Return the edge referenced by the provided object identifier. If no edge is
     * referenced by that identifier, then return null.
     *
     * @param outVertex the identifier of the out-going vertex to retrieve from the
     *                  graph
     * @param inVertex  the identifier of the in-going vertex to retrieve from the
     *                  graph
     * @param label     the label of the edge
     * @return the edge referenced by the provided identifier or null when no such
     * edge exists
     */
    @Override
    public Edge getEdge(Vertex outVertex, Vertex inVertex, String label) {
        return edges.get(ChronoEdge.getEdgeID(outVertex, inVertex, label));
    }

    /**
     * Returns the edge referenced by the parameters
     *
     * @param outVertexIntID the out-going vertex ID
     * @param inVertexIntID  the in-going vertex ID
     * @param labelIntID     the label ID
     * @return the edge
     */
    public Edge getEdge(Integer outVertexIntID, Integer inVertexIntID, Integer labelIntID) {
        String outVertexID = vertexIdManager.get(outVertexIntID);
        Vertex outVertex = getVertex(outVertexID);

        String inVertexID = vertexIdManager.get(inVertexIntID);
        Vertex inVertex = getVertex(inVertexID);

        if (outVertex == null || inVertex == null) return null;

        String label = labelIdManager.getId(labelIntID);
        return edges.get(ChronoEdge.getEdgeID(outVertex, inVertex, label));
    }

    /**
     * Return the edge referenced by the provided object identifier. If no edge is
     * referenced by that identifier, then return null.
     *
     * @param id the identifier of the edge to retrieve from the graph
     * @return the edge referenced by the provided identifier or null when no such
     * edge exists
     */
    @Override
    public Edge getEdge(String id) {
        return edges.get(id);
    }

    /**
     * Return an iterable to all the edges in the graph. If this is not possible for
     * the implementation, then an UnsupportedOperationException can be thrown.
     *
     * @return an iterable reference to all edges in the graph
     */
    @Override
    public Collection<Edge> getEdges() {
        return edges.values();
    }

    /**
     * Return an iterable to all the edges in the graph that have a particular
     * key/value property. If this is not possible for the implementation, then an
     * UnsupportedOperationException can be thrown. The graph implementation should
     * use indexing structures to make this efficient, else a full edge-filter scan
     * is required.
     *
     * @param key   the key of the edge
     * @param value the value of the edge
     * @return an iterable of edges with a provided key and value
     */
    @Override
    public Collection<Edge> getEdges(String key, Object value) {
        return edges.values().parallelStream().filter(v -> {
            Object val = v.getProperty(key);
            return val != null && (val.equals(value));
        }).collect(Collectors.toSet());
    }

    /**
     * Remove the provided vertex from the graph. Upon removing the vertex, all the
     * edges by which the vertex is connected must be removed as well.
     *
     * @param vertex the vertex to remove from the graph
     */
    @Override
    public void removeVertex(Vertex vertex) {
        String id = vertex.getId();
        this.vertices.remove(id);

        // update a key-value map for edges
        Iterator<Entry<String, Edge>> eIter = edges.entrySet().iterator();
        while (eIter.hasNext()) {
            Entry<String, Edge> entry = eIter.next();
            Edge cEdge = entry.getValue();
            if (cEdge.getVertex(Direction.OUT).equals(vertex) || cEdge.getVertex(Direction.IN).equals(vertex)) {
                eIter.remove();
            }
        }
        // indexes
        Iterator<Entry<String, HashSet<Edge>>> outIter = outEdges.entrySet().iterator();
        while (outIter.hasNext()) {
            Entry<String, HashSet<Edge>> entry = outIter.next();
            if (entry.getKey().equals(id)) {
                outIter.remove();
            } else {
                entry.getValue().removeIf(e -> e.getVertex(Direction.IN).equals(vertex));
                if (entry.getValue().isEmpty())
                    outIter.remove();
            }
        }
        Iterator<Entry<String, HashSet<Edge>>> inIter = inEdges.entrySet().iterator();
        while (inIter.hasNext()) {
            Entry<String, HashSet<Edge>> entry = inIter.next();
            if (entry.getKey().equals(id)) {
                inIter.remove();
            } else {
                entry.getValue().removeIf(e -> e.getVertex(Direction.OUT).equals(vertex));
                if (entry.getValue().isEmpty())
                    inIter.remove();
            }
        }
    }

    @Override
    public void removeEdge(Edge edge) {
        this.edges.remove(edge.toString());
        this.outEdges.values().forEach(set -> set.remove(edge));
        this.inEdges.values().forEach(set -> set.remove(edge));
    }

    /**
     * Do Nothing in an in-memory graph
     */
    @Override
    public void shutdown() {
        // Do Nothing
    }

    HashMap<String, HashSet<Edge>> getOutEdges() {
        return outEdges;
    }

    void setOutEdges(HashMap<String, HashSet<Edge>> outEdges) {
        this.outEdges = outEdges;
    }

    HashMap<String, HashSet<Edge>> getInEdges() {
        return inEdges;
    }

    void setInEdges(HashMap<String, HashSet<Edge>> inEdges) {
        this.inEdges = inEdges;
    }

    @Override
    public Collection<EdgeEvent> getEdgeEvents() {
        return getEdges().parallelStream().flatMap(e -> e.getEvents().parallelStream()).collect(Collectors.toSet());
    }

    public int getEventCount(){
        return getEdges().parallelStream().flatMapToInt(e -> IntStream.of(((ChronoEdge)e).getEventCount())).sum();
    }

    @Override
    public NavigableSet<Time> getTimes(TimeComparator timeComparator, boolean scanVertexEvents,
                                       boolean scanEdgeEvents) {
        TreeSet<Time> timeSet = new TreeSet<>(timeComparator);
        if (scanVertexEvents) {
            getVertices().parallelStream().flatMap(v -> v.getTimes(timeComparator).parallelStream())
                    .forEach(timeSet::add);
        }
        if (scanEdgeEvents) {
            for (Edge e : getEdges()) {
                timeSet.addAll(e.getTimes(timeComparator));
            }
        }
        return timeSet;
    }

    @Override
    public void clear() {
        vertices.clear();
        edges.clear();
        outEdges.clear();
        inEdges.clear();
        vertexIdManager = null;
        labelIdManager = null;
    }
}
