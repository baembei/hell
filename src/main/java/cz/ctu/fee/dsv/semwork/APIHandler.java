package cz.ctu.fee.dsv.semwork;

import cz.ctu.fee.dsv.semwork.model.EResourceStatus;
import cz.ctu.fee.dsv.semwork.model.Node;
import cz.ctu.fee.dsv.semwork.model.Resource;
import io.javalin.Javalin;

public class APIHandler {
    private final Node node;

    public APIHandler(Node node) {
        this.node = node;
    }

    public void start(int port) {
        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
        }).start("0.0.0.0", port);

        app.post("/join", ctx -> {
            System.out.println("Joining node " + node.getNodeId());
            node.join();
            ctx.result("JOIN DONE.");
        });

        app.post("/leave", ctx -> {
            System.out.println("Leaving node " + node.getNodeId());
            node.leave();
            ctx.result("Node " + node.getNodeId() + " left.");
        });

        app.post("/kill", ctx -> {
            System.out.println("Killing node " + node.getNodeId());
            node.kill();
            ctx.result("Node " + node.getNodeId() + " is dead.");
        });

        app.post("/revive", ctx -> {
            System.out.println("Reviving node " + node.getNodeId());
            node.revive();
            ctx.result("Node " + node.getNodeId() + " is back.");
        });

        app.get("/get_status", ctx -> {
            System.out.println("Getting status of this node");
            ctx.result(node.getStatus());
        });

        // Preliminary request:
        // expects two resource IDs in the request (e.g. form params "resource1", "resource2")
        app.post("/preliminary_request", ctx -> {
            String resource1Id = ctx.formParam("resource1");
            String resource2Id = ctx.formParam("resource2");

            if (resource1Id == null || resource2Id == null) {
                ctx.status(400).result("Missing resource1 or resource2 parameter");
                return;
            }

            Resource r1 = new Resource(resource1Id);
            Resource r2 = new Resource(resource2Id);

            try {
                node.sendPreliminaryRequest(r1, r2);
                ctx.result("Preliminary request sent for " + resource1Id + " and " + resource2Id);
            } catch (Exception e) {
                ctx.status(500).result("Error sending preliminary request: " + e.getMessage());
            }
        });

        // Request:
        // expects one resource ID (e.g. form param "resource")
        app.post("/request", ctx -> {
            String resourceId = ctx.formParam("resource");
            if (resourceId == null) {
                ctx.status(400).result("Missing resource parameter");
                return;
            }

            try {
                node.sendRequest(new Resource(resourceId));
                ctx.result("Request sent for resource " + resourceId);
            } catch (Exception e) {
                ctx.status(500).result("Error sending request: " + e.getMessage());
            }
        });

        // Acquire:
        // expects one resource ID (e.g. form param "resource")
        app.post("/acquire", ctx -> {
            String resourceId = ctx.formParam("resource");
            if (resourceId == null) {
                ctx.status(400).result("Missing resource parameter");
                return;
            }

            Resource resource = new Resource(resourceId);
            resource.setRequestedBy(node.getNodeId());
            resource.setStatus(EResourceStatus.WAITING);
            try {
                node.acquireResource(resource);
                ctx.result("Acquire attempt for resource " + resourceId + " completed.");
            } catch (Exception e) {
                ctx.status(500).result("Error acquiring resource: " + e.getMessage());
            }
        });

        // Release:
        // expects one resource ID (e.g. form param "resource")
        app.post("/release", ctx -> {
            String resourceId = ctx.formParam("resource");
            if (resourceId == null) {
                ctx.status(400).result("Missing resource parameter");
                return;
            }

            Resource resource = new Resource(resourceId);
            try {
                node.releaseResource(resource);
                ctx.result("Release attempt for resource " + resourceId + " completed.");
            } catch (Exception e) {
                ctx.status(500).result("Error releasing resource: " + e.getMessage());
            }
        });
    }
}