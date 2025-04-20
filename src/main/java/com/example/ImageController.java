package com.example;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

public class ImageController implements HttpHandler {
    private static final Logger logger = Logger.getLogger(ImageController.class.getName());
    private final S3Service s3Service;

    public ImageController() {
        this.s3Service = new S3Service();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        logger.info("Received image request: " + exchange.getRequestMethod() + " " + exchange.getRequestURI());
        
        // Set CORS headers
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        headers.add("Access-Control-Max-Age", "3600");
        
        // Handle preflight requests
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if ("GET".equals(exchange.getRequestMethod())) {
            try {
                // Extract image key from path
                String path = exchange.getRequestURI().getPath();
                String imageName = path.substring("/api/images/".length());
                // Ensure the image name doesn't contain .jpg already
                if (imageName.endsWith(".jpg")) {
                    imageName = imageName.substring(0, imageName.length() - 4);
                }
                // Construct the full S3 key
                String imageKey = "images/" + imageName + ".jpg";
                
                logger.info("Attempting to get presigned URL for key: " + imageKey);
                
                // List available images to debug
                logger.info("Available images in S3:");
                s3Service.listFiles("images/").forEach(file -> logger.info("- " + file));
                
                // Get presigned URL
                String presignedUrl = s3Service.getPresignedUrl(imageKey);
                logger.info("Successfully generated presigned URL");
                
                // Create JSON response with presigned URL
                String response = "{\"url\":\"" + presignedUrl + "\"}";
                
                // Set content type and send response
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                byte[] responseBytes = response.getBytes();
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            } catch (Exception e) {
                logger.severe("Error handling image request: " + e.getMessage());
                String errorResponse = "{\"error\":\"" + e.getMessage() + "\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(500, errorResponse.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(errorResponse.getBytes());
                }
            }
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }
} 