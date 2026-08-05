package com.ats.applicationservice.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JobDto(
        Long id,
        String title,
        List<String> requiredSkills,
        Integer minYearsExperience,
        String status
) {
}
