/*
 * Copyright 2026 autumo GmbH.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package ch.autumo.ifacex.studio.ui;

/**
 * Constants of the Studio's own application configuration.
 *
 * <p>The keys are the ones the Studio stores in the application configuration
 * ({@code cfg/app.cfg} of the user's application data directory) next to the framework's own
 * parameters. They are read and written through
 * {@link ch.autumo.ui.fx.BaseApp#getConfigurationParameter(String)} and
 * {@link ch.autumo.ui.fx.BaseApp#setConfigurationParameter(String, String)}, which keeps the file
 * intact and only adds or replaces the named parameter.</p>
 *
 * <p>The names are the ones of the legacy Swing Studio, so an existing user configuration keeps
 * working. See docs/11-legacy-studio-gui-analysis.md.</p>
 */
public final class StudioConstants {

    /** Root folder of the configuration files, e.g. {@code /home/user/ifaceXProjects}. */
    public static final String PROP_KEY_PROJECT_ROOT = "project.root";

    /** Whether the Studio plays UI sounds ({@code true}/{@code false}). */
    public static final String PROP_KEY_PLAY_SOUNDS = "play.sounds";

    /** Whether the Studio plays its start-up jingle ({@code true}/{@code false}). */
    public static final String PROP_KEY_PLAY_JINGLE = "play.jingle";

    /** Name of the default project root folder below the user's home directory. */
    public static final String DEFAULT_PROJECT_ROOT_FOLDER = "ifaceXProjects";

    /** Utility class: not instantiable. */
    private StudioConstants() {
    }

}
