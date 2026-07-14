package org.sigmod.tcc.clique.sparse;

import org.apache.commons.lang3.time.StopWatch;
import org.sigmod.tcc.clique.CliqueUpdater;
import org.sigmod.tcc.clique.Core;
import org.sigmod.util.MutableInteger;
import org.sigmod.tcc.rgraph.REdge;
import org.sigmod.tcc.rgraph.RGraph;
import org.sigmod.tcc.rgraph.RVertex;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Computes a maximum clique using MC-BRB algorithm.
 */
public class MCBRB {

    public static Logger log = Logger.getLogger(MCBRB.class.getName());

    public static <V extends RVertex, E extends REdge<?>> int compute(Integer srcId, RGraph<V, E> rGraph, List<Integer> sortedIds, int[] degrees, boolean[] visited,
                                                                      CliqueUpdater cliqueUpdater, HashSet<Integer> localClique) {

        int upperBound = MCEGO.compute(srcId, rGraph, sortedIds, degrees, visited,
                cliqueUpdater, localClique);

        if (cliqueUpdater.getMaxClique().size() >= upperBound)
            return upperBound;

        MutableInteger idSize = new MutableInteger(sortedIds.size());
        ArrayList<Integer> localColors = new ArrayList<>(sortedIds.size());

        log.info(() -> String.format("Computing MC-BRB. ID Size: %d, Current Clique: %d, Upper Bound: %d", idSize.get(), cliqueUpdater.getMaxClique().size(), upperBound));
        StopWatch runWatch = new StopWatch();
        runWatch.start();

        int candidateCount = idSize.get();
        for (int i = 0; i < idSize.get(); i++) {
            int uId = sortedIds.get(i);

            if (rGraph.getCore(uId) < cliqueUpdater.getMaxClique().size() || rGraph.getVertex(uId).getNeighborCount() + 1 <= cliqueUpdater.getMaxClique().size()) {
                rGraph.setCore(uId, RGraph.NULL_VALUE);
                candidateCount--;
            }
        }
        int logCandidateCount = candidateCount;
        log.info(() -> String.format("Candidate Vertices: %d", logCandidateCount));

        // Process each vertex in descending order of their ranks
        List<Integer> subIds = new ArrayList<>(upperBound);
        int processedCount = 0;
        for (int uIdx = idSize.get() - 1; uIdx >= 0; uIdx--) {
            int uId = sortedIds.get(uIdx);

            if (rGraph.getUpperBound(uId) <= cliqueUpdater.getMaxClique().size()) {
                continue;
            }

            log.finer(() -> String.format("Processing v(%d), localUpperBound=%d...", uId, rGraph.getUpperBound(uId)));

            if (rGraph.getCore(uId) < cliqueUpdater.getMaxClique().size()) break;
//            if (rGraph.getCore(uId) < cliqueUpdater.getMaxClique().size()) continue;

            int oldMaxCliqueSize = cliqueUpdater.getMaxClique().size();
            // Process higher-ranked neighbors of uId
            subIds.clear();
            rGraph.forEachNeighbor(uId, vId -> {
                if (rGraph.getRank(uId) < rGraph.getRank(vId))
                    subIds.add(vId);
            });


            // Color-based prune
            int colorCount = Color.countColors(subIds, rGraph.getColors(), visited);
            if (subIds.size() < cliqueUpdater.getMaxClique().size() || colorCount < cliqueUpdater.getMaxClique().size())
                continue;

            // Extract subgraph of g
            Arrays.fill(degrees, 0);
            Arrays.fill(visited, false);
            for (int vId : subIds)
                visited[vId] = true;

            // Reduce it to its (maxClique-1)-core
            int oldSubsetSize = subIds.size();

            Core.reduce(rGraph, subIds, visited, degrees, cliqueUpdater.getMaxClique().size() - 1);

            log.fine(() -> String.format("uId: %d, Old ID Size: %d . New ID Size: %d", uId, oldSubsetSize, subIds.size()));

            int colorBound = Color.countColors(subIds, rGraph.getColors(), visited) + 1;
            if (subIds.size() < cliqueUpdater.getMaxClique().size() || colorBound < cliqueUpdater.getMaxClique().size())
                continue;

            // Remap
            log.finer(() -> String.format("v(%d) subgraph remap...", uId));

            DenseGraph subgraph = new DenseGraph(subIds);
            // Convert to adjacency matrix
            log.finer(() -> String.format("v(%d) subgraph compute adjacency matrix...", uId));
            subgraph.convert(rGraph);

            int[] rDegrees = degrees;
            Arrays.fill(rDegrees, 0, 0, subgraph.getSize());
            for (int i = 0; i < subgraph.getSize(); i++) {
                rDegrees[i] = subgraph.getSize() - subgraph.getNeighbors(i).cardinality();
            }

            // Compute KCF-BRB
            MutableInteger cliqueSize = new MutableInteger(1);
            KCFClique kcfClique = new KCFClique(uId, subgraph.getIdList(), subgraph.getSize());
            MutableInteger subIdSize = new MutableInteger(subIds.size());

            KCFBRB.compute(subgraph, kcfClique, cliqueSize, kcfClique.idList, subIdSize, rDegrees, cliqueUpdater.getMaxClique().size());
            log.fine(() -> String.format("After First KCF. ID Size: %d, Clique Size: %d", subIdSize.get(), cliqueSize.get()));

            if (cliqueSize.get() > cliqueUpdater.getMaxClique().size())
                KCFBRB.update(srcId, subgraph, kcfClique, cliqueSize, cliqueUpdater, localClique);

            if (cliqueUpdater.getMaxClique().size() > oldMaxCliqueSize || subIdSize.get() == 0)
                continue;

            log.fine(() -> String.format("MC-BRB. Recursively searching cliques for v(%d)...", uId));
            kcfClique.changes.clear();

            localColors.clear();
            localColors.ensureCapacity(kcfClique.idList.size());
            while (localColors.size() < kcfClique.idList.size())
                localColors.add(RGraph.NULL_VALUE);

            recursiveCliqueSearchColorWithKernelization(srcId, rGraph, degrees, subgraph,
                    kcfClique.idList, 0, subIdSize,
                    localColors,
                    kcfClique, cliqueSize, 0, cliqueUpdater, localClique);

            if (++processedCount % 100 == 0) {
                int finalProcessedCount = processedCount;
                log.info(() -> String.format("%d vertices processed...", finalProcessedCount));
            }
        }

        runWatch.stop();
        log.info(() -> String.format("MC-BRB terminated in %d ms.", runWatch.getTime()));

        return cliqueUpdater.getMaxClique().size();
    }

    private static <V extends RVertex, E extends REdge<?>> void recursiveCliqueSearchColorWithKernelization(Integer srcId, RGraph<V, E> rGraph, int[] degrees, DenseGraph graph,
                                                                                                            List<Integer> idList, int startIdx, MutableInteger idSize,
                                                                                                            List<Integer> colors,
                                                                                                            KCFClique kcfClique, MutableInteger cliqueSize, int level,
                                                                                                            CliqueUpdater cliqueUpdater, Set<Integer> localClique) {
        boolean isKernel = true;

        log.fine(() -> String.format("MC-BRB: Searching clique recursively. Level: %d...", level));
        assert cliqueSize.get() <= cliqueUpdater.getMaxClique().size();
        log.finer(() -> String.format("ID Size: %d", idSize.get()));

        List<Integer> subIds = idList.subList(startIdx, startIdx + idSize.get());
        List<Integer> subColors = colors.subList(startIdx, startIdx + idSize.get());

        assert subIds.size() == idSize.get();
        assert !subIds.isEmpty();

        if (cliqueSize.get() + 3 > cliqueUpdater.getMaxClique().size()) {
            searchTriangleMatrix(graph, subIds, kcfClique, cliqueSize, cliqueUpdater.getMaxClique().size());
            if (cliqueSize.get() > cliqueUpdater.getMaxClique().size()) {
                KCFBRB.update(srcId, graph, kcfClique, cliqueSize, cliqueUpdater, localClique);
            }
            return;
        }

        int endIdx = 0;
        int oldMaxCliqueSize = cliqueUpdater.getMaxClique().size();
        // Obtain Degrees
        Arrays.fill(degrees, 0);
        BitSet subIdSet = new BitSet(graph.getMaxNewId());
        for(Integer subId : subIds){
            subIdSet.set(subId);
        }

        for(int subId : subIds){
            BitSet neighbors = (BitSet) graph.getNeighbors(subId).clone();
            neighbors.and(subIdSet);
            degrees[subId] = neighbors.cardinality() - 1;
        }

        if (level > 0) {
            int[] rDegrees = degrees;
            for (int uId : subIds) {
                rDegrees[uId] = (idSize.get() - 1) - degrees[uId];
            }

            log.finer(() -> String.format("Before KCF. ID Size: %d, Clique size: %d", idSize.get(), cliqueSize.get()));

            KCFBRB.compute(graph, kcfClique, cliqueSize, subIds, idSize, rDegrees, cliqueUpdater.getMaxClique().size());

            log.finer(() -> String.format("After KCF. ID Size: %d, Clique size: %d, max clique size: %d",
                    idSize.get(), cliqueSize.get(), oldMaxCliqueSize));

            for (int i = 0; i < idSize.get(); i++) {
                int uId = subIds.get(i);
                degrees[uId] = idSize.get() - 1 - rDegrees[uId];
            }

            if (cliqueSize.get() > cliqueUpdater.getMaxClique().size()) {
                KCFBRB.update(srcId, graph, kcfClique, cliqueSize, cliqueUpdater, localClique);
            }

            if (idSize.get() == 0 || cliqueUpdater.getMaxClique().size() > oldMaxCliqueSize)
                return;
        }

        subIds = idList.subList(startIdx, startIdx + idSize.get());
        subColors = colors.subList(startIdx, startIdx + idSize.get());

        int startColor = degeneracyMaximalCliqueMatrix(srcId, graph, subIds, degrees, kcfClique, cliqueSize, cliqueUpdater, localClique);

        if (cliqueUpdater.getMaxClique().size() > oldMaxCliqueSize)
            return;

        int threshold = cliqueUpdater.getMaxClique().size() - cliqueSize.get();
        int colorCount = Color.colorMatrix(graph, kcfClique, subIds, subColors, startColor, threshold);
        if (colorCount <= threshold) return;

        int logSubIdSize1 = subIds.size();
        log.finer(() -> String.format("Before Reduce. ID Size: %d, Sub ID Size: %d", idSize.get(), logSubIdSize1));
        assert logSubIdSize1 == idSize.get();

        boolean hasFoundNewMaxClique = reduce(srcId, graph, subIds, idSize, degrees, subColors, kcfClique, cliqueSize, threshold, cliqueUpdater, localClique);
//        assert idList.size() == colors.size();
        if (hasFoundNewMaxClique) return;

        subIds = idList.subList(startIdx, startIdx + idSize.get());
        subColors = colors.subList(startIdx, startIdx + idSize.get());

        int logSubIdSize2 = subIds.size();
        log.finer(() -> String.format("After Reduce. ID Size: %d, Sub ID Size: %d", idSize.get(), logSubIdSize2));
        assert subIds.size() == idSize.get();

        endIdx = splitIds(kcfClique, subIds, subColors, threshold);

        if (colorCount <= threshold + 1) isKernel = false;

        int targetSize = startIdx + idSize.get() + idSize.get();
        ((ArrayList<Integer>) idList).ensureCapacity(targetSize);
        ((ArrayList<Integer>) colors).ensureCapacity(targetSize);

        while (idList.size() < targetSize) {
            idList.add(RGraph.NULL_VALUE);
            colors.add(RGraph.NULL_VALUE);
        }

        int logEndIdx = endIdx;
        log.finer(() -> String.format("End Idx: %d, %b", logEndIdx, cliqueUpdater.getMaxClique().size() == oldMaxCliqueSize));

        for (int i = endIdx; i > 0 && cliqueUpdater.getMaxClique().size() == oldMaxCliqueSize; i--) {
            int nextIdStartIdx = startIdx + idSize.get();
            MutableInteger nextIdEndIdx = new MutableInteger(0);

            int uId = idList.get(startIdx + i - 1);
            BitSet neighbors = graph.getNeighbors(uId);
            int upperBound = 0;

            for (int j = (isKernel ? i : endIdx); j < idSize.get(); j++) {
                int vId = idList.get(startIdx + j);

                if (!neighbors.get(vId)) continue;

                int vColor = colors.get(startIdx + j);

                if (nextIdEndIdx.get() == 0 || vColor != colors.get(nextIdStartIdx + nextIdEndIdx.get() - 1))
                    ++upperBound;

                idList.set(nextIdStartIdx + nextIdEndIdx.get(), vId);
                colors.set(nextIdStartIdx + nextIdEndIdx.get(), vColor);
                assert idList.size() == colors.size();
                nextIdEndIdx.increment();
            }

            if (upperBound + 1 + cliqueSize.get() <= cliqueUpdater.getMaxClique().size()) continue;

            assert !rGraph.getEdge(kcfClique.clique[0], graph.getOldId(uId)).isNull();

            kcfClique.clique[cliqueSize.get()] = uId;
            MutableInteger newCliqueSize = new MutableInteger(cliqueSize.get() + 1);
            int oldSize = nextIdEndIdx.get();

            if (!isKernel) {
                log.finer(() -> "Proceeding with kernelization color...");
                assert upperBound + cliqueSize.get() == cliqueUpdater.getMaxClique().size();

                List<Integer> nextIds = idList.subList(nextIdStartIdx, nextIdStartIdx + nextIdEndIdx.get());
                List<Integer> nextColors = colors.subList(nextIdStartIdx, nextIdStartIdx + nextIdEndIdx.get());

                if (kernelizationColor(srcId, graph, nextIds, nextIdEndIdx, nextColors, kcfClique, newCliqueSize, cliqueUpdater, localClique))
                    continue;
            }

            int oldContractionSize = kcfClique.contractions.size();
            int oldChangesSize = kcfClique.changes.size();

            recursiveCliqueSearchColorWithKernelization(srcId, rGraph, degrees, graph, idList, nextIdStartIdx, nextIdEndIdx,
                    colors, kcfClique, newCliqueSize, level + 1, cliqueUpdater, localClique);

            while (kcfClique.contractions.size() > oldContractionSize)
                kcfClique.contractions.removeLast();

            while (kcfClique.changes.size() > oldChangesSize) {
                int[] change = kcfClique.changes.removeLast();
                graph.getNeighbors(change[0]).flip(change[1]);
                graph.getNeighbors(change[1]).flip(change[0]);
            }

            if (!isKernel && oldSize + endIdx == idSize.get()) break;
        }
    }

    private static boolean kernelizationColor(Integer srcId, DenseGraph graph,
                                              List<Integer> ids, MutableInteger idSize, List<Integer> colors,
                                              KCFClique kcfClique, MutableInteger cliqueSize, CliqueUpdater cliqueUpdater, Set<Integer> localClique) {
        while (true) {
            int idx = -1;
            for (int i = 0; i < idSize.get(); i++) {
                boolean isLeftBoundary = (i == 0) || !colors.get(i).equals(colors.get(i - 1));
                boolean isRightBoundary = (i == idSize.get() - 1) || !colors.get(i).equals(colors.get(i + 1));

                if (isLeftBoundary && isRightBoundary) {
                    idx = i;
                    break;
                }
            }
            if (idx == -1) break;

            int uId = ids.get(idx);
            kcfClique.clique[cliqueSize.getAndIncrement()] = uId;

            int newSize = 0;
            BitSet neighbors = graph.getNeighbors(uId);
            for (int i = 0; i < idSize.get(); i++) {
                if (i == idx) continue;

                if (neighbors.get(ids.get(i))) {
                    ids.set(newSize, ids.get(i));
                    colors.set(newSize++, colors.get(i));
                } else {
                    boolean isColorClassBoundary = (i + 1 == idSize.get()) || !colors.get(i).equals(colors.get(i + 1));
                    if (isColorClassBoundary)
                        if (newSize == 0 || !colors.get(i).equals(colors.get(newSize - 1))) return true;
                }
            }

            idSize.set(newSize);
        }

        if (cliqueSize.get() > cliqueUpdater.getMaxClique().size()) {
            KCFBRB.update(srcId, graph, kcfClique, cliqueSize, cliqueUpdater, localClique);
            return true;
        }

        return false;
    }

    private static int splitIds(KCFClique kcfClique, List<Integer> ids, List<Integer> colors, int threshold) {
        Arrays.fill(kcfClique.heads, -1);
        Arrays.fill(kcfClique.nexts, -1);

        for (int i = ids.size(); i > 0; i--) {
            if (colors.get(i - 1) < threshold) {
                int j = i - 1;

                kcfClique.nexts[ids.get(j)] = kcfClique.heads[colors.get(j)];
                kcfClique.heads[colors.get(j)] = ids.get(j);
            }
        }

        int endIdx = 0;
        for (int i = 0; i < ids.size(); i++) {
            if (colors.get(i) >= threshold) {
                colors.set(endIdx, colors.get(i));
                ids.set(endIdx, ids.get(i));
                endIdx++;
            }
        }

        int newSize = endIdx;
        for (int i = threshold; i > 0; i--) {
            for (int uId = kcfClique.heads[i - 1]; uId != -1; uId = kcfClique.nexts[uId]) {
                ids.set(newSize, uId);
                colors.set(newSize++, i - 1);
            }
        }
        assert newSize == ids.size();
        return endIdx;
    }

    private static void searchTriangleMatrix(DenseGraph subgraph, List<Integer> ids,
                                             KCFClique kcfClique, MutableInteger cliqueSize, int maxCliqueSize) {
        // Handles finding exactly 3 vertices to form a clique that is one larger than the current best clique
        if (cliqueSize.get() + 2 == maxCliqueSize) {
            for (int i = 0; i < ids.size() && cliqueSize.get() < maxCliqueSize; i++) {
                int uId = ids.get(i);
                ArrayList<Integer> neighbors = new ArrayList<>();

                // Collect all neighbors of uId from the rest of the candidate list
                for (int j = i + 1; j < ids.size(); j++) {
                    int vId = ids.get(j);
                    if (subgraph.getNeighbors(uId).get(vId)) {
                        neighbors.add(vId);
                    }
                }

                // Skip if the neighbors of uId are less than 2
                if (neighbors.size() < 2)
                    continue;

                // Search for an edge between any two of uId's neighbor
                for (int j = 0; j < neighbors.size(); j++) {
                    int vId = neighbors.get(j);
                    for (int k = j + 1; k < neighbors.size(); k++) {
                        int wId = neighbors.get(k);

                        // If vId and wId are neighbors, we found a triangle (uId, vId, wId)
                        if (subgraph.getNeighbors(vId).get(wId)) {
                            kcfClique.clique[cliqueSize.getAndIncrement()] = uId;
                            kcfClique.clique[cliqueSize.getAndIncrement()] = vId;
                            kcfClique.clique[cliqueSize.getAndIncrement()] = wId;

                            return;
                        }
                    }
                }
            }
        }
        // Handles the case where we need to find an edge (2 vertices) among the candidates to form a larger clique
        else if (cliqueSize.get() + 1 > maxCliqueSize) {
            for (int i = 0; i < subgraph.getSize() && cliqueSize.get() < maxCliqueSize; i++) {
                int uId = subgraph.getId(i);
                for (int j = i + 1; j < subgraph.getSize(); j++) {
                    int vId = subgraph.getId(j);
                    if (subgraph.getNeighbors(uId).get(vId)) {
                        kcfClique.clique[cliqueSize.getAndIncrement()] = uId;
                        kcfClique.clique[cliqueSize.getAndIncrement()] = vId;

                        return;
                    }
                }
            }
        } else if (cliqueSize.get() == maxCliqueSize) {
            kcfClique.clique[cliqueSize.getAndIncrement()] = subgraph.getId(0);
        }
    }

    private static int degeneracyMaximalCliqueMatrix(Integer srcId, DenseGraph graph, List<Integer> ids, int[] degrees,
                                                     KCFClique kcfClique, MutableInteger cliqueSize, CliqueUpdater cliqueUpdater, Set<Integer> localClique) {
        if (log.isLoggable(Level.FINER)) {
            for (int i = 0; i < ids.size(); i++) {
                int d = 0;
                for (Integer id : ids) {
                    if (graph.isAdjacent(ids.get(i), id)) d++;
                }
                assert graph.isAdjacent(ids.get(i), ids.get(i));
                assert degrees[ids.get(i)] + 1 == d;
            }
        }

        int startColor = 0;
        for (int j = 0; j < ids.size(); j++) {
            int minIdx = j;
            for (int k = j + 1; k < ids.size(); k++) {
                if (degrees[ids.get(k)] < degrees[ids.get(minIdx)]) minIdx = k;
            }

            if (minIdx != j) Collections.swap(ids, j, minIdx);

            int uId = ids.get(j);

            if (degrees[uId] + 1 + j == ids.size()) {
                for (int k = j + 1; k < ids.size(); k++)
                    degrees[ids.get(k)] = ids.size() - 1 - k;

                startColor = Math.max(1, degrees[uId]);

                assert (startColor < ids.size());

                if (degrees[uId] + 1 + cliqueSize.get() > cliqueUpdater.getMaxClique().size()) {
                    for (int k = j; k < ids.size(); k++)
                        kcfClique.clique[cliqueSize.getAndIncrement()] = ids.get(k);

                    KCFBRB.update(srcId, graph, kcfClique, cliqueSize, cliqueUpdater, localClique);
                }
                break;
            }
            for (int k = j + 1; k < ids.size(); k++) {
                if (graph.isAdjacent(uId, ids.get(k)))
                    --degrees[ids.get(k)];
            }
        }

        return startColor;
    }

    private static boolean reduce(Integer srcId, DenseGraph graph,
                                  List<Integer> ids, MutableInteger idSize,
                                  int[] degrees, List<Integer> colors,
                                  KCFClique kcfClique, MutableInteger cliqueSize, int threshold, CliqueUpdater cliqueUpdater, Set<Integer> localClique) {
        Arrays.fill(kcfClique.vis, false);

        List<Integer> neighbors = new ArrayList<>(ids.size());
        List<Integer> neighborColors = new ArrayList<>(ids.size());

        int start = 0;
        for (; start < ids.size(); start++) {
            int uId = ids.get(start);

            if (degrees[uId] < threshold - 1) continue;

            neighbors.clear();
            neighborColors.clear();

            int colorCount = 0;

            for (int i = start + 1; i < ids.size(); i++) {
                int vId = ids.get(i);
                if (graph.isAdjacent(uId, vId)) {
                    neighbors.add(i);
                    int neighborColor = colors.get(i);
                    if (!kcfClique.vis[neighborColor]) {
                        kcfClique.vis[neighborColor] = true;
                        neighborColors.add(neighborColor);
                        colorCount++;
                    }
                }
            }
            assert neighbors.size() == degrees[ids.get(start)];
            for (int color : neighborColors) {
                kcfClique.vis[color] = false;
            }

            if (colorCount < threshold - 1) continue;

            if (degrees[uId] >= threshold + 1) break;

            boolean isClique = true;

            for (int i = 0; i < neighbors.size() && isClique; i++) {
                for (int j = i + 1; j < neighbors.size(); j++) {
                    if (!graph.isAdjacent(ids.get(i), ids.get(j))) {
                        isClique = false;
                        break;
                    }
                }
            }

            if (isClique) {
                kcfClique.clique[cliqueSize.getAndIncrement()] = uId;
                for (int i : neighbors) {
                    kcfClique.clique[cliqueSize.getAndIncrement()] = ids.get(i);
                }
                KCFBRB.update(srcId, graph, kcfClique, cliqueSize, cliqueUpdater, localClique);
                return true;
            }
        }

        if (start > 0) {
            for (int i = start; i < ids.size(); i++) {
                ids.set(i - start, ids.get(i));
                colors.set(i - start, colors.get(i));
            }
            idSize.set(idSize.get() - start);
        }

        return idSize.get() == 0;
    }
}
