package com.gamevault.dto;

import com.gamevault.service.SteamService;

/**
 * Current Steam Store price for a catalog game's linked Steam App ID.
 * `available=false` means there's no Steam App ID configured or the lookup failed;
 * `free=true` means the app is listed but has no purchase price (e.g. free-to-play).
 */
public record SteamPriceResponse(boolean available, boolean free, String currency,
                                  Double initial, Double finalPrice, Integer discountPercent,
                                  String storeUrl) {

    public static SteamPriceResponse unavailable() {
        return new SteamPriceResponse(false, false, null, null, null, null, null);
    }

    public static SteamPriceResponse from(SteamService.SteamPriceInfo info, Long appId) {
        String storeUrl = "https://store.steampowered.com/app/" + appId;
        if (info == null) return unavailable();
        if (info.free()) {
            return new SteamPriceResponse(true, true, null, null, null, null, storeUrl);
        }
        return new SteamPriceResponse(true, false, info.currency(), info.initial(), info.finalPrice(),
                info.discountPercent(), storeUrl);
    }
}
