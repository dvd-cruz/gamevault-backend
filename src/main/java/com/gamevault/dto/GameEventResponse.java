package com.gamevault.dto;

import com.gamevault.model.GameEvent;

import java.util.List;

public record GameEventResponse(
        Long id,
        String hostUsername,
        String hostName,
        String hostAvatar,
        Long gameId,
        String gameTitle,
        String gameCoverUrl,
        String note,
        Long scheduledAt,
        String visibility,
        String platform,
        String mode,
        Long createdAt,
        List<Participant> participants,
        boolean joinedByMe,
        boolean mine
) {
    public record Participant(String username, String name, String avatar) {}

    public static GameEventResponse from(GameEvent e, Long viewerId) {
        return new GameEventResponse(
                e.getId(),
                e.getHost().getUsername(),
                e.getHost().getName(),
                e.getHost().getAvatar(),
                e.getCatalogGame().getId(),
                e.getCatalogGame().getTitle(),
                e.getCatalogGame().getCoverUrl(),
                e.getNote(),
                e.getScheduledAt(),
                e.getVisibility(),
                e.getPlatform(),
                e.getMode(),
                e.getCreatedAt(),
                e.getParticipants().stream()
                        .map(p -> new Participant(p.getUser().getUsername(), p.getUser().getName(), p.getUser().getAvatar()))
                        .toList(),
                e.getParticipants().stream().anyMatch(p -> p.getUser().getId().equals(viewerId)),
                e.getHost().getId().equals(viewerId)
        );
    }
}
