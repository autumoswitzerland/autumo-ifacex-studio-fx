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
package ch.autumo.ifacex.studio.ui;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;

import ch.autumo.ui.utils.Helper;
import ch.autumo.ui.utils.PoFile;

/**
 * Thin i18n facade of the Studio on top of the <b>autumo-ui</b> resource bundle.
 *
 * <p>The catalogue itself lives in autumo-ui: {@code Helper.getMessageBundle(…)} resolves the
 * Studio's gettext PO files ({@code lang/messages.<lang>.po}, see
 * {@link ch.autumo.ui.utils.PoResourceBundle}) first and falls back to the framework bundle for
 * the framework's own keys. This class therefore only adds the Studio's conventions on top of
 * that lookup:</p>
 *
 * <ul>
 *   <li>the message id of an untranslated text is <b>the English text itself</b> (gettext
 *       convention), so {@link #t(String)} falls back to the id;</li>
 *   <li>{@link #t(String, String)} lets metadata-driven texts (parameter labels, help texts) be
 *       translated by their raw configuration key while the English label stays the fallback;</li>
 *   <li>{@link #format(String, Object...)} applies {@link MessageFormat} and survives a malformed
 *       pattern;</li>
 *   <li>{@link #setLanguage(String)} selects the language and reports how many entries the
 *       catalogue carries.</li>
 * </ul>
 *
 * <p>The Studio intentionally does <b>not</b> follow the operating system locale. See
 * docs/12-i18n.md.</p>
 */
public final class Messages {

    /** The source language of the application; also the fallback for missing translations. */
    public static final String LANG_EN = "en";

    /** The primary translation shipped with the application. */
    public static final String LANG_DE = "de";

    /** Languages with a PO catalogue in {@code lang/}. Extend when another PO is added. */
    public static final String[] LANGUAGES = {LANG_EN, LANG_DE};

    /** Base resource path of the Studio UI; must match {@link StudioApp#getBaseResourcePath()}. */
    private static final String BASE = "/ch/autumo/ifacex/studio/ui/";

    /** Classpath location of the PO catalogue, used to count the entries of a language. */
    private static final String PO_RESOURCE = "ch/autumo/ifacex/studio/ui/lang/messages.";

    private Messages() {
    }

    /**
     * Switches the application language via the framework.
     *
     * <p>A region is ignored ({@code de_CH} and {@code de-CH} both select German) and unknown or
     * blank values fall back to English, exactly like {@link Helper#setLanguage(String)}. The
     * framework also moves the JVM default locale and the {@code user.language} system property,
     * so framework bundles and the per-language About HTML follow the choice.</p>
     *
     * @param lang language code, e.g. {@code en} or {@code de}
     * @return number of translations in the catalogue (0 for English apart from the framework-key
     *         overrides)
     */
    public static int setLanguage(String lang) {
        Helper.setLanguage(lang);
        return size();
    }

    /**
     * The active language code, e.g. {@code en} or {@code de}.
     *
     * @return plain ISO 639-1 language code
     */
    public static String getLanguage() {
        return Helper.getLanguage();
    }

    /**
     * Translates {@code id} and returns {@code id} itself when it is not translated.
     *
     * @param id message id, which is the English text
     * @return translated text or the id
     */
    public static String t(String id) {
        return t(id, id);
    }

    /**
     * Translates {@code id} and falls back to {@code english} when there is no translation.
     *
     * <p>This is what the metadata-driven parameter labels use: their <b>message id is the raw
     * configuration key</b> (e.g. {@code data_path}) and the fallback is the English label the code
     * derives from that key ({@code "Data path"}). A catalogue can then translate the parameter by
     * its key, and everything that is not translated stays as it is.</p>
     *
     * @param id message id, e.g. the raw configuration key
     * @param english English fallback text
     * @return translated text or {@code english}
     */
    public static String t(String id, String english) {
        final String translated = lookup(id);
        return translated == null || translated.isEmpty() ? english : translated;
    }

    /**
     * Translates {@code id} and formats it with {@link MessageFormat} ({@code {0}}, {@code {1}}, …).
     *
     * @param id message id
     * @param args {@link MessageFormat} arguments
     * @return formatted text; the raw pattern when it is malformed
     */
    public static String format(String id, Object... args) {
        final String pattern = t(id);
        try {
            return MessageFormat.format(pattern, args);
        } catch (RuntimeException e) {
            // A malformed pattern must never break the UI; show the untranslated pattern.
            return pattern;
        }
    }

    /**
     * Number of entries the PO catalogue of the active language carries.
     *
     * <p>English is the source language and ships only the framework-key overrides, so a value of
     * 0 means "no catalogue at all" (unknown language).</p>
     *
     * @return number of translated messages
     */
    public static int size() {
        return poEntries(Helper.getLanguage()).size();
    }

    /**
     * Looks a message id up in the framework bundle.
     *
     * @param id message id
     * @return translated text or {@code null} when the key is unknown
     */
    private static String lookup(String id) {
        final java.util.ResourceBundle bundle = Helper.getMessageBundle(BASE);
        return bundle != null && bundle.containsKey(id) ? bundle.getString(id) : null;
    }

    /**
     * Reads the PO catalogue of a language from the classpath.
     *
     * @param lang language code
     * @return translation table; empty when the file is missing or unreadable
     */
    private static Map<String, String> poEntries(String lang) {
        final String resource = PO_RESOURCE + lang + ".po";
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
