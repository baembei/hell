package cz.ctu.fee.dsv.semwork.coordinator;

import cz.ctu.fee.dsv.semwork.model.DependencyGraph;
import cz.ctu.fee.dsv.semwork.model.ProcessNode;
import cz.ctu.fee.dsv.semwork.model.ResourceNode;

public class Coordinator {

    private final DependencyGraph graph = new DependencyGraph();

    public Coordinator() {
        System.out.println("Coordinator created. (Global graph initialized)");
    }

    /**
     * Запрос ресурса:
     *  1) Добавляем ребро P->R
     *  2) Проверяем на цикл
     *  3) Если есть цикл, откатываем и возвращаем "DENY"
     *  4) Если нет, добавляем R->P и возвращаем "GRANT"
     */
    public synchronized String requestResource(String processId, String resourceId) {
        ProcessNode p = new ProcessNode(processId);
        ResourceNode r = new ResourceNode(resourceId);

        // Гарантируем наличие вершин в графе
        graph.addNode(p);
        graph.addNode(r);

        // Шаг 1: P->R
        graph.addEdge(p, r);

        // Шаг 2: Проверка цикла
        if (graph.hasCycle()) {
            // Откат
            graph.removeEdge(p, r);
            // Отказ
            return "DENY|" + processId + "|" + resourceId;
        }

        // Шаг 3: Добавить R->P (ресурс выделен)
        graph.addEdge(r, p);
        if (graph.hasCycle()) {
            // Откат (теоретически не должно случиться)
            graph.removeEdge(r, p);
            graph.removeEdge(p, r);
            return "DENY|" + processId + "|" + resourceId;
        }

        // Успех
        return "GRANT|" + processId + "|" + resourceId;
    }

    /**
     * Освобождение ресурса:
     *  - Убираем P->R и R->P, если есть
     */
    public synchronized String releaseResource(String processId, String resourceId) {
        ProcessNode p = new ProcessNode(processId);
        ResourceNode r = new ResourceNode(resourceId);

        graph.removeEdge(p, r);
        graph.removeEdge(r, p);

        return "RELEASED|" + processId + "|" + resourceId;
    }

    /**
     * Для отладки: вывести текущее состояние графа.
     */
    public synchronized String dumpGraph() {
        return graph.dumpGraph();
    }
}