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
            ctx.result("JOIN DONE.");
        });

        app.post("/leave", ctx -> {
            String rabbitIp = ctx.queryParam("rabbitIp");
            int rabbitPort = Integer.parseInt(ctx.queryParam("rabbitPort"));
            node.leave();
            ctx.result("Node " + node.getNodeId() + " left.");
        });

        app.post("/kill", ctx -> {
            node.kill();
            ctx.result("Node " + node.getNodeId() + " is dead.");
        });

        app.post("/revive", ctx -> {
            node.revive();
            ctx.result("Node " + node.getNodeId() + " is back.");
        });

        app.get("/status", ctx -> {
            ctx.result(node.getStatus());
        });
    }
}