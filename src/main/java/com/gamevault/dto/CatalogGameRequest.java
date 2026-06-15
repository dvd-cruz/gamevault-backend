package com.gamevault.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CatalogGameRequest(
        @NotBlank(message = "O título é obrigatório")
        String title,

        String emoji,
        String coverUrl,
        String heroImageUrl,
        Integer heroImagePositionY,
        String iconUrl,
        String logoUrl,
        String platform,
        String genre,
        Long releaseDate,
        String developer,
        String publisher,
        String description,
        String difficulties,
        boolean hasDlc,
        Long steamAppId,
        String psnNpCommunicationId,
        String psnProductId,
        List<String> franchiseNames
) {}
