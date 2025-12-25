package com.revjobs.application.client;

import com.revjobs.common.dto.ApiResponse;
import com.revjobs.common.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserServiceClient {
    
    @GetMapping("/users/{id}")
    ApiResponse<UserDTO> getUserById(@PathVariable Long id);
}

