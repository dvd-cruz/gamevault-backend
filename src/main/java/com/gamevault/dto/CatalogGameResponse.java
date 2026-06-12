package com.gamevault.dto;

import com.gamevault.model.GameCatalog;

import java.util.List;

public record CatalogGameResponse(
        Long id,
        String title,
        String emoji,
        String coverUrl,
        String heroImageUrl,
        Integer heroImagePositionY,
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
        List<TrophyResponse> trophies,
        Double communityRating,
        Integer communityRatingCount,
        Double avgCompletionHours,
        Integer avgCompletionHoursCount,
        Integer playerCount,
        Integer completedCount,
        List<FranchiseResponse> franchises
) {
    public static CatalogGameResponse from(GameCatalog c) {
        return from(c, null);
    }

    public static CatalogGameResponse from(GameCatalog c, java.util.List<com.gamevault.model.Game> libraryEntries) {
        Double communityRating = null;
        Integer communityRatingCount = null;
        Double avgCompletionHours = null;
        Integer avgCompletionHoursCount = null;
        Integer playerCount = null;
        Integer completedCount = null;

        if (libraryEntries != null) {
            playerCount = libraryEntries.size();
            completedCount = (int) libraryEntries.stream().filter(g -> "completed".equals(g.getStatus())).count();
            java.util.List<Integer> ratings = libraryEntries.stream()
                    .map(com.gamevault.model.Game::getRating)
                    .filter(r -> r != null && r > 0)
                    .toList();
            if (!ratings.isEmpty()) {
                communityRatingCount = ratings.size();
                communityRating = ratings.stream().mapToInt(Integer::intValue).average().orElse(0);
            }

            java.util.List<Double> hours = libraryEntries.stream()
                    .filter(g -> "completed".equals(g.getStatus()))
                    .map(com.gamevault.model.Game::getHours)
                    .filter(h -> h != null && h > 0)
                    .toList();
            if (!hours.isEmpty()) {
                avgCompletionHoursCount = hours.size();
                avgCompletionHours = hours.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            }
        }

        return new CatalogGameResponse(
                c.getId(),
                c.getTitle(),
                c.getEmoji(),
                c.getCoverUrl(),
                c.getHeroImageUrl(),
                c.getHeroImagePositionY(),
                c.getPlatform(),
                c.getGenre(),
                c.getReleaseDate(),
                c.getDeveloper(),
                c.getPublisher(),
                c.getDescription(),
                c.getDifficulties(),
                c.isHasDlc(),
                c.getSteamAppId(),
                c.getPsnNpCommunicationId(),
                c.getPsnProductId(),
                c.getTrophies().stream().map(TrophyResponse::from).toList(),
                communityRating,
                communityRatingCount,
                avgCompletionHours,
                avgCompletionHoursCount,
                playerCount,
                completedCount,
                c.getFranchises().stream().map(f -> FranchiseResponse.summary(f, false)).toList()
        );
    }
}
