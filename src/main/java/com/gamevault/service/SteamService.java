package com.gamevault.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Thin client for the Steam Web API (achievement schema + per-player unlock status) and the public Store API (pricing). */
@Service
public class SteamService {

    private final RestClient restClient;
    private final RestClient storeClient;
    private final String apiKey;

    public SteamService(@Value("${app.steam.api-base-url}") String baseUrl,
                        @Value("${app.steam.api-key}") String apiKey) {
        this.restClient = RestClient.create(baseUrl);
        this.storeClient = RestClient.create("https://store.steampowered.com/api");
        this.apiKey = apiKey;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** A single achievement definition for a game, as published by its developer. */
    public record SteamAchievementDef(String name, String displayName, String description, String icon) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SchemaAchievement(String name, String displayName, String description, String icon) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AvailableGameStats(List<SchemaAchievement> achievements) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SchemaGame(AvailableGameStats availableGameStats) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SchemaResponse(SchemaGame game) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PlayerAchievement(String apiname, Integer achieved, Long unlocktime) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PlayerStats(List<PlayerAchievement> achievements, Boolean success) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PlayerStatsResponse(PlayerStats playerstats) {}

    /** All achievement definitions for the given Steam appId. */
    public List<SteamAchievementDef> fetchAchievementSchema(Long appId) {
        if (!isConfigured()) throw new IllegalStateException("Steam API key não está configurada no servidor");
        SchemaResponse resp = restClient.get()
                .uri(uri -> uri.path("/ISteamUserStats/GetSchemaForGame/v2/")
                        .queryParam("key", apiKey)
                        .queryParam("appid", appId)
                        .build())
                .retrieve()
                .body(SchemaResponse.class);
        if (resp == null || resp.game() == null || resp.game().availableGameStats() == null
                || resp.game().availableGameStats().achievements() == null) {
            return List.of();
        }
        return resp.game().availableGameStats().achievements().stream()
                .map(a -> new SteamAchievementDef(a.name(), a.displayName(), a.description(), a.icon()))
                .toList();
    }

    /** Map of achievement apiName → unlock timestamp (epoch seconds) for achievements the player has unlocked. */
    public Map<String, Long> fetchUnlockedAchievements(String steamId, Long appId) {
        if (!isConfigured()) throw new IllegalStateException("Steam API key não está configurada no servidor");
        PlayerStatsResponse resp = restClient.get()
                .uri(uri -> uri.path("/ISteamUserStats/GetPlayerAchievements/v0001/")
                        .queryParam("key", apiKey)
                        .queryParam("steamid", steamId)
                        .queryParam("appid", appId)
                        .build())
                .retrieve()
                .body(PlayerStatsResponse.class);
        if (resp == null || resp.playerstats() == null || resp.playerstats().achievements() == null) {
            return Map.of();
        }
        Map<String, Long> unlocked = new HashMap<>();
        for (PlayerAchievement a : resp.playerstats().achievements()) {
            if (a.apiname() != null && a.achieved() != null && a.achieved() == 1) {
                unlocked.put(a.apiname(), a.unlocktime());
            }
        }
        return unlocked;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GlobalAchievement(String name, Double percent) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GlobalAchievements(List<GlobalAchievement> achievements) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GlobalAchievementsResponse(GlobalAchievements achievementpercentages) {}

    /**
     * Map of achievement apiName → percentage of all players who have unlocked it (e.g. 3.4),
     * as reported globally by Steam. Best-effort: returns an empty map if unavailable. This
     * endpoint is public and does not require an API key.
     */
    public Map<String, Double> fetchGlobalAchievementPercentages(Long appId) {
        try {
            GlobalAchievementsResponse resp = restClient.get()
                    .uri(uri -> uri.path("/ISteamUserStats/GetGlobalAchievementPercentagesForApp/v0002/")
                            .queryParam("gameid", appId)
                            .queryParam("format", "json")
                            .build())
                    .retrieve()
                    .body(GlobalAchievementsResponse.class);
            if (resp == null || resp.achievementpercentages() == null || resp.achievementpercentages().achievements() == null) {
                return Map.of();
            }
            Map<String, Double> result = new HashMap<>();
            for (GlobalAchievement a : resp.achievementpercentages().achievements()) {
                if (a.name() != null && a.percent() != null) result.put(a.name(), a.percent());
            }
            return result;
        } catch (Exception e) {
            return Map.of();
        }
    }

    /* ── Owned games (library import) ── */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OwnedGame(Long appid, String name, Long playtime_forever) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OwnedGames(List<OwnedGame> games) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OwnedGamesResponse(OwnedGames response) {}

    /** A game owned on Steam: appId, title, and total playtime in minutes. */
    public record SteamOwnedGame(Long appId, String name, long playtimeMinutes) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record VanityResult(String steamid, Integer success) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record VanityResponse(VanityResult response) {}

    /** Resolves a vanity URL name (e.g. "gabelogan") to a 64-bit steamId. Returns the input if already numeric. */
    public String resolveSteamId(String input) {
        if (input == null || input.isBlank()) throw new IllegalArgumentException("steamId em falta");
        String s = input.trim();
        if (s.matches("\\d{17}")) return s; // already a steamID64
        if (!isConfigured()) throw new IllegalStateException("Steam API key não está configurada no servidor");
        VanityResponse resp = restClient.get()
                .uri(uri -> uri.path("/ISteamUser/ResolveVanityURL/v0001/")
                        .queryParam("key", apiKey)
                        .queryParam("vanityurl", s)
                        .build())
                .retrieve()
                .body(VanityResponse.class);
        if (resp != null && resp.response() != null && Integer.valueOf(1).equals(resp.response().success())
                && resp.response().steamid() != null) {
            return resp.response().steamid();
        }
        throw new IllegalArgumentException("Não foi possível encontrar esse utilizador Steam");
    }

    /** All games owned by the given steamId (requires a public profile/game details). */
    public List<SteamOwnedGame> fetchOwnedGames(String steamId) {
        if (!isConfigured()) throw new IllegalStateException("Steam API key não está configurada no servidor");
        OwnedGamesResponse resp = restClient.get()
                .uri(uri -> uri.path("/IPlayerService/GetOwnedGames/v1/")
                        .queryParam("key", apiKey)
                        .queryParam("steamid", steamId)
                        .queryParam("include_appinfo", "true")
                        .queryParam("include_played_free_games", "true")
                        .queryParam("format", "json")
                        .build())
                .retrieve()
                .body(OwnedGamesResponse.class);
        if (resp == null || resp.response() == null || resp.response().games() == null) return List.of();
        return resp.response().games().stream()
                .filter(g -> g.appid() != null && g.name() != null && !g.name().isBlank())
                .map(g -> new SteamOwnedGame(g.appid(), g.name(), g.playtime_forever() != null ? g.playtime_forever() : 0L))
                .toList();
    }

    /**
     * Derives a PSN-style rarity tier (0=Ultra Rare, 1=Very Rare, 2=Rare, 3=Common) from a
     * Steam global unlock percentage, so Steam achievements can be displayed with the same
     * rarity scale as PSN trophies.
     */
    public static Integer rarityTierFromPercent(double percent) {
        if (percent < 5) return 0;
        if (percent < 15) return 1;
        if (percent < 40) return 2;
        return 3;
    }

    /** Current Steam Store price for the given appId, in euros (cc=pt), or null if unavailable. */
    public record SteamPriceInfo(String currency, double initial, double finalPrice, int discountPercent, boolean free) {}

    /**
     * Looks up the current price on the Steam Store for the given appId (using the public,
     * unauthenticated Store API — no key required). Returns null if the app has no listed
     * price (e.g. free-to-play) or the lookup fails.
     */
    public SteamPriceInfo fetchPriceOverview(Long appId) {
        JsonNode root = storeClient.get()
                .uri(uri -> uri.path("/appdetails")
                        .queryParam("appids", appId)
                        .queryParam("filters", "price_overview")
                        .queryParam("cc", "pt")
                        .build())
                .retrieve()
                .body(JsonNode.class);
        if (root == null) return null;
        JsonNode entry = root.get(String.valueOf(appId));
        if (entry == null || !entry.path("success").asBoolean(false)) return null;
        JsonNode data = entry.get("data");
        if (data == null || !data.isObject()) {
            // Steam returns an empty array for "data" when the app has no store price (e.g. free-to-play)
            return new SteamPriceInfo(null, 0, 0, 0, true);
        }
        JsonNode po = data.get("price_overview");
        if (po == null) {
            return new SteamPriceInfo(null, 0, 0, 0, true);
        }
        return new SteamPriceInfo(
                po.path("currency").asText(null),
                po.path("initial").asDouble(0) / 100.0,
                po.path("final").asDouble(0) / 100.0,
                po.path("discount_percent").asInt(0),
                false
        );
    }

    /** Basic info about a single DLC, as listed on the Steam Store. */
    public record SteamDlcInfo(Long appId, String name, String headerImage, String currency,
                               Double initial, Double finalPrice, Integer discountPercent, boolean free) {}

    /**
     * Looks up the DLCs published for the given base-game appId. First fetches the base app's
     * "dlc" appid list, then resolves each one's name/cover/price from the Store API.
     * Returns an empty list if the base game has no DLCs or the lookup fails.
     */
    public List<SteamDlcInfo> fetchDlcs(Long appId) {
        JsonNode root = storeClient.get()
                .uri(uri -> uri.path("/appdetails")
                        .queryParam("appids", appId)
                        .queryParam("filters", "basic")
                        .queryParam("cc", "pt")
                        .build())
                .retrieve()
                .body(JsonNode.class);
        if (root == null) return List.of();
        JsonNode entry = root.get(String.valueOf(appId));
        if (entry == null || !entry.path("success").asBoolean(false)) return List.of();
        JsonNode dlcIds = entry.path("data").path("dlc");
        if (!dlcIds.isArray() || dlcIds.isEmpty()) return List.of();

        List<SteamDlcInfo> result = new java.util.ArrayList<>();
        for (JsonNode idNode : dlcIds) {
            long dlcAppId = idNode.asLong();
            try {
                JsonNode dlcRoot = storeClient.get()
                        .uri(uri -> uri.path("/appdetails")
                                .queryParam("appids", dlcAppId)
                                .queryParam("filters", "basic,price_overview")
                                .queryParam("cc", "pt")
                                .build())
                        .retrieve()
                        .body(JsonNode.class);
                if (dlcRoot == null) continue;
                JsonNode dlcEntry = dlcRoot.get(String.valueOf(dlcAppId));
                if (dlcEntry == null || !dlcEntry.path("success").asBoolean(false)) continue;
                JsonNode data = dlcEntry.get("data");
                if (data == null || !data.isObject()) continue;

                String name = data.path("name").asText(null);
                String headerImage = data.path("header_image").asText(null);
                JsonNode po = data.get("price_overview");
                if (po != null) {
                    result.add(new SteamDlcInfo(dlcAppId, name, headerImage,
                            po.path("currency").asText(null),
                            po.path("initial").asDouble(0) / 100.0,
                            po.path("final").asDouble(0) / 100.0,
                            po.path("discount_percent").asInt(0),
                            false));
                } else {
                    result.add(new SteamDlcInfo(dlcAppId, name, headerImage, null, null, null, null, true));
                }
            } catch (Exception e) {
                // skip this DLC if its lookup fails — keep going with the rest
            }
        }
        return result;
    }
}
