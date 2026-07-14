package org.sigmod.tcc.pcsr.analysis;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import org.apache.commons.lang3.time.StopWatch;
import org.sigmod.chronograph.common.EdgeEvent;
import org.sigmod.chronograph.common.Time;
import org.sigmod.chronograph.common.TimeInstant;
import org.sigmod.chronograph.common.VertexEvent;
import org.sigmod.chronograph.memstore.ChronoGraph;
import org.sigmod.chronograph.memstore.ChronoVertex;
import org.sigmod.chronograph.memstore.ChronoVertexEvent;
import org.sigmod.tcc.clique.CliqueUpdater;
import org.sigmod.tcc.clique.sparse.MCBRB;
import org.sigmod.tcc.clique.sparse.MCDD;
import org.sigmod.tcc.tr.SmallGammaTR;
import org.sigmod.tcc.pcsr.PCSR;
import org.sigmod.tcc.pcsr.PEdge;
import org.sigmod.tcc.pcsr.PVertex;
import org.sigmod.tcc.rgraph.RGraph;
import org.sigmod.tcc.tr.algos.IterativeTR;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Computes the largest open TCC using two-pass TBFS algorithm and PCSR
 */
public class TwoPassTBFSApp1 {
    private static final Logger log = Logger.getLogger(TwoPassTBFSApp1.class.getName());
    private static final String EDGE_LABEL = "label";

    public static long START_TIME = Long.MAX_VALUE;
    public static long LAST_TIME = 0;

    public static StopWatch trWatch = new StopWatch();
    public static StopWatch rGraphWatch = new StopWatch();

    /**
     * Checks all input file and prints error
     *
     * @throws FileNotFoundException
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
     * @throws IOException
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
                int logLineRead = lineRead;
                log.info(() -> String.format("%d vertices mapped...", logLineRead));
            }
        }
        br.close();
        ChronoGraph g = new ChronoGraph(vertexToID);

        log.info(() -> "Mapped vertices.");

        addEdgeEvents(g);

        log.info(() -> String.format("Temporal graph created. V = %d, E=%d, Ee = %d", g.getVertices().size(), g.getEdges().size(), g.getEventCount()));

        return g;
    }

    /**
     * Loads the sources from the file or uses all vertices as sources
     *
     * @param g the temporal graph
     * @return the list of source ids
     * @throws IOException if the file is not found or the buffered reader encounters an error
     */
    private static List<Integer> getSources(ChronoGraph g) throws IOException {
        log.info(() -> "Initializing the sources...");

        List<Integer> sources = new ArrayList<>(g.getVertices().size());

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
                    sources.add(((ChronoVertex) newVertex).getIntId());
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
            g.getVertices().stream().map(vertex -> ((ChronoVertex) vertex).getIntId()).forEach(sources::add);
        }

        sources.sort(Comparator.comparingInt(Integer::intValue));
        log.info(() -> "Sources initialized.");

        return sources;
    }

    /**
     * Adds all edge events from the file to the graph
     *
     * @param graph the graph in which all events will be added to
     */
    public static void addEdgeEvents(Graph graph) throws IOException {
        boolean isDirected = true;
        if (System.getProperty("directed") != null)
            isDirected = Boolean.parseBoolean(System.getProperty("directed"));

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

            if (START_TIME > time.getTime())
                START_TIME = time.getTime();
            if (LAST_TIME < time.getTime())
                LAST_TIME = time.getTime();

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
                log.info(() -> String.format("%d edge events added...", logLineRead));
            }
        }
        br.close();
        int logLineRead = lineRead;
        log.info(() -> String.format("%d edge events added.", logLineRead));
    }

    public static RGraph<PVertex, PEdge> createRGraph(ChronoGraph graph, List<Integer> sources) {
        // ----- STATS -----
        int sourcesProcessed = 0;
        trWatch.start();
        trWatch.suspend();
        rGraphWatch.start();
        rGraphWatch.suspend();

        RGraph<PVertex, PEdge> rGraph = new PCSR(graph.getVertices().size());
        List<PEdge> neighbors = new ArrayList<>(graph.getVertices().size());

        SmallGammaTR tempGamma = new SmallGammaTR(graph.getVertices().size());
        BitSet include = new BitSet(graph.getVertices().size());


        for (int sourceId = 0; sourceId < sources.size(); sourceId++) {
            Vertex source = graph.getVertex(sourceId);
            VertexEvent sourceEvent;
            trWatch.resume();

            // Compute isBefore from source
            include.set(0, sourceId);
            rGraph.forEachNeighbor(sourceId, include::clear);
            sourceEvent = new ChronoVertexEvent(source, new TimeInstant(LAST_TIME));
            SmallGammaTR predTR = new IterativeTR(tempGamma).traverseBackward(graph, sourceEvent, EDGE_LABEL, include);
            BitSet pred = predTR.reachableVertices();
            log.fine(() -> String.format("Predecessors of v(%s): %s", source.getId(), pred.cardinality()));

            tempGamma.clear();
            pred.clear(sourceId, rGraph.getVertices().size());

            if (pred.cardinality() == 0) {
                trWatch.suspend();
                continue;
            }

            // Compute isAfter from source
            include.clear();
            for (int i = pred.nextSetBit(0); i >= 0; i = pred.nextSetBit(i + 1)) {
                include.set(i);
            }
            sourceEvent = new ChronoVertexEvent(source, new TimeInstant(START_TIME));
            SmallGammaTR descTR = new IterativeTR(tempGamma).traverseForward(graph, sourceEvent, EDGE_LABEL, include);
            BitSet desc = descTR.reachableVertices();
            log.fine(() -> String.format("Descendants of v(%s): %s", source.getId(), desc.cardinality()));
            tempGamma.clear();

            trWatch.suspend();
            rGraphWatch.resume();

            pred.and(desc);

            log.fine(() -> String.format("Connected component size: %s", pred.cardinality()));
            log.fine(() -> String.format("TCC of v(%s): %s", source.getId(), pred));

            for (int neighborId = pred.nextSetBit(0); neighborId >= 0; neighborId = pred.nextSetBit(neighborId + 1)) {
                neighbors.add(new PEdge(neighborId, PEdge.EDGE_MARKER));
            }

            if (!neighbors.isEmpty()) {
                rGraph.addEdges(sourceId, neighbors);
                neighbors.clear();
            }

            rGraphWatch.suspend();

            if (++sourcesProcessed % 5000 == 0) {
                int logSourcesProcessed = sourcesProcessed;
                log.info(() -> String.format("%d sources processed. density = %f", logSourcesProcessed, rGraph.getDensity()));
            }
        }

        if (log.isLoggable(Level.FINER)) {
            for (int sourceId = 0; sourceId < rGraph.getVertices().size(); sourceId++) {
                PVertex pVertex = rGraph.getVertex(sourceId);
                List<PEdge> edges = rGraph.getEdges(sourceId);
                log.finer(() -> String.format("%s\n\t%d edges: %s", pVertex, edges.size(), edges));
                assert pVertex.getNeighborCount() == edges.size();
            }
        }

        return rGraph;
    }

    public static void main(String[] args) throws IOException {
        log.info(() -> TwoPassTBFSApp1.class.getName() + " started.");
        StopWatch cliqueWatch = new StopWatch();

        checkInputFiles();
        CliqueUpdater cliqueUpdater = new CliqueUpdater();

        ChronoGraph graph = createGraph();

        List<Integer> sources = getSources(graph);

        log.info(() -> "Creating the reachability graph...");
        RGraph<PVertex, PEdge> rGraph = createRGraph(graph, sources);
        log.info(() -> String.format("Temporal reachability computed in %s ms.", trWatch.getTime()));
        log.info(() -> String.format("Reachability graph created in %s ms. V = %d, E = %d", rGraphWatch.getTime(), rGraph.getVertices().size(), rGraph.getEdgeCount()));

        double unDirectedDensity = rGraph.getEdgeCount() / ((double) rGraph.getVertices().size() * (rGraph.getVertices().size() - 1));
        log.info(() -> String.format("Undirected density: %f", unDirectedDensity));

        log.info(() -> "Computing Max Clique...");
        cliqueWatch.start();
        ArrayList<Integer> sortedIds = graph.getVertices().stream().map(vertex -> ((ChronoVertex) vertex).getIntId()).collect(Collectors.toCollection(ArrayList::new));

        // Compute degrees of each vertex
        int[] degrees = new int[sortedIds.size()];
        Arrays.fill(degrees, 0);

        boolean[] visited = new boolean[sortedIds.size()];

        for (int id : sortedIds) {
            degrees[id] = rGraph.getVertex(id).getNeighborCount();
        }

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
                    if (rGraph.getEdge(uId, vId).isNull())
                        log.finer(() -> String.format("v(%d) -> v(%d) not found", uId, vId));
                }
            }
        }

        log.info(() -> String.format("Temporal reachability computed in %s ms.", trWatch.getTime()));
        log.info(() -> String.format("Reachability graph created in %s ms. V = %d, E = %d", rGraphWatch.getTime(), rGraph.getVertices().size(), rGraph.getEdgeCount()));
        log.info(() -> String.format("Max Clique computed in %d ms. Size: %d", cliqueWatch.getTime(), cliqueUpdater.getMaxClique().size()));


        log.finer(() -> String.format("RGraph Max Clique: %s", cliqueUpdater.getMaxClique()));
        log.info(() -> "---Finished---");
    }
}
