package com.example;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;


public class Contact implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if ("POST".equals(exchange.getRequestMethod())) {

            String name = "Unknown";
            String email = "Unknown";
            String message = "No message";

            String requestBody = new String(exchange.getRequestBody().readAllBytes());
            String[] pairs = requestBody.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length > 1) {
                    switch (keyValue[0]) {
                        case "name":
                            name = keyValue[1];
                            break;
                        case "email":
                            email = keyValue[1];
                            break;
                        case "message":
                            message = keyValue[1];
                            break;
                    }
                }
            }

            System.out.println("Name: " + name);
            System.out.println("Email: " + email);
            System.out.println("Message: " + message);

            String response = "<html><body><h1>Form Submitted Successfully!</h1></body></html>";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();

        } else {

            exchange.sendResponseHeaders(405, -1);
        }
    }

}
