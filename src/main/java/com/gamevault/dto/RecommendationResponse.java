package com.gamevault.dto;

import com.gamevault.model.Recommendation;

public record RecommendationResponse(
        Long id,
        String fromUsername,
        String fromName,
        String fromAvatar,
        String gameTitle,
        String gameEmoji,
        String genre,
        String platform,
        String message,
        Long sentAt,
        boolean read
) {
    public static RecommendationResponse from(Recommendation r) {
        return new RecommendationResponse(
                r.getId(),
                r.getFromUser().getUsername(),
                r.getFromUser().getName(),
                r.getFromUser().getAvatar(),
                r.getGameTitle(),
                r.getGameEmoji(),
                r.getGenre(),
                r.getPlatform(),
                r.getMessage(),
                r.getSentAt(),
                r.isRead()
        );
    }
}
