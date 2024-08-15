package com.example;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

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

            try {
                sendEmail(name, email, message);
            } catch (MessagingException e) {
                e.printStackTrace();
            }

            String response = "<html><body><h1>Form Submitted Successfully!</h1></body></html>";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();

        } else {

            exchange.sendResponseHeaders(405, -1);
        }
    }

    private void sendEmail(String name, String email, String messageText) throws MessagingException {

        String host = "smtp.your-email-provider.com"; // Replace with your SMTP host
        String from = "your-email@example.com"; // Your email address
        String password = "your-password"; // Your email password
        String to = "your-email@example.com"; // Your email address to receive the form data

        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject("New Contact Form Submission from " + name);
        message.setText("You have received a new message from " + name + " (" + email + "):\n\n" + messageText);

        Transport.send(message);
    }
}
