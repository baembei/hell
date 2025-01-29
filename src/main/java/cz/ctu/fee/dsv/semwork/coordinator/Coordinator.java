package cz.ctu.fee.dsv.semwork.coordinator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import cz.ctu.fee.dsv.semwork.config.Config;
import cz.ctu.fee.dsv.semwork.config.NodeConfig;
import cz.ctu.fee.dsv.semwork.model.*;
import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.sql.SQLOutput;
import java.util.*;

@Data
public class Coordinator {
    @JsonIgnore
    private final RabbitMQService rabbitMQService;
    @JsonIgnore
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
 private String processJoin(String processId, String ip, int port) {
        Node existingNode = nodes.get(processId);

        if (existingNode == null) {
            Node newNode = new Node(processId, rabbitMQService, ip, port);
            newNode.setGraph(graph);
            nodes.put(processId, newNode);
            newNode.join();
            return "JOIN|OK (created new)";
        } else {
            existingNode.join();
            return "JOIN|OK (already existed)";
        }
    }

    private String handleMessage(String message) throws IOException {
        String[] parts = message.split("\\|");
        String requestType = parts[0];
        String processId = (parts.length > 1) ? parts[1] : null;

        switch (requestType) {
            case "JOIN":
                // JOIN|<processId>|<ip>|<port>
                if (parts.length >= 4) {
                    String ip = parts[2];
                    int port = Integer.parseInt(parts[3]);
                    return processJoin(processId, ip, port);
                } else {
                    return "JOIN|FAIL|BAD_ARGUMENTS";
                }

            case "REQUEST":
                // REQUEST|<processId>|<resource>
                if (parts.length >= 3) {
                    String resourceId = parts[2];
                    return processRequest(processId, resourceId);
                } else {
                    return "REQUEST|FAIL|BAD_ARGUMENTS";
                }

            case "ACQUIRE":
                // ACQUIRE|<processId>|<resource>
                if (parts.length >= 3) {
                    String resource = parts[2];
                    return processAcquire(processId, resource);
                } else {
                    return "ACQUIRE|FAIL|BAD_ARGUMENTS";
                }

            case "RELEASE":
                // RELEASE|<processId>|<resource>
                if (parts.length >= 3) {
                    String resource = parts[2];
                    return processRelease(processId, resource);
                } else {
                    return "RELEASE|FAIL|BAD_ARGUMENTS";
                }

            case "LEAVE":
                // LEAVE|<processId>
                if (parts.length >= 2) {
                    return processLeave(processId);
                } else {
                    return "LEAVE|FAIL|BAD_ARGUMENTS";
                }

            case "PRELIMINARY_REQUEST":
                // PRELIMINARY_REQUEST|<processId>|<res1>|<res2>
                if (parts.length >= 4) {
                    String res1 = parts[2];
                    String res2 = parts[3];
                    return processPreliminaryRequest(nodes.get(processId),
                            resources.get(res1),
                            resources.get(res2));
                } else {
                    return "PRELIMINARY_REQUEST|FAIL|BAD_ARGUMENTS";
                }

            default:
                return "UNKNOWN|FAIL|INVALID_REQUEST_TYPE";
        }
    }

    // NODE LEAVING
    private String processLeave(String processId) {
        Node process = nodes.get(processId);

        if (process == null) {
            return "LEAVE|FAIL|NO_SUCH_NODE";
        }

        // 1) Release all resources owned/requested by the process
        for (Resource r : resources.values()) {
            if (processId.equals(r.getOwner())) {
                r.setOwner(null);
                r.setStatus(EResourceStatus.FREE);
            }
            if (processId.equals(r.getRequestedBy())) {
                r.setRequestedBy(null);
                r.setStatus(EResourceStatus.FREE);
            }
        }

        // 2) Delete all dependencies related to the process
        graph.removeAllDependencies(processId);

        // 3) Delete the process from the list of nodes
        nodes.remove(processId);

        return "LEAVE|OK|" + processId;
    }

    // Release a resource, if the process is the owner
    private String processRelease(String processId, String resourceId) {
        Node process = nodes.get(processId);
        Resource resource = resources.get(resourceId);

        if (process == null) {
            return "RELEASE|FAIL|NO_SUCH_NODE";
        }
        if (resource == null) {
            return "RELEASE|FAIL|NO_SUCH_RESOURCE";
        }

        // Check if the resource is occupied and owned by the process
        if (resource.getStatus() == EResourceStatus.OCCUPIED
                && processId.equals(resource.getOwner())) {

            resource.setStatus(EResourceStatus.FREE);
            resource.setOwner(null);

            return "RELEASE|OK|" + processId + "|" + resourceId;
        } else {
            return "RELEASE|FAIL|NOT_OWNER";
        }
    }

    // Acquire a resource, if it is in WAITING state for the exact process
    private String processAcquire(String processId, String resourceId) {
        Node process = nodes.get(processId);
        System.out.println("PROCESS-ID: " + processId);
        Resource resource = resources.get(resourceId);
        System.out.println("RESOURCE-ID: " + resourceId + " STATUS: " + resource.getStatus() + " REQUESTED BY: " + resource.getRequestedBy());

        if (process == null) {
            return "ACQUIRE|FAIL|NO_SUCH_NODE";
        }
        if (resource == null) {
            return "ACQUIRE|FAIL|NO_SUCH_RESOURCE";
        }

        // Check if the resource is in WAITING state and requested by the process
        if (resource.getStatus() == EResourceStatus.WAITING
                && processId.equals(resource.getRequestedBy())) {

            // Now the process can acquire the resource
            resource.setStatus(EResourceStatus.OCCUPIED);
            resource.setOwner(processId);
            resource.setRequestedBy(null);

            checkPreliminaryConflicts(processId, resourceId);

            return "ACQUIRE|OK|" + processId + "|" + resourceId;
        } else {
            // Resource is not in WAITING state for the process
            return "ACQUIRE|FAIL|NOT_WAITING_FOR_YOU";
        }
    }

    private void checkPreliminaryConflicts(String ownerProcessId, String resourceId) {
        for (Map.Entry<String, Set<String>> entry : preliminaryRequests.entrySet()) {
            // Check if the other process wanted the resource
            String otherProcessId = entry.getKey();
            // Skip the owner process
            if (otherProcessId.equals(ownerProcessId)) {
                continue;
            }

            Set<String> wantedResources = entry.getValue();
            if (wantedResources.contains(resourceId)) {
                // otherProcessId wanted the resource, but now it must wait
                // because ownerProcessId acquired it and now there is a dependency
                // otherProcessId -> ownerProcessId
                graph.addDependency(otherProcessId, ownerProcessId);

                System.out.println("Added dependency " + otherProcessId
                        + " -> " + ownerProcessId
                        + " because " + ownerProcessId
                        + " acquired " + resourceId
                        + " which " + otherProcessId
                        + " had in preliminaryRequests."
                );
            }
        }
    }


    private String processRequest(String processId, String resourceId) throws IOException {

        Node process = nodes.get(processId);
        Resource resource = resources.get(resourceId);
        System.out.println("RECOURCE-ID AND STATUS: " + resource.getResourceId() + resource.getStatus());

        if (process == null) {
            return "REQUEST|FAIL|NO_SUCH_NODE";
        }
        if (resource == null) {
            return "REQUEST|FAIL|NO_SUCH_RESOURCE";
        }

        //If the resource is free, the process can request it
        if (resource.getStatus() == EResourceStatus.FREE) {
            resource.setStatus(EResourceStatus.WAITING);
            System.out.println("Resource status NEW: " + resource.getStatus());
            resource.setRequestedBy(processId);
            System.out.println("Resource requested by: " + resource.getRequestedBy());

            System.out.println("Resource status: " + resource.getStatus());
            String response = "REQUEST|OK|" + processId + "|" + resourceId;
            rabbitMQService.getChannel().basicPublish("", "updates_queue", null, response.getBytes());
            System.out.println("Sent to updates_queue: " + response);
            return response;
        } else {
            String ownerOrRequester;
            if (resource.getStatus() == EResourceStatus.OCCUPIED) {
                if (processId.equals(resource.getOwner())) {
                    return "REQUEST|FAIL|ALREADY_OWNED";
                }
                ownerOrRequester = resource.getOwner();
            } else { // WAITING
                if (processId.equals(resource.getRequestedBy())) {
                    return "REQUEST|FAIL|ALREADY_REQUESTED";
                }
                ownerOrRequester = resource.getRequestedBy();
            }

            // Add dependency to the graph if the resource is occupied or waiting
            graph.addDependency(processId, ownerOrRequester);

            System.out.println("ALL DEPENDENCIES: " + graph.getDependencies());

            if (graph.hasCycle()) {
                //If there is a cycle, remove the dependency and deny the request
                graph.removeDependency(processId, ownerOrRequester);
                System.out.println("ALL DEPENDENCIES: " + graph.getDependencies());
                return "REQUEST|DENY|CYCLE_DETECTED|" + processId + "|" + resourceId;
            }

            return "REQUEST|DENY|" + processId + "|" + resourceId;
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

            System.out.println("ALL PRELIMINARY REQUESTS: " + getPreliminaryRequests());

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