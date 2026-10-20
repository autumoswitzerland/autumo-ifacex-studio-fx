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

import java.util.Locale;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the language precedence of {@link StudioApp#resolveLanguage(Properties)}.
 *
 * <p>The candidate that wins is returned <em>raw</em>; dropping a region and mapping an unsupported
 * language to English is the job of the framework ({@code Helper.setLanguage}), which is why the
 * desktop case only asserts the language part of the locale.</p>
 */
class LanguageResolutionTest {

    private String originalIfacexLang;

    /** Remembers the request parameter so a test can set and the next one can clear it. */
    @BeforeEach
    void rememberRequestParameter() {
        originalIfacexLang = System.getProperty("app.lang");
    }

    /** Restores the environment of the test run. */
    @AfterEach
    void restoreRequestParameter() {
        if (originalIfacexLang == null) {
            System.clearProperty("app.lang");
        } else {
            System.setProperty("app.lang", originalIfacexLang);
        }
    }

    /** The request parameter beats everything, including the configuration. */
    @Test
    void requestParameterWins() {
        System.setProperty("app.lang", "en");
        final Properties config = new Properties();
        config.setProperty("language", "de");
        assertEquals("en", StudioApp.resolveLanguage(config));
    }

    /** The configuration key is the second candidate. */
    @Test
    void configurationLanguageIsUsed() {
        System.clearProperty("app.lang");
        final Properties config = new Properties();
        config.setProperty("language", "de");
        assertEquals("de", StudioApp.resolveLanguage(config));
    }

    /** Its {@code lang} alias is the third candidate and beats the desktop. */
    @Test
    void configurationAliasIsUsed() {
        System.clearProperty("app.lang");
        final Properties config = new Properties();
        config.setProperty("lang", "de");
        assertEquals("de", StudioApp.resolveLanguage(config));
    }

    /** A blank (or absent) value is skipped, not treated as a language. */
    @Test
    void blankValueIsSkipped() {
        System.clearProperty("app.lang");
        final Properties config = new Properties();
        config.setProperty("language", "   ");
        config.setProperty("lang", "de");
        assertEquals("de", StudioApp.resolveLanguage(config));
    }

    /**
     * The installer hands over a {@code null} configuration on the first start of an installed
     * application; the desktop then decides (the language part of the locale).
     */
    @Test
    void nullConfigurationFallsBackToTheDesktop() {
        System.clearProperty("app.lang");
        assertEquals(Locale.getDefault().getLanguage(), StudioApp.resolveLanguage(null));
    }
}
