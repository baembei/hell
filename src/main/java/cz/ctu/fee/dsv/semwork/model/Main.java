package cz.ctu.fee.dsv.semwork.model;

import cz.ctu.fee.dsv.semwork.config.AppConfig;
import cz.ctu.fee.dsv.semwork.config.ConfigLoader;
import cz.ctu.fee.dsv.semwork.coordinator.Coordinator;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Error: No arguments provided. Please specify 'coordinator' or a node ID.");
            return; // Завершает выполнение программы
        }

        AppConfig config = ConfigLoader.loadConfig();
        RabbitMQService rabbitMQService = new RabbitMQService(config.getRabbitmq());

        System.out.println("Connecting to RabbitMQ...");
        rabbitMQService.connect();
        rabbitMQService.setupQueue("updates_queue");
        rabbitMQService.setupExchange("updates_exchange");

        if (args[0].equals("coordinator")) {
            Coordinator coordinator = new Coordinator(rabbitMQService);
            coordinator.start();
            System.out.println("Coordinator started.");
        } else {
            String nodeId = args[0];
            Node node = new Node(nodeId, rabbitMQService);
            node.start();
        }
    }
}

