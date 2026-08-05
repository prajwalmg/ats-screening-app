package com.ats.applicationservice.exception;

public class UpstreamServiceException extends RuntimeException {
    public UpstreamServiceException(String serviceName, String reason) {
        super(serviceName + " is unreachable: " + reason);
    }
}
