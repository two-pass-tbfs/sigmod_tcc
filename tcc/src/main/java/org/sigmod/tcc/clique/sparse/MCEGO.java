package org.sigmod.tcc.clique.sparse;

import org.apache.commons.lang3.time.StopWatch;
import org.sigmod.tcc.clique.CliqueUpdater;
import org.sigmod.tcc.clique.Core;
import org.sigmod.tcc.rgraph.REdge;
import org.sigmod.tcc.rgraph.RGraph;
import org.sigmod.tcc.rgraph.RVertex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Computes a maximum clique using MC-EGO algorithm.
 */
public class MCEGO {
    public static Logger log = Logger.getLogger(MCEGO.class.getName());

    /**
     * Computes the maximum clique using MC-EGO algorithm.
     *
     * @param srcId the current source being processed
     * @param rGraph the reachability graph
     * @param sortedIds the sorted list of vertices to process
     * @param degrees an array to store the degrees of the vertices
     * @param visited a temporary array to mark visited vertices
     * @param cliqueUpdater the clique updater
     * @param localClique the local clique associated with the current source
     * @return the upper bound of the maximum clique
     */
    public static <V extends RVertex, E extends REdge<?>> int compute(Integer srcId, RGraph<V, E> rGraph, List<Integer> sortedIds, int[] degrees, boolean[] visited,
                                                                      CliqueUpdater cliqueUpdater, Set<Integer> localClique) {
        int upperBound = MCDD.compute(srcId, rGraph, sortedIds, degrees, visited, cliqueUpdater, localClique);

        if (cliqueUpdater.getMaxClique().size() >= upperBound) {
            log.info(() -> String.format("MC-EGO terminated. Max Clique Size: %d", cliqueUpdater.getMaxClique().size()));

            return upperBound;
        }

        log.info(() -> String.format("Computing MC-EGO. Current Clique: %d, Upper Bound: %d", cliqueUpdater.getMaxClique().size(), upperBound));
        StopWatch runWatch = new StopWatch();
        runWatch.start();

        // Initialize the ranks of the vertices in idSet for easy comparison
        int maxCore = 0;
        int candidateCount = sortedIds.size();
        for (int i = 0; i < sortedIds.size(); i++) {
            int uId = sortedIds.get(i);
            rGraph.setRank(uId, i);
            if (rGraph.getCore(uId) > maxCore) maxCore = rGraph.getCore(uId);

            if (rGraph.getCore(uId) < cliqueUpdater.getMaxClique().size() || rGraph.getVertex(uId).getNeighborCount() + 1 <= cliqueUpdater.getMaxClique().size()) {
                rGraph.setCore(uId, RGraph.NULL_VALUE);
                candidateCount--;
            }
        }
        int logCandidateCount = candidateCount;
        log.warning(() -> String.format("Candidate Vertices: %d", logCandidateCount));
        List<Integer> subIds = new ArrayList<>(upperBound);

        // Process each vertex in descending order of their ranks
        int[] localColors = new int[sortedIds.size()];

        int maxUpperBound = 0;
        int processedCount = 0;
        LinearHeap heap = new LinearHeap(sortedIds.size(), maxCore);
        for (int i = sortedIds.size() - 1; i >= 0; i--) {
            int uId = sortedIds.get(i);

            if (sortedIds.size() - i + 1 <= cliqueUpdater.getMaxClique().size()) continue;
            if (rGraph.getCore(uId) < cliqueUpdater.getMaxClique().size()) break;

            // Process higher-ranked neighbors of uId
            subIds.clear();
            rGraph.forEachNeighbor(uId, neighbor -> {
                if (rGraph.getRank(uId) < rGraph.getRank(neighbor))
                    subIds.add(neighbor);
            });

            log.finer(() -> String.format("Before k-core Reduction. uId: %d, ID Size: %s", uId, subIds.size()));

            // Prune if the idSubset is less than the maximum clique size or if it has fewer colors than the maximum clique size
            int colorBound = Color.countColors(subIds, rGraph.getColors(), visited) + 1;
            if (subIds.size() < cliqueUpdater.getMaxClique().size() || colorBound < cliqueUpdater.getMaxClique().size())
                continue;

            // Compute degeneracy-based clique for uNeighbors
            Arrays.fill(degrees, 0);

            Arrays.fill(visited, false);
            for (int vId : subIds)
                visited[vId] = true;

            int oldSubsetSize = subIds.size();

            Core.reduce(rGraph, subIds, visited, degrees, cliqueUpdater.getMaxClique().size() - 1);

            log.fine(() -> String.format("After k-core Reduction. New ID Size: %d", subIds.size()));

            if (subIds.size() < oldSubsetSize && Color.countColors(subIds, rGraph.getColors(), visited) + 1 < cliqueUpdater.getMaxClique().size())
                continue;

            int startColor = MCDD.compute(srcId, rGraph, uId, subIds, visited, degrees, cliqueUpdater, localClique, heap);

            if (cliqueUpdater.getMaxClique().size() >= upperBound)
                break;

            Arrays.fill(localColors, RGraph.NULL_VALUE);
            int localColor = Color.color(rGraph, subIds, subIds.size(), subIds.size(), localColors, visited, 0, startColor);
            rGraph.setUpperBound(uId, 1 + localColor);

            int logMaxLocalUpperBound = maxUpperBound;
            log.fine(() -> String.format("uId: %d, Unique Colors: %d, Max Local Upper Bound: %d", uId, rGraph.getUpperBound(uId), logMaxLocalUpperBound));

            if (rGraph.getUpperBound(uId) > maxUpperBound) {
                maxUpperBound = rGraph.getUpperBound(uId);
            }

            if (++processedCount % 2500 == 0) {
                int finalProcessedCount = processedCount;
                log.info(() -> String.format("%d vertices processed...", finalProcessedCount));
            }
        }
        if (maxUpperBound > upperBound) {
            int logMaxUpperBound = maxUpperBound;
            log.warning(() -> String.format("Max Local Upper Bound: %d, Upper Bound: %d", logMaxUpperBound, upperBound));

            maxUpperBound = upperBound;
        }

        int newUpperBound = cliqueUpdater.getMaxClique().size();
        if (maxUpperBound > newUpperBound)
            newUpperBound = maxUpperBound;

        int logNewUpperBound = newUpperBound;

        runWatch.stop();
        log.info(() -> String.format("MC-EGO terminated in %d ms. Clique: %d, Upper Bound: %d", runWatch.getTime(), cliqueUpdater.getMaxClique().size(), logNewUpperBound));

        return newUpperBound;
    }
}
