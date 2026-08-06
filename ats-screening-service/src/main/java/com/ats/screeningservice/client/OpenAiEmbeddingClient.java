package com.ats.screeningservice.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.ats.screeningservice.exception.EmbeddingException;

/**
 * Thin client for OpenAI's embeddings endpoint. One HTTP call embeds a batch
 * of texts (job text + resume text) together rather than two round trips.
 */
@Component
public class OpenAiEmbeddingClient {

    private final RestClient restClient;
    private final String model;

    public OpenAiEmbeddingClient(
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.embedding-model:text-embedding-3-small}") String model,
            @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${http-client.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${openai.read-timeout-ms:8000}") int readTimeoutMs) {
        this.model = model;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .requestFactory(factory)
                .build();
    }

    public List<double[]> embedBatch(List<String> texts) {
        try {
            EmbeddingApiResponse response = restClient.post()
                    .uri("/embeddings")
                    .body(new EmbeddingApiRequest(model, texts))
                    .retrieve()
                    .body(EmbeddingApiResponse.class);
            if (response == null || response.data() == null || response.data().size() != texts.size()) {
                throw new EmbeddingException("Unexpected response shape from OpenAI embeddings API");
            }
            return response.data().stream().map(d -> toArray(d.embedding())).toList();
        } catch (RestClientException e) {
            throw new EmbeddingException("OpenAI embeddings call failed: " + e.getMessage());
        }
    }

    private double[] toArray(List<Double> values) {
        double[] array = new double[values.size()];
        for (int i = 0; i < values.size(); i++) {
            array[i] = values.get(i);
        }
        return array;
    }

    private record EmbeddingApiRequest(String model, List<String> input) {
    }

    private record EmbeddingApiResponse(List<EmbeddingDatum> data) {
    }

    private record EmbeddingDatum(List<Double> embedding) {
    }
}
