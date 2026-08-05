package com.ats.applicationservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.ats.applicationservice.dto.ScreenRequest;
import com.ats.applicationservice.dto.ScreeningResultDto;
import com.ats.applicationservice.exception.UpstreamServiceException;

@Component
public class AtsScreeningServiceClient {

    private final RestClient restClient;

    public AtsScreeningServiceClient(
            @Value("${services.ats-screening-service.base-url}") String baseUrl,
            @Value("${http-client.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${http-client.read-timeout-ms:3000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public ScreeningResultDto screen(ScreenRequest request) {
        try {
            return restClient.post()
                    .uri("/screen")
                    .body(request)
                    .retrieve()
                    .body(ScreeningResultDto.class);
        } catch (RestClientException e) {
            throw new UpstreamServiceException("ats-screening-service", e.getMessage());
        }
    }
}
