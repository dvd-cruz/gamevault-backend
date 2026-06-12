package com.gamevault.dto;

import com.gamevault.model.Franchise;

import java.util.List;

public record FranchiseResponse(
        Long id,
        String name,
        String iconUrl,
        boolean favorite,
        List<CatalogGameSummaryResponse> games
) {
    public static FranchiseResponse from(Franchise f, boolean favorite) {
        return new FranchiseResponse(
                f.getId(),
                f.getName(),
                f.getIconUrl(),
                favorite,
                f.getGames().stream().map(CatalogGameSummaryResponse::from).toList()
        );
    }

    /** Lightweight summary used when listing franchises without their full game list. */
    public static FranchiseResponse summary(Franchise f, boolean favorite) {
        return new FranchiseResponse(f.getId(), f.getName(), f.getIconUrl(), favorite, null);
    }
}
