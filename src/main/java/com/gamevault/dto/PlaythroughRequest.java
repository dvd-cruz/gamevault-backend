package com.gamevault.dto;

import jakarta.validation.constraints.NotBlank;

public record PlaythroughRequest(
        @NotBlank(message = "O tipo de playthrough é obrigatório")
        String type,

        boolean speedrun,
        boolean modded,
        boolean completed,
        Double hours,
        String platform,
        String difficulty,
        String language,
        String notes
) {}
