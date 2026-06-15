package com.gamevault.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.gamevault.dto.FreeGameResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches the games currently free on the Epic Games Store, using Epic's public
 * (unauthenticated) freeGamesPromotions feed. Results are cached in-memory for an hour
 * so the Discover page doesn't hit Epic on every request.
 */
@Service
public class EpicFreeGamesService {

    private static final Logger log = LoggerFactory.getLogger(EpicFreeGamesService.class);
    private static final String URL =
            "https://store-site-backend-static.ak.epicgames.com/freeGamesPromotions?locale=pt-PT&country=PT&allowCountries=PT";
    private static final long CACHE_TTL_MS = 60 * 60 * 1000;

    private final RestClient client = RestClient.create();

    private List<FreeGameResponse> cache = List.of();
    private long cachedAt = 0;

    public synchronized List<FreeGameResponse> getCurrentlyFree() {
        long now = System.currentTimeMillis();
        if (now - cachedAt < CACHE_TTL_MS && !cache.isEmpty()) return cache;
        try {
            JsonNode root = client.get().uri(URL)
                    .header("User-Agent", "Mozilla/5.0")
                    .retrieve().body(JsonNode.class);
            List<FreeGameResponse> result = parse(root, now);
            if (!result.isEmpty()) { cache = result; cachedAt = now; }
            return result;
        } catch (Exception e) {
            log.debug("Epic free games fetch failed: {}", e.getMessage());
            return cache; // serve stale on failure
        }
    }

    private List<FreeGameResponse> parse(JsonNode root, long now) {
        List<FreeGameResponse> out = new ArrayList<>();
        if (root == null) return out;
        JsonNode elements = root.path("data").path("Catalog").path("searchStore").path("elements");
        if (!elements.isArray()) return out;

        for (JsonNode el : elements) {
            // find an active promotional offer that makes the game free (discountPercentage == 0)
            Long freeUntil = activeFreeUntil(el, now);
            if (freeUntil == null) continue;

            String title = el.path("title").asText(null);
            if (title == null || title.isBlank()) continue;

            String original = el.path("price").path("totalPrice").path("fmtPrice").path("originalPrice").asText(null);
            String currency = el.path("price").path("totalPrice").path("currencyCode").asText(null);
            String cover = pickImage(el.path("keyImages"));
            String slug = pickSlug(el);
            String storeUrl = slug != null ? "https://store.epicgames.com/p/" + slug : "https://store.epicgames.com/free-games";

            out.add(new FreeGameResponse(title, cover, original, currency, freeUntil, storeUrl, "Epic Games"));
        }
        return out;
    }

    /** Returns the end timestamp of an active "free" promo for this element, or null if not currently free. */
    private Long activeFreeUntil(JsonNode el, long now) {
        JsonNode groups = el.path("promotions").path("promotionalOffers");
        if (!groups.isArray()) return null;
        for (JsonNode group : groups) {
            for (JsonNode offer : group.path("promotionalOffers")) {
                long start = parseDate(offer.path("startDate").asText(null));
                long end   = parseDate(offer.path("endDate").asText(null));
                int pct    = offer.path("discountSetting").path("discountPercentage").asInt(100);
                // Epic encodes a free game as discountPercentage 0 (price becomes 0)
                if (pct == 0 && start <= now && now < end) return end;
            }
        }
        return null;
    }

    private long parseDate(String iso) {
        if (iso == null) return 0;
        try { return Instant.parse(iso).toEpochMilli(); }
        catch (DateTimeParseException e) { return 0; }
    }

    private String pickImage(JsonNode keyImages) {
        if (!keyImages.isArray()) return null;
        String[] preferred = { "OfferImageWide", "DieselStoreFrontWide", "Thumbnail", "OfferImageTall", "DieselStoreFrontTall" };
        for (String type : preferred) {
            for (JsonNode img : keyImages) {
                if (type.equals(img.path("type").asText())) return img.path("url").asText(null);
            }
        }
        return keyImages.size() > 0 ? keyImages.get(0).path("url").asText(null) : null;
    }

    private String pickSlug(JsonNode el) {
        JsonNode mappings = el.path("catalogNs").path("mappings");
        if (mappings.isArray() && mappings.size() > 0) {
            String s = mappings.get(0).path("pageSlug").asText(null);
            if (s != null && !s.isBlank()) return s;
        }
        for (String field : new String[]{ "productSlug", "urlSlug" }) {
            String s = el.path(field).asText(null);
            if (s != null && !s.isBlank() && !"[]".equals(s)) return s.replace("/home", "");
        }
        JsonNode offerMappings = el.path("offerMappings");
        if (offerMappings.isArray() && offerMappings.size() > 0) {
            return offerMappings.get(0).path("pageSlug").asText(null);
        }
        return null;
    }
}
