package com.gamevault.dto;

public record NotificationResponse(
        String id,
        String type,
        Long at,

        String actorUsername,
        String actorName,
        String actorAvatar,

        String message,

        Long activityId,
        Long gameId,
        String gameTitle,

        Long friendRequestId
) {
}
