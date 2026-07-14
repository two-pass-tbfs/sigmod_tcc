package org.sigmod.tcc.csr.analysis;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import org.apache.commons.lang3.time.StopWatch;
import org.sigmod.chronograph.common.EdgeEvent;
import org.sigmod.chronograph.common.TemporalRelation;
import org.sigmod.chronograph.common.Time;
import org.sigmod.chronograph.common.TimeInstant;
import org.sigmod.chronograph.memstore.ChronoGraph;
import org.sigmod.chronograph.memstore.ChronoVertex;
import org.sigmod.tcc.clique.CliqueUpdater;
import org.sigmod.tcc.clique.sparse.MCBRB;
import org.sigmod.tcc.clique.sparse.MCDD;
import org.sigmod.tcc.csr.CEdge;
import org.sigmod.tcc.csr.CGraph;
import org.sigmod.tcc.csr.CVertex;
import org.sigmod.tcc.tr.BigGammaTR;
import org.sigmod.tcc.tr.SmallGammaTR;
import org.sigmod.tcc.tr.algos.TimeCentricTR;
import org.sigmod.tcc.rgraph.RGraph;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Computes the temporal reachability of a graph using the time-centric approach and CSR
 */
public class TimeCentricApp1 {
    private static final Logger log = Logger.getLogger(TimeCentricApp1.class.getName());
    private static final String EDGE_LABEL = "label";
    private static final Time SOURCE_TIME = new TimeInstant(0);

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
            if (line == null) break;
            if (line.startsWith("#")) continue;

            String[] arr = line.split("[\\s,]+");
            int index = Integer.parseInt(arr[1]);

            vertexToID.put(arr[0], index);

            if (++lineRead % 10000 == 0) {
                int logLineRead = lineRead;
                log.info(() -> logLineRead + " vertices mapped...");
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
     * Compute the temporal reachability using the time-centric approach with fixed-time point iteration
     *
     * @return the program containing the graph and Gamma
     */
    public static BigGammaTR computeTR(ChronoGraph g, HashSet<Vertex> sources) {
        log.info(() -> "Creating the Gamma Table...");
        BigGammaTR bigGamma = new BigGammaTR(g, sources, SOURCE_TIME);
        log.info(() -> "Gamma table created.");

        log.info(() -> "Computing temporal reachability...");

        new TimeCentricTR(g, bigGamma).computeWithIteration(sources, SOURCE_TIME, TemporalRelation.isAfter, true, EDGE_LABEL);
        log.info(() -> "Temporal reachability computed.");
        return bigGamma;
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
                    log.info(() -> logLineRead + " sources added...");
                }
            }
            br.close();
        } else {
            sources.addAll(g.getVertices());
        }
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
        if (System.getProperty("directed") != null) {
            isDirected = Boolean.parseBoolean(System.getProperty("directed"));
        }

        log.info(() -> "Adding edge events...");

        int lineRead = 0;
        BufferedReader br = new BufferedReader(new FileReader(System.getProperty("stream")));

        while (true) {
            String line = br.readLine();
            if (line == null) break;
            if (line.startsWith("#")) continue;

            String[] arr = line.split("[\\s,]+");

            Vertex from = graph.addVertex(arr[0]);
            Vertex to = graph.addVertex(arr[1]);
            Time time = new TimeInstant(Integer.parseInt(arr[2]));

            EdgeEvent edgeEvent;
            Edge fromTo = graph.addEdge(from, to, EDGE_LABEL);
            edgeEvent = fromTo.addEvent(time);
            if (edgeEvent != null) lineRead++;

            if (!isDirected) {
                Edge toFrom = graph.addEdge(to, from, EDGE_LABEL);
                edgeEvent = toFrom.addEvent(time);

                if (edgeEvent != null) lineRead++;
            }

            if (lineRead % 10000 == 0) {
                int logLineRead = lineRead;
                log.info(() -> logLineRead + " edge events added...");
            }
        }
        br.close();
        log.info(() -> "Edge events added.");
    }

    /**
     * Creates the reachability graph by removing non-reciprocal vertex pairs and converting the temporal reachability to CSR
     *
     * @param gammaTable the table containing the temporal reachability
     * @return the reachability graph
     */
    public static RGraph<CVertex, CEdge> createRGraph(BigGammaTR gammaTable) {
        int sourcesProcessed = 0;
        RGraph<CVertex, CEdge> rGraph = null;
        int edgeCount = 0;

        // Remove self-referencing edges and non-reciprocal edges
        for (Vertex source : gammaTable.getSources()) {
            int sourceId = ((ChronoVertex) source).getIntId();
            SmallGammaTR gamma = gammaTable.getGamma(sourceId);

            for (int jId = 0; jId < gamma.size(); jId++) {
                if (sourceId == jId) {
                    gamma.put(sourceId, SmallGammaTR.NULL_TIME);
                    continue;
                }

                if (gamma.get(jId) == SmallGammaTR.NULL_TIME) {
                    gammaTable.set(jId, sourceId, SmallGammaTR.NULL_TIME);
                    continue;
                }

                if (gammaTable.get(jId, sourceId) == SmallGammaTR.NULL_TIME) {
                    gamma.put(jId, SmallGammaTR.NULL_TIME);
                }
            }

            if (++sourcesProcessed % 5000 == 0) {
                int logSourcesProcessed = sourcesProcessed;
                log.info(() -> logSourcesProcessed + " sources processed...");
            }
        }

        // Initialize CSR graph
        edgeCount = gammaTable.getSources().stream()
                .mapToInt(source -> gammaTable.getGamma(source.getId()).getNeighborCount())
                .sum();

        rGraph = new CGraph(gammaTable.getSourceSize(), edgeCount);

        // Add CSR edges
        for (int sourceId = 0; sourceId < rGraph.getVertices().size(); sourceId++) {
            SmallGammaTR gamma = gammaTable.getGamma(sourceId);
            for (int jId = 0; jId < gamma.size(); jId++) {
                if (gamma.get(jId) == SmallGammaTR.NULL_TIME) continue;
                rGraph.addEdge(sourceId, jId, 1);
            }
        }

        return rGraph;
    }

    public static void main(String[] args) throws IOException {
        log.info(() -> TimeCentricApp1.class.getName() + " started.");
        checkInputFiles();
        StopWatch tcWatch = new StopWatch();
        StopWatch cliqueWatch = new StopWatch();

        CliqueUpdater cliqueUpdater = new CliqueUpdater();
        ChronoGraph graph = createGraph();
        HashSet<Vertex> sources = getSources(graph);

        tcWatch.start();
        BigGammaTR bigGamma = computeTR(graph, sources);
        tcWatch.stop();

        int directedEdgeCount = IntStream.range(0, bigGamma.getSourceSize()).mapToObj(bigGamma::getGamma).mapToInt(SmallGammaTR::getNeighborCount).sum();
        log.info(() -> String.format("Temporal reachability computed in %s ms. E = %d", tcWatch.getTime(), directedEdgeCount));
        double directedDensity = directedEdgeCount / ((double) graph.getVertices().size() * (graph.getVertices().size() - 1));
        log.info(() -> String.format("Directed Density: %f", directedDensity));

        log.info(() -> "Creating the reachability graph...");
        StopWatch rGraphWatch = new StopWatch();
        rGraphWatch.start();
        RGraph<CVertex, CEdge> rGraph = createRGraph(bigGamma);
        rGraphWatch.stop();
        log.info(() -> String.format("Reachability graph created in %s ms. V = %d, E = %d", rGraphWatch.getTime(), rGraph.getVertices().size(), rGraph.getEdgeCount()));
        double unDirectedDensity = rGraph.getEdgeCount() / ((double) rGraph.getVertices().size() * (rGraph.getVertices().size() - 1));
        log.info(() -> String.format("Undirected Density: %f", unDirectedDensity));

        log.info(() -> "Computing Max Clique...");

        cliqueWatch.start();
        ArrayList<Integer> sortedIds = graph.getVertices().stream()
                .map(vertex -> ((ChronoVertex) vertex).getIntId())
                .collect(Collectors.toCollection(ArrayList::new));

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
                    if (vId <= uId) continue;
                    if (rGraph.getEdge(uId, vId).isNull())
                        log.severe(() -> String.format("v(%d) -> v(%d) not found", uId, vId));
                }
            }
        }

        log.info(() -> String.format("Temporal reachability computed in %s ms. E = %d", tcWatch.getTime(), directedEdgeCount));
        log.info(() -> String.format("Reachability graph created in %s ms. V = %d, E = %d", rGraphWatch.getTime(), rGraph.getVertices().size(), rGraph.getEdgeCount()));
        log.info(() -> String.format("Max Clique computed in %d ms. Size: %d", cliqueWatch.getTime(), cliqueUpdater.getMaxClique().size()));
        log.finer(() -> String.format("RGraph Max Clique: %s", cliqueUpdater.getMaxClique()));
        log.info(() -> "---Finished---");
    }
}
