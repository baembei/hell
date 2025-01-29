package cz.ctu.fee.dsv.semwork.model;

import lombok.Data;

import java.util.*;

@Data
public class WaitForGraph {
    // Key: the process that depends on others
    // Value: the set of processes it depends on
    private final Map<String, Set<String>> dependencies = new HashMap<>();

    /**
     * Add a dependency: from → to
     * (from depends on to)
     */
    public void addDependency(String from, String to) {
        dependencies
                .computeIfAbsent(from, k -> new HashSet<>())
                .add(to);
    }

    /**
     * Remove a specific dependency: from → to
     */
    public void removeDependency(String from, String to) {
        Set<String> deps = dependencies.get(from);
        if (deps != null) {
            deps.remove(to);
            if (deps.isEmpty()) {
                dependencies.remove(from);
            }
        }
    }

    /**
     * Remove all dependencies of a given process.
     * This effectively removes all edges: processId → X.
     */
    public void removeAllDependencies(String processId) {
        dependencies.remove(processId);
    }

    /**
     * Returns true if there is a cycle (deadlock) in the graph.
     */
    public boolean hasCycle() {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        for (String processId : dependencies.keySet()) {
            if (!visited.contains(processId)) {
                if (dfsDetectCycle(processId, visited, recursionStack)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Helper method to detect a cycle (DFS).
     */
    private boolean dfsDetectCycle(String current,
                                   Set<String> visited,
                                   Set<String> recursionStack) {
        visited.add(current);
        recursionStack.add(current);

        // Processes that 'current' depends on
        Set<String> dependents = dependencies.getOrDefault(current, Collections.emptySet());

        for (String dep : dependents) {
            // if dep hasn't been visited yet, DFS deeper
            if (!visited.contains(dep)) {
                if (dfsDetectCycle(dep, visited, recursionStack)) {
                    return true;
                }
            }
            // if dep is already in recursionStack, we have a cycle
            else if (recursionStack.contains(dep)) {
                return true;
            }
        }

        // done exploring current
        recursionStack.remove(current);
        return false;
    }

    @Override
    public String toString() {
        return dependencies.toString();
    }
}