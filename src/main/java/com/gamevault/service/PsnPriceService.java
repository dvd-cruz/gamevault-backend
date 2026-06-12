package com.gamevault.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

/**
 * Thin client for the public (unauthenticated) PlayStation Store GraphQL API, used to
 * look up the current Store price for a game given its PSN Product ID.
 *
 * Two persisted-query calls are needed:
 *  1. metGetProductById(productId)        -> resolves the product's concept ID
 *  2. metGetPricingDataByConceptId(conceptId) -> returns the current price for that concept
 */
@Service
public class PsnPriceService {

    private static final String GRAPHQL_URL = "https://web.np.playstation.com/api/graphql/v1/op";
    private static final String LOCALE = "pt-PT";

    // Persisted query SHA-256 hashes published by the PS Store web client.
    private static final String HASH_PRODUCT_BY_ID = "a128042177bd93dd831164103d53b73ef790d56f51dae647064cb8f9d9fc9d1a";
    private static final String HASH_PRICING_BY_CONCEPT_ID = "abcb311ea830e679fe2b697a27f755764535d825b24510ab1239a4ca3092bd09";

    private final RestClient restClient;

    public PsnPriceService() {
        this.restClient = RestClient.builder()
                .baseUrl(GRAPHQL_URL)
                .defaultHeader("x-psn-store-locale-override", LOCALE)
                .defaultHeader("content-type", "application/json")
                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36")
                .build();
    }

    public record PsnPriceInfo(String currency, double initial, double finalPrice, int discountPercent, boolean free) {}

    /** Builds a GraphQL "op" request URI, percent-encoding the JSON query params manually since
     *  Spring's UriComponentsBuilder would otherwise misinterpret the literal '{' / '}' chars
     *  in the JSON as URI template variables. */
    private URI buildUri(String operationName, String variablesJson, String sha256Hash) {
        String extensionsJson = "{\"persistedQuery\":{\"version\":1,\"sha256Hash\":\"" + sha256Hash + "\"}}";
        String query = "operationName=" + URLEncoder.encode(operationName, StandardCharsets.UTF_8)
                + "&variables=" + URLEncoder.encode(variablesJson, StandardCharsets.UTF_8)
                + "&extensions=" + URLEncoder.encode(extensionsJson, StandardCharsets.UTF_8);
        return URI.create(GRAPHQL_URL + "?" + query);
    }

    /** Resolves the PSN concept ID for the given Product ID, or null if it can't be found. */
    public String resolveConceptId(String productId) {
        JsonNode root = restClient.get()
                .uri(buildUri("metGetProductById", "{\"productId\":\"" + productId + "\"}", HASH_PRODUCT_BY_ID))
                .retrieve()
                .body(JsonNode.class);
        if (root == null) return null;
        JsonNode concept = root.path("data").path("productRetrieve").path("concept");
        if (!concept.has("id")) return null;
        return concept.get("id").asText(null);
    }

    /** Looks up the current PS Store price for the given concept ID. Returns null if unavailable. */
    public PsnPriceInfo fetchPriceByConceptId(String conceptId) {
        JsonNode root = restClient.get()
                .uri(buildUri("metGetPricingDataByConceptId", "{\"conceptId\":\"" + conceptId + "\"}", HASH_PRICING_BY_CONCEPT_ID))
                .retrieve()
                .body(JsonNode.class);
        if (root == null) return null;
        JsonNode price = root.path("data").path("conceptRetrieve").path("defaultProduct").path("price");
        if (!price.isObject()) return null;

        boolean isFree = price.path("isFree").asBoolean(false);
        if (isFree) return new PsnPriceInfo(null, 0, 0, 0, true);

        String currency = price.path("currencyCode").asText(null);
        double initial = price.path("basePriceValue").asDouble(0) / 100.0;
        double finalPrice = price.path("discountedValue").asDouble(0) / 100.0;
        int discountPercent = parseDiscountPercent(price.path("discountText").asText(null));
        return new PsnPriceInfo(currency, initial, finalPrice, discountPercent, false);
    }

    /** Resolves the concept ID and current price for the given PSN Product ID in one go. Returns null if unavailable. */
    public PsnPriceInfo fetchPriceByProductId(String productId) {
        String conceptId = resolveConceptId(productId);
        if (conceptId == null) return null;
        return fetchPriceByConceptId(conceptId);
    }

    private int parseDiscountPercent(String discountText) {
        if (discountText == null) return 0;
        String digits = discountText.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return 0;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
