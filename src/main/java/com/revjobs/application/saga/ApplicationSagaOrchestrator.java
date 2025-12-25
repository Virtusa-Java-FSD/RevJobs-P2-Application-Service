package com.revjobs.application.saga;

import com.revjobs.application.client.JobServiceClient;
import com.revjobs.application.client.NotificationServiceClient;
import com.revjobs.application.client.UserServiceClient;
import com.revjobs.application.model.Application;
import com.revjobs.application.model.ApplicationSaga;
import com.revjobs.application.repository.ApplicationRepository;
import com.revjobs.application.repository.ApplicationSagaRepository;
import com.revjobs.common.dto.UserDTO;
import com.revjobs.common.event.NotificationEvent;
import com.revjobs.common.saga.SagaStatus;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationSagaOrchestrator {

    private final ApplicationRepository applicationRepository;
    private final ApplicationSagaRepository sagaRepository;
    private final UserServiceClient userServiceClient;
    private final JobServiceClient jobServiceClient;
    private final NotificationServiceClient notificationServiceClient;

    @Async
    @Transactional
    public void orchestrateApplicationCreation(Long applicationId) {
        log.info("Starting saga for application: {}", applicationId);
        
        ApplicationSaga saga = new ApplicationSaga();
        saga.setApplicationId(applicationId);
        saga.setStatus(SagaStatus.STARTED);
        saga.setCurrentStep("INIT");
        saga = sagaRepository.save(saga);

        try {
            // Step 1: Validate Application Data
            Application application = validateApplication(saga, applicationId);
            
            // Step 2: Validate User
            validateUser(saga, application.getApplicantId());
            
            // Step 3: Validate Job
            validateJob(saga, application.getJobId());
            
            // Step 4: Send Notifications
            sendNotifications(saga, application);
            
            // Mark saga as completed
            saga.setStatus(SagaStatus.COMPLETED);
            saga.setCurrentStep("COMPLETED");
            sagaRepository.save(saga);
            
            log.info("Saga completed successfully for application: {}", applicationId);
            
        } catch (Exception e) {
            log.error("Saga failed for application: {}", applicationId, e);
            compensate(saga, e.getMessage());
        }
    }

    private Application validateApplication(ApplicationSaga saga, Long applicationId) {
        saga.setCurrentStep("VALIDATE_APPLICATION");
        saga.setStatus(SagaStatus.APPLICATION_CREATED);
        sagaRepository.save(saga);
        
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "validateUserFallback")
    private void validateUser(ApplicationSaga saga, Long userId) {
        saga.setCurrentStep("VALIDATE_USER");
        sagaRepository.save(saga);
        
        log.info("Validating user: {}", userId);
        UserDTO user = userServiceClient.getUserById(userId).getData();
        
        if (user == null || !user.getActive()) {
            throw new RuntimeException("Invalid or inactive user");
        }
    }

    private void validateUserFallback(ApplicationSaga saga, Long userId, Exception e) {
        log.warn("User validation fallback triggered for user: {}", userId);
        // Continue with saga, user validation is not critical
    }

    @CircuitBreaker(name = "jobService", fallbackMethod = "validateJobFallback")
    private void validateJob(ApplicationSaga saga, Long jobId) {
        saga.setCurrentStep("VALIDATE_JOB");
        sagaRepository.save(saga);
        
        log.info("Validating job: {}", jobId);
        Object job = jobServiceClient.getJobById(jobId).getData();
        
        if (job == null) {
            throw new RuntimeException("Job not found");
        }
    }

    private void validateJobFallback(ApplicationSaga saga, Long jobId, Exception e) {
        log.warn("Job validation fallback triggered for job: {}", jobId);
        // Continue with saga, job validation fallback
    }

    @CircuitBreaker(name = "notificationService", fallbackMethod = "sendNotificationsFallback")
    private void sendNotifications(ApplicationSaga saga, Application application) {
        saga.setCurrentStep("SEND_NOTIFICATIONS");
        saga.setStatus(SagaStatus.NOTIFICATION_SENT);
        sagaRepository.save(saga);
        
        log.info("Sending notifications for application: {}", application.getId());
        
        NotificationEvent event = new NotificationEvent();
        event.setUserId(application.getApplicantId());
        event.setMessage("Your application has been submitted successfully!");
        event.setType("APPLICATION_SUBMITTED");
        event.setTimestamp(LocalDateTime.now());
        
        notificationServiceClient.sendNotification(event);
    }

    private void sendNotificationsFallback(ApplicationSaga saga, Application application, Exception e) {
        log.warn("Notification sending fallback triggered for application: {}", application.getId());
        // Notifications are not critical, continue with saga
    }

    @Transactional
    private void compensate(ApplicationSaga saga, String errorMessage) {
        log.info("Starting compensation for saga: {}", saga.getId());
        
        saga.setStatus(SagaStatus.COMPENSATING);
        saga.setErrorMessage(errorMessage);
        sagaRepository.save(saga);

        try {
            // Compensate: Mark application as failed or delete it
            Application application = applicationRepository.findById(saga.getApplicationId())
                    .orElse(null);
            
            if (application != null) {
                // You could delete or mark as failed
                applicationRepository.delete(application);
                log.info("Application deleted as part of compensation");
            }
            
            saga.setStatus(SagaStatus.COMPENSATED);
            sagaRepository.save(saga);
            
        } catch (Exception e) {
            log.error("Compensation failed for saga: {}", saga.getId(), e);
            saga.setStatus(SagaStatus.FAILED);
            saga.setErrorMessage("Compensation failed: " + e.getMessage());
            sagaRepository.save(saga);
        }
    }
}

