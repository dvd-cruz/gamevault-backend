package com.gamevault.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Client for the unofficial PlayStation Network trophy API v2.
 *
 * Auth flow (from andshrew.github.io/PlayStation-Trophies/#/APIv2):
 *   1. User provides NPSSO token (from ca.account.sony.com/api/v1/ssocookie)
 *   2. GET  /api/authz/v3/oauth/authorize  → 302 → Location contains ?code=
 *   3. POST /api/authz/v3/oauth/token      → { access_token }
 *   4. Use Bearer token on m.np.playstation.com trophy endpoints
 */
@Service
public class PsnService {

    private static final String AUTH_BASE   = "https://ca.account.sony.com";
    private static final String TROPHY_BASE = "https://m.np.playstation.com";

    // Credentials from the official psn-api documentation (andshrew)
    private static final String CLIENT_ID    = "09515159-7237-4370-9b40-3806e67c0891";
    private static final String REDIRECT_URI = "com.scee.psxandroid.scecompcall://redirect";
    // Basic auth = base64(CLIENT_ID:CLIENT_SECRET)
    private static final String BASIC_AUTH   = "Basic MDk1MTUxNTktNzIzNy00MzcwLTliNDAtMzgwNmU2N2MwODkxOnVjUGprYTV0bnRCMktxc1A=";

    private final RestClient  trophyClient;
    private final HttpClient  httpClient;
    private final ObjectMapper objectMapper;

    public PsnService() {
        this.trophyClient = RestClient.create(TROPHY_BASE);
        this.httpClient   = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    // ── DTOs ────────────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TrophyTitleListResponse(
            @JsonProperty("trophyTitles")  List<TrophyTitleEntry> trophyTitles,
            @JsonProperty("nextOffset")    Integer nextOffset,
            @JsonProperty("totalItemCount") Integer totalItemCount
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TrophyTitleEntry(
            @JsonProperty("npCommunicationId") String npCommunicationId,
            @JsonProperty("trophyTitleName")   String trophyTitleName,
            @JsonProperty("npServiceName")     String npServiceName
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TrophyListResponse(
            @JsonProperty("trophies") List<PsnTrophy> trophies
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PsnTrophy(
            @JsonProperty("trophyId")          Integer trophyId,
            @JsonProperty("trophyName")        String  trophyName,
            @JsonProperty("trophyDetail")      String  trophyDetail,
            @JsonProperty("trophyIconUrl")     String  trophyIconUrl,
            @JsonProperty("trophyType")        String  trophyType,
            @JsonProperty("trophyHidden")      Boolean trophyHidden,
            // rarity fields (returned in definitions response):
            @JsonProperty("trophyEarnedRate")  String  trophyEarnedRate,
            @JsonProperty("trophyRare")        Integer trophyRare,
            // earned-trophies response fields:
            @JsonProperty("earned")            Boolean earned,
            @JsonProperty("earnedDateTime")    String  earnedDateTime
    ) {}

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Exchanges an NPSSO token for a PSN access token.
     *
     * Step 1: GET authorize → 302 redirect with ?code=
     * Step 2: POST token    → { access_token }
     */
    public String exchangeNpssoForAccessToken(String npsso) {
        String authCode = getAuthCode(npsso);
        return getAccessToken(authCode);
    }

    private String getAuthCode(String npsso) {
        try {
            String url = AUTH_BASE + "/api/authz/v3/oauth/authorize"
                    + "?access_type=offline"
                    + "&client_id=" + CLIENT_ID
                    + "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8)
                    + "&response_type=code"
                    + "&scope=" + URLEncoder.encode("psn:mobile.v2.core psn:clientapp", StandardCharsets.UTF_8);

            HttpResponse<String> resp = httpClient.send(
                    HttpRequest.newBuilder()
                            .uri(new URI(url))
                            .header("Cookie", "npsso=" + npsso)
                            .GET().build(),
                    HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 302)
                throw new RuntimeException("Esperava redirect 302, recebeu " + resp.statusCode()
                        + ". NPSSO inválido ou expirado? Resposta: " + resp.body());

            String location = resp.headers().firstValue("location").orElse(null);
            if (location == null)
                throw new RuntimeException("Redirect sem Location header");

            // Location: com.scee.psxandroid.scecompcall://redirect?code=...
            int q = location.indexOf('?');
            String query = q >= 0 ? location.substring(q + 1) : "";
            String code = extractParam(query, "code");
            if (code == null) {
                String errDesc = extractParam(query, "error_description");
                throw new RuntimeException(errDesc != null ? errDesc : "Código de autorização não encontrado no redirect");
            }
            return code;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter authorization code PSN: " + e.getMessage());
        }
    }

    private String getAccessToken(String authCode) {
        try {
            String body = "grant_type=authorization_code"
                    + "&code=" + URLEncoder.encode(authCode, StandardCharsets.UTF_8)
                    + "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8)
                    + "&token_format=jwt";

            HttpResponse<String> resp = httpClient.send(
                    HttpRequest.newBuilder()
                            .uri(new URI(AUTH_BASE + "/api/authz/v3/oauth/token"))
                            .header(HttpHeaders.AUTHORIZATION, BASIC_AUTH)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                            .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                    HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200)
                throw new RuntimeException("Erro ao obter access token (" + resp.statusCode() + "): " + resp.body());

            String token = objectMapper.readTree(resp.body()).path("access_token").asText(null);
            if (token == null || token.isEmpty())
                throw new RuntimeException("access_token não encontrado na resposta");
            return token;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter access token PSN: " + e.getMessage());
        }
    }

    /**
     * Returns the PSN account ID for the authenticated user.
     * Uses the trophy summary endpoint with "me" which returns accountId.
     */
    public String fetchAccountId(String accessToken) {
        try {
            var node = objectMapper.readTree(
                    trophyClient.get()
                            .uri("/api/trophy/v1/users/me/trophySummary")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .header("Accept-Language", "en-US")
                            .header("User-Agent", "PlayStation/21090100 CFNetwork/1126 Darwin/19.5.0")
                            .retrieve()
                            .body(String.class));
            String id = node.path("accountId").asText(null);
            if (id == null || id.isEmpty())
                throw new RuntimeException("accountId não encontrado");
            return id;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter account ID PSN: " + e.getMessage());
        }
    }

    /**
     * Fetches trophy definitions for the given NP Communication ID.
     * Tries trophy2 (PS5/PC) first, then trophy (PS3/PS4/Vita).
     * Uses trophyGroupId=all to get all groups in one call.
     */
    public List<PsnTrophy> fetchTrophyDefinitions(String accessToken, String npCommunicationId) {
        Exception lastError = null;
        for (String service : List.of("trophy2", "trophy")) {
            try {
                TrophyListResponse resp = trophyClient.get()
                        .uri(u -> u.path("/api/trophy/v1/npCommunicationIds/{id}/trophyGroups/all/trophies")
                                .queryParam("npServiceName", service)
                                .build(npCommunicationId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .header("Accept-Language", "en-US")
                        .header("User-Agent", "PlayStation/21090100 CFNetwork/1126 Darwin/19.5.0")
                        .retrieve()
                        .body(TrophyListResponse.class);
                if (resp != null && resp.trophies() != null && !resp.trophies().isEmpty())
                    return resp.trophies();
            } catch (Exception e) {
                lastError = e;
            }
        }
        if (lastError != null)
            throw new RuntimeException("Erro ao obter troféus PSN (trophy2 e trophy falharam): " + lastError.getMessage(), lastError);
        return List.of();
    }

    /**
     * Fetches which trophies the user has earned for a specific game.
     * Returns a list where each entry has trophyId + earned + earnedDateTime.
     */
    public List<PsnTrophy> fetchEarnedTrophies(String accessToken, String accountId, String npCommunicationId) {
        for (String service : List.of("trophy2", "trophy")) {
            try {
                TrophyListResponse resp = trophyClient.get()
                        .uri(u -> u.path("/api/trophy/v1/users/{uid}/npCommunicationIds/{id}/trophyGroups/all/trophies")
                                .queryParam("npServiceName", service)
                                .build(accountId, npCommunicationId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .header("Accept-Language", "en-US")
                        .header("User-Agent", "PlayStation/21090100 CFNetwork/1126 Darwin/19.5.0")
                        .retrieve()
                        .body(TrophyListResponse.class);
                if (resp != null && resp.trophies() != null && !resp.trophies().isEmpty())
                    return resp.trophies();
            } catch (Exception ignored) {}
        }
        return List.of();
    }

    /** Returns all trophy titles (games with trophies) for the authenticated user, paginated. */
    public List<TrophyTitleEntry> fetchTrophyTitles(String accessToken, String accountId) {
        List<TrophyTitleEntry> all = new ArrayList<>();
        int offset = 0;
        while (true) {
            final int off = offset;
            try {
                TrophyTitleListResponse resp = trophyClient.get()
                        .uri(u -> u.path("/api/trophy/v1/users/{uid}/trophyTitles")
                                .queryParam("limit", 100)
                                .queryParam("offset", off)
                                .build(accountId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .header("Accept-Language", "en-US")
                        .header("User-Agent", "PlayStation/21090100 CFNetwork/1126 Darwin/19.5.0")
                        .retrieve()
                        .body(TrophyTitleListResponse.class);
                if (resp == null || resp.trophyTitles() == null || resp.trophyTitles().isEmpty()) break;
                all.addAll(resp.trophyTitles());
                if (resp.nextOffset() == null || resp.trophyTitles().size() < 100) break;
                offset = resp.nextOffset();
            } catch (Exception e) { break; }
        }
        return all;
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static String extractParam(String query, String key) {
        if (query == null || query.isEmpty()) return null;
        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key))
                return URLDecoder(kv[1]);
        }
        return null;
    }

    private static String URLDecoder(String s) {
        try { return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8); }
        catch (Exception e) { return s; }
    }
}
