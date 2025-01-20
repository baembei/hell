package cz.ctu.fee.dsv.semwork.model;

import java.util.Objects;

public class ResourceNode implements IGraphNode {
    private final String resourceId;

    public ResourceNode(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceId() {
        return resourceId;
    }

    @Override
    public String getId() {
        return "R:" + resourceId; // Или как вы хотите формировать идентификатор
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourceNode)) return false;
        ResourceNode that = (ResourceNode) o;
        return Objects.equals(resourceId, that.resourceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceId);
    }
}