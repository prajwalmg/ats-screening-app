package com.ats.screeningservice.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ats.screeningservice.client.JobServiceClient;
import com.ats.screeningservice.dto.JobDto;
import com.ats.screeningservice.dto.ScreenRequest;
import com.ats.screeningservice.dto.ScreenResponse;
import com.ats.screeningservice.exception.InvalidJobReferenceException;

/**
 * Orchestrates screening: fetches the job, always computes keyword
 * matched/missing skills as diagnostic context, applies the hard experience
 * gate (constant regardless of strategy), and delegates the numeric score
 * itself to whichever {@link ScoringStrategy} bean is selected by
 * {@code screening.strategy} (bean name = config value). Defaults to
 * "rule-based" so an unset/misconfigured value can't silently break scoring.
 */
@Service
public class ScreeningService {

    private final JobServiceClient jobServiceClient;
    private final Map<String, ScoringStrategy> strategiesByName;

    @Value("${screening.advance-threshold-percent:70}")
    private int advanceThresholdPercent;

    @Value("${screening.strategy:rule-based}")
    private String activeStrategyName;

    public ScreeningService(JobServiceClient jobServiceClient, Map<String, ScoringStrategy> strategiesByName) {
        this.jobServiceClient = jobServiceClient;
        this.strategiesByName = strategiesByName;
    }

    public ScreenResponse screen(ScreenRequest request) {
        JobDto job = jobServiceClient.findJob(request.jobId())
                .orElseThrow(() -> new InvalidJobReferenceException(request.jobId()));

        List<String> requiredSkills = job.requiredSkills() != null ? job.requiredSkills() : List.of();
        List<String> candidateSkills = request.skills() != null ? request.skills() : List.of();
        int minYearsExperience = job.minYearsExperience() != null ? job.minYearsExperience() : 0;
        int candidateYears = request.yearsOfExperience() != null ? request.yearsOfExperience() : 0;

        List<String> matchedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();
        computeSkillOverlap(requiredSkills, candidateSkills, matchedSkills, missingSkills);

        boolean hardFilterPassed = candidateYears >= minYearsExperience;

        ScoringStrategy strategy = resolveStrategy();
        ScoreResult scoreResult = strategy.computeScore(job, candidateSkills, request.resumeText());

        String recommendedStatus;
        if (!hardFilterPassed) {
            recommendedStatus = "REJECTED";
        } else if (scoreResult.score() >= advanceThresholdPercent) {
            recommendedStatus = "ADVANCED";
        } else {
            recommendedStatus = "UNDER_REVIEW";
        }

        String reasoning = buildReasoning(scoreResult.reasoning(), hardFilterPassed, candidateYears,
                minYearsExperience, recommendedStatus);

        return new ScreenResponse(scoreResult.score(), matchedSkills, missingSkills, hardFilterPassed,
                recommendedStatus, reasoning);
    }

    private ScoringStrategy resolveStrategy() {
        ScoringStrategy strategy = strategiesByName.get(activeStrategyName);
        if (strategy == null) {
            throw new IllegalStateException(
                    "Unknown screening.strategy '" + activeStrategyName + "', expected one of " + strategiesByName.keySet());
        }
        return strategy;
    }

    private void computeSkillOverlap(List<String> requiredSkills, List<String> candidateSkills,
            List<String> matchedSkills, List<String> missingSkills) {
        Set<String> normalizedCandidateSkills = candidateSkills.stream()
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        for (String required : requiredSkills) {
            if (normalizedCandidateSkills.contains(required.trim().toLowerCase(Locale.ROOT))) {
                matchedSkills.add(required);
            } else {
                missingSkills.add(required);
            }
        }
    }

    private String buildReasoning(String strategyReasoning, boolean hardFilterPassed, int candidateYears,
            int minYearsExperience, String recommendedStatus) {
        StringBuilder sb = new StringBuilder();
        sb.append(strategyReasoning).append(' ');
        if (hardFilterPassed) {
            sb.append("Meets minimum ").append(minYearsExperience).append(" years experience (candidate has ")
                    .append(candidateYears).append("). ");
        } else {
            sb.append("Below minimum ").append(minYearsExperience).append(" years experience (candidate has ")
                    .append(candidateYears).append("), auto-rejected regardless of score. ");
        }
        sb.append("Recommended status: ").append(recommendedStatus).append(".");
        return sb.toString();
    }
}
