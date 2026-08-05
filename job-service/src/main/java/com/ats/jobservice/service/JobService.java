package com.ats.jobservice.service;

import org.springframework.stereotype.Service;
import java.util.*;
import com.ats.jobservice.dto.CreateJobRequest;
import com.ats.jobservice.entity.Job;
import com.ats.jobservice.entity.JobStatus;
import com.ats.jobservice.repository.JobRepository;

import lombok.*;

@Service
@RequiredArgsConstructor
public class JobService {
    private final JobRepository jobRepository;

    public Job createJob(CreateJobRequest createJobRequest) {
        Job job = new Job();
        job.setTitle(createJobRequest.getTitle());
        job.setDescription(createJobRequest.getDescription());
        job.setDepartment(createJobRequest.getDepartment());
        job.setRequiredSkills(createJobRequest.getRequiredSkills());
        job.setMinYearsExperience(createJobRequest.getMinYearsExperience());
        job.setStatus(JobStatus.OPEN); // Set default status to OPEN

        return jobRepository.save(job);
    }

    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    public Job getJobById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Job not found with id: " + id));
    }
}
