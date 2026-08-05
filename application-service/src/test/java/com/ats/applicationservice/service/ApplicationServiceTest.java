package com.ats.applicationservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ats.applicationservice.client.AtsScreeningServiceClient;
import com.ats.applicationservice.client.JobServiceClient;
import com.ats.applicationservice.client.ResumeParsingServiceClient;
import com.ats.applicationservice.dto.CreateApplicationRequest;
import com.ats.applicationservice.dto.JobDto;
import com.ats.applicationservice.dto.ParsedResumeDto;
import com.ats.applicationservice.dto.ScreeningResultDto;
import com.ats.applicationservice.entity.Application;
import com.ats.applicationservice.entity.ApplicationStatus;
import com.ats.applicationservice.exception.InvalidJobReferenceException;
import com.ats.applicationservice.exception.UpstreamServiceException;
import com.ats.applicationservice.repository.ApplicationRepository;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private JobServiceClient jobServiceClient;
    @Mock
    private ResumeParsingServiceClient resumeParsingServiceClient;
    @Mock
    private AtsScreeningServiceClient atsScreeningServiceClient;

    @InjectMocks
    private ApplicationService applicationService;

    private CreateApplicationRequest request;

    @BeforeEach
    void setUp() {
        request = new CreateApplicationRequest();
        request.setCandidateName("Jane Doe");
        request.setEmail("jane@example.com");
        request.setJobId(1L);
        request.setResumeUrl("http://minio/resumes/x.pdf");
    }

    @Test
    void submitApplication_jobNotFound_throwsInvalidJobReferenceException() {
        when(jobServiceClient.findJob(1L)).thenReturn(Optional.empty());

        assertThrows(InvalidJobReferenceException.class, () -> applicationService.submitApplication(request));
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void submitApplication_highScore_advancesAndStoresScreeningResult() {
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));
        JobDto job = new JobDto(1L, "Backend Engineer", List.of("Java"), 2, "OPEN");
        when(jobServiceClient.findJob(1L)).thenReturn(Optional.of(job));
        when(resumeParsingServiceClient.parse(anyString()))
                .thenReturn(new ParsedResumeDto(List.of("Java"), 5, 100));
        when(atsScreeningServiceClient.screen(any()))
                .thenReturn(new ScreeningResultDto(100, List.of("Java"), List.of(), true, "ADVANCED", "great match"));

        Application result = applicationService.submitApplication(request);

        assertEquals(ApplicationStatus.ADVANCED, result.getStatus());
        assertEquals(100, result.getMatchScore());
        assertEquals("Backend Engineer", result.getJobTitle());
    }

    @Test
    void submitApplication_screeningPipelineFails_fallsBackToUnderReviewWithoutLosingSubmission() {
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));
        JobDto job = new JobDto(1L, "Backend Engineer", List.of("Java"), 2, "OPEN");
        when(jobServiceClient.findJob(1L)).thenReturn(Optional.of(job));
        when(resumeParsingServiceClient.parse(anyString()))
                .thenThrow(new UpstreamServiceException("resume-parsing-service", "timeout"));

        Application result = applicationService.submitApplication(request);

        assertEquals(ApplicationStatus.UNDER_REVIEW, result.getStatus());
        assertNotNull(result.getScreeningNotes());
        verify(applicationRepository, org.mockito.Mockito.times(2)).save(any(Application.class));
    }
}
