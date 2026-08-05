package com.ats.jobservice.dto;

import lombok.*;
import jakarta.validation.constraints.*;

import java.util.*;

@Getter
@Setter
public class CreateJobRequest {

    @NotBlank(message = "Job title is required")
    private String title;

    @NotBlank(message = "Job description is required")
    private String description;

    private String department;
    private List<String> requiredSkills;

    @Min(value = 0, message = "Minimum years of experience must be non-negative")
    private Integer minYearsExperience;
}
