package cz.ctu.fee.dsv.semwork.model;

import java.util.*;

public class WaitForGraph {

    /**
     * Ключ: идентификатор процесса (String, например "P1", "P2").
     * Значение: множество процессов (String), от которых данный процесс ждёт ресурс.
     *
     * Если в WFG есть ребро P -> Q, это значит "P ждёт Q" (P не может продолжить,
     * пока Q не освободит нужный ресурс).
     */
    private final Map<String, Set<String>> adjacency = new HashMap<>();

    /**
     * Добавить процесс в граф (чтобы adjacency не упал на null).
     */
    public synchronized void addProcess(String processId) {
        adjacency.putIfAbsent(processId, new HashSet<>());
    }

    /**
     * Добавить зависимость P->Q.
     * Означает, что процесс P ждёт процесс Q (потому что Q владеет нужным ресурсом).
     */
    public synchronized void addEdge(String p, String q) {
        addProcess(p);
        addProcess(q);
        adjacency.get(p).add(q);
    }

    /**
     * Убрать зависимость P->Q.
     */
    public synchronized void removeEdge(String p, String q) {
        Set<String> set = adjacency.get(p);
        if (set != null) {
            set.remove(q);
        }
    }

    /**
     * Удалить процесс целиком из графа.
     * (Напр., если он завершился и больше не участвует в системе).
     */
    public synchronized void removeProcess(String processId) {
        adjacency.remove(processId);
        adjacency.values().forEach(depSet -> depSet.remove(processId));
    }

    /**
     * Проверка, не появился ли в графе цикл.
     * Если есть цикл => deadlock.
     */
    public synchronized boolean hasCycle() {
        // Для DFS берём три множества: white (не посещенные), gray (в стеке), black (обработанные)
        Set<String> white = new HashSet<>(adjacency.keySet());
        Set<String> gray = new HashSet<>();
        Set<String> black = new HashSet<>();

        for (String node : adjacency.keySet()) {
            if (white.contains(node)) {
                if (dfsCycle(node, white, gray, black)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean dfsCycle(String current,
                             Set<String> white,
                             Set<String> gray,
                             Set<String> black) {
        white.remove(current);
        gray.add(current);

        Set<String> neighbors = adjacency.getOrDefault(current, Collections.emptySet());
        for (String next : neighbors) {
            if (gray.contains(next)) {
                // Нашли цикл (возврат в вершину, которая в стеке).
                return true;
            }
            if (!black.contains(next)) {
                if (dfsCycle(next, white, gray, black)) {
                    return true;
                }
            }
        }

        gray.remove(current);
        black.add(current);
        return false;
    }

    /**
     * Список всех процессов (узлов), о которых знает граф.
     */
    public synchronized Set<String> getAllProcesses() {
        return Collections.unmodifiableSet(adjacency.keySet());
    }

    public synchronized String dumpGraph() {
        StringBuilder sb = new StringBuilder("WaitForGraph:\n");
        for (String from : adjacency.keySet()) {
            for (String to : adjacency.get(from)) {
                sb.append("  ").append(from).append(" -> ").append(to).append("\n");
            }
        }
        return sb.toString();
    }
}