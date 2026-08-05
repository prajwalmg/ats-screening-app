package com.ats.screeningservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ats.screeningservice.client.JobServiceClient;
import com.ats.screeningservice.dto.JobDto;
import com.ats.screeningservice.dto.ScreenRequest;
import com.ats.screeningservice.dto.ScreenResponse;
import com.ats.screeningservice.exception.InvalidJobReferenceException;

@ExtendWith(MockitoExtension.class)
class ScreeningServiceTest {

    @Mock
    private JobServiceClient jobServiceClient;

    private ScreeningService screeningService;

    @BeforeEach
    void setUp() {
        screeningService = new ScreeningService(jobServiceClient);
        ReflectionTestUtils.setField(screeningService, "advanceThresholdPercent", 70);
    }

    @Test
    void screen_fullSkillMatchAndEnoughExperience_advancesWithScore100() {
        JobDto job = new JobDto(1L, "Backend Engineer", List.of("Java", "Spring Boot"), 2, "OPEN");
        when(jobServiceClient.findJob(1L)).thenReturn(Optional.of(job));

        ScreenResponse response = screeningService.screen(
                new ScreenRequest(1L, List.of("Java", "Spring Boot", "Docker"), 5));

        assertEquals(100, response.score());
        assertTrue(response.hardFilterPassed());
        assertEquals("ADVANCED", response.recommendedStatus());
        assertEquals(List.of(), response.missingSkills());
    }

    @Test
    void screen_belowMinimumExperience_isRejectedRegardlessOfSkillMatch() {
        JobDto job = new JobDto(1L, "Staff Engineer", List.of("Java"), 10, "OPEN");
        when(jobServiceClient.findJob(1L)).thenReturn(Optional.of(job));

        ScreenResponse response = screeningService.screen(new ScreenRequest(1L, List.of("Java"), 2));

        assertFalse(response.hardFilterPassed());
        assertEquals("REJECTED", response.recommendedStatus());
    }

    @Test
    void screen_partialMatchBelowThreshold_isUnderReview() {
        JobDto job = new JobDto(1L, "Frontend Engineer", List.of("React", "TypeScript", "GraphQL"), 1, "OPEN");
        when(jobServiceClient.findJob(1L)).thenReturn(Optional.of(job));

        ScreenResponse response = screeningService.screen(new ScreenRequest(1L, List.of("React"), 3));

        assertTrue(response.hardFilterPassed());
        assertEquals("UNDER_REVIEW", response.recommendedStatus());
        assertEquals(List.of("TypeScript", "GraphQL"), response.missingSkills());
    }

    @Test
    void screen_jobNotFound_throwsInvalidJobReferenceException() {
        when(jobServiceClient.findJob(99L)).thenReturn(Optional.empty());

        assertThrows(InvalidJobReferenceException.class,
                () -> screeningService.screen(new ScreenRequest(99L, List.of(), 0)));
    }
}
