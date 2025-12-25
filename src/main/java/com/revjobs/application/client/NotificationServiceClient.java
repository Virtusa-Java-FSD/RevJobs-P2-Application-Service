package com.revjobs.application.client;

import com.revjobs.common.dto.ApiResponse;
import com.revjobs.common.event.NotificationEvent;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service")
public interface NotificationServiceClient {
    
    @PostMapping("/notifications/send")
    ApiResponse<Void> sendNotification(@RequestBody NotificationEvent event);
}

