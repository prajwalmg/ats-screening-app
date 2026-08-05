package com.ats.screeningservice.dto;

import java.util.List;

public record ScreenResponse(
        int score,
        List<String> matchedSkills,
        List<String> missingSkills,
        boolean hardFilterPassed,
        String recommendedStatus,
        String reasoning
) {
}
