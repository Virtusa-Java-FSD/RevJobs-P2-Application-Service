package com.revjobs.application.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "applications", indexes = {
        @Index(name = "idx_app_applicant", columnList = "applicant_id"),
        @Index(name = "idx_app_job", columnList = "job_id"),
        @Index(name = "idx_app_status", columnList = "status"),
        @Index(name = "idx_app_applied_date", columnList = "appliedDate")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_applicant_job", columnNames = { "applicant_id", "job_id" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    @Column(nullable = false, length = 255)
    private String applicantEmail;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(length = 500)
    private String resumeUrl;

    @Column(columnDefinition = "TEXT")
    private String coverLetter;

    @Column(length = 255)
    private String companyName;

    @Column(length = 255)
    private String jobTitle;

    // Personal Information
    @Column(length = 255)
    private String applicantName;

    @Column(length = 20)
    private String applicantPhone;

    @Column(length = 20)
    private String gender;

    @Column(length = 100)
    private String nationality;

    @Column(length = 255)
    private String currentLocation;

    // Professional Details
    private Integer yearsOfExperience;

    @Column(length = 255)
    private String currentCompany;

    @Column(length = 500)
    private String education;

    @Column(columnDefinition = "TEXT")
    private String skills;

    // Additional Information
    @Column(length = 500)
    private String linkedinUrl;

    @Column(length = 500)
    private String portfolioUrl;

    @Column(length = 100)
    private String expectedSalary;

    @Column(length = 100)
    private String noticePeriod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @Column(updatable = false)
    private LocalDateTime appliedDate;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        appliedDate = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = ApplicationStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
