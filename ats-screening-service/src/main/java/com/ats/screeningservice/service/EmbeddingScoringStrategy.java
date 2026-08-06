package com.ats.screeningservice.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.ats.screeningservice.client.OpenAiEmbeddingClient;
import com.ats.screeningservice.dto.JobDto;

/**
 * Semantic-similarity scoring: embeds the job's title/description/required
 * skills and the resume text, then scores on cosine similarity. Catches
 * matches a keyword scan can't — a resume that says "built distributed
 * event pipelines" for a job requiring "Kafka" — at the cost of not being
 * able to point to a specific matched/missing skill list the way the
 * rule-based strategy can.
 */
@Component("embedding")
public class EmbeddingScoringStrategy implements ScoringStrategy {

    private final OpenAiEmbeddingClient embeddingClient;

    public EmbeddingScoringStrategy(OpenAiEmbeddingClient embeddingClient) {
        this.embeddingClient = embeddingClient;
    }

    @Override
    public ScoreResult computeScore(JobDto job, List<String> candidateSkills, String resumeText) {
        String jobText = buildJobText(job);
        String resume = (resumeText == null || resumeText.isBlank())
                ? String.join(", ", candidateSkills != null ? candidateSkills : List.of())
                : resumeText;

        List<double[]> embeddings = embeddingClient.embedBatch(List.of(jobText, resume));
        double similarity = cosineSimilarity(embeddings.get(0), embeddings.get(1));
        int score = (int) Math.round(clamp(similarity, 0, 1) * 100);

        String reasoning = "Semantic similarity between resume and job description: %d%% (cosine similarity %.3f)."
                .formatted(score, similarity);
        return new ScoreResult(score, reasoning);
    }

    private String buildJobText(JobDto job) {
        StringBuilder sb = new StringBuilder();
        sb.append(job.title() != null ? job.title() : "").append(". ");
        if (job.description() != null) {
            sb.append(job.description()).append(". ");
        }
        if (job.requiredSkills() != null && !job.requiredSkills().isEmpty()) {
            sb.append("Required skills: ").append(String.join(", ", job.requiredSkills())).append(".");
        }
        return sb.toString();
    }

    private double cosineSimilarity(double[] a, double[] b) {
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
