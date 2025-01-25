package cz.ctu.fee.dsv.semwork.model;

import cz.ctu.fee.dsv.semwork.config.AppConfig;
import cz.ctu.fee.dsv.semwork.config.ConfigLoader;
import cz.ctu.fee.dsv.semwork.coordinator.Coordinator;

public class Main {
    public static void main(String[] args) throws Exception {
        AppConfig config = ConfigLoader.loadConfig();
        RabbitMQService rabbitMQService = new RabbitMQService(config.getRabbitmq());

        rabbitMQService.connect();

        if (args[0].equals("coordinator")) {
            Coordinator coordinator = new Coordinator(rabbitMQService);
            coordinator.start();
        } else {
            String nodeId = args[0];
            Node node = new Node(nodeId, rabbitMQService);
            node.start();
        }
    }
}

