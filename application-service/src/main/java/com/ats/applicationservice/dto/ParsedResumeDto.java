package com.ats.applicationservice.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ParsedResumeDto(
        List<String> skills,
        Integer yearsOfExperience,
        int rawTextLength,
        String resumeTextExcerpt
) {
}
