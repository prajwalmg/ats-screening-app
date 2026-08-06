package com.ats.screeningservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ats.screeningservice.client.OpenAiEmbeddingClient;
import com.ats.screeningservice.dto.JobDto;

/**
 * Runs the exact same job/resume pair through both scoring strategies so the
 * two approaches can be compared side by side. The embedding client is
 * mocked with a fixed, known pair of vectors (cosine similarity 0.8) rather
 * than calling OpenAI for real — this test is about proving the mechanism
 * (interface contract, cosine-similarity math, score derivation) is
 * correct, not about validating real-world embedding quality, which isn't
 * something a deterministic unit test should depend on.
 */
@ExtendWith(MockitoExtension.class)
class ScoringStrategyComparisonTest {

    private static final JobDto JOB = new JobDto(
            1L,
            "Backend Engineer",
            "We need a backend engineer skilled in Java, Spring Boot, and PostgreSQL to build scalable APIs.",
            List.of("Java", "Spring Boot", "PostgreSQL"),
            3,
            "OPEN");

    private static final List<String> CANDIDATE_SKILLS = List.of("Java", "Spring Boot", "PostgreSQL", "Docker");

    private static final String RESUME_TEXT =
            "Experienced backend engineer with 5 years building Java and Spring Boot services "
                    + "backed by PostgreSQL, deployed via Docker.";

    @Mock
    private OpenAiEmbeddingClient embeddingClient;

    @Test
    void ruleBasedAndEmbeddingStrategies_scoreTheSameJobResumePairDifferently() {
        RuleBasedScoringStrategy ruleBased = new RuleBasedScoringStrategy();
        ScoreResult ruleBasedResult = ruleBased.computeScore(JOB, CANDIDATE_SKILLS, RESUME_TEXT);

        // Fixed vectors with a known cosine similarity of 0.8 (dot=0.8, both unit norm),
        // standing in for what OpenAI would actually return for job-text vs resume-text.
        when(embeddingClient.embedBatch(anyList())).thenReturn(List.of(
                new double[] {1.0, 0.0},
                new double[] {0.8, 0.6}));

        EmbeddingScoringStrategy embedding = new EmbeddingScoringStrategy(embeddingClient);
        ScoreResult embeddingResult = embedding.computeScore(JOB, CANDIDATE_SKILLS, RESUME_TEXT);

        // All 3 required skills present in candidateSkills -> perfect keyword overlap.
        assertEquals(100, ruleBasedResult.score());
        // Cosine similarity 0.8 -> score 80, independent of the keyword overlap above.
        assertEquals(80, embeddingResult.score());

        // Same inputs, two legitimately different numbers for two legitimately
        // different reasons — that's the point of the comparison.
        assertNotEquals(ruleBasedResult.reasoning(), embeddingResult.reasoning());
    }
}
