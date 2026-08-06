package com.ats.applicationservice;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.ats.applicationservice.client.AtsScreeningServiceClient;
import com.ats.applicationservice.client.JobServiceClient;
import com.ats.applicationservice.client.ResumeParsingServiceClient;
import com.ats.applicationservice.dto.JobDto;
import com.ats.applicationservice.dto.ParsedResumeDto;
import com.ats.applicationservice.dto.ScreeningResultDto;

/**
 * Exercises the real ApplicationController + ApplicationService + repository
 * against a throwaway Postgres container. The three sibling services
 * (job-service, resume-parsing-service, ats-screening-service) aren't part of
 * this test's scope, so their REST clients are mocked at the bean level —
 * everything else in the request path is real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ApplicationServiceApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobServiceClient jobServiceClient;
    @MockitoBean
    private ResumeParsingServiceClient resumeParsingServiceClient;
    @MockitoBean
    private AtsScreeningServiceClient atsScreeningServiceClient;

    @Test
    void contextLoads() {
    }

    @Test
    void createApplication_happyPath_persistsAndReturnsScreeningResult() throws Exception {
        when(jobServiceClient.findJob(1L))
                .thenReturn(Optional.of(new JobDto(1L, "Backend Engineer", List.of("Java"), 2, "OPEN")));
        when(resumeParsingServiceClient.parse(anyString()))
                .thenReturn(new ParsedResumeDto(List.of("Java"), 5, 100, "resume text excerpt"));
        when(atsScreeningServiceClient.screen(any()))
                .thenReturn(new ScreeningResultDto(100, List.of("Java"), List.of(), true, "ADVANCED", "great match"));

        String body = """
                {"candidateName":"Jane Doe","email":"jane@example.com","jobId":1,"resumeUrl":"http://minio/x.pdf"}
                """;

        mockMvc.perform(post("/applications").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("ADVANCED"))
                .andExpect(jsonPath("$.matchScore").value(100));
    }

    @Test
    void createApplication_invalidJob_returns400() throws Exception {
        when(jobServiceClient.findJob(anyLong())).thenReturn(Optional.empty());

        String body = """
                {"candidateName":"Jane Doe","email":"jane@example.com","jobId":999,"resumeUrl":"http://minio/x.pdf"}
                """;

        mockMvc.perform(post("/applications").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getApplications_supportsStatusFilterAndPagination() throws Exception {
        mockMvc.perform(get("/applications").param("status", "ADVANCED").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
