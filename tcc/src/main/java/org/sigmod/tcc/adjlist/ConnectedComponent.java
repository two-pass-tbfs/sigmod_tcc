package org.sigmod.tcc.adjlist;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import org.sigmod.chronograph.common.EdgeEvent;

import java.util.*;

/**
 * Finds the connected component using Disjoint-Set Union algorithm
 */
public class ConnectedComponent {

    /**
     * Find the top-level parent of a vertex.
     *
     * @param parents the parents of the vertices
     * @param x       the vertex to merge
     * @return the parent vertex
     */
    public static Vertex root(HashMap<Vertex, Vertex> parents, Vertex x) {
        if (parents.get(x).equals(x)) return x;
        return root(parents, parents.get(x));
    }

    /**
     * Finds the connected components of a graph.
     *
     * @param edgeEvents
     * @return
     */
    public static Map<Vertex, Collection<Vertex>> compute(List<EdgeEvent> edgeEvents) {
        HashMap<Vertex, Vertex> parents = edgeEvents.stream().collect(HashSet<Vertex>::new, (set, edgeEvent) -> {
            set.add(edgeEvent.getVertex(Direction.OUT));
            set.add(edgeEvent.getVertex(Direction.IN));
        }, Set::addAll).stream().collect(HashMap::new, (map, vertex) -> map.put(vertex, vertex), HashMap::putAll);

        edgeEvents.forEach(edgeEvent -> {
            Vertex key = root(parents, edgeEvent.getVertex(Direction.OUT));
            Vertex value = root(parents, edgeEvent.getVertex(Direction.IN));
            parents.put(key, value);
        });

        parents.forEach((vertex, parent) -> {
            Vertex newParent = root(parents, parent);
            parents.put(vertex, newParent);
        });

        Map<Vertex, Collection<Vertex>> components = new HashMap<>();
        parents.forEach((vertex, parent) -> components.computeIfAbsent(parent, _ -> new ArrayList<>()).add(vertex));

        return components;
    }
}
