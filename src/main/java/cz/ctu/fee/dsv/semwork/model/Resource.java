package cz.ctu.fee.dsv.semwork.model;

import lombok.Data;

@Data
public class Resource {
    private final String resourceId;
    private EResourceStatus status;
    private String owner;

    public Resource(String resourceId) {
        this.resourceId = resourceId;
        this.status = EResourceStatus.FREE;
    }

    public void acquire() {
        this.status = EResourceStatus.OCCUPIED;
    }

    public void release() {
        this.status = EResourceStatus.FREE;
    }
}
