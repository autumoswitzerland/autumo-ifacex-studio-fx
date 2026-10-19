/*
 * Copyright 2026 autumo GmbH.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package ch.autumo.ifacex.studio.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import ch.autumo.ui.fx.controller.actions.installer.BaseStep;

/**
 * Tests the installer configuration of the Studio.
 *
 * <p>The framework loads the steps of the installer from {@code cfg/installer-steps.cfg}, resolves
 * the classes named there and instantiates each of them with its no-argument constructor
 * ({@code InstallAction.refillSteps()}), so a typo in the file or a missing constructor breaks the
 * installation of the application. The settings step is the Studio's own contribution to the
 * installer; its texts and its configuration keys are pinned here.</p>
 */
class InstallerStepsTest {

    /** Installer step list, relative to the Studio's base resource path. */
    private static final String STEPS = "/ch/autumo/ifacex/studio/ui/cfg/installer-steps.cfg";

    @AfterEach
    void restoreLanguage() {
        Messages.setLanguage(Messages.LANG_EN);
    }

    @Test
    void everyListedStepIsInstantiable() throws Exception {
        final Properties steps = loadSteps();
        assertTrue(steps.size() >= 3, "the installer needs at least the about, settings and done step");
        for (int i = 1; i <= steps.size(); i++) {
            final String className = steps.getProperty(String.valueOf(i));
            assertNotNull(className, "step " + i + " is missing in cfg/installer-steps.cfg");
            final Class<?> clazz = Class.forName(className);
            assertTrue(BaseStep.class.isAssignableFrom(clazz), className + " is not an installer step");
            // The framework instantiates the steps exactly like this.
            assertNotNull(clazz.getDeclaredConstructor().newInstance());
        }
    }

    @Test
    void theSettingsStepIsPartOfTheInstaller() {
        assertTrue(loadSteps().containsValue(StepSettings.class.getName()),
                "the Studio's settings step must be listed in cfg/installer-steps.cfg");
    }

    @Test
    void settingsTextsAreTranslated() {
        Messages.setLanguage("en");
        assertEquals("General Settings", Messages.t("General Settings"));
        assertEquals("Configuration root folder:", Messages.t("Configuration root folder:"));
        assertEquals("Play sounds?", Messages.t("Play sounds?"));
        assertEquals("Play jingle?", Messages.t("Play jingle?"));
        Messages.setLanguage("de");
        assertEquals("Allgemeine Einstellungen", Messages.t("General Settings"));
        assertEquals("Wurzelverzeichnis der Konfiguration:", Messages.t("Configuration root folder:"));
        assertEquals("Klang abspielen?", Messages.t("Play sounds?"));
        assertEquals("Jingle abspielen?", Messages.t("Play jingle?"));
    }

    @Test
    void settingsUseTheLegacyConfigurationKeys() {
        // An existing user configuration of the legacy Studio keeps working.
        assertEquals("project.root", StudioConstants.PROP_KEY_PROJECT_ROOT);
        assertEquals("play.sounds", StudioConstants.PROP_KEY_PLAY_SOUNDS);
        assertEquals("play.jingle", StudioConstants.PROP_KEY_PLAY_JINGLE);
        assertTrue(StudioConstants.DEFAULT_PROJECT_ROOT_FOLDER.length() > 0);
    }

    /**
     * Loads the installer step list that ships with the Studio.
     *
     * @return the configured steps
     */
    private Properties loadSteps() {
        final Properties steps = new Properties();
        try (InputStream in = InstallerStepsTest.class.getResourceAsStream(STEPS)) {
            assertNotNull(in, "cfg/installer-steps.cfg must ship with the Studio");
            steps.load(in);
        } catch (Exception e) {
            throw new AssertionError("cfg/installer-steps.cfg is not readable: " + e, e);
        }
        return steps;
    }
}
