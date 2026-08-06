package com.ats.screeningservice.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.ats.screeningservice.dto.JobDto;

/**
 * The original scoring approach: score is pure keyword-overlap percentage
 * between the job's required skills and the resume's parsed skills.
 * Transparent and explainable — an admin (or a rejected candidate) can see
 * exactly why a score came out the way it did.
 */
@Component("rule-based")
public class RuleBasedScoringStrategy implements ScoringStrategy {

    @Override
    public ScoreResult computeScore(JobDto job, List<String> candidateSkills, String resumeText) {
        List<String> requiredSkills = job.requiredSkills() != null ? job.requiredSkills() : List.of();
        List<String> skills = candidateSkills != null ? candidateSkills : List.of();

        Set<String> normalizedCandidateSkills = skills.stream()
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        long matchedCount = requiredSkills.stream()
                .filter(required -> normalizedCandidateSkills.contains(required.trim().toLowerCase(Locale.ROOT)))
                .count();

        int score = requiredSkills.isEmpty()
                ? 100
                : Math.round(matchedCount * 100f / requiredSkills.size());

        String reasoning = "Keyword skill match: %d/%d required skills (%d%%).".formatted(
                matchedCount, requiredSkills.size(), score);
        return new ScoreResult(score, reasoning);
    }
}
