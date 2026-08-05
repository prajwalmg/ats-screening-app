package com.ats.applicationservice.service;

import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ats.applicationservice.client.AtsScreeningServiceClient;
import com.ats.applicationservice.client.JobServiceClient;
import com.ats.applicationservice.client.ResumeParsingServiceClient;
import com.ats.applicationservice.dto.CreateApplicationRequest;
import com.ats.applicationservice.dto.JobDto;
import com.ats.applicationservice.dto.ParsedResumeDto;
import com.ats.applicationservice.dto.ScreenRequest;
import com.ats.applicationservice.dto.ScreeningResultDto;
import com.ats.applicationservice.entity.Application;
import com.ats.applicationservice.entity.ApplicationStatus;
import com.ats.applicationservice.exception.InvalidJobReferenceException;
import com.ats.applicationservice.repository.ApplicationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {
    private final ApplicationRepository applicationRepository;
    private final JobServiceClient jobServiceClient;
    private final ResumeParsingServiceClient resumeParsingServiceClient;
    private final AtsScreeningServiceClient atsScreeningServiceClient;

    public Application submitApplication(CreateApplicationRequest request) {
        JobDto job = jobServiceClient.findJob(request.getJobId())
                .orElseThrow(() -> new InvalidJobReferenceException(request.getJobId()));

        Application application = new Application();
        application.setCandidateName(request.getCandidateName());
        application.setEmail(request.getEmail());
        application.setPhone(request.getPhone());
        application.setJobId(request.getJobId());
        application.setJobTitle(job.title());
        application.setResumeUrl(request.getResumeUrl());
        application.setCoverLetterText(request.getCoverLetterText());
        application.setStatus(ApplicationStatus.SUBMITTED);
        application = applicationRepository.save(application);

        runScreeningPipeline(application);
        return applicationRepository.save(application);
    }

    /**
     * Orchestrates resume parsing + screening synchronously so the caller gets a
     * final score/status in the same response. A failure here must not lose the
     * already-submitted application — it's left in UNDER_REVIEW for a human to
     * pick up instead of propagating the error to the client.
     */
    private void runScreeningPipeline(Application application) {
        try {
            ParsedResumeDto parsed = resumeParsingServiceClient.parse(application.getResumeUrl());
            application.setParsedSkills(parsed.skills());
            application.setYearsOfExperience(parsed.yearsOfExperience());

            ScreeningResultDto result = atsScreeningServiceClient.screen(
                    new ScreenRequest(application.getJobId(), parsed.skills(), parsed.yearsOfExperience()));

            application.setMatchScore(result.score());
            application.setMatchedSkills(result.matchedSkills());
            application.setMissingSkills(result.missingSkills());
            application.setScreeningNotes(result.reasoning());
            application.setStatus(ApplicationStatus.valueOf(result.recommendedStatus()));
        } catch (Exception e) {
            log.warn("Automated screening failed for application {}: {}", application.getId(), e.getMessage());
            application.setStatus(ApplicationStatus.UNDER_REVIEW);
            application.setScreeningNotes("Automated screening unavailable: " + e.getMessage());
        }
    }

    public Page<Application> getApplications(Long jobId, ApplicationStatus status, Pageable pageable) {
        if (jobId != null && status != null) {
            return applicationRepository.findByJobIdAndStatus(jobId, status, pageable);
        }
        if (jobId != null) {
            return applicationRepository.findByJobId(jobId, pageable);
        }
        if (status != null) {
            return applicationRepository.findByStatus(status, pageable);
        }
        return applicationRepository.findAll(pageable);
    }

    public Application getApplicationById(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Application not found with id: " + id));
    }
}
