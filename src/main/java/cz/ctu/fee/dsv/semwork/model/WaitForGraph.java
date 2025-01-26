package cz.ctu.fee.dsv.semwork.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WaitForGraph {
    private final Map<String, Set<String>> dependencies = new HashMap<>();

    public void addDependency(String processId, String resource) {
        dependencies.computeIfAbsent(processId, k -> new HashSet<>()).add(resource);
    }

    public void removeDependency(String processId, String resource) {
        if (dependencies.containsKey(processId)) {
            dependencies.get(processId).remove(resource);
            if (dependencies.get(processId).isEmpty()) {
                dependencies.remove(processId);
            }
        }
    }

    public boolean canGrantAccess(String processId, String resource) {
        // Проверяем, вызовет ли добавление зависимости дедлок
        return !detectDeadlock(processId, resource);
    }

    private boolean detectDeadlock(String processId, String resource) {
        // Реализуйте логику проверки дедлоков
        return false;
    }

    public void markResourceAcquired(String processId, String resource) {
    }
}