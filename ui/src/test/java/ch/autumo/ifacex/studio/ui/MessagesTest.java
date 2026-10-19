/*
 * Copyright 2026 autumo GmbH.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package ch.autumo.ifacex.studio.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the Studio i18n facade against the shipped PO catalogue.
 *
 * <p>The lookup itself happens in autumo-ui ({@code Helper.getMessageBundle} + PO bundle); these
 * tests pin the Studio conventions on top of it: English is the source language (the msgid is the
 * English text), the fallback variant for metadata-driven texts, {@link java.text.MessageFormat}
 * formatting and the language switch.</p>
 */
class MessagesTest {

    private Locale defaultLocale;

    @BeforeEach
    void rememberLocale() {
        defaultLocale = Locale.getDefault();
    }

    @AfterEach
    void restore() {
        Messages.setLanguage(Messages.LANG_EN);
        Locale.setDefault(defaultLocale);
    }

    @Test
    void englishIsTheDefaultAndUsesTheSourceText() {
        Messages.setLanguage(null);
        // English needs no catalogue: the msgid already is the English text.
        assertEquals("File", Messages.t("File"));
        assertEquals("Add standard mapping", Messages.t("Add standard mapping"));
        // The only English catalogue entries are the framework keys whose id is not English text.
        assertEquals("ifaceX Studio FX", Messages.t("application.name"));
        assertEquals("About ifaceX Studio FX", Messages.t("About.title"));
    }

    @Test
    void unknownMessageFallsBackToItself() {
        Messages.setLanguage("en");
        assertEquals("Not a known message", Messages.t("Not a known message"));
    }

    @Test
    void mergesTheFrameworkCatalogueBelowTheApplicationOne() {
        // 'button.Alright' is a framework key only (autumo-ui dialogs); the Studio's catalogue does
        // not define it, so its value proves that the framework PO is merged in.
        Messages.setLanguage("en");
        assertEquals("Alright", Messages.t("button.Alright"));
        assertEquals("File", Messages.t("menu.File"));
        Messages.setLanguage("de");
        assertEquals("Okay", Messages.t("button.Alright"));
        assertEquals("Datei", Messages.t("menu.File"));
        // Both catalogues define 'application.name'; the application's entry wins.
        assertNotEquals("autumo UI", Messages.t("application.name"));
        assertEquals("ifaceX Studio FX", Messages.t("application.name"));
    }

    @Test
    void unknownKeyWithoutBundleEntryIsNotNull() {
        Messages.setLanguage("de");
        // A framework key nobody defines must not blow up the lookup.
        assertEquals("no.such.key.anywhere", Messages.t("no.such.key.anywhere"));
    }

    @Test
    void germanOverridesTheFrameworkKeys() {
        Messages.setLanguage("de");
        assertEquals("Über ifaceX Studio FX", Messages.t("About.title"));
        assertEquals("ifaceX Studio FX", Messages.t("application.name"));
    }

    @Test
    void loadsGermanTranslations() {
        final int loaded = Messages.setLanguage("de");
        assertTrue(loaded > 100, "the German PO should carry the whole UI text");
        assertEquals("Datei", Messages.t("File"));
        assertEquals("Standard-Mapping hinzufügen", Messages.t("Add standard mapping"));
        assertEquals("Geändert", Messages.t("Modified"));
        assertEquals("Überschriebene Werte", Messages.t("Overwritten Values"));
    }

    @Test
    void parameterLabelsAreTranslatedByKeyWithEnglishFallback() {
        Messages.setLanguage("de");
        assertEquals("Datenpfad", Messages.t("data_path", "Data path"));
        assertEquals("Feldpfad-Zugriff", Messages.t("field_path_access", "Field path access"));
        // Help texts use "<key>.help".
        assertEquals("Pfad zum Daten-Array in der JSON-Antwort.",
                Messages.t("data_path.help", "Path to the data array within the JSON response."));
    }

    @Test
    void parameterFallbackIsUsedWhenNotTranslated() {
        Messages.setLanguage("de");
        // A parameter without a PO entry must stay on its English fallback, not go blank.
        assertEquals("Some Fallback", Messages.t("not_translated_key", "Some Fallback"));
    }

    @Test
    void decodesMultiLineAndUtf8Translations() {
        Messages.setLanguage("de");
        assertEquals("Noch keine Mappings — eines mit den Schaltflächen oben hinzufügen "
                + "(oder auf das dnd-Symbol klicken).",
                Messages.t("No mappings yet — add one with the buttons above "
                        + "(or click the dnd icon)."));
        assertTrue(Messages.t("Example: 'target-field:{source-field-1} and {source-field-4} "
                + "whatever'.\nFirst the target field, then ':' and then the value — one or more "
                + "source fields enclosed in curly brackets. The line must contain exactly one "
                + "colon.").startsWith("Beispiel:"));
    }

    @Test
    void formatsTranslatedPatterns() {
        Messages.setLanguage("de");
        assertEquals("main.ifcx gespeichert (.ifcx)",
                Messages.format("Saved {0} ({1})", "main.ifcx", ".ifcx"));
        assertEquals("Kein Formular für: ROOT", Messages.format("No form for: {0}", "ROOT"));
    }

    @Test
    void switchingLanguageAlsoMovesTheJvmDefaultLocale() {
        Messages.setLanguage("de");
        assertEquals("de", Locale.getDefault().getLanguage(),
                "framework bundles (autumo-ui) resolve through the JVM default locale");
        Messages.setLanguage("en");
        assertEquals("en", Locale.getDefault().getLanguage());
    }

    @Test
    void unknownLanguageFallsBackToEnglish() {
        Messages.setLanguage("xx");
        assertEquals("en", Messages.getLanguage(), "unknown languages fall back to English");
        assertEquals("Saved a (b)", Messages.format("Saved {0} ({1})", "a", "b"));
    }

    @Test
    void regionIsIgnoredForTheLoadedCatalogue() {
        Messages.setLanguage("de_CH");
        assertEquals("de", Messages.getLanguage());
        assertEquals("Datei", Messages.t("File"));
    }

    @Test
    void malformedPatternDoesNotBreakTheUi() {
        Messages.setLanguage("en");
        // "{x}" is not a valid MessageFormat argument; the raw pattern must survive instead of
        // throwing into the UI.
        assertEquals("Saved {x}", Messages.format("Saved {x}", "a"));
    }
}
