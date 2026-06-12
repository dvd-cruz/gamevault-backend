package com.gamevault.dto;

import jakarta.validation.constraints.NotBlank;

/** Request body for manually registering/editing a DLC entry on a catalog game (used for non-Steam games). */
public record CatalogDlcRequest(
        @NotBlank(message = "O nome da DLC é obrigatório")
        String name,

        Double price,
        String storeUrl,
        String coverUrl
) {}
