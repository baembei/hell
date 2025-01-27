package cz.ctu.fee.dsv.semwork.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Message {

    private EMessageType type;
    private String processId;
    private String resourceId;

    public Message() {}

    public Message(EMessageType type, String processId, String resourceId) {
        this.type = type;
        this.processId = processId;
        this.resourceId = resourceId;
    }

    @JsonProperty
    public EMessageType getType() {
        return type;
    }
    public void setType(EMessageType type) {
        this.type = type;
    }

    @JsonProperty
    public String getProcessId() {
        return processId;
    }
    public void setProcessId(String processId) {
        this.processId = processId;
    }

    @JsonProperty
    public String getResourceId() {
        return resourceId;
    }
    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public static Message request(String p, String r) {
        return new Message(EMessageType.REQUEST_ACCESS, p, r);
    }
    public static Message release(String p, String r) {
        return new Message(EMessageType.RELEASE_ACCESS, p, r);
    }
}
