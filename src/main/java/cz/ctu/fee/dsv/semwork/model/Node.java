package cz.ctu.fee.dsv.semwork.model;

import lombok.Data;

@Data
public class Node {
    private final String nodeId;
    private final String ip;
    private final int port;
    private final RabbitMQService rabbitMQService;
    private boolean isAlive;
    private WaitForGraph graph;

    public Node(String id, RabbitMQService rabbitMQService, String ip, int port) {
        this.nodeId = id;
        this.rabbitMQService = rabbitMQService;
        this.ip = ip;
        this.port = port;
        this.isAlive = true;
        this.graph = new WaitForGraph();
    }

    public void start() throws Exception {
        rabbitMQService.getChannel().queueBind("updates_queue", "updates_exchange", "");
        rabbitMQService.getChannel().basicConsume("updates_queue", true, (consumerTag, delivery) -> {
            String message = new String(delivery.getBody());
            System.out.println("Node " + nodeId + " received: " + message);
            processUpdate(message);
        }, consumerTag -> {});
        System.out.println("Node " + nodeId + " listening for updates...");
    }

    public void sendRequest(String resource) throws Exception {
        String request = "REQUEST|" + nodeId + "|" + resource;
        rabbitMQService.getChannel().basicPublish("", "requests_queue", null, request.getBytes());
        System.out.println("Node " + nodeId + " sent: " + request);
    }

    // Method to handle incoming messages from the coordinator
    private void processUpdate(String message) {
        if (message.startsWith("UPDATE_GRAPH")) {
            System.out.println("Node " + nodeId + " received graph update: " + message);
        } else if (message.startsWith("GRANT")) {
            System.out.println("Node " + nodeId + " access granted.");
        } else if (message.startsWith("DENY")) {
            System.out.println("Node " + nodeId + " access denied.");
        }
    }

    public void join() {
        try {
            rabbitMQService.connect();
            start();
            if(!isAlive){
                isAlive=true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void leave() {
        System.out.println("Node " + nodeId + " leaving the topology");
        try {
            String message = "LEAVE|" + nodeId;
            rabbitMQService.getChannel().basicPublish("", "requests_queue", null, message.getBytes());
            System.out.println("Node " + nodeId + " sent leave message: " + message);
            rabbitMQService.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Simulate node failure
    public void kill() {
        this.isAlive = false;
        try {
            if (rabbitMQService != null) {
                rabbitMQService.close();
            } else {
                System.out.println("RabbitMQService is already null.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Simulate node revival
    public void revive() {
        this.isAlive = true;
        try {
            rabbitMQService.connect();
            start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendPreliminaryRequest(String resource) throws Exception {
        String request = "PRELIMINARY_REQUEST|" + nodeId + "|" + resource;
        rabbitMQService.getChannel().basicPublish("", "requests_queue", null, request.getBytes());
        System.out.println("Node " + nodeId + " sent preliminary request: " + request);
    }

    public void acquireResource(String resource) throws Exception {
        String message = "ACQUIRE|" + nodeId + "|" + resource;
        rabbitMQService.getChannel().basicPublish("", "requests_queue", null, message.getBytes());
        System.out.println("Node " + nodeId + " acquired resource: " + resource);
    }

    private String processAcquire(String processId, String resource) {
        graph.markResourceAcquired(processId, resource);
        return "ACQUIRE_CONFIRMED|" + processId + "|" + resource;
    }

    public void releaseResource(String resource) throws Exception {
        String message = "RELEASE|" + nodeId + "|" + resource;
        rabbitMQService.getChannel().basicPublish("", "requests_queue", null, message.getBytes());
        System.out.println("Node " + nodeId + " released resource: " + resource);
    }

    // Status
    public void printStatus() {
        System.out.println(getStatus());
    }

    public String getStatus() {
        return "Node ID: " + nodeId + ", Alive: " + isAlive;
    }
}