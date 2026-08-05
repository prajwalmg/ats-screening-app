package com.ats.screeningservice.client;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.ats.screeningservice.dto.JobDto;
import com.ats.screeningservice.exception.UpstreamServiceException;

@Component
public class JobServiceClient {

    private final RestClient restClient;

    public JobServiceClient(
            @Value("${services.job-service.base-url}") String baseUrl,
            @Value("${http-client.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${http-client.read-timeout-ms:3000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public Optional<JobDto> findJob(Long jobId) {
        try {
            JobDto job = restClient.get()
                    .uri("/jobs/{id}", jobId)
                    .retrieve()
                    .body(JobDto.class);
            return Optional.ofNullable(job);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new UpstreamServiceException("job-service", e.getMessage());
        }
    }
}
