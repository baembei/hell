package cz.ctu.fee.dsv.semwork.model;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import cz.ctu.fee.dsv.semwork.config.RabbitConfig;

public class RabbitMQService {
    private final RabbitConfig rabbitConfig;
    private Connection connection;
    private Channel channel;

    public RabbitMQService(RabbitConfig rabbitConfig) {
        this.rabbitConfig = rabbitConfig;
    }

    public void connect() throws Exception {
        System.out.println("RabbitMQService: Connecting to RabbitMQ...");
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitConfig.getHosts().get(0).getHost());
        factory.setPort(rabbitConfig.getHosts().get(0).getPort());
        factory.setUsername(rabbitConfig.getUsername());
        factory.setPassword(rabbitConfig.getPassword());

        try {
            this.connection = factory.newConnection();
            System.out.println("RabbitMQService: Connection established.");
            this.channel = connection.createChannel();
            System.out.println("RabbitMQService: Channel created.");
        } catch (Exception e) {
            System.err.println("RabbitMQService: Error during RabbitMQ connection: " + e.getMessage());
            throw e;
        }
    }


    public void setupQueue(String queueName) throws Exception {
        channel.queueDeclare(queueName, true, false, false, null);
        System.out.println("Queue " + queueName + " created.");
    }

    public void setupExchange(String exchangeName) throws Exception {
        System.out.println("Setting up exchange: " + exchangeName);
        try {
            channel.exchangeDeclare(exchangeName, "fanout", true);
        } catch (Exception e) {
            System.err.println("Error while declaring exchange: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("Exchange '" + exchangeName + "' created successfully.");
    }

    public Channel getChannel() {
        return channel;
    }

    public void close() throws Exception {
        if (channel != null) channel.close();
        if (connection != null) connection.close();
    }
}

