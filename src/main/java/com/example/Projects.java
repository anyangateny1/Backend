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

            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);

            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }

    private String getProjects() {
        String response = "";
        Connection conn = null;
        Statement stmt = null;
        try {
            String dbUrl = System.getenv("DATABASE_URL");
        
            if (dbUrl == null || dbUrl.isEmpty()) {
                dbUrl = "jdbc:postgresql://localhost:5432/website"; 
            } else {

                dbUrl = dbUrl.replace("postgres://", "jdbc:postgresql://");

            }
        
            Class.forName("org.postgresql.Driver");
    
            conn = DriverManager.getConnection(dbUrl, "anyangateny", "04052004"); 
    
            stmt = conn.createStatement();
            String sql = "SELECT id, project_name, project_date, description, img_url, tags FROM projects";
            ResultSet rs = stmt.executeQuery(sql);
    
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
            rs.close();
            stmt.close();
            conn.close();
    
            response = projectsArray.toString();
        } catch (Exception e) {
            // Log the exception for debugging
            logger.severe("Error retrieving projects: " + e.getMessage());
            response = "{\"error\":\"An error occurred: " + e.getMessage() + "\"}";
        } finally {
            if (stmt != null) try { stmt.close(); } catch (Exception e) { }
            if (conn != null) try { conn.close(); } catch (Exception e) { }
        }
        return response;
    }
    

    
}
