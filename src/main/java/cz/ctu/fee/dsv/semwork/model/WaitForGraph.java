package cz.ctu.fee.dsv.semwork.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
     * Completely remove a process from the graph,
     * including both outgoing and incoming edges.
     * Typically used when the process leaves or terminates.
     */
    public void removeNode(String processId) {
        // 1) Remove all outgoing edges from the given process
        dependencies.remove(processId);
        // 2) Remove all incoming edges pointing to the given process
        for (Set<String> depSet : dependencies.values()) {
            depSet.remove(processId);
        }
    }

    @Override
    public String toString() {
        return dependencies.toString();
    }
}