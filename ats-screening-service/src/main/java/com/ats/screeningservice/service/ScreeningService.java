package com.ats.screeningservice.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ats.screeningservice.client.JobServiceClient;
import com.ats.screeningservice.dto.JobDto;
import com.ats.screeningservice.dto.ScreenRequest;
import com.ats.screeningservice.dto.ScreenResponse;
import com.ats.screeningservice.exception.InvalidJobReferenceException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScreeningService {

    private final JobServiceClient jobServiceClient;

    @Value("${screening.advance-threshold-percent:70}")
    private int advanceThresholdPercent;

    public ScreenResponse screen(ScreenRequest request) {
        JobDto job = jobServiceClient.findJob(request.jobId())
                .orElseThrow(() -> new InvalidJobReferenceException(request.jobId()));

        List<String> requiredSkills = job.requiredSkills() != null ? job.requiredSkills() : List.of();
        List<String> candidateSkills = request.skills() != null ? request.skills() : List.of();
        int minYearsExperience = job.minYearsExperience() != null ? job.minYearsExperience() : 0;
        int candidateYears = request.yearsOfExperience() != null ? request.yearsOfExperience() : 0;

        Set<String> normalizedCandidateSkills = candidateSkills.stream()
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        List<String> matchedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();
        for (String required : requiredSkills) {
            if (normalizedCandidateSkills.contains(required.trim().toLowerCase(Locale.ROOT))) {
                matchedSkills.add(required);
            } else {
                missingSkills.add(required);
            }
        }

        int score = requiredSkills.isEmpty()
                ? 100
                : Math.round(matchedSkills.size() * 100f / requiredSkills.size());

        boolean hardFilterPassed = candidateYears >= minYearsExperience;

        String recommendedStatus;
        if (!hardFilterPassed) {
            recommendedStatus = "REJECTED";
        } else if (score >= advanceThresholdPercent) {
            recommendedStatus = "ADVANCED";
        } else {
            recommendedStatus = "UNDER_REVIEW";
        }

        String reasoning = buildReasoning(requiredSkills.size(), matchedSkills.size(), score,
                hardFilterPassed, candidateYears, minYearsExperience, recommendedStatus);

        return new ScreenResponse(score, matchedSkills, missingSkills, hardFilterPassed, recommendedStatus, reasoning);
    }

    private String buildReasoning(int requiredCount, int matchedCount, int score, boolean hardFilterPassed,
            int candidateYears, int minYearsExperience, String recommendedStatus) {
        StringBuilder sb = new StringBuilder();
        sb.append("Matched ").append(matchedCount).append("/").append(requiredCount)
                .append(" required skills (").append(score).append("%). ");
        if (hardFilterPassed) {
            sb.append("Meets minimum ").append(minYearsExperience).append(" years experience (candidate has ")
                    .append(candidateYears).append("). ");
        } else {
            sb.append("Below minimum ").append(minYearsExperience).append(" years experience (candidate has ")
                    .append(candidateYears).append("), auto-rejected regardless of skill match. ");
        }
        sb.append("Recommended status: ").append(recommendedStatus).append(".");
        return sb.toString();
    }
}
