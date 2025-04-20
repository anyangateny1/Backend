package com.example;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.logging.Logger;

public class S3Service {
    private static final Logger logger = Logger.getLogger(S3Service.class.getName());
    private final S3Client s3Client;
    private final String bucketName;
    private final S3Presigner presigner;

    public S3Service() {
        this.s3Client = AWSConfig.getS3Client();
        this.bucketName = AWSConfig.getBucketName();
        this.presigner = S3Presigner.builder()
                .region(AWSConfig.getRegion())
                .credentialsProvider(AWSConfig.getCredentialsProvider())
                .build();
        logger.info("S3Service initialized with bucket: " + bucketName);
    }

    public String getPresignedUrl(String key) {
        logger.info("Generating presigned URL for key: " + key);
        try {
            // Verify the object exists first
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            s3Client.headObject(headRequest);

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(60))
                    .getObjectRequest(GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build())
                    .build();

            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
            String url = presignedRequest.url().toString();
            logger.info("Generated presigned URL for key: " + key);
            return url;
        } catch (NoSuchKeyException e) {
            logger.severe("Object not found in S3: " + key);
            throw new RuntimeException("Image not found: " + key);
        } catch (Exception e) {
            logger.severe("Error generating presigned URL for key " + key + ": " + e.getMessage());
            throw new RuntimeException("Failed to generate presigned URL: " + e.getMessage());
        }
    }

    private String convertS3UriToKey(String s3Uri) {
        logger.info("Converting URI to key: " + s3Uri);
        
        if (s3Uri == null) {
            return null;
        }

        // If it's already a relative path starting with /api/images, convert it
        if (s3Uri.startsWith("/api/images/")) {
            String imageName = s3Uri.substring("/api/images/".length());
            // Remove any file extension if present
            if (imageName.contains(".")) {
                imageName = imageName.substring(0, imageName.lastIndexOf('.'));
            }
            String key = "images/" + imageName;
            logger.info("Converted to key: " + key);
            return key;
        }

        // If it's an S3 URI
        if (s3Uri.startsWith("s3://")) {
            String[] parts = s3Uri.substring(5).split("/", 2);
            if (parts.length > 1) {
                String key = parts[1].toLowerCase();
                // If it's already an images path, use it as is
                if (key.startsWith("images/")) {
                    return key;
                }
                // Otherwise, ensure it's in the images directory
                return "images/" + key.substring(key.lastIndexOf('/') + 1);
            }
        }

        // If it's already a proper key format
        if (s3Uri.startsWith("images/")) {
            return s3Uri;
        }

        // Default case: assume it's a bare image name
        return "images/" + s3Uri;
    }

    public String readJsonFile(String key) {
        logger.info("Reading JSON file from S3: " + key);
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    s3Client.getObject(getObjectRequest), StandardCharsets.UTF_8));

            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }

            // Parse JSON and add presigned URLs
            JSONArray jsonArray = new JSONArray(jsonContent.toString());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject project = jsonArray.getJSONObject(i);
                String imgUrl = project.getString("imgUrl");
                String s3Key = convertS3UriToKey(imgUrl);
                if (s3Key != null) {
                    project.put("imgUrl", getPresignedUrl(s3Key));
                }
            }

            logger.info("Successfully processed JSON file from S3");
            return jsonArray.toString();
        } catch (Exception e) {
            logger.severe("Failed to read file from S3: " + e.getMessage());
            throw new RuntimeException("Failed to read file from S3: " + e.getMessage());
        }
    }

    public void uploadFile(String key, InputStream fileInputStream, String contentType) {
        logger.info("Uploading file to S3: " + key);
        try {
            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(objectRequest, RequestBody.fromInputStream(fileInputStream, fileInputStream.available()));
            logger.info("Successfully uploaded file to S3: " + key);
        } catch (Exception e) {
            logger.severe("Failed to upload file to S3: " + e.getMessage());
            throw new RuntimeException("Failed to upload file to S3: " + e.getMessage());
        }
    }

    public List<String> listFiles(String prefix) {
        logger.info("Listing files in S3 with prefix: " + prefix);
        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build();

            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);
            List<String> files = listResponse.contents().stream()
                    .map(S3Object::key)
                    .collect(Collectors.toList());
            logger.info("Found " + files.size() + " files");
            return files;
        } catch (Exception e) {
            logger.severe("Failed to list files from S3: " + e.getMessage());
            throw new RuntimeException("Failed to list files from S3: " + e.getMessage());
        }
    }

    public void deleteFile(String key) {
        logger.info("Deleting file from S3: " + key);
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            logger.info("Successfully deleted file from S3: " + key);
        } catch (Exception e) {
            logger.severe("Failed to delete file from S3: " + e.getMessage());
            throw new RuntimeException("Failed to delete file from S3: " + e.getMessage());
        }
    }

    public byte[] getImage(String imageName) {
        logger.info("Getting image from S3: " + imageName);
        try {
            String key = convertS3UriToKey(imageName);
            logger.info("Converted key: " + key);
            
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            logger.info("Created GetObjectRequest for bucket: " + bucketName + ", key: " + key);

            try {
                ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
                logger.info("Successfully retrieved image from S3: " + key);
                return objectBytes.asByteArray();
            } catch (S3Exception e) {
                logger.severe("S3 Exception: " + e.getMessage() + ", Error Code: " + e.awsErrorDetails().errorCode());
                throw e;
            }
        } catch (Exception e) {
            logger.severe("Failed to get image from S3: " + e.getMessage());
            if (e.getCause() != null) {
                logger.severe("Caused by: " + e.getCause().getMessage());
            }
            throw new RuntimeException("Failed to get image from S3: " + e.getMessage(), e);
        }
    }
} 