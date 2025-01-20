package cz.ctu.fee.dsv.semwork.worker;

import cz.ctu.fee.dsv.semwork.worker.Worker;

import java.util.Scanner;

public class MainWorker {

    private static Worker worker;

    public static void main(String[] args) throws Exception {
        String processId = "P1";
        if (args.length > 0) {
            processId = args[0];
        }

        System.out.println("=== Worker " + processId + " starting ===");
        worker = new Worker(processId);

        // TODO: Подключиться к RabbitMQ / HTTP
        // Пример RabbitMQ (псевдокод):
        /*
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // Подписаться на exchange "updates" (fanout), создавая временную очередь:
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, "updates_exchange", "");
        channel.basicConsume(queueName, true, (tag, delivery) -> {
            String msg = new String(delivery.getBody(), StandardCharsets.UTF_8);
            worker.onBroadcast(msg);
        }, tag -> {});
        */

        // Запустить CLI, чтобы руками вводить команды "req R1" / "rel R1" / "exit"
        runCli();
    }

    private static void runCli() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.print("Enter command (req <R>, rel <R>, exit): ");
            String line = sc.nextLine();
            if (line == null) continue;
            line = line.trim();
            if (line.equalsIgnoreCase("exit")) {
                break;
            }
            String[] parts = line.split("\\s+");
            if (parts.length < 2) {
                System.out.println("Usage: req R1  or  rel R1  or  exit");
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
        System.out.println("Worker CLI exited.");
    }
}
