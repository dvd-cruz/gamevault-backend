package com.gamevault.dto;

import com.gamevault.model.GameCatalog;

/** Lightweight catalog game reference used when embedding games in other responses (e.g. a franchise's game list). */
public record CatalogGameSummaryResponse(
        Long id,
        String title,
        String emoji,
        String coverUrl,
        String platform,
        String genre
) {
    public static CatalogGameSummaryResponse from(GameCatalog c) {
        return new CatalogGameSummaryResponse(c.getId(), c.getTitle(), c.getEmoji(), c.getCoverUrl(), c.getPlatform(), c.getGenre());
    }
}
