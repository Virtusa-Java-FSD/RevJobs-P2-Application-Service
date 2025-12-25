package com.revjobs.application.dto;

import com.revjobs.application.model.ApplicationStatus;
import lombok.Data;

@Data
public class StatusUpdateRequest {
    private ApplicationStatus status;
}
