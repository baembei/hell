package cz.ctu.fee.dsv.semwork;

import cz.ctu.fee.dsv.semwork.model.Node;
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
    }
}