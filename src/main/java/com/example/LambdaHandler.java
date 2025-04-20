package com.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class LambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final S3Service s3Service;
    private final Projects projects;
    private final Contact contact;

    public LambdaHandler() {
        this.s3Service = new S3Service();
        this.projects = new Projects(s3Service);
        this.contact = new Contact();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type");
        response.setHeaders(headers);

        try {
            String path = input.getPath();
            String httpMethod = input.getHttpMethod();
            context.getLogger().log("Processing request: " + httpMethod + " " + path);

            if ("OPTIONS".equals(httpMethod)) {
                response.setStatusCode(200);
                return response;
            }

            if ("/api/projects".equals(path) && "GET".equals(httpMethod)) {
                String projectsJson = projects.getProjects();
                response.setStatusCode(200);
                response.setBody(projectsJson);
                return response;
            }

            if ("/api/contact".equals(path) && "POST".equals(httpMethod)) {
                JSONObject requestBody = new JSONObject(input.getBody());
                contact.handleContactRequest(requestBody);
                response.setStatusCode(200);
                response.setBody("{\"message\":\"Message sent successfully\"}");
                return response;
            }

            if (path != null && path.startsWith("/api/images/") && "GET".equals(httpMethod)) {
                String imageName = path.substring("/api/images/".length());
                context.getLogger().log("Fetching image: " + imageName);
                byte[] imageData = s3Service.getImage(imageName);
                String base64Image = java.util.Base64.getEncoder().encodeToString(imageData);
                response.setStatusCode(200);
                response.setBody(base64Image);
                headers.put("Content-Type", "image/jpeg");
                return response;
            }

            response.setStatusCode(404);
            response.setBody("{\"error\":\"Not Found\"}");
            return response;

        } catch (Exception e) {
            context.getLogger().log("Error processing request: " + e.getMessage());
            context.getLogger().log("Stack trace: " + java.util.Arrays.toString(e.getStackTrace()));
            response.setStatusCode(500);
            response.setBody("{\"error\":\"Internal Server Error\", \"message\":\"" + e.getMessage() + "\"}");
            return response;
        }
    }
} 