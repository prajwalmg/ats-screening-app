package com.ats.applicationservice.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
public class CreateApplicationRequest {

    @NotBlank(message = "Candidate name is required")
    private String candidateName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    private String email;

    private String phone;

    @NotNull(message = "Job id is required")
    private Long jobId;

    @NotBlank(message = "Resume URL is required")
    private String resumeUrl;

    private String coverLetterText;
}
