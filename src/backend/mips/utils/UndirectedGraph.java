package backend.mips.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UndirectedGraph<T> {
    private final Set<T> vertices;
    private final Map<T, Set<T>> edges;

    public UndirectedGraph() {
        this.vertices = new HashSet<>();
        this.edges = new HashMap<>();
    }

    public void addVertex(T vertex) {
        if (!vertices.contains(vertex)) {
            this.vertices.add(vertex);
            this.edges.put(vertex, new HashSet<>());
        }
    }

    public void addEdge(T v1, T v2) {
        edges.get(v1).add(v2);
        edges.get(v2).add(v1);
    }

    public Set<T> getVertices() {
        return vertices;
    }

    public Set<T> getNeighbors(T vertex) {
        return edges.get(vertex);
    }

    public int getEdgeCount(T v) {
        return edges.get(v).size();
    }
}
