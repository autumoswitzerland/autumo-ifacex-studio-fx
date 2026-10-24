/**
 * Copyright 2026 autumo GmbH, Michael Gasche.
 * All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of autumo GmbH The intellectual and technical
 * concepts contained herein are proprietary to autumo GmbH
 * and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from autumo GmbH.
 */
package ch.autumo.ifacex.studio.core.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An encoded secret: the encryption mode and the encoded value.
 *
 * <p>The legacy format keeps an encoded secret inside a larger string, {@code {mode:value}} -
 * the engine reads it that way. The new format (see docs/08-config-format.md, 8.5) stores it as a
 * structured object instead, {@code { enc: 3, value: "..." }}. The model holds the structured
 * form; the properties writer converts it back for the engine.</p>
 *
 * @param mode  the encryption mode (1..4)
 * @param value the encoded value
 */
public record Secret(int mode, String value) {

    /** The legacy notation of an encoded secret: {@code {mode:value}}. */
    private static final Pattern LEGACY = Pattern.compile("^\\{(\\d+):(.*)\\}$");

    /**
     * Whether the text is an encoded secret in the legacy notation.
     *
     * @param text the text
     * @return {@code true} if the text is an encoded secret
     */
    public static boolean isLegacySecret(String text) {
        return text != null && LEGACY.matcher(text).matches();
    }

    /**
     * Parses an encoded secret in the legacy notation.
     *
     * @param text the text, e.g. {@code {3:abc==}}
     * @return the secret, or {@code null} if the text is not an encoded secret
     */
    public static Secret parseLegacy(String text) {
        if (text == null) {
            return null;
        }
        final Matcher matcher = LEGACY.matcher(text);
        if (!matcher.matches()) {
            return null;
        }
        try {
            return new Secret(Integer.parseInt(matcher.group(1)), matcher.group(2));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * The legacy notation of this secret: {@code {mode:value}} - the way the engine reads it.
     *
     * @return the text
     */
    public String toLegacy() {
        return "{" + this.mode + ":" + this.value + "}";
    }
}
