package org.sigmod.tcc.clique.sparse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Holds the KCF-clique algorithm data.
 */
public class KCFClique {
    List<Integer> idList;
//    int cliqueSize;

    int[] clique;
    List<Integer> contractions;
    List<int[]> changes;

    int firstId;

    int[] heads;
    int[] tempIds;
    int[] nexts;
    int[] idxs;

    boolean[] vis;

    public static Logger log = Logger.getLogger(KCFClique.class.getName());

    public KCFClique(int firstId, List<Integer> idList, int upperBound) {
        this.firstId = firstId;
        this.idList = idList;

        this.clique = new int[upperBound];
        this.clique[0] = firstId;
        this.contractions = new ArrayList<>();
        this.changes = new ArrayList<>();

        this.heads =  new int[idList.size()];
        Arrays.fill(heads, -1);
        this.tempIds = new int[idList.size()];
        this.nexts = new int[idList.size()];

        this.idxs = new int[idList.size()];

        this.vis = new boolean[idList.size()];
    }
}
