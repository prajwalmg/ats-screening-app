package com.ats.applicationservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.ats.applicationservice.dto.ParseResumeRequest;
import com.ats.applicationservice.dto.ParsedResumeDto;
import com.ats.applicationservice.exception.UpstreamServiceException;

@Component
public class ResumeParsingServiceClient {

    private final RestClient restClient;

    public ResumeParsingServiceClient(
            @Value("${services.resume-parsing-service.base-url}") String baseUrl,
            @Value("${http-client.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${services.resume-parsing-service.read-timeout-ms:15000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public ParsedResumeDto parse(String resumeUrl) {
        try {
            return restClient.post()
                    .uri("/parse")
                    .body(new ParseResumeRequest(resumeUrl))
                    .retrieve()
                    .body(ParsedResumeDto.class);
        } catch (RestClientException e) {
            throw new UpstreamServiceException("resume-parsing-service", e.getMessage());
        }
    }
}
