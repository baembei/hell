package cz.ctu.fee.dsv.semwork.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Scanner;
import com.rabbitmq.client.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class MainWorker {

    private static final String REQUESTS_QUEUE = "requests_queue";
    private static final String UPDATES_EXCHANGE = "updates_exchange";

    public static void main(String[] args) {
        String processId = "P1";
        if (args.length > 0) {
            processId = args[0];
        }
        System.out.println("=== Worker " + processId + " starting ===");

        try {
            // 1. Создаем подключение к RabbitMQ
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            // 2. Объявим очередь и exchange (на всякий случай идемпотентно)
            channel.queueDeclare(REQUESTS_QUEUE, false, false, false, null);
            channel.exchangeDeclare(UPDATES_EXCHANGE, BuiltinExchangeType.FANOUT);

            // 3. Создаем временную очередь для приёма broadcast
            String tempQueue = channel.queueDeclare().getQueue();
            channel.queueBind(tempQueue, UPDATES_EXCHANGE, "");

            // 4. Создаем Worker
            ObjectMapper mapper = new ObjectMapper();
            Worker worker = new Worker(processId, channel, REQUESTS_QUEUE, mapper);

            // 5. Подписываемся на фан-аут обменник (updates_exchange)
            DeliverCallback deliverCb = (consumerTag, delivery) -> {
                String broadcastJson = new String(delivery.getBody(), StandardCharsets.UTF_8);
                worker.onBroadcast(broadcastJson);
            };
            CancelCallback cancelCb = consumerTag -> {};
            channel.basicConsume(tempQueue, true, deliverCb, cancelCb);

            // 6. Запускаем CLI-цикл
            runCLI(worker);

        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    private static void runCLI(Worker worker) {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.print("Command (req <R>, rel <R>, exit): ");
            String line = sc.nextLine();
            if (line == null) continue;
            line = line.trim();
            if (line.equalsIgnoreCase("exit")) {
                System.out.println("Exiting CLI...");
                break;
            }
            String[] parts = line.split("\\s+");
            if (parts.length < 2) {
                System.out.println("Usage: req R1 or rel R1");
                continue;
            }
            String cmd = parts[0];
            String resourceId = parts[1];
            if ("req".equalsIgnoreCase(cmd)) {
                worker.requestResource(resourceId);
            } else if ("rel".equalsIgnoreCase(cmd)) {
                worker.releaseResource(resourceId);
            } else {
                System.out.println("Unknown command: " + cmd);
            }
        }
        sc.close();
    }
}
