package com.gamevault.dto;

import com.gamevault.model.Activity;

import java.util.List;

public record ActivityResponse(
        Long id,
        String type,
        Long createdAt,

        String actorUsername,
        String actorName,
        String actorAvatar,

        Long gameId,
        String gameTitle,
        String gameEmoji,
        String gameCoverUrl,
        String gameGenre,
        String gamePlatform,
        String gameStatus,

        Integer count,
        Integer rating,
        Double hours,
        String message,
        String image,
        String reviewPlatform,

        long likeCount,
        boolean likedByMe,
        List<CommentResponse> comments,
        Long editedAt
) {
    public static ActivityResponse from(Activity a, long likeCount, boolean likedByMe, List<CommentResponse> comments) {
        var actor = a.getActor();
        var c = a.getCatalogGame();
        return new ActivityResponse(
                a.getId(),
                a.getType(),
                a.getCreatedAt(),
                actor.getUsername(),
                actor.getName(),
                actor.getAvatar(),
                c.getId(),
                c.getTitle(),
                c.getEmoji(),
                c.getCoverUrl(),
                c.getGenre(),
                c.getPlatform(),
                "added".equals(a.getType()) ? a.getMessage() : null,
                a.getCount(),
                a.getRating(),
                a.getHours(),
                a.getMessage(),
                a.getImage(),
                a.getPlatform(),
                likeCount,
                likedByMe,
                comments,
                a.getEditedAt()
        );
    }
}
