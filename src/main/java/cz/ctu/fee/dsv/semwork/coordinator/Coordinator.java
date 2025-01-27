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
        System.out.println("Coordinator starting...");
        System.out.println("Setting up queues and exchange...");
        rabbitMQService.setupQueue("updates_queue");
        rabbitMQService.setupQueue("requests_queue");
        rabbitMQService.setupExchange("updates_exchange");
        System.out.println("Setup completed.");

        System.out.println("Starting to consume requests_queue...");
        rabbitMQService.getChannel().basicConsume("requests_queue", true, (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println("Coordinator received: " + message);
            String response = handleMessage(message);
            rabbitMQService.getChannel().basicPublish("updates_exchange", "", null, response.getBytes());
        }, consumerTag -> {
            System.out.println("Consumer " + consumerTag + " cancelled");
        });

        System.out.println("Coordinator is listening for requests...");
    }

    private String handleMessage(String message) {
        String[] parts = message.split("\\|");
        String requestType = parts[0];
        String processId = parts[1];
        String resource = parts.length > 2 ? parts[2] : "";

        switch (requestType) {
            case "JOIN":
                return processJoin(processId);
            case "PRELIMINARY_REQUEST":
                return processPreliminaryRequest(processId, resource);
            case "REQUEST":
                return processRequest(processId, resource);
            case "ACQUIRE":
                return processAcquire(processId, resource);
            case "RELEASE":
                return processRelease(processId, resource);
            case "JOIN_CONFIRMED":
                return processJoin(processId);
            default:
                System.out.println("Unknown request type: " + requestType);
                return "";
        }
    }

    private String processRelease(String processId, String resource) {
        return processId;
    }

    private String processAcquire(String processId, String resource) {
        return processId;
    }

    private String processPreliminaryRequest(String processId, String resource) {
        if (graph.canGrantAccess(processId, resource)) {
            System.out.println("Preliminary grant for " + processId + " to access " + resource);
            return "PRELIMINARY_GRANT|" + processId + "|" + resource;
        } else {
            System.out.println("Preliminary deny for " + processId + " to access " + resource);
            return "PRELIMINARY_DENY|" + processId + "|" + resource;
        }
    }

    private String processRequest(String processId, String resource) {
        if (graph.canGrantAccess(processId, resource)) {
            graph.addDependency(processId, resource);
            return "GRANT|" + processId + "|" + resource;
        } else {
            return "DENY|" + processId + "|" + resource;
        }
    }

    private String processJoin(String processId) {
        System.out.println("Node " + processId + " joined.");
        return "JOIN_CONFIRMED|" + processId;
    }

    private String processKill(String processId) {
        System.out.println("Node " + processId + " killed.");
        return "KILL_CONFIRMED|" + processId;
    }
}