/*
 * Copyright 2026 autumo GmbH.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package ch.autumo.ifacex.studio.ui;


import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import ch.autumo.ifacex.studio.core.cli.CommandLine;

import ch.autumo.ui.Constants;
import ch.autumo.ui.fx.BaseApp;
import ch.autumo.ui.utils.Helper;
import ch.autumo.ui.utils.UI;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;

import javafx.stage.Stage;

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

    /** Creates the application; the framework instantiates exactly one. */
    public StudioApp() {
    }

    /** Application base resource path (autumo-ui convention). */
    private static final String BASE = "/ch/autumo/ifacex/studio/ui/";

    /** System property that overrides the configured language, e.g. {@code -Difacex.lang=de}. */
    private static final String PROP_LANG = "ifacex.lang";

    /** Legacy alias of {@link Constants#PROP_KEY_LANGUAGE}. */
    private static final String PROP_LANG_ALIAS = "lang";

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
     * <p>The language is taken from {@code -Difacex.lang=de}, otherwise from {@code language=de} in
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
    protected Pane preInitialize(Stage primaryStage, Properties configuration) {
        instance = this;
        // Precedence: -Difacex.lang=de > "language=de" in cfg/app.cfg (autumo-ui convention,
        // Constants.PROP_KEY_LANGUAGE) > the "lang" alias > the desktop language of the JVM
        // (user.language, so a German desktop starts in German) > English for everything else.
        final String lang = resolveLanguage(configuration);
        // Keep the autumo-ui framework in sync first (it owns the window title, About/license HTML
        // panes and framework dialogs), then set our own PO catalogue on top of it.
        Helper.setLanguage(lang);
        final int translations = Messages.setLanguage(lang);
        if (translations > 0) {
            System.out.println("[dev] language=" + Messages.getLanguage()
                    + " (" + translations + " translations)");
        }
        // Fonts (Inter) are loaded by the autumo-ui framework (BaseApp.start), which is the
        // standard font of the framework; the Studio only consumes the family names in CSS.
        // Create the main view and hand it to the autumo-ui loader: UI.setup() remembers our
        // base resource path, loads main.fxml into the view and binds this controller. It is
        // also what makes the framework's info panes (UI.showAbout, …) work, and it walks our
        // own package for main.fxml / html/*.html.
        final MainView view = new MainView(this, primaryStage, null);
        final MainModel model = new MainModel(view);
        view.setModel(model);
        controller = new MainController(this, primaryStage, model, view);
        view.setController(controller);
        UI.setup(this, getBaseResourcePath(), view, "main.fxml");
        return view;
    }

    /**
     * Resolves the language the Studio starts in.
     *
     * <p>In order: the {@code -Difacex.lang} system property, the {@code language} key of the
     * application configuration ({@link Constants#PROP_KEY_LANGUAGE}) and its {@code lang} alias,
     * then the language of the desktop ({@link Locale#getDefault()}, which the JVM filled from the
     * operating system — e.g. {@code de_CH} on a Swiss German desktop).</p>
     *
     * <p>The result is not checked here: {@link Helper#setLanguage(String)} normalises a region
     * away ({@code de_CH} → {@code de}) and maps an unsupported or blank value — including a
     * desktop language the application has no PO catalogue for — to English.</p>
     *
     * @param configuration application configuration, may be {@code null}
     * @return the resolved language code, e.g. {@code de}
     */
    static String resolveLanguage(Properties configuration) {
        // No configuration at all: the first start of an installed application, where the
        // framework runs the installer before it writes cfg/app.cfg (see isPortableApp()).
        final Properties config = configuration == null ? new Properties() : configuration;
        String lang = System.getProperty(PROP_LANG);
        if (isBlank(lang)) {
            lang = config.getProperty(Constants.PROP_KEY_LANGUAGE);
        }
        if (isBlank(lang)) {
            lang = config.getProperty(PROP_LANG_ALIAS);
        }
        if (isBlank(lang)) {
            lang = Locale.getDefault().getLanguage();
        }
        return lang;
    }

    /**
     * Checks whether a language configuration value is absent or only whitespace; such a value
     * does not take part in the precedence and the next candidate is used instead.
     *
     * @param value value to check, may be {@code null}
     * @return {@code true} when the value carries nothing
     */
    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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
        // installed the autumo-ui base style (MainView passes 'null' as style sheet on purpose).
        // Our own style sheet — the application stylesheet of the autumo-ui resource structure
        // (style/application.css, DevGuide §4.5) — goes on top of the base look.
        final Scene scene = primaryStage.getScene();
        final String appCss = Helper.getAppResource(this, Constants.STYLESHEET);
        scene.getStylesheets().add(appCss);
        primaryStage.setWidth(1100);
        primaryStage.setHeight(720);

        // Headless UI checks live in the JUnit tests of this module (see src/test/java):
        // the whole shell is built there on the JavaFX thread and asserted, so nothing
        // about it is wired into the application itself.
        primaryStage.setScene(scene);
        primaryStage.show();

        // Dev convenience: apply style edits without a restart (no-op in packaged builds).
        CssLiveReload.install(scene, appCss);

        // Optional: open a configuration file handed in on the command line. Two shapes are
        // accepted, because two launchers exist:
        //   --open <file>   the development launcher "bin/run.sh some.ifacex";
        //   <file>          the packaged (jpackage) launcher, which the OS passes verbatim when
        //                   the user double-clicks a registered .ifcx/.ifacex file or uses
        //                   "Open With". The association is declared in product/packaging/.
        final Path toOpen = CommandLine.openTarget(args);
        if (toOpen != null) {
            try {
                controller.openFile(toOpen);
            } catch (Exception e) {
                System.err.println("Could not open " + toOpen + ": " + e);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>The framework has already stored the application configuration and the recent files on
     * exit (see {@link ch.autumo.ui.fx.BaseApp#stop()}); the Studio has nothing else to release.</p>
     */
    @Override
    public void finish(Stage primaryStage) {
        // nothing to clean up
    }

    /**
     * The Studio's application base resource path; the framework loads {@code main.fxml},
     * {@code html/*.html}, the PO catalogue and the images from here.
     *
     * @return base resource path, e.g. {@code /ch/autumo/ifacex/studio/ui/}
     */
    @Override
    public String getBaseResourcePath() {
        return BASE;
    }

    /**
     * {@inheritDoc}
     *
     * @return the bundled {@code cfg/app.cfg} template of the Studio
     */
    @Override
    public String getConfigTemplateFile() {
        return BASE + "cfg/app.cfg";
    }

    /**
     * {@inheritDoc}
     *
     * @return the bundled base-configuration template, {@code cfg/base.ifc}
     */
    @Override
    public String getBaseConfigTemplateFile() {
        return BASE + "cfg/base.ifc";
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
        return "0.1.0-SNAPSHOT";
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

    /**
     * {@inheritDoc}
     *
     * <p><b>This is the switch that writes {@code cfg/app.cfg}.</b> The Studio is an installed
     * application: on the first start no application configuration exists in the user's
     * application data directory, so the framework hands {@code null} to
     * {@link #preInitialize(Stage, Properties)} and runs the installer instead - the steps listed
     * in {@code ui/…/ui/cfg/installer-steps.cfg}, the last of which calls
     * {@link ch.autumo.ui.fx.BaseApp#completed(ProcessException)}. That writes the application
     * configuration into the user's application data directory
     * ({@code <app data>/autumo/ifaceXStudioFX/cfg/app.cfg}) and calls
     * {@link #initialize(Stage, Properties)} afterwards.</p>
     *
     * <p>From then on that file is the configuration: the general values of the bundled template
     * {@code ui/…/ui/cfg/app.cfg} plus every user setting - the recent files, {@code language} and
     * the values the installer steps collect via
     * {@link ch.autumo.ui.fx.BaseApp#setConfigurationParameter(String, String)}. The template
     * itself is never touched; it is the general configuration the file is created from.</p>
     *
     * @return {@code false}: the Studio is installed, not portable
     */
    @Override
    protected boolean isPortableApp() {
        return false;
    }
}
