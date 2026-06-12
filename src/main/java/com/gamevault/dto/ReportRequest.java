package com.gamevault.dto;

import jakarta.validation.constraints.NotBlank;

public record ReportRequest(
        @NotBlank String reason,
        String description
) {}
