package com.revjobs.application.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revjobs.application.model.Application;
import com.revjobs.application.model.ApplicationStatus;
import com.revjobs.application.repository.ApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApplicationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationRepository applicationRepository;

    private Application testApplication;

    @BeforeEach
    void setUp() {
        applicationRepository.deleteAll();

        testApplication = new Application();
        testApplication.setApplicantId(100L);
        testApplication.setJobId(200L);
        testApplication.setApplicantEmail("test@example.com");
        testApplication.setResumeUrl("https://example.com/resume.pdf");
        testApplication.setCoverLetter("I am interested in this position");
        testApplication.setCompanyName("Tech Corp");
        testApplication.setJobTitle("Software Engineer");
        testApplication.setStatus(ApplicationStatus.PENDING);
    }

    @Test
    void createApplication_Success() throws Exception {
        mockMvc.perform(post("/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testApplication)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Application submitted successfully"))
                .andExpect(jsonPath("$.data.applicantId").value(100))
                .andExpect(jsonPath("$.data.jobId").value(200))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void createApplication_DuplicateApplication_ReturnsBadRequest() throws Exception {
        // Create first application
        applicationRepository.save(testApplication);

        // Try to create duplicate
        mockMvc.perform(post("/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testApplication)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("You have already applied for this job"));
    }

    @Test
    void getApplicationById_Success() throws Exception {
        Application saved = applicationRepository.save(testApplication);

        mockMvc.perform(get("/applications/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(saved.getId()))
                .andExpect(jsonPath("$.data.applicantId").value(100))
                .andExpect(jsonPath("$.data.jobId").value(200));
    }

    @Test
    void getApplicationById_NotFound() throws Exception {
        mockMvc.perform(get("/applications/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Application not found"));
    }

    @Test
    void getAllApplications_ReturnsListOfApplications() throws Exception {
        applicationRepository.save(testApplication);

        Application app2 = new Application();
        app2.setApplicantId(101L);
        app2.setJobId(201L);
        app2.setApplicantEmail("test2@example.com");
        app2.setResumeUrl("https://example.com/resume2.pdf");
        app2.setStatus(ApplicationStatus.PENDING);
        applicationRepository.save(app2);

        mockMvc.perform(get("/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void getApplicationsByApplicant_ReturnsApplicantApplications() throws Exception {
        applicationRepository.save(testApplication);

        mockMvc.perform(get("/applications/applicant/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].applicantId").value(100));
    }

    @Test
    void getApplicationsByJob_ReturnsJobApplications() throws Exception {
        applicationRepository.save(testApplication);

        mockMvc.perform(get("/applications/job/200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].jobId").value(200));
    }

    @Test
    void updateApplicationStatus_Success() throws Exception {
        Application saved = applicationRepository.save(testApplication);

        mockMvc.perform(put("/applications/" + saved.getId() + "/status")
                .param("status", "ACCEPTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }

    @Test
    void deleteApplication_Success() throws Exception {
        Application saved = applicationRepository.save(testApplication);

        mockMvc.perform(delete("/applications/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Application deleted successfully"));
    }
}
