package cz.ctu.fee.dsv.semwork.coordinator;

import cz.ctu.fee.dsv.semwork.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;


public class MainCoordinator {

    private static final String REQUESTS_QUEUE = "requests_queue";
    private static final String UPDATES_EXCHANGE = "updates_exchange";

    private static Coordinator coordinator;
    private static Channel channel;
    private static ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        System.out.println("=== Coordinator is starting ===");
        coordinator = new Coordinator();

        try {
            // 1. Подключаемся к RabbitMQ
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            // factory.setUsername("guest");
            // factory.setPassword("guest");
            Connection connection = factory.newConnection();
            channel = connection.createChannel();

            // 2. Объявляем очередь (requests_queue) и exchange (updates_exchange)
            channel.queueDeclare(REQUESTS_QUEUE, false, false, false, null);
            channel.exchangeDeclare(UPDATES_EXCHANGE, BuiltinExchangeType.FANOUT);

            // 3. Подписываемся на очередь requests_queue
            DeliverCallback deliverCb = (consumerTag, delivery) -> {
                String body = new String(delivery.getBody(), StandardCharsets.UTF_8);
                handleRequest(body);
            };
            CancelCallback cancelCb = consumerTag -> {};
            channel.basicConsume(REQUESTS_QUEUE, true, deliverCb, cancelCb);

            System.out.println("=== Coordinator ready. Listening for requests... ===");
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    /**
     * Десериализуем JSON -> Message, смотрим type, вызываем Coordinator.
     */
    private static void handleRequest(String json) {
        try {
            Message msg = mapper.readValue(json, Message.class);
            System.out.println("[Coordinator] Received: " + msg.getType()
                    + " from " + msg.getProcessId() + " for " + msg.getResourceId());

            switch (msg.getType()) {
                case REQUEST_ACCESS:
                    boolean granted = coordinator.requestResource(msg.getProcessId(), msg.getResourceId());
                    if (granted) {
                        broadcast(new Message(EMessageType.GRANT_ACCESS, msg.getProcessId(), msg.getResourceId()));
                    } else {
                        broadcast(new Message(EMessageType.DENY_ACCESS, msg.getProcessId(), msg.getResourceId()));
                    }
                    break;

                case RELEASE_ACCESS:
                    coordinator.releaseResource(msg.getProcessId(), msg.getResourceId());
                    broadcast(new Message(EMessageType.RELEASE_ACCESS, msg.getProcessId(), msg.getResourceId()));
                    break;

                default:
                    System.out.println("Unknown message type: " + msg.getType());
            }
            System.out.println(coordinator.dumpGraph());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Рассылка всем воркерам (через фан-аут exchange).
     */
    private static void broadcast(Message msg) {
        try {
            String json = mapper.writeValueAsString(msg);
            channel.basicPublish(UPDATES_EXCHANGE, "", null, json.getBytes(StandardCharsets.UTF_8));
            System.out.println("[Coordinator] Broadcast: " + json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
