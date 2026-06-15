package com.gamevault.dto;

/** A game currently free (temporary giveaway) on a store, with its normal price and free-until date. */
public record FreeGameResponse(
        String title,
        String coverUrl,
        String originalPrice, // formatted, e.g. "19,99 €"
        String currency,
        Long freeUntil,       // epoch millis the promo ends
        String storeUrl,
        String store          // "Epic Games" or "Steam"
) {}
