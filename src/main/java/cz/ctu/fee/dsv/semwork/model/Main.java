package cz.ctu.fee.dsv.semwork.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import cz.ctu.fee.dsv.semwork.coordinator.Coordinator;
import cz.ctu.fee.dsv.semwork.config.Config;
import cz.ctu.fee.dsv.semwork.config.NodeConfig;
import cz.ctu.fee.dsv.semwork.config.RabbitConfig;

import java.io.File;
import java.util.Optional;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Error: No arguments provided. Please specify 'coordinator' or a node ID.");
            return;
        }

        // Читаем конфигурацию из config.yml
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Config config = mapper.readValue(new File("config.yml"), Config.class);

        RabbitConfig rabbitConfig = config.getRabbitmq();
        RabbitMQService rabbitMQService = new RabbitMQService(rabbitConfig);
        rabbitMQService.connect();

        if (args[0].equalsIgnoreCase("coordinator")) {
            Coordinator coordinator = new Coordinator(rabbitMQService);
            coordinator.start();
            System.out.println("Coordinator started.");
        } else {
            String nodeId = args[0];

            Optional<NodeConfig> nodeConfigOpt = config.getNodes().stream()
                    .filter(node -> node.getId().equals(nodeId))
                    .findFirst();

            if (nodeConfigOpt.isEmpty()) {
                System.out.println("Error: Node ID '" + nodeId + "' not found in config.yml.");
                return;
            }

            NodeConfig nodeConfig = nodeConfigOpt.get();

            Node node = new Node(nodeConfig.getId(), rabbitMQService, nodeConfig.getIp(), nodeConfig.getPort());
            node.start();
        }
    }
}