package cz.ctu.fee.dsv.semwork.coordinator;

import cz.ctu.fee.dsv.semwork.model.DependencyGraph;
import cz.ctu.fee.dsv.semwork.model.ProcessNode;
import cz.ctu.fee.dsv.semwork.model.ResourceNode;

public class Coordinator {
    private final DependencyGraph graph = new DependencyGraph();

    public Coordinator() {
        System.out.println("Coordinator created (global graph init).");
    }

    /**
     * Запрос ресурса (Ломет):
     *  1) P->R
     *  2) Проверка цикла
     *  3) Если цикл, откат -> DENY
     *  4) Иначе R->P -> GRANT
     */
    public synchronized boolean requestResource(String processId, String resourceId) {
        ProcessNode p = new ProcessNode(processId);
        ResourceNode r = new ResourceNode(resourceId);

        graph.addNode(p);
        graph.addNode(r);
        graph.addEdge(p, r);

        if (graph.hasCycle()) {
            graph.removeEdge(p, r);
            return false; // DENY
        }
        // добавляем R->P
        graph.addEdge(r, p);
        if (graph.hasCycle()) {
            // откат
            graph.removeEdge(r, p);
            graph.removeEdge(p, r);
            return false;
        }
        return true; // GRANT
    }

    /**
     * Освобождение ресурса
     */
    public synchronized void releaseResource(String processId, String resourceId) {
        ProcessNode p = new ProcessNode(processId);
        ResourceNode r = new ResourceNode(resourceId);

        graph.removeEdge(p, r);
        graph.removeEdge(r, p);
    }

    public synchronized String dumpGraph() {
        return graph.dumpGraph();
    }
}