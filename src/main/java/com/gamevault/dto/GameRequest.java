package com.gamevault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

public record GameRequest(
        @NotBlank(message = "O estado é obrigatório")
        String status,

        Double hours,

        @Min(0) @Max(5)
        Integer rating,

        String notes,

        String review,

        @Min(0) @Max(100)
        Integer progress,

        List<String> tags
) {}
