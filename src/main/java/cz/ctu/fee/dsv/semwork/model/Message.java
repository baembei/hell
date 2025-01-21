package cz.ctu.fee.dsv.semwork.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Общая структура сообщения (JSON), которое
 * мы шлем через RabbitMQ.
 */
public class Message {

    private EMessageType type;  // REQUEST_ACCESS, GRANT_ACCESS, ...
    private String processId;   // "P1", "P2" ...
    private String resourceId;  // "R1", "R2" ...

    // Пустой конструктор для Jackson
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

    // Фабричные методы, по желанию
    public static Message request(String p, String r) {
        return new Message(EMessageType.REQUEST_ACCESS, p, r);
    }
    public static Message release(String p, String r) {
        return new Message(EMessageType.RELEASE_ACCESS, p, r);
    }
}
