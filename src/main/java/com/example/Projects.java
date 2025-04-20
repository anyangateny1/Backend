package com.example;

import java.util.logging.Logger;

public class Projects {
    private static final Logger logger = Logger.getLogger(Projects.class.getName());
    private static final String FILE_KEY = "projects.json";
    private final S3Service s3Service;

    public Projects(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    public String getProjects() {
        try {
            logger.info("Fetching projects from S3");
            String projects = s3Service.readJsonFile(FILE_KEY);
            logger.info("Successfully fetched projects from S3");
            return projects;
        } catch (Exception e) {
            logger.severe("Error reading projects from S3: " + e.getMessage());
            throw new RuntimeException("Error reading projects from S3", e);
        }
    }
}
