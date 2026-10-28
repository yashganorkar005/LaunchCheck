package com.launchcheck.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
        @NotBlank(message = "Project name is required")
        @Size(max = 255, message = "Project name must be at most 255 characters")
        String name,

        @NotBlank(message = "Frontend URL is required")
        @Size(max = 1024, message = "Frontend URL must be at most 1024 characters")
        String frontendUrl,

        @NotBlank(message = "Backend URL is required")
        @Size(max = 1024, message = "Backend URL must be at most 1024 characters")
        String backendUrl
) {
}
