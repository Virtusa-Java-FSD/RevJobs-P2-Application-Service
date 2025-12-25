package com.revjobs.application.repository;

import com.revjobs.application.model.Application;
import com.revjobs.application.model.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByApplicantId(Long applicantId);
    List<Application> findByJobId(Long jobId);
    Optional<Application> findByApplicantIdAndJobId(Long applicantId, Long jobId);
    List<Application> findByStatus(ApplicationStatus status);
}

