package com.gamevault.dto;

import com.gamevault.model.Comment;

public record CommentResponse(
        Long id,
        String text,
        Long createdAt,
        String authorUsername,
        String authorName,
        String authorAvatar,
        boolean mine
) {
    public static CommentResponse from(Comment c, Long viewerId) {
        var author = c.getActor();
        return new CommentResponse(
                c.getId(),
                c.getText(),
                c.getCreatedAt(),
                author.getUsername(),
                author.getName(),
                author.getAvatar(),
                viewerId != null && viewerId.equals(author.getId())
        );
    }
}
