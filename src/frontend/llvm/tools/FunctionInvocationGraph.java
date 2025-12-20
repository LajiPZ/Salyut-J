package frontend.llvm.tools;

import frontend.llvm.value.Function;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

// Don't think this is useful
public class FunctionInvocationGraph {
    private HashMap<Function, Set<Function>> edges = new HashMap<>();

    public void addEdge(Function from, Function to) {
        edges.computeIfAbsent(from, k -> new HashSet<>()).add(to);
    }

    public boolean hasCycle(Function current) {
        return dfs(current, new HashSet<>(), new HashSet<>());
    }

    private boolean dfs(Function current, Set<Function> visited, Set<Function> active) {
        if (active.contains(current)) {
            return true;
        }
        if (visited.contains(current)) {
            return false;
        }
        visited.add(current);
        active.add(current);
        for (Function next : edges.getOrDefault(current, Set.of())) {
            if (dfs(next, visited, active)) return true;
        }
        active.remove(current);
        return false;
    }
}
