package com.ats.jobservice.entity;

import java.time.LocalDateTime;
import java.util.*;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 8000)
    private String description;

    private String department;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_required_skills", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "skill")
    private List<String> requiredSkills;

    private Integer minYearsExperience;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
