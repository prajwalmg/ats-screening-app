package com.ats.applicationservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ats.applicationservice.entity.Application;
import com.ats.applicationservice.entity.ApplicationStatus;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    Page<Application> findByJobIdAndStatus(Long jobId, ApplicationStatus status, Pageable pageable);
    Page<Application> findByJobId(Long jobId, Pageable pageable);
    Page<Application> findByStatus(ApplicationStatus status, Pageable pageable);
}
