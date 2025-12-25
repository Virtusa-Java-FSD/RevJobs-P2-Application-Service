package com.revjobs.application.service;

import com.revjobs.application.client.NotificationServiceClient;
import com.revjobs.application.model.Application;
import com.revjobs.application.model.ApplicationStatus;
import com.revjobs.application.repository.ApplicationRepository;
import com.revjobs.application.saga.ApplicationSagaOrchestrator;
import com.revjobs.common.dto.ApiResponse;
import com.revjobs.common.event.NotificationEvent;
import com.revjobs.common.exception.BadRequestException;
import com.revjobs.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationSagaOrchestrator sagaOrchestrator;
    private final NotificationServiceClient notificationServiceClient;

    @Transactional
    public Application createApplication(Application application) {
        log.info("Creating application for job: {} by user: {}",
                application.getJobId(), application.getApplicantId());

        // Check if application already exists
        if (applicationRepository.findByApplicantIdAndJobId(
                application.getApplicantId(), application.getJobId()).isPresent()) {
            throw new BadRequestException("You have already applied for this job");
        }

        // Save application
        Application saved = applicationRepository.save(application);

        // Start saga asynchronously
        sagaOrchestrator.orchestrateApplicationCreation(saved.getId());

        return saved;
    }

    public List<Application> getAllApplications() {
        return applicationRepository.findAll();
    }

    public Application getApplicationById(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
    }

    public List<Application> getApplicationsByApplicant(Long applicantId) {
        return applicationRepository.findByApplicantId(applicantId);
    }

    public List<Application> getApplicationsByJob(Long jobId) {
        return applicationRepository.findByJobId(jobId);
    }

    @Transactional
    public Application updateApplicationStatus(Long id, ApplicationStatus status) {
        Application application = getApplicationById(id);
        application.setStatus(status);
        Application saved = applicationRepository.save(application);

        // Send notification about status change
        try {
            NotificationEvent event = new NotificationEvent();
            event.setUserId(saved.getApplicantId());
            event.setMessage("Your application status has been updated to: " + status);
            event.setType("APPLICATION_STATUS_UPDATE");
            event.setTimestamp(LocalDateTime.now());
            notificationServiceClient.sendNotification(event);
        } catch (Exception e) {
            log.error("Failed to send notification for application status update", e);
            // Don't fail the transaction just because notification failed
        }

        return saved;
    }

    @Transactional
    public void deleteApplication(Long id) {
        Application application = getApplicationById(id);
        applicationRepository.delete(application);
    }
}
