package com.ats.screeningservice.exception;

public class UpstreamServiceException extends RuntimeException {
    public UpstreamServiceException(String serviceName, String reason) {
        super(serviceName + " is unreachable: " + reason);
    }
}
