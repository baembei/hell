package cz.ctu.fee.dsv.semwork.model;

import cz.ctu.fee.dsv.semwork.model.IGraphNode;

import java.util.*;


public class DependencyGraph {

    private final Map<IGraphNode, Set<IGraphNode>> adjacency = new HashMap<>();

    public synchronized void addNode(IGraphNode node) {
        adjacency.putIfAbsent(node, new HashSet<>());
    }

    public synchronized void addEdge(IGraphNode from, IGraphNode to) {
        adjacency.putIfAbsent(from, new HashSet<>());
        adjacency.get(from).add(to);
    }

    public synchronized void removeEdge(IGraphNode from, IGraphNode to) {
        Set<IGraphNode> edges = adjacency.get(from);
        if (edges != null) {
            edges.remove(to);
        }
    }

    public synchronized boolean hasCycle() {
        Set<IGraphNode> white = new HashSet<>(adjacency.keySet());
        Set<IGraphNode> gray = new HashSet<>();
        Set<IGraphNode> black = new HashSet<>();

        for (IGraphNode node : adjacency.keySet()) {
            if (white.contains(node)) {
                if (dfsCycle(node, white, gray, black)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean dfsCycle(IGraphNode current,
                             Set<IGraphNode> white,
                             Set<IGraphNode> gray,
                             Set<IGraphNode> black) {
        white.remove(current);
        gray.add(current);

        Set<IGraphNode> neighbors = adjacency.getOrDefault(current, Collections.emptySet());
        for (IGraphNode next : neighbors) {
            if (gray.contains(next)) return true;
            if (!black.contains(next) && dfsCycle(next, white, gray, black)) {
                return true;
            }
        }

        gray.remove(current);
        black.add(current);
        return false;
    }

    public synchronized String dumpGraph() {
        StringBuilder sb = new StringBuilder("DependencyGraph:\n");
        for (IGraphNode from : adjacency.keySet()) {
            for (IGraphNode to : adjacency.get(from)) {
                sb.append("  ").append(from.getId()).append(" -> ").append(to.getId()).append("\n");
            }
        }
        return sb.toString();
    }
}