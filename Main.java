
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class Main {

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(
           new InetSocketAddress("0.0.0.0", Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"))), 0
        );

        server.createContext("/", Main::handleRequest);

        server.start();

        System.out.println("================================");
        System.out.println("       TRUTHLENS AI");
        System.out.println("================================");
        System.out.println("Server running at:");
        System.out.println("http://localhost:8080");
    }

    private static void handleRequest(HttpExchange exchange)
            throws IOException {

        String path = exchange.getRequestURI().getPath();

        if (path.equals("/analyze")
                && exchange.getRequestMethod().equals("POST")) {

            handleAnalysis(exchange);
            return;
        }

        if (exchange.getRequestMethod().equals("GET")) {
            serveWebsite(exchange, path);
            return;
        }

        sendResponse(exchange, 405, "Method Not Allowed",
                "text/plain; charset=UTF-8");
    }

    // Serve website files
    private static void serveWebsite(
            HttpExchange exchange, String path) throws IOException {

        if (path.equals("/")) {
            path = "/index.html";
        }

        String fileName = path.substring(1);

        if (fileName.contains("..") || fileName.contains("/")) {
            sendResponse(exchange, 403, "Access denied",
                    "text/plain; charset=UTF-8");
            return;
        }

        Path file = Paths.get(fileName);

        if (!Files.isRegularFile(file)) {
            sendResponse(exchange, 404, "File not found",
                    "text/plain; charset=UTF-8");
            return;
        }

        String type = "text/plain; charset=UTF-8";

        if (fileName.endsWith(".html")) {
            type = "text/html; charset=UTF-8";
        } else if (fileName.endsWith(".css")) {
            type = "text/css; charset=UTF-8";
        } else if (fileName.endsWith(".js")) {
            type = "application/javascript; charset=UTF-8";
        }

        byte[] content = Files.readAllBytes(file);

        exchange.getResponseHeaders().set(
            "Content-Type", type
        );

        exchange.sendResponseHeaders(200, content.length);

        try (OutputStream out = exchange.getResponseBody()) {
            out.write(content);
        }
    }

    // Analyze submitted message
    private static void handleAnalysis(
            HttpExchange exchange) throws IOException {

        String body = new String(
            exchange.getRequestBody().readAllBytes(),
            StandardCharsets.UTF_8
        );

        Map<String, String> parameters =
                new HashMap<>();

        for (String pair : body.split("&")) {

            String[] parts = pair.split("=", 2);

            if (parts.length == 2) {
                parameters.put(
                    URLDecoder.decode(parts[0], "UTF-8"),
                    URLDecoder.decode(parts[1], "UTF-8")
                );
            }
        }

        String message = parameters.getOrDefault("message", "");

        if (message.trim().length() < 10) {
            sendResponse(exchange, 400,
                    "{\"error\":\"Message is too short\"}",
                    "application/json; charset=UTF-8");
            return;
        }

        Map<String, Object> analysis = analyzeMessage(message);

        String json = createJson(analysis);

        sendResponse(exchange, 200, json,
                "application/json; charset=UTF-8");
    }

    // Simulated message analysis engine
    
private static Map<String, Object> analyzeMessage(
        String message) {

    String text = message.toLowerCase(Locale.ROOT);

    int score = 5;

    List<String> reasons = new ArrayList<>();

    // Account-related patterns
    if (text.contains("account profile update")) {
        score += 12;
        reasons.add("Unexpected account profile update.");
    }

    if (text.contains("pending confirmation")) {
        score += 12;
        reasons.add("Requests confirmation of unexplained activity.");
    }

    // Time pressure
    if (text.contains("within 24 hours")
            || text.contains("limited time")
            || text.contains("immediately")) {

        score += 18;
        reasons.add("Creates time pressure to encourage quick action.");
    }

    // Threat of service interruption
    if (text.contains("uninterrupted access")
            || text.contains("account will be blocked")
            || text.contains("services will be suspended")) {

        score += 15;
        reasons.add("Suggests possible service interruption.");
    }

    // Verification requests
    if (text.contains("verify your account")
            || text.contains("account verification")) {

        score += 15;
        reasons.add("Contains an unexpected verification request.");
    }

    // Payment requests
    if (text.contains("processing fee")
            || text.contains("advance payment")
            || text.contains("pay a fee")) {

        score += 20;
        reasons.add("Contains an unexpected payment request.");
    }

    // Sensitive information
    if (text.contains("otp")
            || text.contains("one-time password")
            || text.contains("bank details")
            || text.contains("card details")
            || text.contains("password")) {

        score += 25;
        reasons.add("Refers to sensitive authentication or financial information.");
    }

    // Subtle account-related patterns
if (text.contains("change in your registered contact details")
        && text.contains("customer support portal")
        && text.contains("review your account information")) {

    score += 35;

    reasons.add(
        "Combines an unexpected account-change notice "
        + "with a request to use a support portal."
    );
}
    // Combination of suspicious patterns
    if (text.contains("account profile update")
            && text.contains("within 24 hours")
            && text.contains("uninterrupted access")) {

        score += 15;
        reasons.add("Combines account activity, urgency and access pressure.");
    }

    score = Math.min(score, 98);

    String category;
    String recommendation;

    if (score >= 70) {
        category = "Potential Scam";
        recommendation =
            "Do not share sensitive information or make payments. "
            + "Verify the sender through official channels.";

    } else if (score >= 40) {
        category = "Suspicious Message";
        recommendation =
            "Proceed carefully. Verify the sender and information "
            + "independently before taking action.";

    } else {
        category = "Likely Genuine";
        recommendation =
            "No major suspicious patterns were identified. "
            + "Verify important messages independently.";
    }

    if (reasons.isEmpty()) {
        reasons.add("No major suspicious patterns were detected.");
    }

    Map<String, Object> result = new LinkedHashMap<>();

    result.put("category", category);
    result.put("riskScore", score);
    result.put("reasons", reasons);
    result.put("recommendation", recommendation);

    return result;
}


    // Convert analysis into JSON
    private static String createJson(Map<String, Object> data) {

        StringBuilder json = new StringBuilder();

        json.append("{");

        json.append("\"category\":\"")
            .append(escapeJson((String) data.get("category")))
            .append("\",");

        json.append("\"riskScore\":")
            .append(data.get("riskScore"))
            .append(",");

        json.append("\"reasons\":[");

        List<String> reasons =
                (List<String>) data.get("reasons");

        for (int i = 0; i < reasons.size(); i++) {

            if (i > 0) {
                json.append(",");
            }

            json.append("\"")
                .append(escapeJson(reasons.get(i)))
                .append("\"");
        }

        json.append("],");

        json.append("\"recommendation\":\"")
            .append(escapeJson(
                (String) data.get("recommendation")
            ))
            .append("\"");

        json.append("}");

        return json.toString();
    }

    private static String escapeJson(String value) {

        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    // Send HTTP response
    private static void sendResponse(
            HttpExchange exchange,
            int status,
            String response,
            String contentType) throws IOException {

        byte[] bytes = response.getBytes(
            StandardCharsets.UTF_8
        );

        exchange.getResponseHeaders().set(
            "Content-Type", contentType
        );

        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
