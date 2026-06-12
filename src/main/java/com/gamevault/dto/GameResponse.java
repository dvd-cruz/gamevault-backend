package com.gamevault.dto;

import com.gamevault.model.Game;
import com.gamevault.model.GameSession;
import com.gamevault.model.Playthrough;
import com.gamevault.model.Trophy;
import com.gamevault.model.UnlockedTrophy;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record GameResponse(
        Long id,
        String title,
        String emoji,
        String coverUrl,
        String heroImageUrl,
        Integer heroImagePositionY,
        String status,
        String platform,
        String genre,
        Double hours,
        Integer rating,
        String notes,
        String review,
        String reviewPlatform,
        boolean favorite,
        Integer progress,
        List<PlaythroughResponse> playthroughs,
        List<String> tags,
        List<GameTrophyResponse> trophies,
        Long addedAt,
        Long statusUpdatedAt,
        Double communityRating
) {
    public static GameResponse from(Game g) {
        return from(g, null);
    }

    public static GameResponse from(Game g, Double communityRating) {
        var c = g.getCatalogGame();

        Set<Long> unlockedTrophyIds = g.getUnlockedTrophies().stream()
                .map(ut -> ut.getTrophy().getId())
                .collect(Collectors.toSet());
        var unlockedAtByTrophyId = g.getUnlockedTrophies().stream()
                .collect(Collectors.toMap(ut -> ut.getTrophy().getId(), UnlockedTrophy::getUnlockedAt, (a, b) -> a));
        var markedAtByTrophyId = g.getUnlockedTrophies().stream()
                .collect(Collectors.toMap(ut -> ut.getTrophy().getId(), UnlockedTrophy::getMarkedAt, (a, b) -> a));
        var sourceByTrophyId = g.getUnlockedTrophies().stream()
                .collect(Collectors.toMap(ut -> ut.getTrophy().getId(), UnlockedTrophy::getSource, (a, b) -> a));

        List<GameTrophyResponse> trophies = c.getTrophies().stream()
                .map(t -> GameTrophyResponse.from(t, unlockedTrophyIds.contains(t.getId()), unlockedAtByTrophyId.get(t.getId()), markedAtByTrophyId.get(t.getId()), sourceByTrophyId.get(t.getId())))
                .toList();

        return new GameResponse(
                g.getId(),
                c.getTitle(),
                c.getEmoji(),
                c.getCoverUrl(),
                c.getHeroImageUrl(),
                c.getHeroImagePositionY(),
                g.getStatus(),
                c.getPlatform(),
                c.getGenre(),
                g.getHours(),
                g.getRating(),
                g.getNotes(),
                g.getReview(),
                g.getReviewPlatform(),
                g.isFavorite(),
                g.getProgress(),
                g.getPlaythroughs().stream().map(PlaythroughResponse::from).toList(),
                g.getTags(),
                trophies,
                g.getAddedAt(),
                g.getStatusUpdatedAt(),
                communityRating
        );
    }

    public record SessionResponse(Long id, String date, Double hours, String notes) {
        public static SessionResponse from(GameSession s) {
            return new SessionResponse(s.getId(), s.getDate(), s.getHours(), s.getNotes());
        }
    }

    public record PlaythroughResponse(Long id, String type, boolean speedrun, boolean modded, boolean completed, Double hours, String platform, String difficulty, String language, String notes, List<SessionResponse> sessions) {
        public static PlaythroughResponse from(Playthrough p) {
            return new PlaythroughResponse(p.getId(), p.getType(), p.isSpeedrun(), p.isModded(), p.isCompleted(), p.getHours(), p.getPlatform(), p.getDifficulty(), p.getLanguage(), p.getNotes(),
                    p.getSessions().stream().map(SessionResponse::from).toList());
        }
    }

    public record GameTrophyResponse(Long id, String name, String description, String iconUrl, String trophyType, boolean unlocked, Long unlockedAt, Long markedAt, Double earnedRate, Integer rarity, String source) {
        public static GameTrophyResponse from(Trophy t, boolean unlocked, Long unlockedAt) {
            return new GameTrophyResponse(t.getId(), t.getName(), t.getDescription(), t.getIconUrl(), t.getTrophyType(), unlocked, unlockedAt, unlockedAt, t.getEarnedRate(), t.getRarity(), unlocked ? "manual" : null);
        }
        public static GameTrophyResponse from(Trophy t, boolean unlocked, Long unlockedAt, Long markedAt) {
            return new GameTrophyResponse(t.getId(), t.getName(), t.getDescription(), t.getIconUrl(), t.getTrophyType(), unlocked, unlockedAt, markedAt, t.getEarnedRate(), t.getRarity(), unlocked ? "manual" : null);
        }
        public static GameTrophyResponse from(Trophy t, boolean unlocked, Long unlockedAt, Long markedAt, String source) {
            return new GameTrophyResponse(t.getId(), t.getName(), t.getDescription(), t.getIconUrl(), t.getTrophyType(), unlocked, unlockedAt, markedAt, t.getEarnedRate(), t.getRarity(), unlocked ? source : null);
        }
    }
}
