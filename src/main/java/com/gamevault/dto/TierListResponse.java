package com.gamevault.dto;

import com.gamevault.model.TierList;
import com.gamevault.model.TierListEntry;

import java.util.List;

public record TierListResponse(
        Long id,
        String title,
        String ownerUsername,
        String ownerName,
        Long createdAt,
        Long updatedAt,
        List<Entry> entries
) {
    public record Entry(Long gameId, String gameTitle, String coverUrl, String tier, Integer position) {
        public static Entry from(TierListEntry e) {
            return new Entry(
                    e.getCatalogGame().getId(),
                    e.getCatalogGame().getTitle(),
                    e.getCatalogGame().getCoverUrl(),
                    e.getTier(),
                    e.getPosition()
            );
        }
    }

    public static TierListResponse from(TierList t) {
        return new TierListResponse(
                t.getId(),
                t.getTitle(),
                t.getOwner().getUsername(),
                t.getOwner().getName(),
                t.getCreatedAt(),
                t.getUpdatedAt(),
                t.getEntries().stream().map(Entry::from).toList()
        );
    }
}
