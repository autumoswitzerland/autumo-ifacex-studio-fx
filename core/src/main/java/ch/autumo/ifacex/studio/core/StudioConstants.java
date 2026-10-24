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
package ch.autumo.ifacex.studio.core;

import ch.autumo.commons.utils.system.OSUtils;

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

    /** Version */
    public static final String VERSION = "3.0.0";
	
    /** Base resource path of the Studio UI; must match {@link StudioApp#getBaseResourcePath()}. */
    public static final String BASE = "/ch/autumo/ifacex/studio/ui/";
	
    /** IPC timeout. */
    public static final String PROP_KEY_TIMEOUT = "timeout";
    
    /** Root folder of the configuration files, e.g. {@code /home/user/ifaceXProjects}. */
    public static final String PROP_KEY_PROJECT_ROOT = "project.root";

    /** Folder of the ifaceX test instance whose {@code cfg/ifacex.cfg} provides the
     * encryption seed ({@code secret_key_seed}). */
    public static final String PROP_KEY_IFACEX_FOLDER = "ifaceXFolder";

    /** Whether the Studio plays UI sounds ({@code true}/{@code false}). */
    public static final String PROP_KEY_PLAY_SOUNDS = "play.sounds";

    /** Whether the Studio plays its start-up jingle ({@code true}/{@code false}). */
    public static final String PROP_KEY_PLAY_JINGLE = "play.jingle";

    /** Name of the default project root folder below the user's home directory. */
    public static final String DEFAULT_PROJECT_ROOT_FOLDER = "ifaceXProjects";

    /** Extension of the legacy properties configuration (Java properties). */
    public static final String EXT_IFACEX = ".ifacex";

    /** Extension of the new YAML configuration. */
    public static final String EXT_IFCX = ".ifcx";

    /** Plain YAML spelling the open dialog also accepts. */
    public static final String EXT_YAML = ".yaml";

    /** Plain YAML spelling the open dialog also accepts. */
    public static final String EXT_YML = ".yml";

    public static final String IFACEX_PATH_REL_INTERFACES = "interfaces" + OSUtils.FILE_SEPARATOR;
    public static final String IFACEX_PATH_REL_LIC = "lic" + OSUtils.FILE_SEPARATOR;
    public static final String IFACEX_FILE_REL_00_TEMPLATE = IFACEX_PATH_REL_INTERFACES +  "00_console_out.template";
    public static final String IFACEX_FILE_BASE_CFG = "cfg" + OSUtils.FILE_SEPARATOR + "base.ifc";
    public static final String IFACEX_FILE_INTERFACES_CFG = "cfg" + OSUtils.FILE_SEPARATOR + "interfaces.cfg";
    
}
