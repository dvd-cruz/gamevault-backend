package com.gamevault.dto;

import com.gamevault.model.User;

public record MeResponse(
        Long id,
        String name,
        String username,
        String email,
        String avatar,
        String bio,
        String location,
        String steamId,
        Long joinedAt,
        boolean admin,
        boolean privateProfile,
        boolean psnLinked
) {
    public static MeResponse from(User u) {
        return new MeResponse(u.getId(), u.getName(), u.getUsername(), u.getEmail(),
                u.getAvatar(), u.getBio(), u.getLocation(), u.getSteamId(), u.getJoinedAt(), u.isAdmin(), u.isPrivateProfile(),
                u.getPsnAccountId() != null && !u.getPsnAccountId().isBlank());
    }
}
