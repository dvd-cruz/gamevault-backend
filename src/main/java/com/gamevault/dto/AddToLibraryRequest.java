package com.gamevault.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AddToLibraryRequest(
        @NotNull(message = "O jogo do catálogo é obrigatório")
        Long catalogGameId,

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
