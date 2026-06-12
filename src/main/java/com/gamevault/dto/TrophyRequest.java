package com.gamevault.dto;

import jakarta.validation.constraints.NotBlank;

public record TrophyRequest(
        @NotBlank(message = "O nome do troféu é obrigatório")
        String name,

        String description,

        String iconUrl
) {}
