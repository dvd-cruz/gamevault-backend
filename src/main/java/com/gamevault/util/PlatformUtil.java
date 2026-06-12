package com.gamevault.util;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Normalizes free-text platform lists into the canonical comma-separated form, e.g. "PC, PS5, Xbox Series X/S". */
public final class PlatformUtil {

    private PlatformUtil() {}

    /**
     * Splits on commas or slashes surrounded by whitespace (so "Xbox Series X/S" stays intact),
     * trims each entry, drops blanks/duplicates and re-joins with ", ".
     */
    public static String normalize(String platform) {
        if (platform == null || platform.isBlank()) return null;
        return Arrays.stream(platform.split("\\s*,\\s*|\\s+/\\s+"))
                .map(String::trim)
                .filter(p -> !p.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));
    }
}
