package com.example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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

            String name = "default";
            String email = "deafult";
            String message = "default";

            InputStream inputStream = exchange.getRequestBody();
            String requestBody = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("Raw request body: " + requestBody);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(requestBody);

            name = jsonNode.path("name").asText(name);
            email = jsonNode.path("email").asText(email);
            message = jsonNode.path("message").asText(message);

            String host = System.getenv("SMTP_HOST");
            String port = System.getenv("SMTP_PORT");
            String username = System.getenv("SMTP_USER");
            String password = System.getenv("SMTP_PASSWORD");
            String to = System.getenv("EMAIL_TO");

            Properties properties = System.getProperties();
            properties.setProperty("mail.smtp.host", host);
            properties.setProperty("mail.smtp.port", port);
            properties.setProperty("mail.smtp.auth", "true");
            properties.setProperty("mail.smtp.starttls.enable", "true");

            Session session = Session.getDefaultInstance(properties, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });


            try {

                MimeMessage mimeMessage = new MimeMessage(session);

                mimeMessage.setFrom(new InternetAddress(username));

                mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

                mimeMessage.setSubject("atenyanyang.com");

                mimeMessage.setText("New email.\n\n" +
                                    "Name: " + name + "\n" +
                                    "Email: " + email + "\n" +
                                    "Message: " + message);

                Transport.send(mimeMessage);
                
                String response = "<html><body><h1>Form Submitted Successfully!</h1></body></html>";
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();

            } catch (MessagingException mex) {
                mex.printStackTrace();

                String response = "<html><body><h1>There was an error submitting the form.</h1></body></html>";
                exchange.sendResponseHeaders(500, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }

        } else {

            exchange.sendResponseHeaders(405, -1);

        }
    }
}
