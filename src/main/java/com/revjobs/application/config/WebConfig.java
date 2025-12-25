package com.revjobs.application.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve files from uploads directory
        String uploadPath = "file:" + new File("uploads/resumes/").getAbsolutePath() + "/";

        registry.addResourceHandler("/uploads/resumes/**")
                .addResourceLocations(uploadPath);
    }
}
