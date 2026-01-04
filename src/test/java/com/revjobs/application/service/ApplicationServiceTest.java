package com.revjobs.application.service;

import com.revjobs.application.client.NotificationServiceClient;
import com.revjobs.application.model.Application;
import com.revjobs.application.model.ApplicationStatus;
import com.revjobs.application.repository.ApplicationRepository;
import com.revjobs.application.saga.ApplicationSagaOrchestrator;
import com.revjobs.common.exception.BadRequestException;
import com.revjobs.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApplicationSagaOrchestrator sagaOrchestrator;

    @Mock
    private NotificationServiceClient notificationServiceClient;

    @InjectMocks
    private ApplicationService applicationService;

    private Application testApplication;

    @BeforeEach
    void setUp() {
        testApplication = new Application();
        testApplication.setId(1L);
        testApplication.setApplicantId(100L);
        testApplication.setJobId(200L);
        testApplication.setApplicantEmail("test@example.com");
        testApplication.setResumeUrl("https://example.com/resume.pdf");
        testApplication.setCoverLetter("I am interested in this position");
        testApplication.setStatus(ApplicationStatus.PENDING);
    }

    @Test
    void createApplication_Success() {
        // Given
        when(applicationRepository.findByApplicantIdAndJobId(100L, 200L))
                .thenReturn(Optional.empty());
        when(applicationRepository.save(any(Application.class)))
                .thenReturn(testApplication);
        doNothing().when(sagaOrchestrator).orchestrateApplicationCreation(anyLong());

        // When
        Application result = applicationService.createApplication(testApplication);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getApplicantId()).isEqualTo(100L);
        assertThat(result.getJobId()).isEqualTo(200L);
        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.PENDING);

        verify(applicationRepository).findByApplicantIdAndJobId(100L, 200L);
        verify(applicationRepository).save(testApplication);
        verify(sagaOrchestrator).orchestrateApplicationCreation(1L);
    }

    @Test
    void createApplication_ThrowsBadRequestException_WhenDuplicateApplication() {
        // Given
        when(applicationRepository.findByApplicantIdAndJobId(100L, 200L))
                .thenReturn(Optional.of(testApplication));

        // When & Then
        assertThatThrownBy(() -> applicationService.createApplication(testApplication))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("You have already applied for this job");

        verify(applicationRepository).findByApplicantIdAndJobId(100L, 200L);
        verify(applicationRepository, never()).save(any());
        verify(sagaOrchestrator, never()).orchestrateApplicationCreation(anyLong());
    }

    @Test
    void getAllApplications_ReturnsListOfApplications() {
        // Given
        Application app2 = new Application();
        app2.setId(2L);
        app2.setApplicantId(101L);
        app2.setJobId(201L);

        List<Application> applications = Arrays.asList(testApplication, app2);
        when(applicationRepository.findAll()).thenReturn(applications);

        // When
        List<Application> result = applicationService.getAllApplications();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(testApplication, app2);
        verify(applicationRepository).findAll();
    }

    @Test
    void getApplicationById_Success() {
        // Given
        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(testApplication));

        // When
        Application result = applicationService.getApplicationById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(applicationRepository).findById(1L);
    }

    @Test
    void getApplicationById_ThrowsResourceNotFoundException_WhenNotFound() {
        // Given
        when(applicationRepository.findById(999L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> applicationService.getApplicationById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Application not found");

        verify(applicationRepository).findById(999L);
    }

    @Test
    void getApplicationsByApplicant_ReturnsApplicantApplications() {
        // Given
        List<Application> applications = Arrays.asList(testApplication);
        when(applicationRepository.findByApplicantId(100L))
                .thenReturn(applications);

        // When
        List<Application> result = applicationService.getApplicationsByApplicant(100L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getApplicantId()).isEqualTo(100L);
        verify(applicationRepository).findByApplicantId(100L);
    }

    @Test
    void getApplicationsByJob_ReturnsJobApplications() {
        // Given
        List<Application> applications = Arrays.asList(testApplication);
        when(applicationRepository.findByJobId(200L))
                .thenReturn(applications);

        // When
        List<Application> result = applicationService.getApplicationsByJob(200L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getJobId()).isEqualTo(200L);
        verify(applicationRepository).findByJobId(200L);
    }

    @Test
    void updateApplicationStatus_Success() {
        // Given
        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(testApplication));

        Application updatedApp = new Application();
        updatedApp.setId(1L);
        updatedApp.setApplicantId(100L);
        updatedApp.setJobId(200L);
        updatedApp.setStatus(ApplicationStatus.ACCEPTED);

        when(applicationRepository.save(any(Application.class)))
                .thenReturn(updatedApp);

        doNothing().when(notificationServiceClient).sendNotification(any());

        // When
        Application result = applicationService.updateApplicationStatus(1L, ApplicationStatus.ACCEPTED);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
        verify(applicationRepository).findById(1L);
        verify(applicationRepository).save(any(Application.class));
        verify(notificationServiceClient).sendNotification(any());
    }

    @Test
    void updateApplicationStatus_ContinuesEvenWhenNotificationFails() {
        // Given
        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(testApplication));

        Application updatedApp = new Application();
        updatedApp.setId(1L);
        updatedApp.setStatus(ApplicationStatus.REJECTED);

        when(applicationRepository.save(any(Application.class)))
                .thenReturn(updatedApp);

        doThrow(new RuntimeException("Notification service unavailable"))
                .when(notificationServiceClient).sendNotification(any());

        // When
        Application result = applicationService.updateApplicationStatus(1L, ApplicationStatus.REJECTED);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        verify(applicationRepository).findById(1L);
        verify(applicationRepository).save(any(Application.class));
    }

    @Test
    void deleteApplication_Success() {
        // Given
        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(testApplication));
        doNothing().when(applicationRepository).delete(testApplication);

        // When
        applicationService.deleteApplication(1L);

        // Then
        verify(applicationRepository).findById(1L);
        verify(applicationRepository).delete(testApplication);
    }

    @Test
    void deleteApplication_ThrowsResourceNotFoundException_WhenNotFound() {
        // Given
        when(applicationRepository.findById(999L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> applicationService.deleteApplication(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Application not found");

        verify(applicationRepository).findById(999L);
        verify(applicationRepository, never()).delete(any());
    }
}
