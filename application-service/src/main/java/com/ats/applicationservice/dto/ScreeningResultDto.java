package com.ats.applicationservice.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ScreeningResultDto(
        int score,
        List<String> matchedSkills,
        List<String> missingSkills,
        boolean hardFilterPassed,
        String recommendedStatus,
        String reasoning
) {
}
