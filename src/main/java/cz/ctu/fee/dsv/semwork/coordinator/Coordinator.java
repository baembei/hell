package cz.ctu.fee.dsv.semwork.coordinator;

import cz.ctu.fee.dsv.semwork.model.RabbitMQService;
import cz.ctu.fee.dsv.semwork.model.WaitForGraph;

public class Coordinator {
    private final RabbitMQService rabbitMQService;
    private final WaitForGraph graph;

    public Coordinator(RabbitMQService rabbitMQService) {
        this.rabbitMQService = rabbitMQService;
        this.graph = new WaitForGraph();
    }

    public void start() throws Exception {
        System.out.println("Setting up queues and exchange...");
        rabbitMQService.setupQueue("updates_queue");
        rabbitMQService.setupQueue("requests_queue");
        rabbitMQService.setupExchange("updates_exchange");
        System.out.println("Setup completed.");

        System.out.println("Starting to consume requests_queue...");
        rabbitMQService.getChannel().basicConsume("requests_queue", true, (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            // Логика обработки запроса (e.g., REQUEST|P1|R1)
            System.out.println("Received: " + message);

            // Проверьте граф на циклы, выдайте GRANT/DENY
            String response = processRequest(message);

            // Рассылаем результат всем через exchange
            rabbitMQService.getChannel().basicPublish("updates_exchange", "", null, response.getBytes());
        },  consumerTag -> {
            System.out.println("Consumer " + consumerTag + " cancelled");
        });

        // Example of sending a message
//        String initialMessage = "REQUEST|Coordinator|R1";
//        rabbitMQService.getChannel().basicPublish("updates_exchange", "", null, initialMessage.getBytes("UTF-8"));
//        System.out.println("Node Coordinator sent: " + initialMessage);
    }

    private String processRequest(String message) {
        // Разбираем сообщение, обновляем граф, проверяем дедлоки
        return "GRANT|P1|R1"; // или DENY|P1|R1
    }
}