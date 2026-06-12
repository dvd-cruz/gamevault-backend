package com.gamevault.dto;

import com.gamevault.service.PsnPriceService;

/**
 * Current PlayStation Store price for a catalog game's linked PSN Product ID.
 * `available=false` means there's no PSN Product ID configured or the lookup failed;
 * `free=true` means the product is listed but currently has no purchase price (e.g. included in PS Plus).
 */
public record PsnPriceResponse(boolean available, boolean free, String currency,
                                Double initial, Double finalPrice, Integer discountPercent,
                                String storeUrl) {

    public static PsnPriceResponse unavailable() {
        return new PsnPriceResponse(false, false, null, null, null, null, null);
    }

    public static PsnPriceResponse from(PsnPriceService.PsnPriceInfo info, String conceptId) {
        if (info == null) return unavailable();
        String storeUrl = conceptId != null ? "https://store.playstation.com/concept/" + conceptId : null;
        if (info.free()) {
            return new PsnPriceResponse(true, true, null, null, null, null, storeUrl);
        }
        return new PsnPriceResponse(true, false, info.currency(), info.initial(), info.finalPrice(),
                info.discountPercent(), storeUrl);
    }
}
