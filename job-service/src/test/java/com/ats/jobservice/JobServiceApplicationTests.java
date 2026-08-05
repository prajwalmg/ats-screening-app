package com.ats.jobservice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Runs the real JobController + JobService + JobRepository against a throwaway
 * Postgres container (Flyway migration included) instead of mocks, so it
 * exercises the actual persistence + validation + exception-handling wiring.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class JobServiceApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void createJob_returnsCreatedJobWithGeneratedId() throws Exception {
        String requestBody = """
                {
                  "title": "Backend Engineer",
                  "description": "Java role",
                  "requiredSkills": ["Java", "Spring"],
                  "minYearsExperience": 2
                }
                """;

        mockMvc.perform(post("/jobs").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Backend Engineer"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void createJob_blankTitle_returns400WithValidationDetails() throws Exception {
        String requestBody = """
                {
                  "title": "",
                  "description": "Java role"
                }
                """;

        mockMvc.perform(post("/jobs").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void getJobById_notFound_returnsCleanJson404() throws Exception {
        mockMvc.perform(get("/jobs/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Job not found with id: 999999"));
    }
}
