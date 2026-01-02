package com.revjobs.application.controller;

import com.revjobs.application.model.Application;
import com.revjobs.application.model.ApplicationStatus;
import com.revjobs.application.service.ApplicationService;
import com.revjobs.application.service.FileStorageService;
import com.revjobs.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final FileStorageService fileStorageService;

    @PostMapping
    public ResponseEntity<ApiResponse<Application>> createApplication(@RequestBody Application application) {
        Application created = applicationService.createApplication(application);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Application submitted successfully", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Application>>> getAllApplications() {
        List<Application> applications = applicationService.getAllApplications();
        return ResponseEntity.ok(ApiResponse.success(applications));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Application>> getApplicationById(@PathVariable Long id) {
        Application application = applicationService.getApplicationById(id);
        return ResponseEntity.ok(ApiResponse.success(application));
    }

    @GetMapping("/applicant/{applicantId}")
    public ResponseEntity<ApiResponse<List<Application>>> getApplicationsByApplicant(@PathVariable Long applicantId) {
        List<Application> applications = applicationService.getApplicationsByApplicant(applicantId);
        return ResponseEntity.ok(ApiResponse.success(applications));
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<ApiResponse<List<Application>>> getApplicationsByJob(@PathVariable Long jobId) {
        List<Application> applications = applicationService.getApplicationsByJob(jobId);
        return ResponseEntity.ok(ApiResponse.success(applications));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Application>> updateStatus(
            @PathVariable Long id, @RequestBody com.revjobs.application.dto.StatusUpdateRequest request) {
        Application updated = applicationService.updateApplicationStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Status updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteApplication(@PathVariable Long id) {
        applicationService.deleteApplication(id);
        return ResponseEntity.ok(ApiResponse.success("Application deleted successfully", null));
    }

    /**
     * Upload resume file
     * 
     * @param file the resume file to upload
     * @return the URL path to access the uploaded file
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String fileUrl = fileStorageService.storeFile(file);
            Map<String, String> response = new HashMap<>();
            response.put("url", fileUrl);
            response.put("message", "File uploaded successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * Download/view uploaded file
     * 
     * @param filename the name of the file to download
     * @return the file as a resource
     */
    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        try {
            Resource resource = fileStorageService.loadFileAsResource(filename);

            // Determine content type
            String contentType = "application/octet-stream";
            if (filename.toLowerCase().endsWith(".pdf")) {
                contentType = "application/pdf";
            } else if (filename.toLowerCase().endsWith(".doc")) {
                contentType = "application/msword";
            } else if (filename.toLowerCase().endsWith(".docx")) {
                contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
