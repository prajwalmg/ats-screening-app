package com.ats.resumeparsingservice.dto;

import jakarta.validation.constraints.NotBlank;

public record ParseRequest(
        @NotBlank(message = "resumeUrl is required") String resumeUrl
) {
}
