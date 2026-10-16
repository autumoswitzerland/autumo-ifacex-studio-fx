/*
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
 * 
 */
package ch.autumo.ifacex.studio.core.i18n;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;

/**
 * Message lookup on top of gettext PO files ({@link PoFile}).
 *
 * <p><b>English is the source language and the default.</b> Every message id is the English
 * text itself (the gettext convention), so the application is complete in English without any
 * resource file, and a translation file only replaces texts that were actually translated. The
 * application therefore does <b>not</b> follow the operating system locale: earlier builds
 * showed German framework texts because autumo-ui resolves its bundle with the JVM default
 * locale, which on a {@code de_CH} machine is German.</p>
 *
 * <p>Pick a language with {@link #setLanguage(String)}; it loads
 * {@code lang/messages.<lang>.po} from the classpath and <em>also</em> sets the JVM default
 * locale, so framework bundles (autumo-ui) follow the choice. See docs/12-i18n.md.</p>
 */
public final class Messages {

    /** The source language of the application; also the fallback for missing translations. */
    public static final String LANG_EN = "en";

    /** Languages with a PO file in {@code lang/}. Extend when another PO is added. */
    public static final String[] LANGUAGES = {LANG_EN, "de"};

    private static final String BASE = "lang/messages";

    private static Locale locale = Locale.US;
    private static Map<String, String> table = Map.of();

    private Messages() {
    }

    /**
     * Switches the application language.
     *
     * @param lang language code, e.g. {@code en} or {@code de}; blank or unknown falls back to
     *             English
     * @return number of loaded translations (0 for English / no PO file)
     */
    public static synchronized int setLanguage(String lang) {
        final String code = lang == null || lang.isBlank()
                ? LANG_EN : lang.trim().toLowerCase(Locale.ROOT);
        locale = code.equals(LANG_EN) ? Locale.US : Locale.forLanguageTag(code.replace('_', '-'));
        table = code.equals(LANG_EN) ? Map.of() : load(code);
        // Framework bundles (autumo-ui) resolve through the JVM default locale; make them
        // follow the application language instead of the operating system's.
        Locale.setDefault(locale);
        return table.size();
    }

    /** The active language code, e.g. {@code en} or {@code de}. */
    public static String getLanguage() {
        return locale.toLanguageTag();
    }

    /** Translates {@code id}; returns {@code id} itself when it is not translated. */
    public static String t(String id) {
        return t(id, null);
    }

    /**
     * Translates {@code id} in the given <em>context</em> (PO {@code msgctxt}). Contexts make
     * an ambiguous English source such as "New" translatable per use.
     */
    public static String t(String id, String context) {
        final String translated = table.get(PoFile.key(context, id));
        return translated == null || translated.isEmpty() ? id : translated;
    }

    /** Translates {@code id} and formats it with {@link MessageFormat} ({@code {0}}, {@code {1}}, …). */
    public static String format(String id, Object... args) {
        final String pattern = t(id);
        try {
            return MessageFormat.format(pattern, args);
        } catch (RuntimeException e) {
            // A malformed pattern must never break the UI; show the untranslated pattern.
            return pattern;
        }
    }

    /** Number of translations currently loaded (0 = pure English). */
    public static int size() {
        return table.size();
    }

    private static Map<String, String> load(String lang) {
        final String resource = BASE + "." + lang + ".po";
        try (InputStream in = Messages.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                return Map.of();
            }
            return PoFile.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            // A broken translation file degrades to English rather than to a crash.
            return Map.of();
        }
    }
}
