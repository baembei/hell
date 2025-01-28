package cz.ctu.fee.dsv.semwork.model;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Base64;

public class RabbitMQStatus {
    private static final String RABBITMQ_API_URL = "http://localhost:15672/api/overview";
    private static final String RABBITMQ_VHOSTS_URL = "http://localhost:15672/api/vhosts";
    private static final String USERNAME = "guest";
    private static final String PASSWORD = "guest";

    public static void main(String[] args) {
        try {
            // Get connection count from /api/overview
            JSONObject overviewJson = getRabbitMQJson(RABBITMQ_API_URL);
            int connectionCount = overviewJson.getJSONObject("object_totals").getInt("connections");

            // Get vhost count from /api/vhosts
            JSONArray vhostsJson = getRabbitMQJsonArray(RABBITMQ_VHOSTS_URL);
            int vhostCount = vhostsJson.length(); // Количество виртуальных хостов

            System.out.println("🔹 Connection count: " + connectionCount);
            System.out.println("🔹 Virtual host count: " + vhostCount);

        } catch (Exception e) {
            System.err.println("Ошибка при получении данных: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Method to get JSON object from API (e.g., overview)
    private static JSONObject getRabbitMQJson(String apiUrl) throws Exception {
        String response = sendHttpRequest(apiUrl);
        return new JSONObject(response);
    }

    // Method to get JSON array from API (e.g., vhosts)
    private static JSONArray getRabbitMQJsonArray(String apiUrl) throws Exception {
        String response = sendHttpRequest(apiUrl);
        return new JSONArray(response);
    }

    // Method to send HTTP GET request
    private static String sendHttpRequest(String apiUrl) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        // Adding basic auth
        String auth = USERNAME + ":" + PASSWORD;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        connection.setRequestProperty("Authorization", "Basic " + encodedAuth);

        // Reading the response
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        return response.toString();
    }
}