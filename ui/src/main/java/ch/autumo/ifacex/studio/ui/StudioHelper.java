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

import java.io.File;
import java.text.MessageFormat;

import ch.autumo.commons.utils.system.OSUtils;
import ch.autumo.ifacex.studio.core.StudioConstants;
import ch.autumo.ui.fx.BaseApp;
import ch.autumo.ui.utils.Messages;

/**
 * Studio Helper functions.
 */
public class StudioHelper {

    /**
     * Translates {@code id} and returns {@code id} itself when it is not translated.
     *
     * @param id message id, which is the English text
     * @return translated text or the id
     */
    public static String t(String id) {
    	return Messages.t(id, StudioConstants.BASE);
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
    	return Messages.t(id, english, StudioConstants.BASE);
    }

    /**
     * Translates {@code id} and formats it with {@link MessageFormat} ({@code {0}}, {@code {1}}, …).
     *
     * @param id message id
     * @param basePath base path, e.g. "ch/autumo/ui"
     * @param args {@link MessageFormat} arguments
     * @return formatted text; the raw pattern when it is malformed
     */
    public static String format(String id, Object... args) {
    	return Messages.format(id, StudioConstants.BASE, args);
    }
    
    /**
     * Check ifaceX folder.
     * 
     * @param app base app
     * @param folder root folder
     * @return true if it is a valid ifaceX, otherwise false
     */
    public static boolean checkIfacexFolder(BaseApp app, String folder) {
    	if (folder == null)
    		return false;
    	
    	if (!folder.endsWith(OSUtils.FILE_SEPARATOR))
    		folder = folder + OSUtils.FILE_SEPARATOR;
    	
		final String licensePath = folder + StudioConstants.IFACEX_PATH_REL_LIC;
		final String baseCfg = folder + StudioConstants.IFACEX_FILE_BASE_CFG;    	
		final String ifxCfg = folder + StudioConstants.IFACEX_FILE_INTERFACES_CFG; 	
    	final String ifFolder = folder + StudioConstants.IFACEX_PATH_REL_INTERFACES;
    	if (!new File(licensePath).exists() || 
    		!new File(baseCfg).exists() || 
    		!new File(ifxCfg).exists() ||
    		!new File(ifFolder).exists() ) {
        	return false;
    	}
    	return true;
    }
    
}
