package org.sigmod.tcc.adjlist.analysis;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import org.apache.commons.lang3.time.StopWatch;
import org.sigmod.chronograph.common.EdgeEvent;
import org.sigmod.chronograph.common.Time;
import org.sigmod.chronograph.common.TimeInstant;
import org.sigmod.chronograph.memstore.ChronoGraph;
import org.sigmod.chronograph.memstore.ChronoVertex;
import org.sigmod.tcc.adjlist.ALEdge;
import org.sigmod.tcc.adjlist.ALGraph;
import org.sigmod.tcc.adjlist.ALVertex;
import org.sigmod.tcc.clique.CliqueUpdater;
import org.sigmod.tcc.clique.sparse.MCBRB;
import org.sigmod.tcc.clique.sparse.MCDD;
import org.sigmod.tcc.adjlist.ConnectedComponent;
import org.sigmod.util.MutableInteger;
import org.sigmod.tcc.rgraph.*;

import java.io.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Computes the largest open TCC using reversed-time scan application and adjacency list.
 */
public class ReverseScanApp1 {
    private static final Logger log = Logger.getLogger(ReverseScanApp1.class.getName());
    private static final String EDGE_LABEL = "label";
    private static final Time SOURCE_TIME = new TimeInstant(0);

    /**
     * Checks all input files and prints errors
     */
    private static void checkInputFiles() throws FileNotFoundException {
        boolean error = false;

        File sources = System.getProperty("sources") == null ? null : new File(System.getProperty("sources"));
        if (sources != null && sources.exists()) {
            log.info(() -> "Sources found in " + System.getProperty("sources"));
        } else {
            log.info(() -> "Sources file not initialized. Using all vertices as sources.");
        }

        File vertices = System.getProperty("vertices") == null ? null : new File(System.getProperty("vertices"));
        if (vertices != null && vertices.exists()) {
            log.info(() -> "Vertices found in " + System.getProperty("vertices"));
        } else {
            error = true;
            if (vertices == null)
                log.severe(() -> "-Dvertices=/path/ must be set.");
            else
                log.severe(() -> System.getProperty("vertices") + " does not exist.");
        }

        File stream = System.getProperty("stream") == null ? null : new File(System.getProperty("stream"));
        if (stream != null && stream.exists()) {
            log.info(() -> "Graph stream found in " + System.getProperty("stream"));
        } else {
            error = true;
            if (stream == null)
                log.severe(() -> "-Dstream=/path/ must be set.");
            else
                log.severe(() -> System.getProperty("stream") + " does not exist.");
        }

        if (System.getProperty("directed") == null || Boolean.parseBoolean(System.getProperty("directed")))
            log.info(() -> "Temporal graph is directed.");
        else
            log.info(() -> "Temporal graph is undirected.");

        if (error)
            throw new FileNotFoundException("Error in checking data sources. Please check the error messages above.");
    }

    /**
     * Loads the temporal graph from the file
     *
     * @return the temporal graph
     */
    public static ChronoGraph createGraph() throws IOException {
        log.info(() -> "Mapping vertices to integer IDs...");

        int lineRead = 0;
        BufferedReader br = new BufferedReader(new FileReader(System.getProperty("vertices")));
        HashMap<String, Integer> vertexToID = new HashMap<>();
        while (true) {
            String line = br.readLine();
            if (line == null)
                break;
            if (line.startsWith("#"))
                continue;

            String[] arr = line.split("[\\s,]+");
            int index = Integer.parseInt(arr[1]);

            vertexToID.put(arr[0], index);

            if (++lineRead % 10000 == 0) {
                log.info(lineRead + " vertices mapped...");
            }
        }
        br.close();
        ChronoGraph g = new ChronoGraph(vertexToID);
        log.info(() -> "Mapped vertices.");

        addEdgeEvents(g);

        log.info(() -> String.format("Temporal graph created. V = %d, E=%d, Ee = %d", g.getVertices().size(), g.getEdges().size(), g.getEdgeEvents().size()));

        return g;
    }

    /**
     * Computes the fixed-time reversed-time scan
     *
     * @param g       the temporal graph
     * @param sources the sources to compute
     * @return the adjacency-list-based reachability graph
     */
    public static RGraph<ALVertex, ALEdge> computeFixedTimeRS(ChronoGraph g, HashSet<Vertex> sources) {
        HashSet<Integer> sourcesIds = sources.stream().map(vertex -> ((ChronoVertex) vertex).getIntId()).collect(Collectors.toCollection(HashSet::new));
        RGraph<ALVertex, ALEdge> rGraph = new ALGraph(sourcesIds);

        MutableInteger edgeEventsProcessed = new MutableInteger(0);

        NavigableMap<Time, List<EdgeEvent>> eventsByTime = g.getEdgeEvents().stream().distinct()
                .filter(edgeEvent -> edgeEvent.getLabel().equals(EDGE_LABEL))
                .collect(Collectors.groupingBy(EdgeEvent::getTime, TreeMap::new, Collectors.toList()));

        eventsByTime.descendingMap().forEach((time, edgeEvents) -> {
            boolean hasChanged = true;
            while (hasChanged) {
                hasChanged = false;
                for (EdgeEvent edgeEvent : edgeEvents) {
                    ChronoVertex outV = (ChronoVertex) edgeEvent.getVertex(Direction.OUT);
                    ChronoVertex inV = (ChronoVertex) edgeEvent.getVertex(Direction.IN);

                    Collection<ALEdge> rEdges = rGraph.getEdges(inV.getIntId());
                    int prevEdgeCount = rGraph.getVertex(outV.getIntId()).getNeighborCount();
                    rGraph.addEdges(outV.getIntId(), rEdges);
                    int newEdgeCount = rGraph.getVertex(outV.getIntId()).getNeighborCount();

                    if (newEdgeCount == prevEdgeCount)
                        continue;

                    hasChanged = true;
                }
            }
            if (edgeEventsProcessed.addAndGet(edgeEvents.size()) % 1000 == 0) {
                log.info(() -> String.format("%d edge events processed...", edgeEventsProcessed.get()));
            }
        });

        return rGraph;
    }

    /**
     * Compute reversed-time scan
     *
     * @param g       the temporal graph
     * @param sources the sources to compute
     * @return the adjacency-list-based reachability graph
     */
    public static RGraph<ALVertex, ALEdge> computeTR(ChronoGraph g, HashSet<Vertex> sources) {
        HashSet<Integer> sourcesIds = sources.stream().map(vertex -> ((ChronoVertex) vertex).getIntId()).collect(Collectors.toCollection(HashSet::new));
        RGraph<ALVertex, ALEdge> rGraph = new ALGraph(sourcesIds);

        MutableInteger count = new MutableInteger(0);
        NavigableMap<Time, List<EdgeEvent>> eventsByTime = g.getEdgeEvents().stream().distinct()
                .filter(edgeEvent -> edgeEvent.getLabel().equals(EDGE_LABEL))
                .filter(edgeEvent -> {
                    Time time = edgeEvent.getTime();

                    return SOURCE_TIME.equals(time) || SOURCE_TIME.getStartTime() < time.getStartTime();
                })
                .collect(Collectors.groupingBy(EdgeEvent::getTime, TreeMap::new, Collectors.toList()));

        BiConsumer<Time, List<EdgeEvent>> eventProcessor = (_, edgeEvents) -> {
            Map<Vertex, Collection<Vertex>> components = ConnectedComponent.compute(edgeEvents);
            Set<ALEdge> edges = new HashSet<>();

            for (Collection<Vertex> values : components.values()) {
                edges.clear();
                for (Vertex v : values) {
                    ChronoVertex chronoVertex = (ChronoVertex) v;
                    Collection<ALEdge> rEdges = rGraph.getEdges(chronoVertex.getIntId());
                    edges.addAll(rEdges);
                }

                for (Vertex v : values) {
                    ChronoVertex chronoVertex = (ChronoVertex) v;
                    rGraph.addEdges(chronoVertex.getIntId(), edges);
                }
            }

            if (count.addAndGet(edgeEvents.size()) % 5000 == 0) {
                log.info(count.get() + " edge events processed...");
            }
        };

        eventsByTime.descendingMap().forEach(eventProcessor);

        return rGraph;
    }

    /**
     * Adds all edge events from the file to the graph
     *
     * @param graph the graph in which all events will be added to
     */
    public static void addEdgeEvents(Graph graph) throws IOException {
        boolean isDirected = true;
        if (System.getProperty("directed") != null) {
            isDirected = Boolean.parseBoolean(System.getProperty("directed"));
        }

        int lineRead = 0;
        BufferedReader br = new BufferedReader(new FileReader(System.getProperty("stream")));

        log.info(() -> "Adding edge events...");
        while (true) {
            String line = br.readLine();
            if (line == null)
                break;
            if (line.startsWith("#"))
                continue;

            String[] arr = line.split("[\\s,]+");

            Vertex from = graph.addVertex(arr[0]);
            Vertex to = graph.addVertex(arr[1]);
            Time time = new TimeInstant(Integer.parseInt(arr[2]));

            EdgeEvent edgeEvent;
            Edge fromTo = graph.addEdge(from, to, EDGE_LABEL);
            edgeEvent = fromTo.addEvent(time);
            if (edgeEvent != null)
                lineRead++;

            if (!isDirected) {
                Edge toFrom = graph.addEdge(to, from, EDGE_LABEL);
                edgeEvent = toFrom.addEvent(time);

                if (edgeEvent != null)
                    lineRead++;
            }

            if (lineRead % 10000 == 0) {
                int logLineRead = lineRead;
                log.info(() -> String.format("%d edge events added", logLineRead));
            }
        }

        br.close();
        int logLineRead = lineRead;
        log.info(() -> String.format("%d edge events added.", logLineRead));
    }

    /**
     * Creates the reachability graph by removing non-reciprocal vertex pairs
     *
     * @param rGraph the pre-initialized reachability graph
     */
    private static void createRGraph(RGraph<ALVertex, ALEdge> rGraph) {
        int sourcesProcessed = 0;
        for (RVertex uVertex : rGraph.getVertices()) {
            int edgeCount = rGraph.getEdges(uVertex.getId()).size();
            Iterator<ALEdge> edgeIterator = rGraph.edgeIterator(uVertex.getId());
            while (edgeIterator.hasNext()) {
                ALEdge edge = edgeIterator.next();
                // Remove self-loop and non-reciprocal pairs
                if (uVertex.getId() == edge.getDest() || rGraph.getEdge(edge.getDest(), uVertex.getId()) == null) {
                    edgeIterator.remove();
                    edgeCount--;
                }
            }
            uVertex.setNeighborCount(edgeCount);

            if (++sourcesProcessed % 5000 == 0) {
                int logSourcesProcessed = sourcesProcessed;
                log.info(() -> String.format("%d sources processed.", logSourcesProcessed));
            }
        }

        if (log.isLoggable(Level.FINER)) {
            for (RVertex vVertex : rGraph.getVertices()) {
                Collection<ALEdge> edges = rGraph.getEdges(vVertex.getId());
                log.finer(() -> String.format("%s\n\t%d edges: %s", vVertex, vVertex.getNeighborCount(), edges));
            }
        }
    }

    /**
     * Loads the sources from the file
     *
     * @param g the temporal graph
     * @return the sources
     */
    private static HashSet<Vertex> getSources(ChronoGraph g) throws IOException {
        log.info(() -> "Initializing the sources...");

        HashSet<Vertex> sources = new HashSet<>();

        if (System.getProperty("sources") != null) {
            int lineRead = 0;
            BufferedReader br = new BufferedReader(new FileReader(System.getProperty("sources")));
            while (true) {
                String line = br.readLine();
                if (line == null) break;
                if (line.startsWith("#")) continue;

                String[] arr = line.split("[\\s,]+");

                try {
                    Vertex newVertex = g.addVertex(arr[0]);
                    sources.add(newVertex);
                } catch (Exception e) {
                    g.getVertex(arr[0]);
                }

                if (++lineRead % 10000 == 0) {
                    int logLineRead = lineRead;
                    log.info(() -> String.format("%d sources added...", logLineRead));
                }
            }
            br.close();
        } else {
            sources.addAll(g.getVertices());
        }
        log.info(() -> "Sources initialized.");

        return sources;
    }

    public static void main(String[] args) throws IOException {
        log.info(ReverseScanApp1.class.getName() + " started.");
        checkInputFiles();
        StopWatch trWatch = new StopWatch();
        StopWatch rGraphWatch = new StopWatch();
        StopWatch cliqueWatch = new StopWatch();

        CliqueUpdater cliqueUpdater = new CliqueUpdater();

        ChronoGraph graph = createGraph();
        HashSet<Vertex> sources = getSources(graph);

        trWatch.start();
        RGraph<ALVertex, ALEdge> rGraph = computeFixedTimeRS(graph, sources);
        trWatch.stop();
        int directedEdgeCount = rGraph.getEdgeCount();
        log.info(() -> String.format("Temporal reachability computed in %s ms. E = %d", trWatch.getTime(), directedEdgeCount));

        double directedDensity = directedEdgeCount / ((double) rGraph.getVertices().size() * (rGraph.getVertices().size() - 1));
        log.info(() -> String.format("Directed Density: %f", directedDensity));

        log.info("Creating the reachability graph...");
        rGraphWatch.start();
        createRGraph(rGraph);
        rGraphWatch.stop();
        log.info(() -> String.format("Reachability graph created in %s ms. V = %d, E = %d", rGraphWatch.getTime(), rGraph.getVertices().size(), rGraph.getEdgeCount()));

        double unDirectedDensity = rGraph.getEdgeCount() / ((double) rGraph.getVertices().size() * (rGraph.getVertices().size() - 1));
        log.info(() -> String.format("Undirected Density: %f", unDirectedDensity));

        log.info(() -> "Computing Max Clique...");
        cliqueWatch.start();

        ArrayList<Integer> sortedIds = graph.getVertices().stream().map(vertex -> ((ChronoVertex) vertex).getIntId()).collect(Collectors.toCollection(ArrayList::new));

        // Compute degrees of each vertex
        int[] degrees = new int[sortedIds.size()];
        Arrays.fill(degrees, 0);
        for (int id : sortedIds) {
            degrees[id] = rGraph.getVertex(id).getNeighborCount();
        }

        boolean[] visited = new boolean[sortedIds.size()];

        sortedIds.sort(Comparator.comparingInt(id -> rGraph.getVertex((Integer) id).getNeighborCount()).reversed());
        HashSet<Integer> localClique = new HashSet<>();
        MCDD.degreeHeuristic(null, rGraph, sortedIds, 10, cliqueUpdater, localClique);

        MCBRB.compute(null, rGraph, sortedIds, degrees, visited, cliqueUpdater, localClique);
        cliqueWatch.stop();

        if (log.isLoggable(Level.FINER)) {
            for (int uId : cliqueUpdater.getMaxClique()) {
                for (int vId : cliqueUpdater.getMaxClique()) {
                    if (vId <= uId)
                        continue;
                    if (rGraph.getEdge(uId, vId) == null)
                        log.severe(() -> String.format("v(%d) -> v(%d) not found", uId, vId));
                }
            }
        }

        log.info(() -> String.format("Temporal reachability computed in %s ms. E = %d", trWatch.getTime(), directedEdgeCount));
        log.info(() -> String.format("Reachability graph created in %s ms. V = %d, E = %d", rGraphWatch.getTime(), rGraph.getVertices().size(), rGraph.getEdgeCount()));
        log.info(() -> String.format("Max Clique computed in %d ms. Size: %d", cliqueWatch.getTime(), cliqueUpdater.getMaxClique().size()));


        log.finer(() -> String.format("RGraph Max Clique: %s", cliqueUpdater.getMaxClique()));
        log.info(() -> "---Finished---");
    }
}
