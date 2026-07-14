package org.sigmod.tcc.clique.sparse;

import org.sigmod.tcc.clique.CliqueUpdater;
import org.sigmod.util.MutableInteger;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Computes the maximum clique using the KCF-BRB algorithm.
 */
public class KCFBRB {
    private static final Logger log = Logger.getLogger(KCFBRB.class.getName());

    /**
     * Computes the KCF-BRB algorithm.
     *
     * @param graph         the dense graph
     * @param kcfClique     the KCF-clique
     * @param cliqueSize    the clique size
     * @param ids           the list of IDs
     * @param idSize        the number of ids
     * @param rDegrees      the reverse degree counts
     * @param maxCliqueSize the maximum clique size
     * @return the clique found
     */
    public static KCFClique compute(DenseGraph graph, KCFClique kcfClique, MutableInteger cliqueSize,
                                    List<Integer> ids, MutableInteger idSize, int[] rDegrees, int maxCliqueSize) {
        int[] idxs = kcfClique.idxs;

        Deque<Integer> del = new ArrayDeque<>();
        Deque<Integer> degreeOne = new ArrayDeque<>();
        Deque<Integer> degreeTwo = new ArrayDeque<>();
        Deque<Integer> degreeThree = new ArrayDeque<>();


        for (int i = 0; i < idSize.get(); i++) {
            idxs[ids.get(i)] = i;
            i = putIntoOneVector(maxCliqueSize, i, ids, idxs, rDegrees[ids.get(i)], true, kcfClique.clique,
                    del, degreeOne, degreeTwo, degreeThree, cliqueSize, idSize);
        }
        if (log.isLoggable(Level.FINER)) {
            log.finer(() -> String.format("Size: %d, IDS: %s", idSize.get(), ids));
            log.finer(() -> String.format("Del: %s", del));
            log.finer(() -> String.format("Clique Size: %d", cliqueSize.get()));
            log.finer(() -> String.format("Deg One: %s", degreeOne));
            log.finer(() -> String.format("Deg Two: %s", degreeTwo));
            log.finer(() -> String.format("Deg Three: %s", degreeThree));
        }

        while (!del.isEmpty() || !degreeOne.isEmpty() || !degreeTwo.isEmpty() || !degreeThree.isEmpty()) {
            while (!del.isEmpty()) {
                if (log.isLoggable(Level.FINE)) {
                    log.fine(() -> "!del.isEmpty");
                    log.finer(() -> String.format("Size: %d, DELS: %s", del.size(), del));
                    log.finer(() -> String.format("Size: %d, IDS: %s", idSize.get(), ids.subList(0, idSize.get())));
                }

                int uId = del.pop();

                rDegrees[uId] = 0;
                assert idxs[uId] < idSize.get() && ids.get(idxs[uId]) == uId;

                int idx = idxs[uId];
                ids.set(idx, ids.get(idSize.decrementAndGet()));
                idxs[ids.get(idx)] = idx;

                BitSet neighbors = graph.getNeighbors(uId);
                for (int i = 0; i < idSize.get(); i++) {
                    int vId = ids.get(i);
                    int oldRDegree = rDegrees[vId];
                    if (!neighbors.get(vId)) {
                        --rDegrees[vId];
                    }
                    i = putIntoOneVectorEq(maxCliqueSize, i, ids, idxs, rDegrees[vId], oldRDegree != rDegrees[vId], kcfClique.clique,
                            del, degreeOne, degreeTwo, degreeThree, cliqueSize, idSize);
                }
            }

            if (log.isLoggable(Level.FINER)) {
                for (int i = 0; i < idSize.get(); i++) {
                    assert idxs[ids.get(i)] == i;
                }
            }

            while (del.isEmpty() && !degreeOne.isEmpty()) {
                if (log.isLoggable(Level.FINE)) {
                    log.fine(() -> "!degreeOne.isEmpty.");
                    log.finer(() -> String.format("Size: %d, DEGREE ONES: %s", degreeOne.size(), degreeOne));
                    log.finer(() -> String.format("Size: %d, IDS: %s", idSize.get(), ids.subList(0, idSize.get())));
                }

                int uId = degreeOne.pop();

                if (rDegrees[uId] != 1) continue;

                kcfClique.clique[cliqueSize.getAndIncrement()] = uId;
                assert idxs[uId] < idSize.get() && ids.get(idxs[uId]) == uId;

                int idx = idxs[uId];
                rDegrees[uId] = 0;

                ids.set(idx, ids.get(idSize.decrementAndGet()));
                idxs[ids.get(idx)] = idx;

                BitSet neighbors = graph.getNeighbors(uId);
                idx = -1;
                for (int j = 0; j < idSize.get(); j++) {
                    if (!neighbors.get(ids.get(j))) {
                        idx = j;
                        break;
                    }
                }
                assert idx != -1;

                uId = ids.get(idx);
                neighbors = graph.getNeighbors(uId);
                rDegrees[uId] = 0;

                ids.set(idx, ids.get(idSize.decrementAndGet()));
                idxs[ids.get(idx)] = idx;

                for (int i = 0; i < idSize.get(); i++) {
                    int vId = ids.get(i);
                    int oldRDegree = rDegrees[vId];

                    if (!neighbors.get(vId)) {
                        rDegrees[vId]--;
                    }

                    i = putIntoOneVector(maxCliqueSize, i, ids, idxs, rDegrees[vId], oldRDegree != rDegrees[vId], kcfClique.clique,
                            del, degreeOne, degreeTwo, degreeThree, cliqueSize, idSize);
                }
            }

            if (log.isLoggable(Level.FINER)) {
                for (int i = 0; i < idSize.get(); i++) {
                    assert idxs[ids.get(i)] == i;
                }
            }

            while (del.isEmpty() && degreeOne.isEmpty() && !degreeTwo.isEmpty()) {
                if (log.isLoggable(Level.FINE)) {
                    log.fine(() -> "!degreeTwo.isEmpty");
                    log.finer(() -> String.format("Size: %d, DEGREE TWO: %s", degreeTwo.size(), degreeTwo));
                    log.finer(() -> String.format("Size: %d, IDS: %s", idSize.get(), ids.subList(0, idSize.get())));
                    log.finer(() -> String.format("Max clique: %d", maxCliqueSize));
                }

                int uId = degreeTwo.pop();

                if (rDegrees[uId] != 2) continue;

                BitSet neighbors = graph.getNeighbors(uId);

                assert idxs[uId] < idSize.get() && ids.get(idxs[uId]) == uId;

                int idx = idxs[uId];
                rDegrees[uId] = 0;

                ids.set(idx, ids.get(idSize.decrementAndGet()));
                idxs[ids.get(idx)] = idx;

                int idx1 = -1, idx2 = -1;
                for (int j = 0; j < idSize.get(); j++) {
                    int vId = ids.get(j);
                    if (!neighbors.get(vId)) {
                        if (idx1 == -1) idx1 = j;
                        else {
                            idx2 = j;
                            break;
                        }
                    }
                }
                assert idx1 != -1 && idx2 != -1 && idx1 < idx2;

                BitSet neighbors1 = graph.getNeighbors(ids.get(idx1));
                BitSet neighbors2 = graph.getNeighbors(ids.get(idx2));
                if (!neighbors1.get(ids.get(idx2))) { // IF v1 and v2 are not neighbors
                    // Apply the reduction rule
                    kcfClique.clique[cliqueSize.getAndIncrement()] = uId;

                    rDegrees[ids.get(idx2)] = 0;
                    ids.set(idx2, ids.get(idSize.decrementAndGet()));
                    idxs[ids.get(idx2)] = idx2;

                    rDegrees[ids.get(idx1)] = 0;
                    ids.set(idx1, ids.get(idSize.decrementAndGet()));
                    idxs[ids.get(idx1)] = idx1;

                    // Trigger the update cascade
                    for (int i = 0; i < idSize.get(); i++) {
                        int vId = ids.get(i);
                        int oldRDegree = rDegrees[vId];

                        if (!neighbors1.get(vId)) {
                            --rDegrees[vId];
                        }

                        if (!neighbors2.get(vId)) {
                            --rDegrees[vId];
                        }

                        i = putIntoOneVector(maxCliqueSize, i, ids, idxs, rDegrees[vId], oldRDegree != rDegrees[vId], kcfClique.clique,
                                del, degreeOne, degreeTwo, degreeThree, cliqueSize, idSize);

                    }
                } else {
                    kcfClique.clique[cliqueSize.getAndIncrement()] = -1;

                    int vId = ids.get(idx1), oldDegreeV = rDegrees[vId];
                    --rDegrees[vId];

                    kcfClique.contractions.add(vId);
                    kcfClique.contractions.add(ids.get(idx2));
                    kcfClique.contractions.add(uId);
                    kcfClique.contractions.add(1);

                    rDegrees[ids.get(idx2)] = 0;
                    ids.set(idx2, ids.get(idSize.decrementAndGet()));
                    idxs[ids.get(idx2)] = idx2;

                    for (int i = 0; i < idSize.get(); i++) {
                        if (ids.get(i) == vId) continue;

                        int oldRDegreeW = rDegrees[ids.get(i)];

                        if (!neighbors2.get(ids.get(i))) {
                            if (neighbors1.get(ids.get(i))) {
                                neighbors1.flip(ids.get(i));
                                graph.flip(ids.get(i), vId);

                                rDegrees[vId]++;
                                kcfClique.changes.add(new int[]{vId, ids.get(i)});

                            } else {
                                rDegrees[ids.get(i)]--;

                            }
                        }
                        i = putIntoOneVector(maxCliqueSize, i, ids, idxs, rDegrees[ids.get(i)], oldRDegreeW != rDegrees[ids.get(i)], kcfClique.clique,
                                del, degreeOne, degreeTwo, degreeThree, cliqueSize, idSize);
                    }
                    putIntoOneVector(maxCliqueSize, idxs[vId], ids, idxs, rDegrees[vId], oldDegreeV != rDegrees[vId], kcfClique.clique,
                            del, degreeOne, degreeTwo, degreeThree, cliqueSize, idSize);
                }
            }

            if (log.isLoggable(Level.FINER)) {
                for (int i = 0; i < idSize.get(); i++) {
                    assert idxs[ids.get(i)] == i;
                }
            }


            while (del.isEmpty() && degreeOne.isEmpty() && degreeTwo.isEmpty() && !degreeThree.isEmpty()) {
                if (log.isLoggable(Level.FINE)) {
                    log.fine(() -> "!degreeThree.isEmpty");
                    log.finer(() -> String.format("Size: %d, DEGREE THREES: %s", degreeThree.size(), degreeThree));
                    log.finer(() -> String.format("Size: %d, IDS: %s", idSize.get(), ids.subList(0, idSize.get())));
                }

                int uId = degreeThree.pop();
                if (rDegrees[uId] != 3) continue;

                BitSet neighbors = graph.getNeighbors(uId);
                assert idxs[uId] < idSize.get() && ids.get(idxs[uId]) == uId;

                int idx = idxs[uId];
                rDegrees[uId] = 0;

                ids.set(idx, ids.get(idSize.decrementAndGet()));
                idxs[ids.get(idx)] = idx;

                int idx1 = -1;
                int idx2 = -1;
                int idx3 = -1;

                for (int j = 0; j < idSize.get(); j++) {
                    int vId = ids.get(j);
                    if (!neighbors.get(vId)) { // If v is a non-neighbor of u...
                        if (idx1 == -1) {
                            idx1 = j; // Found the first one.
                        } else if (idx2 == -1) {
                            idx2 = j; // Found the second one.
                        } else {
                            idx3 = j; // Found the third one.
                            break; // No need to search further.
                        }
                    }
                }
                assert idx1 != -1 && idx2 != -1 && idx3 != -1 && idx1 < idx2 && idx2 < idx3;

                BitSet neighbors1 = graph.getNeighbors(ids.get(idx1));
                BitSet neighbors2 = graph.getNeighbors(ids.get(idx2));
                BitSet neighbors3 = graph.getNeighbors(ids.get(idx3));

                int connected1 = 0, connected2 = 0, connected3 = 0;

                if (neighbors1.get(ids.get(idx2))) connected1 = 1;
                if (neighbors1.get(ids.get(idx3))) connected2 = 1;
                if (neighbors2.get(ids.get(idx3))) connected3 = 1;

                int totalConnected = connected1 + connected2 + connected3;

                if (totalConnected == 0) {
                    kcfClique.clique[cliqueSize.getAndIncrement()] = uId;

                    rDegrees[ids.get(idx3)] = 0;
                    ids.set(idx3, ids.get(idSize.decrementAndGet()));
                    idxs[ids.get(idx3)] = idx3;

                    rDegrees[ids.get(idx2)] = 0;
                    ids.set(idx2, ids.get(idSize.decrementAndGet()));
                    idxs[ids.get(idx2)] = idx2;

                    rDegrees[ids.get(idx1)] = 0;
                    ids.set(idx1, ids.get(idSize.decrementAndGet()));
                    idxs[ids.get(idx1)] = idx1;

                    for (int i = 0; i < idSize.get(); i++) {
                        int vId = ids.get(i);
                        int oldRDegree = rDegrees[vId];

                        if (!neighbors1.get(ids.get(i))) --rDegrees[vId];
                        if (!neighbors2.get(ids.get(i))) --rDegrees[vId];
                        if (!neighbors3.get(ids.get(i))) --rDegrees[vId];

                        i = putIntoOneVector(maxCliqueSize, i, ids, idxs, rDegrees[vId], oldRDegree != rDegrees[vId], kcfClique.clique,
                                del, degreeOne, degreeTwo, degreeThree, cliqueSize, idSize);
                    }

                } else if (totalConnected == 1) {
                    kcfClique.clique[cliqueSize.getAndIncrement()] = -1;

                    if (connected3 == 1) { // Edge is (v2, v3). Isolate v1. Swap v1 and v2
                        Collections.swap(ids, idx1, idx2);

                        BitSet tempNeighbors = neighbors1;
                        neighbors1 = neighbors2;
                        neighbors2 = tempNeighbors;

                        idxs[ids.get(idx1)] = idx1;
//                        idxs[ids.get(idx2)] = idx2; // just for completion
                    } else if (connected1 == 1) {
                        Collections.swap(ids, idx2, idx3);

                        BitSet tempNeighbors = neighbors2;
                        neighbors2 = neighbors3;
                        neighbors3 = tempNeighbors;

                        // Update idxs for readability
//                        idxs[ids.get(idx2)] = idx2; // Update rank for original v3
//                        idxs[ids.get(idx3)] = idx3; // Update rank for original v2
                    }

                    int vId = ids.get(idx1);
                    int oldRDegreeV = rDegrees[vId];
                    rDegrees[vId] -= 2;

                    kcfClique.contractions.add(vId);
                    kcfClique.contractions.add(ids.get(idx3));
                    kcfClique.contractions.add(uId);
                    kcfClique.contractions.add(1); // Type 1 contraction

                    rDegrees[ids.get(idx3)] = 0;
                    rDegrees[ids.get(idx2)] = 0;
                    assert idx2 != idx3;

                    if (idx2 < idx3) {
                        ids.set(idx3, ids.get(idSize.decrementAndGet()));
                        idxs[ids.get(idx3)] = idx3;

                        ids.set(idx2, ids.get(idSize.decrementAndGet()));
                        idxs[ids.get(idx2)] = idx2;

                    } else {
                        ids.set(idx2, ids.get(idSize.decrementAndGet()));
                        idxs[ids.get(idx2)] = idx2;

                        ids.set(idx3, ids.get(idSize.decrementAndGet()));
                        idxs[ids.get(idx3)] = idx3;
                    }

                    for (int i = 0; i < idSize.get(); i++) {
                        if (ids.get(i) == vId)
                            continue;

                        int oldRDegreeW = rDegrees[ids.get(i)];

                        if (!neighbors2.get(ids.get(i))) {
                            rDegrees[ids.get(i)]--;
                        }

                        if (!neighbors3.get(ids.get(i))) {
                            if (neighbors1.get(ids.get(i))) {
                                neighbors1.flip(ids.get(i));
                                graph.flip(ids.get(i), vId);

                                kcfClique.changes.add(new int[]{vId, ids.get(i)});
                                ++rDegrees[vId];
                            } else {
                                --rDegrees[ids.get(i)];
                            }

                        }
                        i = putIntoOneVector(maxCliqueSize, i, ids, idxs, rDegrees[ids.get(i)], oldRDegreeW != rDegrees[ids.get(i)], kcfClique.clique,
                                del, degreeOne, degreeTwo, degreeThree, cliqueSize, idSize);
                    }
                    putIntoOneVector(maxCliqueSize, idxs[vId], ids, idxs, rDegrees[vId], oldRDegreeV != rDegrees[vId], kcfClique.clique,
                            del, degreeOne, degreeTwo, degreeThree, cliqueSize, idSize);
                } else if (totalConnected == 2) {
                    kcfClique.clique[cliqueSize.getAndIncrement()] = -1;

                    if (connected3 == 0) { // Missing edge (v2, v3). v1 is center
                        Collections.swap(ids, idx1, idx3);

                        BitSet tempNeighbors = neighbors1;
                        neighbors1 = neighbors3;
                        neighbors3 = tempNeighbors;

                        idxs[ids.get(idx1)] = idx1;
//                        idxs[ids.get(idx3)] = idx3;
                    } else if (connected2 == 0) { // Missing edge (v1, v3). v2 is center
                        Collections.swap(ids, idx2, idx3);

                        BitSet tempNeighbors = neighbors2;
                        neighbors2 = neighbors3;
                        neighbors3 = tempNeighbors;

                        idxs[ids.get(idx2)] = idx2;
//                        idxs[ids.get(idx3)] = idx3;
                    }

                    int vId = ids.get(idx1);
                    int oldDegreeV = rDegrees[vId];
                    --rDegrees[vId];

                    int wId = ids.get(idx2);
                    int oldDegreeW = rDegrees[wId];
                    --rDegrees[wId];

                    kcfClique.contractions.add(vId);
                    kcfClique.contractions.add(wId);
                    kcfClique.contractions.add(ids.get(idx3));
                    kcfClique.contractions.add(uId);
                    kcfClique.contractions.add(2);

                    rDegrees[ids.get(idx3)] = 0;

                    ids.set(idx3, ids.get(idSize.decrementAndGet()));
                    idxs[ids.get(idx3)] = idx3;

                    for (int i = 0; i < idSize.get(); i++) {
                        if (ids.get(i) == vId || ids.get(i) == wId)
                            continue;

                        int oldRDegree = rDegrees[ids.get(i)];

                        if (!neighbors3.get(ids.get(i))) {
                            --rDegrees[ids.get(i)];

                            if (neighbors1.get(ids.get(i))) {
                                neighbors1.flip(ids.get(i));
                                graph.flip(ids.get(i), vId);

                                kcfClique.changes.add(new int[]{vId, ids.get(i)});
                                ++rDegrees[vId];

                                ++rDegrees[ids.get(i)];

                            }
                            if (neighbors2.get(ids.get(i))) {
                                neighbors2.flip(ids.get(i));
                                graph.flip(ids.get(i), wId);
                                kcfClique.changes.add(new int[]{wId, ids.get(i)});
                                ++rDegrees[wId];
                                ++rDegrees[ids.get(i)];
                            }
                        }
                        i = putIntoOneVector(maxCliqueSize, i, ids, idxs, rDegrees[ids.get(i)], oldRDegree != rDegrees[ids.get(i)], kcfClique.clique,
                                del, degreeOne, degreeTwo, degreeThree, cliqueSize, idSize);
                    }

                    putIntoOneVector(maxCliqueSize, idxs[vId], ids, idxs, rDegrees[vId], oldDegreeV != rDegrees[vId], kcfClique.clique,
                            del, degreeOne, degreeTwo, degreeThree, cliqueSize, idSize);
                    putIntoOneVector(maxCliqueSize, idxs[wId], ids, idxs, rDegrees[wId], oldDegreeW != rDegrees[wId], kcfClique.clique,
                            del, degreeOne, degreeTwo, degreeThree, cliqueSize, idSize);
                } else {
                    assert totalConnected == 3;
                    kcfClique.clique[cliqueSize.getAndIncrement()] = -1;

                    int vId1 = ids.get(idx1);
                    int oldDegreeV1 = rDegrees[vId1];
                    int vId2 = ids.get(idx2);
                    int oldDegreeV2 = rDegrees[vId2];
                    int vId3 = ids.get(idx3);
                    int oldDegreeV3 = rDegrees[vId3];

                    --rDegrees[vId3];

                    neighbors1.flip(vId2);
                    neighbors2.flip(vId1);
                    kcfClique.changes.add(new int[]{vId1, vId2});

                    kcfClique.contractions.add(vId1);
                    kcfClique.contractions.add(vId2);
                    kcfClique.contractions.add(vId3);
                    kcfClique.contractions.add(uId);
                    kcfClique.contractions.add(3);

                    for (int i = 0; i < idSize.get(); i++) {
                        if (ids.get(i) == vId1 || ids.get(i) == vId2 || ids.get(i) == vId3)
                            continue;

                        int oldNonDegree = rDegrees[ids.get(i)];

                        boolean conn1 = neighbors1.get(ids.get(i));
                        boolean conn2 = neighbors2.get(ids.get(i));
                        boolean conn3 = neighbors3.get(ids.get(i));

                        if (conn1 && !conn2) { // Neighbor of v1 but not v2
                            neighbors1.flip(ids.get(i));
                            graph.flip(ids.get(i), vId1);
                            kcfClique.changes.add(new int[]{vId1, ids.get(i)});
                            ++rDegrees[vId1];
                            ++rDegrees[ids.get(i)];

                        }

                        if (conn2 && !conn3) { // Neighbor of v2 but not v3
                            neighbors2.flip(ids.get(i));
                            graph.flip(ids.get(i), vId2);
                            kcfClique.changes.add(new int[]{vId2, ids.get(i)});
                            ++rDegrees[vId2];
                            ++rDegrees[ids.get(i)];
                        }

                        if (conn3 && !conn1) { // Neighbor of v3 but not v1
                            neighbors3.flip(ids.get(i));
                            graph.flip(ids.get(i), vId3);
                            kcfClique.changes.add(new int[]{vId3, ids.get(i)});
                            ++rDegrees[vId3];
                            ++rDegrees[ids.get(i)];
                        }

                        boolean isDegreeChanged = oldNonDegree != rDegrees[ids.get(i)];
                        i = putIntoOneVector(maxCliqueSize, i, ids, idxs, rDegrees[ids.get(i)], isDegreeChanged, kcfClique.clique,
                                del, degreeOne, degreeTwo, degreeThree, cliqueSize, idSize);
                    }

                    boolean isChangedV1 = oldDegreeV1 != rDegrees[vId1];
                    boolean isChangedV2 = oldDegreeV2 != rDegrees[vId2];
                    boolean isChangedV3 = oldDegreeV3 != rDegrees[vId3];
                    putIntoOneVector(maxCliqueSize, idxs[vId1], ids, idxs, rDegrees[vId1], isChangedV1, kcfClique.clique,
                            del, degreeOne, degreeTwo, degreeThree, cliqueSize, idSize);
                    putIntoOneVector(maxCliqueSize, idxs[vId2], ids, idxs, rDegrees[vId2], isChangedV2, kcfClique.clique,
                            del, degreeOne, degreeTwo, degreeThree, cliqueSize, idSize);
                    putIntoOneVector(maxCliqueSize, idxs[vId3], ids, idxs, rDegrees[vId3], isChangedV3, kcfClique.clique,
                            del, degreeOne, degreeTwo, degreeThree, cliqueSize, idSize);
                }
            }

            if (log.isLoggable(Level.FINER)) {
                for (int i = 0; i < idSize.get(); i++) {
                    assert idxs[ids.get(i)] == i;
                }
            }
        }

        return kcfClique;
    }

    public static int putIntoOneVectorEq(int maxCliqueSize, int idx, List<Integer> ids, int[] idxs,
                                         int rDegree, boolean check, int[] currentClique,
                                         Deque<Integer> del, Deque<Integer> degreeOne, Deque<Integer> degreeTwo, Deque<Integer> degreeThree,
                                         MutableInteger cliqueSize, MutableInteger idSize) {
        if (check) {
            if (rDegree <= 3 && idSize.get() - rDegree + cliqueSize.get() > maxCliqueSize) {
                switch (rDegree) {
                    case 0:
                        currentClique[cliqueSize.getAndIncrement()] = ids.get(idx);

                        ids.set(idx, ids.get(idSize.decrementAndGet()));
                        idxs[ids.get(idx)] = idx;
                        --idx;
                        break;
                    case 1:
                        degreeOne.push(ids.get(idx));
                        break;
                    case 2:
                        degreeTwo.push(ids.get(idx));
                        break;
                    case 3:
                        degreeThree.push(ids.get(idx));
                        break;
                }
            }
        } else {
            if (idSize.get() - rDegree + cliqueSize.get() == maxCliqueSize) {
                del.push(ids.get(idx));
            }
        }
        return idx;
    }

    public static int putIntoOneVector(int maxCliqueSize, int idx, List<Integer> ids, int[] idxs,
                                       int rDegree, boolean check, int[] currentClique,
                                       Deque<Integer> del, Deque<Integer> degreeOne, Deque<Integer> degreeTwo, Deque<Integer> degreeThree,
                                       MutableInteger cliqueSize, MutableInteger idSize) {

        if (idSize.get() - rDegree + cliqueSize.get() <= maxCliqueSize) {
            del.add(ids.get(idx));
        } else if (check && rDegree <= 3) {
            switch (rDegree) {
                case 0:
                    currentClique[cliqueSize.getAndIncrement()] = ids.get(idx);

                    ids.set(idx, ids.get(idSize.decrementAndGet()));
                    idxs[ids.get(idx)] = idx;
                    --idx;
                    break;
                case 1:
                    degreeOne.push(ids.get(idx));
                    break;
                case 2:
                    degreeTwo.push(ids.get(idx));
                    break;
                case 3:
                    degreeThree.push(ids.get(idx));
                    break;
            }
        }

        return idx;
    }

    public static void update(Integer srcId, DenseGraph graph, KCFClique kcfClique, MutableInteger cliqueSize,
                              CliqueUpdater cliqueUpdater, Set<Integer> localClique) {
        assert cliqueUpdater.getMaxClique().size() < cliqueSize.get();
        int contractionSize = kcfClique.contractions.size();

        HashSet<Integer> newMaxClique = new HashSet<>(cliqueSize.get());

        for (int k = cliqueSize.get() - 1; k >= 0; k--) {
            if (kcfClique.clique[k] == -1) {
                int type = kcfClique.contractions.get(contractionSize - 1);

                if (type == 1) {
                    contractionSize -= 4;

                    // Get the vertices involved in this contraction
                    int vId1 = graph.getOldId(kcfClique.contractions.get(contractionSize));
                    int vId2 = graph.getOldId(kcfClique.contractions.get(contractionSize + 1));
                    int uId = graph.getOldId(kcfClique.contractions.get(contractionSize + 2));

                    if (newMaxClique.contains(vId1))
                        newMaxClique.add(vId2);
                    else
                        newMaxClique.add(uId);
                } else if (type == 2) {
                    contractionSize -= 5;

                    int vId1 = graph.getOldId(kcfClique.contractions.get(contractionSize));
                    int vId2 = graph.getOldId(kcfClique.contractions.get(contractionSize + 1));
                    int vId3 = graph.getOldId(kcfClique.contractions.get(contractionSize + 2));
                    int uId = graph.getOldId(kcfClique.contractions.get(contractionSize + 3));

                    if (!newMaxClique.contains(vId1) && !newMaxClique.contains(vId2))
                        newMaxClique.add(uId);
                    else
                        newMaxClique.add(vId3);
                } else if (type == 3) {
                    contractionSize -= 5;
                    int vId1 = graph.getOldId(kcfClique.contractions.get(contractionSize));
                    int vId2 = graph.getOldId(kcfClique.contractions.get(contractionSize + 1));
                    int vId3 = graph.getOldId(kcfClique.contractions.get(contractionSize + 2));
                    int uId = graph.getOldId(kcfClique.contractions.get(contractionSize + 3));

                    boolean in1 = newMaxClique.contains(vId1);
                    boolean in2 = newMaxClique.contains(vId2);
                    boolean in3 = newMaxClique.contains(vId3);

                    int inCount = (in1 ? 1 : 0) + (in2 ? 1 : 0) + (in3 ? 1 : 0);
                    if (inCount == 0)
                        newMaxClique.add(uId);
                    else if (inCount == 1) {
                        if (in1) newMaxClique.add(vId2);
                        else if (in2) newMaxClique.add(vId3);
                        else newMaxClique.add(vId1);
                    } else {
                        if (!in1) newMaxClique.add(vId1);
                        else if (!in2) newMaxClique.add(vId2);
                        else newMaxClique.add(vId3);
                    }
                } else {
                    throw new IllegalStateException("Invalid contraction type");
                }
            } else {
                int id = kcfClique.clique[k];
                if (k != 0)
                    id = graph.getOldId(id);
                newMaxClique.add(id);
            }
        }

        if (localClique.size() < newMaxClique.size()) {
            localClique.clear();
            localClique.addAll(newMaxClique);
        }
        cliqueUpdater.update(newMaxClique, srcId);
    }
}
