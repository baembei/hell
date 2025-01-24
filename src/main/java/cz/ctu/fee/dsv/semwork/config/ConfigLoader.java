package cz.ctu.fee.dsv.semwork.config;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;

public class ConfigLoader {
    public static AppConfig loadConfig() {
        InputStream inputStream = null;
        try {
            inputStream = ConfigLoader.class.getClassLoader().getResourceAsStream("config.yml");
            if (inputStream == null) {
                throw new RuntimeException("config.yml not found");
            }
            Yaml yaml = new Yaml();
            return yaml.loadAs(inputStream, AppConfig.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config.yml", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    // Log the exception or handle it as needed
                }
            }
        }
    }

    public static void main(String[] args) {
        AppConfig config = loadConfig();
        System.out.println("RabbitMQ Hosts: " + config.getRabbitmq().getHosts());
        System.out.println("Coordinator: " + config.getCoordinator());
        System.out.println("Nodes: " + config.getNodes());
        System.out.println("Resources: " + config.getResources());
    }
}

