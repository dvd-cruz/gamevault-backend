package com.gamevault.dto;

import com.gamevault.model.Trophy;

public record TrophyResponse(Long id, String name, String description, String iconUrl, String trophyType, Double earnedRate, Integer rarity) {
    public static TrophyResponse from(Trophy t) {
        return new TrophyResponse(t.getId(), t.getName(), t.getDescription(), t.getIconUrl(), t.getTrophyType(), t.getEarnedRate(), t.getRarity());
    }
}
