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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the message lookup against the shipped German PO file.
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
        final int loaded = Messages.setLanguage(null);
        assertEquals(0, loaded, "English needs no PO file");
        assertEquals("File", Messages.t("File"));
        assertEquals("Add standard mapping", Messages.t("Add standard mapping"));
    }

    @Test
    void unknownMessageFallsBackToItself() {
        Messages.setLanguage("en");
        assertEquals("Not a known message", Messages.t("Not a known message"));
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
        assertEquals(0, Messages.size(), "no PO file for xx");
        assertEquals("Saved a (b)", Messages.format("Saved {0} ({1})", "a", "b"));
    }

    @Test
    void malformedPatternDoesNotBreakTheUi() {
        Messages.setLanguage("en");
        // "{x}" is not a valid MessageFormat argument; the raw pattern must survive instead of
        // throwing into the UI.
        assertEquals("Saved {x}", Messages.format("Saved {x}", "a"));
    }
}
