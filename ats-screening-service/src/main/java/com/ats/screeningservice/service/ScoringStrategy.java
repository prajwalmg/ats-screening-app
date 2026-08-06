package com.ats.screeningservice.service;

import java.util.List;

import com.ats.screeningservice.dto.JobDto;

/**
 * Pluggable scoring mechanism, selected at runtime by
 * {@code screening.strategy} (see {@link ScreeningService}). Implementations
 * are registered as named Spring beans — the bean name is the strategy's
 * config key.
 */
public interface ScoringStrategy {
    ScoreResult computeScore(JobDto job, List<String> candidateSkills, String resumeText);
}
