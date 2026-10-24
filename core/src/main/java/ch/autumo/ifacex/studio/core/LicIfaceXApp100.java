/**
 * Copyright 2020 autumo GmbH, Michael Gasche.
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
package ch.autumo.ifacex.studio.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import ch.autumo.commons.application.AbstractVersion;
import ch.autumo.commons.application.LicensedApplication;
import ch.autumo.commons.application.RegistrationVersion;
import ch.autumo.commons.utils.license.InlineChecks;
import ch.autumo.commons.utils.security.CryptUtils;
import ch.autumo.commons.utils.system.OSUtils;

/**
 * License Application (Registration 1.0.0).
 */
public class LicIfaceXApp100 implements LicensedApplication {

    public static final String IFACEX_PATH_REL_LIC = "lic" + OSUtils.FILE_SEPARATOR;
    public static final String IFACEX_FILE_BASE_CFG = "cfg" + OSUtils.FILE_SEPARATOR + "base.ifc";
	
	private static final RegVersion REG_VERSION = new RegVersion();

	private String licensePath = null;
	
	private String ap_ls_ad = null;
	private String ap_ls_ak = null;
	private String pub_key = null;
	private String licType = null;
	
	private String seed = null;

	/**
	 * Constructor.
	 */
	public LicIfaceXApp100() {
	}
	
	@Override
	public void initializeBaseConfig(String rootFolder) throws Exception {		
		String root = rootFolder;
		
		if (!root.endsWith(OSUtils.FILE_SEPARATOR))
			root += OSUtils.FILE_SEPARATOR;
		
		licensePath = root + IFACEX_PATH_REL_LIC;
		final String baseCfg = root + IFACEX_FILE_BASE_CFG;
		
    	final Properties baseProps = getBaseConfig(baseCfg);
		ap_ls_ad = CryptUtils.decodeIfNecessary(baseProps.getProperty(LicensedApplication.AP_LS_AD_KEY), this, true);
		ap_ls_ak = CryptUtils.decodeIfNecessary(baseProps.getProperty(LicensedApplication.AP_LS_AK_KEY), this, true);
		licType = CryptUtils.decodeIfNecessary(baseProps.getProperty(LicensedApplication.LIC_TYPE_KEY), this, true);
		
		String raw_license_public_key = baseProps.getProperty(LicensedApplication.LICENSE_PUBLIC_KEY);
		if (raw_license_public_key != null && raw_license_public_key.length() > 0) {
			pub_key = CryptUtils.decodeIfNecessary(raw_license_public_key, this, true);
		}
	}
	
	public void setSeed(String seed) {
		this.seed = seed;
	}
	
	// ---- licensedApplication
	@Override
	public RegistrationVersion getRegVersion() {
		return REG_VERSION;
	}
	@Override
	public String getAppTitle() {
		return "ifaceX";
	}
	@Override
	public String getAppVersion() {
		return StudioConstants.VERSION;
	}
	@Override
	public String getProductKey() {
		return "2a6cd4c14abd7350";
	}
	@Override
	public String getLicInfoTitle() {
		return getAppTitle() + " " + getAppVersion() + " - License Info";
	}
	@Override
	public boolean isAllowed2ReadEnvData() {
		return false;
	}
	@Override
	public void setAllowed2ReadEnvData(boolean allow) {
	}
	@Override
	public String getRuntimeType() {
		return null;
	}
	@Override
	public String getLicType() {
		return licType;
	}
	@Override
	public InlineChecks getInlineChecks() {
		return null;
	}
	@Override
	public String getLicSrvUrl() {
		return ap_ls_ad;
	}
	@Override
	public String getApiKey() {
		return ap_ls_ak;
	}
	@Override
	public String getPublicKey() {
		return pub_key;
	}
	
	// ---- SecureApplication
	
	@Override
	public String getInternalSecurityKey() {
		return "6f9ea9ae6efc899083b087a178492216";
	}	
	@Override
	public String getUniqueSecurityKey() {
		return seed;
	}
	@Override
	public String getPassphrase() {
		return seed.substring(CryptUtils.KEYDATA.LEN_3, CryptUtils.KEYDATA.LEN_4);
	}
	@Override
	public String getLicensePath() {
		return licensePath;
	}

	public static Properties getBaseConfig(String baseFile) throws IOException {
		// read base config
		final Properties baseProps = new Properties();
	    try (FileInputStream fis = new FileInputStream(baseFile)) {
	    	baseProps.load(fis);
	    }
		return baseProps;
	}
	
	private static class RegVersion extends AbstractVersion {
	    /**
	     * From version 1.0.0 - (open)
	     */
	    // 5 characters, registration version; changes only when new purchase is necessary for a new version
	    private static final String REGISTRATION_VERSION_100 = "1.0.0"; 
	    // 25 characters, registration key changes only when new purchase is necessary for a new version
	    private static final String REGISTRATION_KEY_100 = "ifx.g$644o!6t7t.uojanifxl";

	    /**
	     * Constructor for Register console tool.
	     */
	    public RegVersion() {
		}
	    @Override
	    public String getRegistrationVersion() {
	        return REGISTRATION_VERSION_100;
	    }
		@Override
		public String getRegistrationKey() {
			return REGISTRATION_KEY_100;
		}
	}
	
}
