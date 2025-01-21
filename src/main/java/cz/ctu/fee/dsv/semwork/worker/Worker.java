package cz.ctu.fee.dsv.semwork.worker;

import cz.ctu.fee.dsv.semwork.model.DependencyGraph;

import cz.ctu.fee.dsv.semwork.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Worker {

    private final String processId;
    private final Channel channel;
    private final String requestsQueue;
    private final ObjectMapper mapper;

    public Worker(String processId, Channel channel, String requestsQueue, ObjectMapper mapper) {
        this.processId = processId;
        this.channel = channel;
        this.requestsQueue = requestsQueue;
        this.mapper = mapper;
    }

    /**
     * Послать "REQUEST_ACCESS" (запрос ресурса)
     */
    public void requestResource(String resourceId) {
        sendMessage(new Message(EMessageType.REQUEST_ACCESS, processId, resourceId));
    }

    /**
     * Послать "RELEASE_ACCESS"
     */
    public void releaseResource(String resourceId) {
        sendMessage(new Message(EMessageType.RELEASE_ACCESS, processId, resourceId));
    }

    private void sendMessage(Message msgObj) {
        try {
            String json = mapper.writeValueAsString(msgObj);
            channel.basicPublish("", requestsQueue, null, json.getBytes(StandardCharsets.UTF_8));
            System.out.println("[Worker " + processId + "] Sent: " + json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Когда получаем Broadcast: десериализуем JSON -> Message, смотрим тип.
     */
    public void onBroadcast(String json) {
        try {
            Message msg = mapper.readValue(json, Message.class);
            EMessageType t = msg.getType();
            System.out.println("[Worker " + processId + "] Broadcast received: " + t
                    + " (process=" + msg.getProcessId() + ", resource=" + msg.getResourceId() + ")");

            // Если GRANT_ACCESS / DENY_ACCESS / RELEASE_ACCESS — реагируем при необходимости
            if (t == EMessageType.GRANT_ACCESS && msg.getProcessId().equals(processId)) {
                System.out.println("[Worker " + processId + "] Resource " + msg.getResourceId() + " granted to me!");
            }
            if (t == EMessageType.DENY_ACCESS && msg.getProcessId().equals(processId)) {
                System.out.println("[Worker " + processId + "] Resource " + msg.getResourceId() + " denied to me!");
            }
            if (t == EMessageType.RELEASE_ACCESS) {
                System.out.println("[Worker " + processId + "] Notice: resource " + msg.getResourceId()
                        + " was released by " + msg.getProcessId());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}