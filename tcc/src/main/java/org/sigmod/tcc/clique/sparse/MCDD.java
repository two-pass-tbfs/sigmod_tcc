package org.sigmod.tcc.clique.sparse;

import org.apache.commons.lang3.time.StopWatch;
import org.sigmod.tcc.clique.CliqueUpdater;
import org.sigmod.tcc.rgraph.REdge;
import org.sigmod.tcc.rgraph.RGraph;
import org.sigmod.tcc.rgraph.RVertex;

import java.util.*;
import java.util.logging.Logger;

/**
 * Computes a maximum degree-based clique or a degeneracy-based clique
 */
public class MCDD {

    public static Logger log = Logger.getLogger(MCDD.class.getName());

    /**
     * Computes a maximum degree-based clique or a degeneracy-based clique
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
    public static <V extends RVertex, E extends REdge<?>> int compute(Integer srcId, RGraph<V, E> rGraph, List<Integer> sortedIds,
                                                                      int[] degrees, boolean[] visited, CliqueUpdater cliqueUpdater, Set<Integer> localClique) {
        log.info(() -> "Computing MC-DD");
        StopWatch runWatch = new StopWatch();
        runWatch.start();
        Arrays.fill(visited, false);

        int threshold = cliqueUpdater.getMaxClique().size();

        int queueHead = 0;
        int queueN = 0; // Stores the number of vertices that cannot be part of the clique
        int newSize = 0; // Stores the number of vertices that can be part of the clique
        int[] queue = new int[sortedIds.size()];

        for (int i = sortedIds.size() - 1; i >= 0; i--) {
            int uId = sortedIds.get(i);
            if (degrees[uId] < threshold) {
                queue[queueN++] = uId;
                degrees[uId] = RGraph.NULL_VALUE; // Mark immediately so neighbors don't process it
            } else {
                break;
            }
        }

        while (queueHead < queueN) {
            int uId = queue[queueHead++];

            Iterator<E> edgeIterator = rGraph.edgeIterator(uId);

            while (edgeIterator.hasNext()) {
                E edge = edgeIterator.next();

                if (edge.isNull()) continue;
                int vId = edge.getDest();

                if (degrees[vId] == RGraph.NULL_VALUE) continue;

                if (degrees[vId]-- == threshold) {
                    degrees[vId] = RGraph.NULL_VALUE; // Mark immediately so neighbors don't process it
                    queue[queueN++] = vId;
                }
            }
        }

        // Move vertices with degrees less than the threshold in front
        for (int i = 0; i < queueN; i++) {
            sortedIds.set(i, queue[i]);
            visited[queue[i]] = true;
            rGraph.setCore(queue[i], RGraph.NULL_VALUE);
        }

        // Store the surviving vertices next
        for (int i = 0; i < sortedIds.size(); i++) {
            if (degrees[i] >= threshold) {
                sortedIds.set(queueN + (newSize++), i);
            }
        }

        assert queueN + newSize == sortedIds.size();

        int upperBound = sortedIds.size();
        if (newSize == 0) {
            upperBound = cliqueUpdater.getMaxClique().size();

            int logUpperBound = upperBound;
            log.fine(() -> String.format("No new vertices. Max clique size: %d", logUpperBound));
        } else {
            log.info(() -> "Computing degeneracy clique...");

            LinearHeap heap = new LinearHeap(rGraph.getVertices().size(), newSize - 1);
            heap.init(newSize, newSize - 1, queueN, sortedIds, degrees, false);
            int maxCore = 0;
            HashSet<Integer> candidateClique = new HashSet<>(newSize + 1);
            for (int i = 0; i < newSize; i++) {
                LinearHeap.Element element = heap.popMin();

                int uId = element.id();
                int degree = element.key();

                if (degree > maxCore)
                    maxCore = degree;

                rGraph.setCore(uId, maxCore);
                sortedIds.set(queueN + i, uId);
                if (degree + i + 1 == newSize) {
                    int xSize = heap.getIds(queueN, sortedIds, i + 1);

                    assert xSize == newSize;

                    for (int j = i; j < newSize; j++) {
                        assert !visited[sortedIds.get(queueN + j)];

                        rGraph.setCore(sortedIds.get(queueN + j), maxCore);
                        candidateClique.add(sortedIds.get(queueN + j));
                    }
                    break;
                }

                visited[uId] = true;

                rGraph.forEachNeighbor(uId, vId -> {
                    if (!visited[vId]) heap.decrement(vId, 1);
                });
            }

            int logMaxCore = maxCore;
            log.info(() -> String.format("Degeneracy clique size: %d, Max Core: %d", candidateClique.size(), logMaxCore));

            if (localClique.size() < candidateClique.size()) {
                localClique.clear();
                localClique.addAll(candidateClique);
            }
            cliqueUpdater.update(candidateClique, srcId);

            if (cliqueUpdater.getMaxClique().size() == maxCore + 1) {
                upperBound = maxCore + 1;
            } else {
                log.info(() -> "Computing colors...");

                Arrays.fill(visited, false);
                int startIdx = 0;
                int maxCliqueSize = cliqueUpdater.getMaxClique().size();
                while (startIdx < sortedIds.size() && rGraph.getCore(sortedIds.get(startIdx)) < maxCliqueSize)
                    ++startIdx;

                int numColor = Color.color(rGraph, sortedIds, sortedIds.size(), sortedIds.size(), rGraph.getColors(), visited, startIdx, 0);

                int logUpperBound = upperBound;
                log.info(() -> String.format("Number of colors: %d, Upper bound: %d", numColor, logUpperBound));
                assert numColor <= upperBound;

                upperBound = numColor;
                if (cliqueUpdater.getMaxClique().size() < upperBound) {
                    log.info(() -> String.format("Maximum clique %d is less than number of colors %d. Greedily extend clique...",
                            cliqueUpdater.getMaxClique().size(), numColor));

                    for (int uId : candidateClique) visited[uId] = true;

                    for (int i = sortedIds.size() - candidateClique.size(); i >= 0; i--) {
                        int uId = sortedIds.get(i - 1);
                        int count = 0;

                        if (rGraph.getCore(uId) < candidateClique.size()) break;

                        Iterator<E> edgeIterator = rGraph.edgeIterator(uId);
                        while (edgeIterator.hasNext()) {
                            E edge = edgeIterator.next();
                            if (edge.isNull()) continue;

                            int vId = edge.getDest();
                            if (visited[vId]) count++;
                        }

                        if (count == candidateClique.size()) {
                            candidateClique.add(uId);
                            visited[uId] = true;
                        }
                    }

                    log.info(() -> String.format("Greedy extend clique size: %d", candidateClique.size()));

                    if (localClique.size() < candidateClique.size()) {
                        localClique.clear();
                        localClique.addAll(candidateClique);
                    }
                    cliqueUpdater.update(candidateClique, srcId);
                }
            }
        }

        int logUpperBound = upperBound;
        runWatch.stop();
        log.info(() -> String.format("MC-DD terminated in %d ms. Max Clique Size: %d. Upperbound: %d", runWatch.getTime(), cliqueUpdater.getMaxClique().size(), logUpperBound));

        Arrays.fill(visited, false);
        Arrays.fill(degrees, 0);

        return upperBound;
    }

    /**
     * Computes a maximum degree-based clique
     *
     * @param srcId         the current source being processed
     * @param rGraph        the reachability graph
     * @param uId           the current vertex being processed
     * @param ids           the sorted list of vertices to process
     * @param visited       a temporary array to mark visited vertices
     * @param degrees       an array to store the degrees of the vertices
     * @param cliqueUpdater the clique updater
     * @param localClique   the local clique associated with the current source
     * @param heap          the heap
     * @return the start color of the clique
     */
    public static <V extends RVertex, E extends REdge<?>> int compute(Integer srcId, RGraph<V, E> rGraph, int uId, List<Integer> ids,
                                                                      boolean[] visited, int[] degrees, CliqueUpdater cliqueUpdater, Set<Integer> localClique,
                                                                      LinearHeap heap) {
        log.fine(() -> String.format("Computing maximum degree-based clique of v(%d)...", uId));

        int currentCliqueSize = 1;

        for (Integer id : ids) {
            visited[id] = true;
        }

        boolean sparse;
        int edgeCount = 0;
        int maxDegree = 0;
        for (int id : ids) {
            edgeCount += degrees[id];
            if (degrees[id] > maxDegree)
                maxDegree = degrees[id];
        }

        assert maxDegree <= ids.size() - 1;

//        sparse = (double) edgeCount * (ids.size() * (ids.size() - 1)) < 0.5;
        sparse = edgeCount * 10 < ids.size() * (ids.size() - 1);

        boolean logSparse = sparse;
        log.finer(() -> String.format("Sparse: %b", logSparse));

        if (sparse) {
//            heap = new LinearHeap(rGraph.getVertices().size(), maxDegree);
            heap.init(ids.size(), ids.size() - 1, 0, ids, degrees, false);
        }

        int startColor = 0;
        for (int jIdx = 0; jIdx < ids.size(); jIdx++) {
            int id, degree;
            if (sparse) {
                LinearHeap.Element element = heap.popMin();
                id = element.id();
                degree = element.key();
                ids.set(jIdx, id);
            } else {
                int minIdx = jIdx;
                for (int k = jIdx + 1; k < ids.size(); k++) {
                    if (degrees[ids.get(k)] < degrees[ids.get(minIdx)])
                        minIdx = k;
                }
                if (minIdx != jIdx) {
                    Collections.swap(ids, jIdx, minIdx);
                }
                id = ids.get(jIdx);
                degree = degrees[id];
            }
            if (degree + jIdx + 1 == ids.size()) {
                BitSet candidateCliqueSet = new BitSet(ids.size());
                candidateCliqueSet.set(uId);

                startColor = degree + 1;
                if (sparse) {
                    int newSize = jIdx + 1;
                    heap.getIds(0, ids, newSize);
                    assert (newSize == ids.size());
                }

                if (degree + 1 + currentCliqueSize > cliqueUpdater.getMaxClique().size()) {
                    for (int k = jIdx; k < ids.size(); k++) {
                        candidateCliqueSet.set(ids.get(k));
                    }

                    if (localClique.size() < candidateCliqueSet.cardinality()) {
                        localClique.clear();
                        for (int i = candidateCliqueSet.nextSetBit(0); i >= 0; i = candidateCliqueSet.nextSetBit(i + 1)) {
                            localClique.add(i);
                            if (i == Integer.MAX_VALUE) break; // Avoid infinite loop if MAX_VALUE is set
                        }
                    }
                    cliqueUpdater.update(localClique, srcId);
                }
                break;
            }

            visited[id] = false;

            rGraph.forEachNeighbor(id, vId -> {
                if (!visited[vId]) return;

                if (sparse)
                    heap.decrement(vId, 1);
                else
                    --degrees[vId];
            });
        }

        for (Integer id : ids) {
            visited[id] = false;
        }

        return startColor;
    }


    /**
     * Computes a maximum degree-based clique
     *
     * @param srcId the current source being processed
     * @param rGraph the reachability graph
     * @param sortedIds the sorted list of vertices to process
     * @param threshold the number of vertices to check
     * @param cliqueUpdater the clique updater
     * @param localClique the local clique associated with the current source
     * @return the size of the maximum clique
     */
    public static <V extends RVertex, E extends REdge<?>> int degreeHeuristic(Integer srcId, RGraph<V, E> rGraph, List<Integer> sortedIds,
                                                                              int threshold, CliqueUpdater cliqueUpdater, Set<Integer> localClique) {
        log.info(() -> "Computing MC-DD-H...");

        if (threshold > sortedIds.size())
            threshold = sortedIds.size();

        StopWatch runWatch = new StopWatch();
        runWatch.start();

        // Process at most threshold vertices
        for (int i = 0; i < threshold; i++) {
            Integer uId = sortedIds.get(i);

            Set<Integer> candidateClique = new HashSet<>(Math.max(16, rGraph.getVertex(uId).getNeighborCount()));
            candidateClique.add(uId);

            // Sort neighbors by degree order
            List<Integer> neighbors = new ArrayList<>(rGraph.getVertex(uId).getNeighborCount());

            rGraph.forEachNeighbor(uId, neighbors::add);

            neighbors.sort(Comparator.comparingInt(vId -> rGraph.getVertex((Integer) vId).getNeighborCount()).reversed());

            Set<Integer> commonCandidates = new HashSet<>(neighbors);

            for (Integer vId : neighbors) {
                if (candidateClique.size() + commonCandidates.size() <= cliqueUpdater.getMaxClique().size())
                    break;

                if (!commonCandidates.contains(vId))
                    continue;

                candidateClique.add(vId);

                Set<Integer> vNeighbors = rGraph.getNeighbors(vId);
                commonCandidates.retainAll(vNeighbors);

                commonCandidates.remove(vId);
            }

            if (localClique.size() < candidateClique.size()) {
                localClique.clear();
                localClique.addAll(candidateClique);
            }

            // Update maxClique if we found a larger candidate clique
            if (candidateClique.size() > cliqueUpdater.getMaxClique().size()) {
                cliqueUpdater.update(candidateClique, srcId);

                // Early termination if we found a clique containing all vertices
                if (cliqueUpdater.getMaxClique().size() >= sortedIds.size())
                    break;
            }
        }

        runWatch.stop();

        log.info(() -> String.format("MC-DD-H: %d, Global: %d processed in %d ms.", localClique.size(), cliqueUpdater.getMaxClique().size(), runWatch.getTime()));
        return cliqueUpdater.getMaxClique().size();
    }
}