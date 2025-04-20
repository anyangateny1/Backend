package com.example;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

public class AWSConfig {
    private static final String REGION = "ap-southeast-2";
    private static final String BUCKET_NAME = "anyang-personal-website";
    
    private static S3Client s3Client;
    private static AwsCredentialsProvider credentialsProvider;
    
    public static S3Client getS3Client() {
        if (s3Client == null) {
            s3Client = S3Client.builder()
                    .region(Region.of(REGION))
                    .credentialsProvider(getCredentialsProvider())
                    .build();
        }
        return s3Client;
    }
    
    public static AwsCredentialsProvider getCredentialsProvider() {
        if (credentialsProvider == null) {
            credentialsProvider = DefaultCredentialsProvider.create();
        }
        return credentialsProvider;
    }
    
    public static String getBucketName() {
        return BUCKET_NAME;
    }
    
    public static Region getRegion() {
        return Region.of(REGION);
    }
} 