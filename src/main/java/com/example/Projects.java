package com.example;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Logger;

public class Projects implements HttpHandler {
    private static final Logger logger = Logger.getLogger(Projects.class.getName());

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            String response = getProjects();
            Headers headers = exchange.getResponseHeaders();

            // Set response headers for CORS and content type
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.getResponseHeaders().set("Content-Type", "application/json");

            // Send response
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        } else {
            exchange.sendResponseHeaders(405, -1); // Method Not Allowed
        }
    }

    private String getProjects() {
        String response = "";
        try (Connection conn = createConnection();
             Statement stmt = conn.createStatement()) {

            String sql = "SELECT id, project_name, project_date, description, img_url, tags FROM projects";
            try (ResultSet rs = stmt.executeQuery(sql)) {
                JSONArray projectsArray = new JSONArray();
                while (rs.next()) {
                    JSONObject project = new JSONObject();
                    project.put("id", rs.getInt("id"));
                    project.put("project_name", rs.getString("project_name"));
                    project.put("project_date", rs.getString("project_date"));
                    project.put("description", rs.getString("description"));
                    project.put("img_url", rs.getString("img_url"));
                    project.put("tags", rs.getString("tags"));
                    projectsArray.put(project);
                }
                response = projectsArray.toString();
            }
        } catch (Exception e) {
            logger.severe("Error retrieving projects: " + e.getMessage());
            response = "{\"error\":\"An error occurred\"}";
        }
        return response;
    }

    private Connection createConnection() throws Exception {
        String dbUrl = System.getenv("DATABASE_URL");
        if (dbUrl == null) {
            dbUrl = "jdbc:sqlite:src/main/resources/website.db"; // Local development path
        }

        if (dbUrl.startsWith("jdbc:postgresql:")) {
            // For PostgreSQL
            Class.forName("org.postgresql.Driver");
        } else {
            // For SQLite
            Class.forName("org.sqlite.JDBC");
        }

        return DriverManager.getConnection(dbUrl);
    }
}
