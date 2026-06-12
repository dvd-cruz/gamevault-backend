package com.gamevault.dto;

import com.gamevault.service.SteamService;

/** A single DLC entry, as listed on the Steam Store for a catalog game's linked Steam App ID. */
public record SteamDlcResponse(Long appId, String name, String headerImage, String storeUrl,
                                boolean free, String currency, Double initial, Double finalPrice, Integer discountPercent) {

    public static SteamDlcResponse from(SteamService.SteamDlcInfo info) {
        String storeUrl = "https://store.steampowered.com/app/" + info.appId();
        if (info.free()) {
            return new SteamDlcResponse(info.appId(), info.name(), info.headerImage(), storeUrl, true, null, null, null, null);
        }
        return new SteamDlcResponse(info.appId(), info.name(), info.headerImage(), storeUrl, false,
                info.currency(), info.initial(), info.finalPrice(), info.discountPercent());
    }
}
