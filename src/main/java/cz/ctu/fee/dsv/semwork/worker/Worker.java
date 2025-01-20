package cz.ctu.fee.dsv.semwork.worker;

import cz.ctu.fee.dsv.semwork.model.DependencyGraph;

public class Worker {

    private final String processId;

    public Worker(String processId) {
        this.processId = processId;
    }

    /**
     * Послать координатору сообщение "REQUEST|processId|resourceId"
     * (либо через RabbitMQ, либо через HTTP).
     */
    public void requestResource(String resourceId) {
        String msg = "REQUEST|" + processId + "|" + resourceId;
        sendMessage(msg);
    }

    /**
     * Послать "RELEASE|processId|resourceId"
     */
    public void releaseResource(String resourceId) {
        String msg = "RELEASE|" + processId + "|" + resourceId;
        sendMessage(msg);
    }

    /**
     * Этот метод "отправляет" сообщение координатору.
     * Реализуйте в зависимости от транспорта (RabbitMQ/HTTP).
     */
    private void sendMessage(String msg) {
        // TODO: например, channel.basicPublish("", "requests_queue", null, msg.getBytes());
        System.out.println("[Worker " + processId + "] Sending message: " + msg);
    }

    /**
     * Когда приходит BROADCAST из Coordinator’а ("GRANT|P|R", "DENY|P|R", "RELEASED|P|R", etc.)
     */
    public void onBroadcast(String broadcastMsg) {
        // Парсим строку, если нужно
        System.out.println("[Worker " + processId + "] Broadcast received: " + broadcastMsg);

        // Если broadcastMsg начинается с "GRANT|..." и processId == наш,
        // значит ресурс выделили нам -> можем "использовать ресурс".
        // Если "DENY|..." и processId == наш -> отказ. И так далее.
    }
}
