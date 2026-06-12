package com.gamevault.dto;

import com.gamevault.model.GameList;
import com.gamevault.model.GameListItem;

import java.util.List;

public record GameListResponse(
        Long id,
        String slug,
        String title,
        String description,
        String authorUsername,
        String authorName,
        String authorAvatar,
        Long createdAt,
        List<ItemResponse> games
) {
    public static GameListResponse from(GameList l) {
        return new GameListResponse(
                l.getId(),
                l.getSlug(),
                l.getTitle(),
                l.getDescription(),
                l.getAuthor().getUsername(),
                l.getAuthor().getName(),
                l.getAuthor().getAvatar(),
                l.getCreatedAt(),
                l.getGames().stream().map(ItemResponse::from).toList()
        );
    }

    public record ItemResponse(Long id, String title, String emoji, String genre, String note, Integer position) {
        public static ItemResponse from(GameListItem i) {
            return new ItemResponse(i.getId(), i.getTitle(), i.getEmoji(), i.getGenre(), i.getNote(), i.getPosition());
        }
    }
}
