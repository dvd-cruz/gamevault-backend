package com.gamevault.dto;

import com.gamevault.model.GroupPost;
import com.gamevault.model.UserGroup;

import java.util.Comparator;
import java.util.List;

public record GroupResponse(
        Long id,
        String name,
        String description,
        String ownerUsername,
        Long createdAt,
        boolean mine,
        List<Member> members,
        List<Post> posts
) {
    public record Member(String username, String name, String avatar) {}

    public record Post(Long id, String authorUsername, String authorName, String authorAvatar,
                       String message, Long createdAt, boolean mine) {
        public static Post from(GroupPost p, Long viewerId) {
            return new Post(
                    p.getId(),
                    p.getAuthor().getUsername(), p.getAuthor().getName(), p.getAuthor().getAvatar(),
                    p.getMessage(), p.getCreatedAt(),
                    p.getAuthor().getId().equals(viewerId)
            );
        }
    }

    /** Summary (group list): members but no posts. */
    public static GroupResponse summary(UserGroup g, Long viewerId) {
        return build(g, viewerId, false);
    }

    /** Full detail: members + posts (newest first). */
    public static GroupResponse full(UserGroup g, Long viewerId) {
        return build(g, viewerId, true);
    }

    private static GroupResponse build(UserGroup g, Long viewerId, boolean withPosts) {
        return new GroupResponse(
                g.getId(),
                g.getName(),
                g.getDescription(),
                g.getOwner().getUsername(),
                g.getCreatedAt(),
                g.getOwner().getId().equals(viewerId),
                g.getMembers().stream()
                        .sorted(Comparator.comparing(m -> m.getJoinedAt()))
                        .map(m -> new Member(m.getUser().getUsername(), m.getUser().getName(), m.getUser().getAvatar()))
                        .toList(),
                withPosts
                        ? g.getPosts().stream().map(p -> Post.from(p, viewerId)).toList()
                        : List.of()
        );
    }
}
