package com.gamevault.dto;

import com.gamevault.model.CatalogDlc;

/** A manually-registered DLC entry for a catalog game (used for non-Steam games). */
public record CatalogDlcResponse(Long id, String name, Double price, String storeUrl, String coverUrl, boolean manual) {

    public static CatalogDlcResponse from(CatalogDlc dlc) {
        return new CatalogDlcResponse(dlc.getId(), dlc.getName(), dlc.getPrice(), dlc.getStoreUrl(), dlc.getCoverUrl(), true);
    }
}
