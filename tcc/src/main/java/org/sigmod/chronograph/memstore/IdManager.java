package org.sigmod.chronograph.memstore;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Manages the integer representation of vertex IDs
 */
public class IdManager {

    private HashMap<String, Integer> idToIdx;
    private String[] idxToID;

    /**
     * Initializes the collection of integer ids, string ids
     */
    public IdManager(HashSet<String> idSet) {
        idxToID = new String[idSet.size()];
        idToIdx = new HashMap<>();
        int cnt = 0;
        for (String id : idSet) {
            idToIdx.put(id, cnt);
            idxToID[cnt] = id;
            cnt++;
        }
    }

    /**
     * Initializes the collection of vertex IDs
     */
    public IdManager(HashMap<String, Integer> idSet) {
        idxToID = new String[idSet.size() + 1];
        idToIdx = idSet;

        idSet.forEach((id, index) -> idxToID[index] = id);
    }

    public Integer get(String vertexId) {
        return idToIdx.get(vertexId);
    }

    /**
     * Returns the vertex ID mapped to the index
     *
     * @param vertexIdx the vertex index
     * @return the string vertex ID
     */
    public String get(Integer vertexIdx) {
        return idxToID[vertexIdx];
    }

    public Integer size() {
        return idToIdx.size();
    }

    public HashMap<String, Integer> idToIdxMap() {
        return idToIdx;
    }

    public String[] idxToID() {
        return idxToID;
    }
}
