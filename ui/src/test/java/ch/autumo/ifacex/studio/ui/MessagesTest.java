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
package ch.autumo.ifacex.studio.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.autumo.ifacex.studio.core.StudioConstants;
import ch.autumo.ui.utils.Messages;

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
        Messages.setLanguage(Messages.LANG_EN, StudioConstants.BASE);
        Locale.setDefault(defaultLocale);
    }

    @Test
    void englishIsTheDefaultAndUsesTheSourceText() {
        Messages.setLanguage(null, StudioConstants.BASE);
        
        // English needs no catalogue: the msgid already is the English text.
        assertEquals("File", StudioHelper.t("File"));
        assertEquals("Add standard mapping", StudioHelper.t("Add standard mapping"));
        // The only English catalogue entries are the framework keys whose id is not English text.
        assertEquals("ifaceX Studio FX", StudioHelper.t("application.name"));
        assertEquals("About ifaceX Studio FX", StudioHelper.t("About.title"));
    }

    @Test
    void unknownMessageFallsBackToItself() {
        Messages.setLanguage("en", StudioConstants.BASE);
        
        assertEquals("Not a known message", StudioHelper.t("Not a known message"));
    }

    @Test
    void mergesTheFrameworkCatalogueBelowTheApplicationOne() {
        // 'button.Alright' is a framework key only (autumo-ui dialogs); the Studio's catalogue does
        // not define it, so its value proves that the framework PO is merged in.
        Messages.setLanguage("en", StudioConstants.BASE);
        
        assertEquals("Alright", StudioHelper.t("button.Alright"));
        assertEquals("File", StudioHelper.t("menu.File"));
        
        Messages.setLanguage("de", StudioConstants.BASE);
        
        assertEquals("Okay", StudioHelper.t("button.Alright"));
        assertEquals("Datei", StudioHelper.t("menu.File"));
        // Both catalogues define 'application.name'; the application's entry wins.
        assertNotEquals("autumo UI", StudioHelper.t("application.name"));
        assertEquals("ifaceX Studio FX", StudioHelper.t("application.name"));
    }

    @Test
    void unknownKeyWithoutBundleEntryIsNotNull() {
        Messages.setLanguage("de", StudioConstants.BASE);
        
        // A framework key nobody defines must not blow up the lookup.
        assertEquals("no.such.key.anywhere", StudioHelper.t("no.such.key.anywhere"));
    }

    @Test
    void germanOverridesTheFrameworkKeys() {
        Messages.setLanguage("de", StudioConstants.BASE);
        
        assertEquals("Über ifaceX Studio FX", StudioHelper.t("About.title"));
        assertEquals("ifaceX Studio FX", StudioHelper.t("application.name"));
    }

    @Test
    void loadsGermanTranslations() {
        final int loaded = Messages.setLanguage("de", StudioConstants.BASE);
        
        assertEquals("Datei", StudioHelper.t("File"));
        assertEquals("Standard-Mapping hinzufügen", StudioHelper.t("Add standard mapping"));
        assertEquals("Geändert", StudioHelper.t("Modified"));
        assertEquals("Überschriebene Werte", StudioHelper.t("Overwritten Values"));
        
        assertTrue(loaded > 100, "the German PO should carry the whole UI text > 0, size is: " + loaded);
    }

    @Test
    void parameterLabelsAreTranslatedByKeyWithEnglishFallback() {
        Messages.setLanguage("de", StudioConstants.BASE);
        
        assertEquals("Datenpfad", StudioHelper.t("data_path", "Data path"));
        assertEquals("Feldpfad-Zugriff", StudioHelper.t("field_path_access", "Field path access"));
        // Help texts use "<key>.help".
        assertEquals("Pfad zum Daten-Array in der JSON-Antwort.",
        		StudioHelper.t("data_path.help", "Path to the data array within the JSON response."));
    }

    @Test
    void parameterFallbackIsUsedWhenNotTranslated() {
        Messages.setLanguage("de", StudioConstants.BASE);
        
        // A parameter without a PO entry must stay on its English fallback, not go blank.
        assertEquals("Some Fallback", StudioHelper.t("not_translated_key", "Some Fallback"));
    }

    @Test
    void decodesMultiLineAndUtf8Translations() {
        Messages.setLanguage("de", StudioConstants.BASE);
        
        assertEquals("Noch keine Mappings — eines mit den Schaltflächen oben hinzufügen "
                + "(oder auf das dnd-Symbol klicken).",
                StudioHelper.t("No mappings yet — add one with the buttons above "
                        + "(or click the dnd icon)."));
        assertTrue(StudioHelper.t("Example: 'target-field:{source-field-1} and {source-field-4} "
                + "whatever'.\nFirst the target field, then ':' and then the value — one or more "
                + "source fields enclosed in curly brackets. The line must contain exactly one "
                + "colon.").startsWith("Beispiel:"));
    }

    @Test
    void formatsTranslatedPatterns() {
        Messages.setLanguage("de", StudioConstants.BASE);
        
        assertEquals("main.ifcx gespeichert (.ifcx)",
        		StudioHelper.format("Saved {0} ({1})", "main.ifcx", ".ifcx"));
        assertEquals("Kein Formular für: ROOT", StudioHelper.format("No form for: {0}", "ROOT"));
    }

    @Test
    void switchingLanguageAlsoMovesTheJvmDefaultLocale() {
        Messages.setLanguage("de", StudioConstants.BASE);
        
        assertEquals("de", Locale.getDefault().getLanguage(),
                "framework bundles (autumo-ui) resolve through the JVM default locale");
        Messages.setLanguage("en", StudioConstants.BASE);
        assertEquals("en", Locale.getDefault().getLanguage());
    }

    @Test
    void unknownLanguageFallsBackToEnglish() {
        Messages.setLanguage("xx", StudioConstants.BASE);
        
        assertEquals("en", Messages.getLanguage(), "unknown languages fall back to English");
        assertEquals("Saved a (b)", StudioHelper.format("Saved {0} ({1})", "a", "b"));
    }

    @Test
    void regionIsIgnoredForTheLoadedCatalogue() {
        Messages.setLanguage("de_CH", StudioConstants.BASE);
        
        assertEquals("de", Messages.getLanguage());
        assertEquals("Datei", StudioHelper.t("File"));
    }

    @Test
    void malformedPatternDoesNotBreakTheUi() {
        Messages.setLanguage("en", StudioConstants.BASE);
        
        // "{x}" is not a valid MessageFormat argument; the raw pattern must survive instead of
        // throwing into the UI.
        assertEquals("Saved {x}", StudioHelper.format("Saved {x}", "a"));
    }
}
