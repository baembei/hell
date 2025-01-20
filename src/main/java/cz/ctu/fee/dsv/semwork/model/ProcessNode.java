package cz.ctu.fee.dsv.semwork.model;

import java.util.Objects;

public class ProcessNode implements IGraphNode {
    private final String processId;

    public ProcessNode(String processId) {
        this.processId = processId;
    }

    @Override
    public String getId() {
        return "P:" + processId;
    }

    // equals / hashCode: считаем равными, если совпадает processId
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProcessNode)) return false;
        ProcessNode that = (ProcessNode) o;
        return Objects.equals(processId, that.processId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processId);
    }
}