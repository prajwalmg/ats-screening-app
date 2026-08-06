package com.ats.resumestorageservice.service;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ats.resumestorageservice.dto.UploadResult;
import com.ats.resumestorageservice.exception.ResumeStorageException;

import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResumeStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".docx");
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;
    // 7 days is the maximum a SigV4 presigned URL can be valid for. Comfortably
    // covers the actual usage pattern (resume-parsing-service downloads the
    // file within seconds of upload, as part of the same application
    // submission) — nothing in this system needs the URL to stay valid longer.
    private static final int PRESIGNED_URL_EXPIRY_SECONDS = (int) TimeUnit.DAYS.toSeconds(7);

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    private static final int BUCKET_INIT_MAX_ATTEMPTS = 10;
    private static final long BUCKET_INIT_RETRY_DELAY_MS = 2000;

    /**
     * Retries because in docker-compose MinIO's container can still be
     * accepting TCP connections but not yet ready to serve API calls when
     * this service starts — rather than depend on getting container
     * healthcheck ordering exactly right, this just waits it out. Bucket
     * creation is a no-op against Railway's managed bucket storage (it
     * already exists), but harmless to check either way.
     */
    @PostConstruct
    void ensureBucket() throws InterruptedException {
        for (int attempt = 1; attempt <= BUCKET_INIT_MAX_ATTEMPTS; attempt++) {
            try {
                boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
                if (!exists) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                }
                return;
            } catch (Exception e) {
                if (attempt == BUCKET_INIT_MAX_ATTEMPTS) {
                    throw new ResumeStorageException("Failed to initialize the resume bucket after "
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
        String url = presignedDownloadUrl(objectKey);
        return new UploadResult(url, file.getOriginalFilename(), file.getSize());
    }

    /**
     * The bucket is private by default (not every S3-compatible provider
     * supports the AWS bucket-policy API used to make one publicly
     * readable — Railway's managed bucket storage doesn't), so callers get
     * a time-limited signed URL instead of a plain public one.
     */
    private String presignedDownloadUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectKey)
                    .expiry(PRESIGNED_URL_EXPIRY_SECONDS)
                    .build());
        } catch (Exception e) {
            throw new ResumeStorageException("Failed to generate a download URL for the resume: " + e.getMessage());
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Resume file is required");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("Resume file exceeds the 5MB size limit");
        }
        String contentType = file.getContentType();
        boolean typeOk = contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType);
        boolean extensionOk = ALLOWED_EXTENSIONS.contains(extractExtension(file.getOriginalFilename()).toLowerCase());
        if (!typeOk && !extensionOk) {
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

}
