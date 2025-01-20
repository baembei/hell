package cz.ctu.fee.dsv.semwork.coordinator;

import cz.ctu.fee.dsv.semwork.coordinator.Coordinator;

public class MainCoordinator {

    // Один объект Coordinator на всё приложение (глобальный)
    private static Coordinator coordinator;

    public static void main(String[] args) throws Exception {
        System.out.println("=== Coordinator is starting ===");
        coordinator = new Coordinator();

        // TODO: Подключение к RabbitMQ или запуск REST. Ниже — пример, если мы делаем RabbitMQ Consumer.
        /*
            - Создаем соединение (ConnectionFactory, channel)
            - Объявляем очередь, скажем, "requests_queue"
            - Начинаем принимать сообщения
            - В callback вызываем handleMessage(...)
        */

        // Пример псевдокода:

        /*
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        // ... other settings ...
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare("requests_queue", false, false, false, null);
        channel.basicConsume("requests_queue", true, (consumerTag, delivery) -> {
            String msg = new String(delivery.getBody(), StandardCharsets.UTF_8);
            handleMessage(msg, channel);
        }, consumerTag -> {});
        */

        System.out.println("=== Coordinator ready. Waiting for messages... ===");
        // (приложение продолжает работать, слушая очередь)
    }

    /**
     * Обработка входящего сообщения: форматы "REQUEST|P|R" / "RELEASE|P|R"
     */
    private static void handleMessage(String msg) {
        String[] parts = msg.split("\\|");
        if (parts.length < 3) {
            System.out.println("Unknown message: " + msg);
            return;
        }

        String command = parts[0];
        String processId = parts[1];
        String resourceId = parts[2];
        String response;

        switch (command) {
            case "REQUEST":
                response = coordinator.requestResource(processId, resourceId);
                // response может быть "DENY|P|R" или "GRANT|P|R"
                broadcast(response);
                System.out.println(coordinator.dumpGraph());
                break;
            case "RELEASE":
                response = coordinator.releaseResource(processId, resourceId);
                // response = "RELEASED|P|R"
                broadcast(response);
                System.out.println(coordinator.dumpGraph());
                break;
            default:
                System.out.println("Unknown command: " + command);
        }
    }

    /**
     * Рассылаем обновление всем воркерам (через exchange, например).
     */
    private static void broadcast(String message) {
        // TODO: publish в fanout-exchange "updates"
        // или еще как-то отправить воркерам
        System.out.println("Broadcast: " + message);
    }
}
