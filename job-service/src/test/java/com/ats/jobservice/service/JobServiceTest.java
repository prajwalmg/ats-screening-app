package com.ats.jobservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ats.jobservice.dto.CreateJobRequest;
import com.ats.jobservice.entity.Job;
import com.ats.jobservice.entity.JobStatus;
import com.ats.jobservice.repository.JobRepository;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @InjectMocks
    private JobService jobService;

    @Test
    void createJob_setsStatusToOpenAndSaves() {
        CreateJobRequest request = new CreateJobRequest();
        request.setTitle("Backend Engineer");
        request.setDescription("Java role");
        request.setRequiredSkills(List.of("Java", "Spring"));
        request.setMinYearsExperience(2);

        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Job result = jobService.createJob(request);

        assertEquals(JobStatus.OPEN, result.getStatus());
        assertEquals("Backend Engineer", result.getTitle());
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void getJobById_notFound_throwsNoSuchElementException() {
        when(jobRepository.findById(99L)).thenReturn(Optional.empty());

        NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> jobService.getJobById(99L));
        assertTrue(ex.getMessage().contains("99"));
    }

    @Test
    void getJobById_found_returnsJob() {
        Job job = new Job();
        job.setId(1L);
        job.setTitle("Existing Job");
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));

        Job result = jobService.getJobById(1L);

        assertEquals("Existing Job", result.getTitle());
    }
}
