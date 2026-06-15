package com.gamevault.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.gamevault.dto.FreeGameResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds games temporarily free-to-keep on Steam (100% off for a limited time), using Steam's
 * public featuredcategories "specials" feed. Steam has no weekly giveaway like Epic, but
 * occasional 100%-off keep-forever promos show up here. Cached in-memory for an hour.
 */
@Service
public class SteamFreeGamesService {

    private static final Logger log = LoggerFactory.getLogger(SteamFreeGamesService.class);
    private static final String URL = "https://store.steampowered.com/api/featuredcategories?cc=pt&l=portuguese";
    private static final long CACHE_TTL_MS = 60 * 60 * 1000;

    private final RestClient client = RestClient.create();

    private List<FreeGameResponse> cache = List.of();
    private long cachedAt = 0;

    public synchronized List<FreeGameResponse> getCurrentlyFree() {
        long now = System.currentTimeMillis();
        if (now - cachedAt < CACHE_TTL_MS && cachedAt != 0) return cache;
        try {
            JsonNode root = client.get().uri(URL)
                    .header("User-Agent", "Mozilla/5.0")
                    .retrieve().body(JsonNode.class);
            List<FreeGameResponse> result = parse(root);
            cache = result; cachedAt = now;
            return result;
        } catch (Exception e) {
            log.debug("Steam free games fetch failed: {}", e.getMessage());
            return cache;
        }
    }

    private List<FreeGameResponse> parse(JsonNode root) {
        List<FreeGameResponse> out = new ArrayList<>();
        if (root == null) return out;
        JsonNode items = root.path("specials").path("items");
        if (!items.isArray()) return out;
        for (JsonNode it : items) {
            // free-to-keep = 100% off right now
            if (it.path("discount_percent").asInt(0) != 100) continue;
            long appId = it.path("id").asLong(0);
            String title = it.path("name").asText(null);
            if (appId == 0 || title == null) continue;

            int original = it.path("original_price").asInt(0); // cents
            String currency = it.path("currency").asText(null);
            String originalFmt = original > 0
                    ? String.format("%.2f%s", original / 100.0, "EUR".equals(currency) ? " €" : (currency != null ? " " + currency : ""))
                    : null;
            String cover = it.path("large_capsule_image").asText(null);
            if (cover == null) cover = it.path("header_image").asText(null);
            Long freeUntil = it.path("discount_expiration").isNumber()
                    ? it.path("discount_expiration").asLong() * 1000 : null;

            out.add(new FreeGameResponse(title, cover, originalFmt, currency, freeUntil,
                    "https://store.steampowered.com/app/" + appId, "Steam"));
        }
        return out;
    }
}
