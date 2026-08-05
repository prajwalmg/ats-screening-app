package com.ats.resumeparsingservice.dto;

import java.util.List;

public record ParsedResumeResponse(
        List<String> skills,
        Integer yearsOfExperience,
        int rawTextLength
) {
}
