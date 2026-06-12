package com.gamevault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record SessionRequest(
        @NotBlank(message = "A data é obrigatória")
        String date,

        @Positive(message = "As horas devem ser positivas")
        Double hours,

        String notes
) {}
