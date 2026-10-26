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


import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import ch.autumo.commons.utils.system.OSUtils;
import ch.autumo.ifacex.studio.core.LicIfaceXApp100;
import ch.autumo.ifacex.studio.core.StudioConstants;
import ch.autumo.ifacex.studio.ui.controller.MainController;
import ch.autumo.ifacex.studio.ui.controller.actions.installer.StepSettings;
import ch.autumo.ifacex.studio.ui.model.MainModel;
import ch.autumo.ifacex.studio.ui.view.MainView;
import ch.autumo.ui.Constants;
import ch.autumo.ui.fx.BaseApp;
import ch.autumo.ui.fx.view.SplashScreen;
import ch.autumo.ui.utils.UI;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Studio application entry point, built on the <b>autumo-ui</b> framework.
 *
 * <p>The framework ({@link BaseApp}) provides the application shell and product concerns
 * (lifecycle, installer, licensing/registration, i18n, theming, recent files, update check).
 * The Studio-specific editor UI — the static {@code main.fxml} shell and the metadata-driven
 * {@link FormBuilder} forms — stays our own.</p>
 *
 * <p>The Studio is installed by the framework's installer ({@link #isPortableApp()}); the
 * registration/licensing UI is wired in later. See docs 04 and 07 (decision D12).</p>
 */
public class StudioApp extends BaseApp {

	public static int DEFAULT_TIMEOUT = 60;
	
    private int timeOut = DEFAULT_TIMEOUT;
    private String projectRoot = null;
    private String ifacexFolder = null;
	private String ifacexSeed = null;
    
    private LicIfaceXApp100 ifacexApp = null;
    
    
    /** Creates the application; the framework instantiates exactly one. */
    public StudioApp() {
        // File endings the Studio accepts from the command line / double-click: the legacy
        // properties format (.ifacex) and the new YAML format (.ifcx, plus the plain .yaml/.yml
        // spellings the open dialog also offers). A file passed on the command line is only
        // treated as a config when it carries one of these — and the framework knows no endings
        // by itself, the application has to declare them.
        this.registerOpenExtension(StudioConstants.EXT_IFACEX);
        this.registerOpenExtension(StudioConstants.EXT_IFCX);
        this.registerOpenExtension(StudioConstants.EXT_YAML);
        this.registerOpenExtension(StudioConstants.EXT_YML);
    }

    /** Current application instance (the framework creates exactly one). */
    private static StudioApp instance;

    private MainController controller;

    /**
     * The running application instance, or {@code null} before start-up.
     *
     * @return application instance
     */
    public static StudioApp get() {
        return instance;
    }

    /**
     * Resolves the language before the UI is built, keeps the autumo-ui framework in sync and
     * installs the application view.
     *
     * <p>The language is taken from {@code -Dapp.lang=de}, otherwise from {@code language=de} in
     * {@code cfg/app.cfg} (autumo-ui convention), otherwise from its {@code lang} alias, otherwise
     * from the desktop language of the operating system. Only when none of them is a language the
     * application carries a translation for, English is used — that is also the rule for an
     * unsupported desktop language such as French without a PO catalogue. English is the first
     * candidate only for an unknown value, never because of the operating system alone.</p>
     *
     * <p>The framework is switched first (it owns the window title, the About/license panes and
     * framework dialogs), then the Studio's own PO catalogue is layered on top.</p>
     *
     * <p>On the very first start of an installed application {@code configuration} is
     * {@code null}: the framework has not written {@code cfg/app.cfg} yet, because that happens in
     * the installer phase ({@code BaseApp.completed}, see {@link #isPortableApp()}). The language
     * is then resolved from the request parameter and the desktop only.</p>
     *
     * @param primaryStage  primary window
     * @param configuration resolved application configuration, {@code null} before the installer
     *                      wrote {@code cfg/app.cfg}
     * @return the root pane of the application view
     */
    @Override
    public Pane preInitialize(Stage primaryStage, Properties configuration) {

    	instance = this;

        final MainView view = new MainView(this, primaryStage, Constants.STYLESHEET);
        final MainModel model = new MainModel(view);
        view.setModel(model);
        
        controller = new MainController(this, primaryStage, model, view);
        view.setController(controller);
        
        UI.setup(this, getBaseResourcePath(), view, "main.fxml");
        
        return view;
    }

    /**
     * Finishes the window set-up after the view exists: adds the Studio style sheet, sizes the
     * stage, shows the window and opens the file handed in on the command line.
     *
     * @param primaryStage  primary window
     * @param configuration resolved application configuration
     */
    @Override
    protected void initialize(Stage primaryStage, Properties configuration) {
        final List<String> args = getParameters().getRaw();

        // The scene already exists: BaseBorderView (via MainView) created it on the stage and
        // installed the autumo-ui base style first and the application's own style sheet
        // (style/application.css, DevGuide §4.5) on top of it.
        final Scene scene = primaryStage.getScene();
        // Size the window to ~1600x1000, but never larger than the available
        // screen: on smaller displays fall back to the previous size (1100x720).
        final Screen screen = Screen.getPrimary();
        final Rectangle2D bounds = screen.getVisualBounds();
        final double targetW = 1600.0;
        final double targetH = 1000.0;
        final double w = Math.min(targetW, bounds.getWidth());
        final double h = Math.min(targetH, bounds.getHeight());
        primaryStage.setWidth(Math.max(w, 1100.0));
        primaryStage.setHeight(Math.max(h, 720.0));

        // Headless UI checks live in the JUnit tests of this module (see src/test/java):
        // the whole shell is built there on the JavaFX thread and asserted, so nothing
        // about it is wired into the application itself.
        primaryStage.setScene(scene);
        primaryStage.show();

        // Props
        final String timeoutStr = getConfigurationParameter(StudioConstants.PROP_KEY_TIMEOUT);
        if (timeoutStr != null) {
            try {
            	timeOut = Integer.parseInt(timeoutStr);
			} catch (Exception e) {
				LOG.warn("Couldn't load process timeout, using "+DEFAULT_TIMEOUT+" seconds.", e);
				timeOut = DEFAULT_TIMEOUT;
			}
        } else {
        	timeOut = DEFAULT_TIMEOUT;
        }
        
        ifacexApp = new LicIfaceXApp100();
        
        projectRoot = getConfigurationParameter(StudioConstants.PROP_KEY_PROJECT_ROOT);
        ifacexSeed = getConfigurationParameter(ch.autumo.commons.Constants.SEC_KEY_SEED);
        if (ifacexSeed != null && ifacexSeed.length() > 0) {
            ifacexApp.setSeed(ifacexSeed);
        }
        ifacexFolder = getConfigurationParameter(StudioConstants.PROP_KEY_IFACEX_FOLDER);
        if (ifacexFolder != null && ifacexFolder.length() > 0) {
            try {
				ifacexApp.initializeBaseConfig(ifacexFolder);
			} catch (Exception e) {
				LOG.error("Couldn't initialize ifaceX test instance!", e);
				UI.showError(this, e);
			}
        }
        		
        		
        // Optional: open a configuration file handed in on the command line. Two shapes are
        // accepted, because two launchers exist:
        //   --open <file>   the development launcher "bin/run.sh some.ifacex";
        //   <file>          the packaged (jpackage) launcher, which the OS passes verbatim when
        //                   the user double-clicks a registered .ifcx/.ifacex file or uses
        //                   "Open With". The association is declared in product/packaging/.
        // Only the endings registered in the constructor are accepted for a bare argument; the
        // framework itself knows none. See BaseApp.registerOpenExtension.
        final Path toOpen = openTargetFromCommandLine(args);
        if (toOpen != null) {
            try {
                controller.openFile(toOpen);
            } catch (Exception e) {
                System.err.println("Could not open " + toOpen + ": " + e);
            }
        }
    }

    /**
     * How long the splash screen of the Studio stays up.
     *
     * <p>The About page of the navigation pane is loaded afterwards: the engine of its WebView
     * blocks the JavaFX thread for about a second, and the splash screen should animate in that
     * time.</p>
     */
    private static final Duration SPLASH_DURATION = Duration.seconds(3);

    /**
     * The Studio shows a splash screen while it starts: the ifaceX logo and the scanner bar of
     * the framework. The screen stays up for a few seconds, so it is seen even though the Studio
     * starts quickly.
     */
    @Override
	protected boolean isSplashScreenEnabled() {
		return true;
	}

    /**
     * The splash screen of the Studio: the ifaceX logo, the scanner bar of the framework and the
     * start-up jingle of the legacy Studio.
     *
     * <p>The jingle plays only if the setting says so - the one of the first-run wizard
     * ({@code play.jingle}, see {@link StepSettings}). The framework creates the splash screen
     * after the configuration has been read, so the user's choice is known here. An unset setting
     * means the jingle plays - the same default the setting of the wizard has.</p>
     *
     * @return the splash screen of the Studio
     */
    @Override
    protected SplashScreen createSplashScreen() {
        final SplashScreen splash = new SplashScreen()
                .setDuration(SPLASH_DURATION)
                .setLogo(getClass().getResource("icons/autumo-ifacex-splash.png"));
        if (booleanParameter(StudioConstants.PROP_KEY_PLAY_JINGLE, false)) {
            splash.setJingle(getClass().getResource("sound/autumo-jingle-1.0.wav"));
        }
        return splash;
    }

    /**
     * The Studio ends the JVM with the application: it embeds the Swing code editor (the raw
     * tab), and the AWT event dispatch thread of that editor is not a daemon thread - it would
     * keep the JVM alive after the JavaFX runtime is gone.
     */
    @Override
    protected boolean isJvmExitOnStop() {
        return true;
    }

    /**
     * Reads a boolean parameter of the application configuration.
     *
     * @param key          the parameter
     * @param defaultValue the value if the parameter is not set
     * @return the value of the parameter
     */
    private boolean booleanParameter(String key, boolean defaultValue) {
        final String value = getConfigurationParameter(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

	/**
     * {@inheritDoc}
     *
     * <p>The framework has already stored the application configuration and the recent files on
     * exit (see {@link ch.autumo.ui.fx.BaseApp#stop()}); the Studio releases the raw editor:
     * it is a Swing component, and removing it makes macOS call into the native input method,
     * which can block the AWT thread forever and keep the JVM from exiting.</p>
     */
    @Override
    public void finish(Stage primaryStage) {
        if (controller != null) {
            controller.disposeEditor();
        }
    }

    /**
     * The Studio's application base resource path; the framework loads {@code main.fxml},
     * {@code html/*.html}, the PO catalogue and the images from here.
     *
     * @return base resource path, e.g. {@code /ch/autumo/ifacex/studio/ui/}
     */
    @Override
    public String getBaseResourcePath() {
        return StudioConstants.BASE;
    }

    /**
     * {@inheritDoc}
     *
     * @return the bundled {@code cfg/app.cfg} template of the Studio
     */
    @Override
    public String getConfigTemplateFile() {
        return StudioConstants.BASE + "cfg/app.cfg";
    }

    /**
     * {@inheritDoc}
     *
     * @return the bundled base-configuration template, {@code cfg/base.ifc}
     */
    @Override
    public String getBaseConfigTemplateFile() {
        return StudioConstants.BASE + "cfg/base.ifc";
    }

    /**
     * {@inheritDoc}
     *
     * @return the identifier used for the in-memory (unsaved) configuration
     */
    @Override
    public String getInMemoryConfigName() {
        return "ifacex-studio-fx";
    }

    /**
     * {@inheritDoc}
     *
     * @return the Studio version
     */
    @Override
    public String getAppVersion() {
        return "3.0.0";
    }

    /**
     * {@inheritDoc}
     *
     * @return the human readable application title
     */
    @Override
    public String getAppTitle() {
        return "ifaceX Studio FX";
    }

    /**
     * {@inheritDoc}
     *
     * @return the internal application name (no spaces), used for directories and files
     */
    @Override
    public String getInternalAppName() {
        return "ifaceXStudioFX";
    }

    public int getTimeOut() {
		return timeOut;
	}

	public void setTimeOut(int timeOut) {
		this.timeOut = timeOut;
	}

	public String getProjectRoot() {
		return projectRoot;
	}

	public void setProjectRoot(String projectRoot) {
		this.projectRoot = projectRoot;
	}

	public String getIfacexFolder() {
    	if (!ifacexFolder.endsWith(OSUtils.FILE_SEPARATOR))
    		ifacexFolder = ifacexFolder + OSUtils.FILE_SEPARATOR;
		return ifacexFolder;
	}

	public void setIfacexFolder(String ifacexFolder) {
    	if (!ifacexFolder.endsWith(OSUtils.FILE_SEPARATOR))
    		ifacexFolder = ifacexFolder + OSUtils.FILE_SEPARATOR;
		this.ifacexFolder = ifacexFolder;
	}

	public String getIfacexSeed() {
		return ifacexSeed;
	}

	public void setIfacexSeed(String ifacexSeed) {
		this.ifacexSeed = ifacexSeed;
	}

	public LicIfaceXApp100 getIfacexApp() {
		return ifacexApp;
	}
	
}
