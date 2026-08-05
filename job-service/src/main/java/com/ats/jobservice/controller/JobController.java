package com.ats.jobservice.controller;

import java.util.*;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import com.ats.jobservice.dto.CreateJobRequest;
import com.ats.jobservice.entity.Job;
import com.ats.jobservice.service.*;

import lombok.*;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class JobController {
    private final JobService jobService;
    
    @PostMapping
    public ResponseEntity<Job> createJob(@Vaild @RequestBody CreateJobRequest request) {
        Job created = jobService.createJob(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<Job>> getAllJobs() {
        return ResponseEntity.ok(jobService.getAllJobs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJobById(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.getJobById(id));
    }
}
