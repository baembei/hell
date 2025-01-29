package cz.ctu.fee.dsv.semwork.coordinator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import cz.ctu.fee.dsv.semwork.config.Config;
import cz.ctu.fee.dsv.semwork.config.NodeConfig;
import cz.ctu.fee.dsv.semwork.model.*;

import java.io.File;
import java.util.*;

public class Coordinator {
    private final RabbitMQService rabbitMQService;
    private final WaitForGraph graph;
    private final Map<String, Set<String>> preliminaryRequests;
    private Map<String, Node> nodes = new HashMap<>();
    private Map<String, Resource> resources = new HashMap<>();

    public Coordinator(RabbitMQService rabbitMQService) throws Exception {
        this.rabbitMQService = rabbitMQService;
        this.graph = new WaitForGraph();
        this.preliminaryRequests = new HashMap<>();

        try {
            loadConfigData();
        } catch (Exception e) {
            System.out.println("Error loading config data: " + e.getMessage());
            throw e;
        }

        System.out.println("Getting Resources " + getResources());
        System.out.println("Getting Nodes " + getNodes());
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

        String resource1 = parts.length > 2 ? parts[2] : "";
        String resource2 = parts.length > 3 ? parts[3] : "";

        Resource res1 = resources.get(resource1);
        Resource res2 = resources.get(resource2);
        Node process = nodes.get(processId);

        switch (requestType) {
            case "JOIN":
                return processJoin(processId);
            case "PRELIMINARY_REQUEST":
                return processPreliminaryRequest(process, res1, res2);
//            case "REQUEST":
//                return processRequest(processId, resource);
//            case "ACQUIRE":
//                return processAcquire(processId, resource);
//            case "RELEASE":
//                return processRelease(processId, resource);
            case "LEAVE":
                return processLeave(processId);
            default:
                System.out.println("Unknown request type: " + requestType);
                return "";
        }
    }

    private String processLeave(String processId) {
        return ".";
    }

    private String processRelease(String processId, String resource) {
        return ".";
    }

    private String processAcquire(String processId, String resource) {
        return ".";
    }

    private String processRequest(String processId, String resource) {
        return ".";
    }

    private String processJoin(String processId) {
        nodes.get(processId).join();
        if(nodes.containsKey(processId)){
            return "JOIN|OK";
        } else {
            return "JOIN|FAIL";
        }
    }

    private String processPreliminaryRequest(Node process, Resource resource1, Resource resource2) {
        System.out.println("Processing preliminary request from " + process.getNodeId() + " for resources " + resource1.getResourceId() + " and " + resource2.getResourceId());

        boolean resource1Occupied = resource1.getStatus() == EResourceStatus.OCCUPIED;
        boolean resource2Occupied = resource2.getStatus() == EResourceStatus.OCCUPIED;

        if (!resource1Occupied && !resource2Occupied) {
            preliminaryRequests.putIfAbsent(process.getNodeId(), new HashSet<>());
            preliminaryRequests.get(process.getNodeId()).add(resource1.getResourceId());
            preliminaryRequests.get(process.getNodeId()).add(resource2.getResourceId());

            System.out.println("Preliminary request granted: " + process.getNodeId() + " can request " + resource1.getResourceId() + " and " + resource2.getResourceId());
            return "PRELIMINARY_GRANT|" + process.getNodeId() + "|" + resource1.getResourceId() + "|" + resource2.getResourceId();
        } else {

            if (resource1Occupied) {
                graph.addDependency(process.getNodeId(), resource1.getOwner());
            }
            if (resource2Occupied) {
                graph.addDependency(process.getNodeId(), resource2.getOwner());
            }

            System.out.println("Preliminary request denied: " + process.getNodeId() +
                    " must wait for " + (resource1Occupied ? resource1.getOwner() : "") +
                    (resource2Occupied ? ", " + resource2.getOwner() : ""));

            return "PRELIMINARY_DENY|" + process.getNodeId() + "|" +
                    (resource1Occupied ? resource1.getOwner() : "NONE") + "|" +
                    (resource2Occupied ? resource2.getOwner() : "NONE");
        }

    }

    private void loadConfigData() throws Exception {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Config config = mapper.readValue(new File("src/main/resources/config.yml"), Config.class);

        // Initialize nodes
        for (NodeConfig nodeConfig : config.getNodes()) {
            Node node = new Node(nodeConfig.getId(), rabbitMQService, nodeConfig.getIp(), nodeConfig.getPort());
            nodes.put(nodeConfig.getId(), node);
        }

        // Initialize resources
        for (String resourceId : config.getResources()) {
            Resource resource = new Resource(resourceId);
            resources.put(resourceId, resource);
        }

        System.out.println("Loaded nodes: " + nodes.keySet());
        System.out.println("Loaded resources: " + resources.keySet());
    }

    public Map<String, Node> getNodes() {
        return nodes;
    }

    public Map<String, Resource> getResources() {
        return resources;
    }
}