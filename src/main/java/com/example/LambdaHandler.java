package com.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final S3Service s3Service;
    private final Projects projects;
    private final Contact contact;

    private static final Map<String, CacheEntry> urlCache = new ConcurrentHashMap<>();

    private static class CacheEntry {
        private final String value;
        private final long expiryTime;

        public CacheEntry(String value, long expiryTimeSeconds) {
            this.value = value;
            this.expiryTime = System.currentTimeMillis() + (expiryTimeSeconds * 1000);
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    public LambdaHandler() {
        this.s3Service = new S3Service();
        this.projects = new Projects(s3Service);
        this.contact = new Contact();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        
        // Set CORS headers for all responses
        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", "*");  // Allow all origins
        headers.put("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        headers.put("Access-Control-Allow-Headers", "*");
        headers.put("Access-Control-Max-Age", "3600");
        response.setHeaders(headers);

        // Handle preflight requests
        if ("OPTIONS".equals(input.getHttpMethod())) {
            response.setStatusCode(200);
            return response;
        }

        try {
            String path = input.getPath();
            String httpMethod = input.getHttpMethod();
            context.getLogger().log("Processing request: " + httpMethod + " " + path);

            // Add content type for non-OPTIONS requests
            headers.put("Content-Type", "application/json");
            response.setHeaders(headers);

            if ("/api/projects".equals(path) && "GET".equals(httpMethod)) {
                try {
                    String projectsJson = projects.getProjects();
                    response.setStatusCode(200);
                    response.setBody(projectsJson);
                    return response;
                } catch (Exception e) {
                    context.getLogger().log("Error getting projects: " + e.getMessage());
                    throw e;
                }
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
                context.getLogger().log("Fetching presigned URL for image: " + imageName);
                
                // Check cache first
                String cacheKey = "image_url_" + imageName;
                String cachedUrl = getCachedValue(cacheKey);
                if (cachedUrl != null) {
                    JSONObject jsonResponse = new JSONObject();
                    jsonResponse.put("url", cachedUrl);
                    response.setStatusCode(200);
                    response.setBody(jsonResponse.toString());
                    return response;
                }
                
                String s3Key = s3Service.convertS3UriToKey(imageName);
                String presignedUrl = s3Service.getPresignedUrl(s3Key);
                
                // Cache the URL
                cacheValue(cacheKey, presignedUrl, 3300); // Cache for 55 minutes (presigned URL valid for 60)
                
                JSONObject jsonResponse = new JSONObject();
                jsonResponse.put("url", presignedUrl);
                response.setStatusCode(200);
                response.setBody(jsonResponse.toString());
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

    private String getCachedValue(String key) {
        CacheEntry entry = urlCache.get(key);
        if (entry != null && !entry.isExpired()) {
            return entry.value;
        }
        if (entry != null) {
            urlCache.remove(key);
        }
        return null;
    }

    private void cacheValue(String key, String value, long expiryTimeSeconds) {
        urlCache.put(key, new CacheEntry(value, expiryTimeSeconds));
    }
} 