package com.gamevault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RecommendationRequest(
        @NotNull(message = "O destinatário é obrigatório")
        Long toUserId,

        @NotBlank(message = "O título do jogo é obrigatório")
        String gameTitle,

        String gameEmoji,
        String genre,
        String platform,
        String message
) {}
