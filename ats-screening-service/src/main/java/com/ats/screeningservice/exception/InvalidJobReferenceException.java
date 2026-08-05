package com.ats.screeningservice.exception;

public class InvalidJobReferenceException extends RuntimeException {
    public InvalidJobReferenceException(Long jobId) {
        super("Job not found with id: " + jobId);
    }
}
