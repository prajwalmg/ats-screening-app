package com.ats.resumestorageservice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Runs against a real, throwaway MinIO container rather than mocking the S3
 * client — the bug this project actually hit in production (Railway's
 * bucket rejecting the old public-bucket-policy call) was exactly the kind
 * of thing a mocked client would never have caught.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ResumeStorageServiceApplicationTests {

    @Container
    static MinIOContainer minio = new MinIOContainer("minio/minio:latest");

    @DynamicPropertySource
    static void minioProperties(DynamicPropertyRegistry registry) {
        registry.add("minio.endpoint", minio::getS3URL);
        registry.add("minio.access-key", minio::getUserName);
        registry.add("minio.secret-key", minio::getPassword);
        registry.add("minio.bucket", () -> "resumes-test");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void uploadResume_pdf_returnsPresignedDownloadUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "not a real pdf but bytes are enough".getBytes());

        mockMvc.perform(multipart("/resumes").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resumeUrl").exists())
                .andExpect(jsonPath("$.fileName").value("resume.pdf"));
    }

    @Test
    void uploadResume_wrongType_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart("/resumes").file(file))
                .andExpect(status().isBadRequest());
    }
}
