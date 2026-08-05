package com.ats.applicationservice.entity;

import java.time.LocalDateTime;
import java.util.*;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String candidateName;
    private String email;
    private String phone;

    private Long jobId;
    private String jobTitle;

    private String resumeUrl;

    @Column(length = 4000)
    private String coverLetterText;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    private LocalDateTime submittedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "application_parsed_skills", joinColumns = @JoinColumn(name = "application_id"))
    @Column(name = "skill")
    private List<String> parsedSkills;

    private Integer yearsOfExperience;

    private Integer matchScore;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "application_matched_skills", joinColumns = @JoinColumn(name = "application_id"))
    @Column(name = "skill")
    private List<String> matchedSkills;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "application_missing_skills", joinColumns = @JoinColumn(name = "application_id"))
    @Column(name = "skill")
    private List<String> missingSkills;

    @Column(length = 1000)
    private String screeningNotes;

    @PrePersist
    void onCreate() {
        this.submittedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ApplicationStatus.SUBMITTED;
        }
    }
}
