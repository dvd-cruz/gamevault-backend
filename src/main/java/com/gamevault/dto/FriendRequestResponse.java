package com.gamevault.dto;

import com.gamevault.model.Friendship;

public record FriendRequestResponse(
        Long id,
        UserResponse fromUser,
        Long createdAt
) {
    public static FriendRequestResponse from(Friendship f) {
        return new FriendRequestResponse(f.getId(), UserResponse.from(f.getRequester()), f.getCreatedAt());
    }
}
