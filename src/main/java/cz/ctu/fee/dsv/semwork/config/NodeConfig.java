package cz.ctu.fee.dsv.semwork.config;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NodeConfig {
    private String id;
    private String ip;
    private int port;
}