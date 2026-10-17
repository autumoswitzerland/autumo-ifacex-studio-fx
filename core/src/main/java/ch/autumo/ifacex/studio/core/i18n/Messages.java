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
 * resource file, and a translation file only replaces texts that were actually translated.
 * The one exception are the <b>metadata-driven parameter labels</b>: their message id is the
 * raw configuration key (e.g. {@code data_path}) with the derived English label as the
 * fallback — see {@link #t(String, String)}. The application does <b>not</b> follow the
 * operating system locale: earlier builds showed German framework texts because autumo-ui
 * resolves its bundle with the JVM default locale, which on a {@code de_CH} machine is
 * German.</p>
 *
 * <p>Pick a language with {@link #setLanguage(String)}; it loads
 * {@code lang/messages.<lang>.po} from the classpath and <em>also</em> sets the JVM default
 * locale and the {@code user.language} system property, so especially autumo-ui framework
 * resources follow the choice: the resource bundle for the window title and the
 * {@code html/about_<lang>.html} files of the About pane. See docs/12-i18n.md.</p>
 */
public final class Messages {

    /** The source language of the application; also the fallback for missing translations. */
    public static final String LANG_EN = "en";

    /** The primary translation shipped with the application. */
    public static final String LANG_DE = "de";

    /** Languages with a PO file in {@code lang/}. Extend when another PO is added. */
    public static final String[] LANGUAGES = {LANG_EN, LANG_DE};

    private static final String BASE = "lang/messages";

    private static Locale locale = Locale.US;
    private static Map<String, String> table = Map.of();

    private Messages() {
    }

    /**
     * Switches the application language.
     *
     * <p>A region is ignored ({@code de_CH} and {@code de-CH} both select the German PO file),
     * matching autumo-ui's {@code isSupportedLang(…)}; blank or unknown values fall back to
     * English.</p>
     *
     * @param lang language code, e.g. {@code en} or {@code de}
     * @return number of loaded translations (0 for English / no PO file)
     */
    public static synchronized int setLanguage(String lang) {
        final String code = languageCode(lang);
        locale = code.equals(LANG_EN) ? Locale.US : Locale.forLanguageTag(code);
        table = code.equals(LANG_EN) ? Map.of() : load(code);
        // Framework bundles (autumo-ui) resolve through the JVM default locale; make them
        // follow the application language instead of the operating system's.
        Locale.setDefault(locale);
        // autumo-ui picks HTML resources (e.g. the About pane) by the "user.language" system
        // property; keep it in sync with the chosen language so java code is not duplicated per
        // language.
        System.setProperty("user.language", locale.getLanguage());
        return table.size();
    }

    /**
     * Reduces a language specification to the plain ISO 639-1 code used for the PO file lookup:
     * blanks become English, regions are dropped ({@code de_CH} → {@code de}).
     *
     * @param lang language specification, may be {@code null}
     * @return language code, never blank
     */
    private static String languageCode(String lang) {
        if (lang == null || lang.isBlank()) {
            return LANG_EN;
        }
        final String code = lang.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        final int region = code.indexOf('-');
        return region > 0 ? code.substring(0, region) : code;
    }

    /**
     * The active language code, e.g. {@code en} or {@code de}.
     *
     * <p>This is always a plain ISO 639-1 code — the same value {@link #setLanguage(String)}
     * accepts — so it can be compared and passed around without normalisation. (The
     * underlying locale may be more specific, e.g. {@code en-US}, for text formatting.)</p>
     */
    public static String getLanguage() {
        return locale.getLanguage();
    }

    /** Translates {@code id}; returns {@code id} itself when it is not translated. */
    public static String t(String id) {
        return t(id, id);
    }

    /**
     * Translates {@code id} and falls back to {@code english} when there is no translation.
     *
     * <p>This is what the metadata-driven parameter labels use: their <b>message id is the raw
     * configuration key</b> (e.g. {@code data_path}) and the fallback is the English label the
     * code derives from that key ({@code "Data path"}). A translation file can then translate
     * the parameter by its key, and everything that is not translated stays as it is.</p>
     */
    public static String t(String id, String english) {
        final String translated = table.get(PoFile.key(null, id));
        return translated == null || translated.isEmpty() ? english : translated;
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
