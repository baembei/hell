package cz.ctu.fee.dsv.semwork.model;

import com.rabbitmq.client.Channel;
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
        System.out.println("Node " + nodeId + " starting...");

        try {
            rabbitMQService.setupQueue("updates_queue");
            rabbitMQService.setupQueue("requests_queue");
            rabbitMQService.setupExchange("updates_exchange");

            subscribeToQueue("updates_queue");
        } catch (Exception e) {
            System.err.println("Error setting up queue or exchange: " + e.getMessage());
        }

        System.out.println("Node " + nodeId + " started.");
    }

    private void subscribeToQueue(String queueName) throws Exception {
        Channel channel = rabbitMQService.getChannel();
        channel.basicConsume(queueName, true, (consumerTag, message) -> {
            String msg = new String(message.getBody(), "UTF-8");
            processMessage(msg);
        }, consumerTag -> {});
    }

    private void processMessage(String message) {
        System.out.println("Node " + nodeId + " received message: " + message);
        // Add your message processing logic here
    }

    // Method to handle incoming messages from the coordinator
    private void processUpdate(String message) {
        String[] parts = message.split("\\|");

        if (parts.length < 2) {
            System.out.println("Node " + nodeId + " received malformed message: " + message);
            return;
        }

        if (parts[0].equals("REQUEST")) {
            if (parts[1].equals("OK")) {
                if (parts.length >= 4) {
                    String pId = parts[2];
                    String rId = parts[3];
                    System.out.println("Node " + nodeId + " sees: process " + pId
                            + " got resource " + rId + " (WAITING).");
                } else {
                    System.out.println("Node " + nodeId + " received incomplete OK message: " + message);
                }
            } else if (parts[1].equals("DENY")) {
                System.out.println("Node " + nodeId + " sees: request denied => " + message);
            } else {
                System.out.println("Node " + nodeId + " received unknown request status: " + message);
            }
        } else {
            System.out.println("Node " + nodeId + " received unknown message type: " + message);
        }
    }


    public void join() {
        try {
            rabbitMQService.connect();
            start();

            if (!isAlive) {
                isAlive = true;
            }

            String message = "JOIN|" + nodeId + "|" + ip + "|" + port;
            rabbitMQService.getChannel().basicPublish("", "requests_queue", null, message.getBytes());
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
            join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getStatus() {
        return "Node ID: " + nodeId + ", Alive: " + isAlive;
    }

    public void sendPreliminaryRequest(Resource resource1, Resource resource2) throws Exception {
        String request = "PRELIMINARY_REQUEST|" + nodeId + "|" + resource1.getResourceId() + "|" + resource2.getResourceId();
        rabbitMQService.getChannel().basicPublish("", "requests_queue", null, request.getBytes());
        System.out.println("Node " + nodeId + " sent: " + request);
    }

    public void sendRequest(Resource resource) throws Exception {
        String request = "REQUEST|" + nodeId + "|" + resource.getResourceId();
        rabbitMQService.getChannel().basicPublish("", "requests_queue", null, request.getBytes());
        System.out.println("Node " + nodeId + " sent: " + request);
    }

    public void acquireResource(Resource resource) throws Exception {

        System.out.println("HELOOOOOOOOO" + resource.getStatus() + resource.getRequestedBy());
        if (resource.getStatus() == EResourceStatus.WAITING) {
            if (nodeId.equals(resource.getRequestedBy())) {
                resource.acquire();
                System.out.println("Node " + nodeId + " acquired resource: " + resource.getResourceId());
            } else {
                System.out.println("Node " + nodeId + " tried to acquire resource " + resource.getResourceId() + " but it was requested by another node!");
            }
        } else {
            System.out.println("Node " + nodeId + " tried to acquire resource " + resource.getResourceId() + " but it's already taken!");
        }
    }

    public void releaseResource(Resource resource) throws Exception {
        if(resource.getStatus() == EResourceStatus.OCCUPIED){
            resource.release();
            System.out.println("Node " + nodeId + " released resource: " + resource.getResourceId());
        } else {
            System.out.println("Node " + nodeId + " tried to release resource " + resource.getResourceId() + " but it's already free!");
        }

        rabbitMQService.getChannel().basicPublish("", "updates_queue", null, ("RELEASE|" + nodeId + "|" + resource.getResourceId()).getBytes());
    }
}