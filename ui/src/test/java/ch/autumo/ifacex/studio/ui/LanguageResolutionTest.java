/*
 * Copyright 2026 autumo GmbH.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
        originalIfacexLang = System.getProperty("ifacex.lang");
    }

    /** Restores the environment of the test run. */
    @AfterEach
    void restoreRequestParameter() {
        if (originalIfacexLang == null) {
            System.clearProperty("ifacex.lang");
        } else {
            System.setProperty("ifacex.lang", originalIfacexLang);
        }
    }

    /** The request parameter beats everything, including the configuration. */
    @Test
    void requestParameterWins() {
        System.setProperty("ifacex.lang", "en");
        final Properties config = new Properties();
        config.setProperty("language", "de");
        assertEquals("en", StudioApp.resolveLanguage(config));
    }

    /** The configuration key is the second candidate. */
    @Test
    void configurationLanguageIsUsed() {
        System.clearProperty("ifacex.lang");
        final Properties config = new Properties();
        config.setProperty("language", "de");
        assertEquals("de", StudioApp.resolveLanguage(config));
    }

    /** Its {@code lang} alias is the third candidate and beats the desktop. */
    @Test
    void configurationAliasIsUsed() {
        System.clearProperty("ifacex.lang");
        final Properties config = new Properties();
        config.setProperty("lang", "de");
        assertEquals("de", StudioApp.resolveLanguage(config));
    }

    /** A blank (or absent) value is skipped, not treated as a language. */
    @Test
    void blankValueIsSkipped() {
        System.clearProperty("ifacex.lang");
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
        System.clearProperty("ifacex.lang");
        assertEquals(Locale.getDefault().getLanguage(), StudioApp.resolveLanguage(null));
    }
}
