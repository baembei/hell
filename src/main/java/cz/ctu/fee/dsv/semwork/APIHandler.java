package cz.ctu.fee.dsv.semwork;

import cz.ctu.fee.dsv.semwork.model.Node;
import io.javalin.Javalin;

public class APIHandler {
    private final Node node;

    public APIHandler(Node node) {
        this.node = node;
    }

    public void start(int port) {
        Javalin app = Javalin.create().start(port);

        app.post("/join", ctx -> {
            String rabbitIp = ctx.queryParam("rabbitIp");
            int rabbitPort = Integer.parseInt(ctx.queryParam("rabbitPort"));

            System.out.println(node.getNodeId() + "is joining: " + rabbitIp + ":" + rabbitPort);
            node.join(rabbitIp, rabbitPort);
            ctx.result("Node joined the topology.");
        });

        app.post("/leave", ctx -> {
            String rabbitIp = ctx.queryParam("rabbitIp");
            int rabbitPort = Integer.parseInt(ctx.queryParam("rabbitPort"));

            System.out.println(node.getNodeId() + " is leaving: " + rabbitIp + ":" + rabbitPort);
            node.leave(rabbitIp, rabbitPort);
            ctx.result("Node left the topology.");
        });

        app.post("/kill", ctx -> {
            node.kill();
            ctx.result("Node has been killed.");
        });

        app.post("/revive", ctx -> {
            node.revive();
            ctx.result("Node revived.");
        });

        app.get("/status", ctx -> {
            ctx.result(node.getStatus());
        });
    }
}