package com.gamevault.dto;

import com.gamevault.model.User;

public record UserResponse(
        Long id,
        String name,
        String username,
        String avatar,
        String bio,
        String location,
        Long joinedAt,
        boolean privateProfile
) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getUsername(),
                u.getAvatar(), u.getBio(), u.getLocation(), u.getJoinedAt(), u.isPrivateProfile());
    }
}
