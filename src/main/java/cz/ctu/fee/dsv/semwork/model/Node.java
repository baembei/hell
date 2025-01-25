package cz.ctu.fee.dsv.semwork.model;

public class Node {
    private final String nodeId;
    private final RabbitMQService rabbitMQService;

    public Node(String nodeId, RabbitMQService rabbitMQService) {
        this.nodeId = nodeId;
        this.rabbitMQService = rabbitMQService;
    }

    public void start() throws Exception {
        rabbitMQService.getChannel().queueBind("updates_queue", "updates_exchange", "");

        System.out.println("listening to updates_queue as NODE " + nodeId);
        // Подписываемся на обменник
        rabbitMQService.getChannel().basicConsume("updates_queue", true, (consumerTag, delivery) -> {
            String message = new String(delivery.getBody());
            System.out.println("Node " + nodeId + " received: " + message);
            // Логика обработки ответа (e.g., GRANT/DENY)
        }, consumerTag -> {});

        // Отправляем запрос
        String request = "REQUEST|" + nodeId + "|R1";
        rabbitMQService.getChannel().basicPublish("", "requests_queue", null, request.getBytes());
        System.out.println("Node " + nodeId + " sent: " + request);
    }
}