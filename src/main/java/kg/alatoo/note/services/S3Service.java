package kg.alatoo.note.services;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;

import java.io.File;

public class S3Service {

    private final AmazonS3 s3Client;

    public S3Service(String region) {
        this.s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .build();
    }

    public void uploadFile(String bucketName, String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, file);
        s3Client.putObject(putObjectRequest);
    }

    // Other methods to interact with S3...
}