package com.revjobs.application.client;

import com.revjobs.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "job-service")
public interface JobServiceClient {
    
    @GetMapping("/jobs/{id}")
    ApiResponse<Object> getJobById(@PathVariable Long id);
}

