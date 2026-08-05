package com.ats.resumestorageservice.service;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ats.resumestorageservice.dto.UploadResult;
import com.ats.resumestorageservice.exception.ResumeStorageException;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResumeStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    private final MinioClient minioClient;

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.bucket}")
    private String bucket;

    private static final int BUCKET_INIT_MAX_ATTEMPTS = 10;
    private static final long BUCKET_INIT_RETRY_DELAY_MS = 2000;

    /**
     * Retries because in docker-compose MinIO's container can still be
     * accepting TCP connections but not yet ready to serve API calls when
     * this service starts — rather than depend on getting container
     * healthcheck ordering exactly right, this just waits it out.
     */
    @PostConstruct
    void ensureBucket() throws InterruptedException {
        for (int attempt = 1; attempt <= BUCKET_INIT_MAX_ATTEMPTS; attempt++) {
            try {
                boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
                if (!exists) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                }
                minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                        .bucket(bucket)
                        .config(publicReadPolicy(bucket))
                        .build());
                return;
            } catch (Exception e) {
                if (attempt == BUCKET_INIT_MAX_ATTEMPTS) {
                    throw new ResumeStorageException("Failed to initialize MinIO bucket after "
                            + BUCKET_INIT_MAX_ATTEMPTS + " attempts: " + e.getMessage());
                }
                Thread.sleep(BUCKET_INIT_RETRY_DELAY_MS);
            }
        }
    }

    public UploadResult store(MultipartFile file) {
        validate(file);
        String objectKey = UUID.randomUUID() + extractExtension(file.getOriginalFilename());
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception e) {
            throw new ResumeStorageException("Failed to store resume: " + e.getMessage());
        }
        String url = endpoint + "/" + bucket + "/" + objectKey;
        return new UploadResult(url, file.getOriginalFilename(), file.getSize());
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Resume file is required");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("Resume file exceeds the 5MB size limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only PDF and DOCX resumes are accepted");
        }
    }

    private String extractExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }

    private String publicReadPolicy(String bucketName) {
        return """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": "*",
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(bucketName);
    }
}
