package com.ats.screeningservice.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record ScreenRequest(
        @NotNull(message = "jobId is required") Long jobId,
        List<String> skills,
        Integer yearsOfExperience
) {
}
