package org.sigmod.tcc.clique;

import java.util.HashSet;
import java.util.Set;

/**
 * Constructs the maximum clique.
 */
public class CliqueUpdater {

    private Set<Integer> maxClique = new HashSet<>();

    private Integer srcId;

    public void update(Set<Integer> newClique, Integer srcId) {
        if (maxClique == null || maxClique.size() < newClique.size()) {
            maxClique = newClique;
            this.srcId = srcId;
        }
    }

    public Set<Integer> getMaxClique() {
        return maxClique;
    }

    public Integer getSrcId() {
        return srcId;
    }

}
