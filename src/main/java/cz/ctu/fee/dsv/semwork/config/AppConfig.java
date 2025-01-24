package cz.ctu.fee.dsv.semwork.config;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class AppConfig {
    private RabbitConfig rabbitmq;
    private CoordinatorConfig coordinator;
    private List<NodeConfig> nodes;
    private List<String> resources;
}

