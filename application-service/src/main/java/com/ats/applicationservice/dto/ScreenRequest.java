package com.ats.applicationservice.dto;

import java.util.List;

public record ScreenRequest(Long jobId, List<String> skills, Integer yearsOfExperience, String resumeText) {
}
