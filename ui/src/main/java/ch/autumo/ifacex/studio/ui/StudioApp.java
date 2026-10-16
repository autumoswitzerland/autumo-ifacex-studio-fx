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
 * 
 */
package ch.autumo.ifacex.studio.ui;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import ch.autumo.ui.fx.BaseApp;
import ch.autumo.ui.utils.Helper;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 * Studio application entry point, built on the <b>autumo-ui</b> framework.
 *
 * <p>The framework ({@link BaseApp}) provides the application shell and product concerns
 * (lifecycle, installer, licensing/registration, i18n, theming, recent files, update check).
 * The Studio-specific editor UI — the static {@code main.fxml} shell and the metadata-driven
 * {@link FormBuilder} forms — stays our own.</p>
 *
 * <p>This milestone runs in {@link #isPortableApp() portable mode} (no installer/licensing) so
 * development does not require a license. The installer and registration/licensing UI are wired
 * in later. See docs 04 and 07 (decision D12).</p>
 */
public class StudioApp extends BaseApp {

    /** Application base resource path (autumo-ui convention). */
    private static final String BASE = "/ch/autumo/ifacex/studio/ui/";

    /** Current application instance (the framework creates exactly one). */
    private static StudioApp instance;

    private Parent root;
    private MainController controller;

    /**
     * The running application instance, or {@code null} before start-up.
     *
     * @return application instance
     */
    public static StudioApp get() {
        return instance;
    }

    @Override
    protected Pane preInitialize(Stage primaryStage, Properties configuration) {
        instance = this;
        loadFonts();
        final URL fxml = StudioApp.class.getResource("main.fxml");
        final FXMLLoader loader = new FXMLLoader(fxml);
        try {
            root = loader.load();
        } catch (IOException e) {
            throw new IllegalStateException("Could not load main.fxml", e);
        }
        controller = loader.getController();
        return (Pane) root;
    }

    @Override
    protected void initialize(Stage primaryStage, Properties configuration) {
        final List<String> args = getParameters().getRaw();

        // Build the scene and apply the theme (autumo-ui dark base + our overrides).
        final Scene scene = new Scene(root, 1100, 720);
        scene.getStylesheets().add(Helper.getBaseStyle());
        scene.getStylesheets().add(
                StudioApp.class.getResource("studio.css").toExternalForm());

        // Headless self-test: load a config file, exercise the UI, print a summary and exit.
        if (args.size() >= 2 && "--selftest".equals(args.get(0))) {
            try {
                System.out.println("SELFTEST: " + controller.selfTest(Path.of(args.get(1))));
                System.out.println("SELFTEST-STYLES: count=" + scene.getStylesheets().size()
                        + " " + scene.getStylesheets());
            } catch (Exception e) {
                System.out.println("SELFTEST-FAILED: " + e);
            }
            Platform.exit();
            return;
        }

        primaryStage.setScene(scene);
        primaryStage.show();

        // Optional: preload a configuration file, e.g. "bin/run.sh some.ifacex".
        if (args.size() >= 2 && "--open".equals(args.get(0))) {
            try {
                controller.loadConfig(Path.of(args.get(1)));
            } catch (Exception e) {
                System.err.println("Could not open " + args.get(1) + ": " + e);
            }
        }
    }

    @Override
    public void finish(Stage primaryStage) {
        // nothing to clean up in portable mode
    }

    /**
     * Loads the bundled <b>Inter</b> font faces (SIL OFL, see {@code fonts/Inter-OFL.txt}) so the
     * UI has the modern web-GUI look. Called before the scene is built; the family is applied in
     * {@code studio.css}.
     */
    private static void loadFonts() {
        for (final String face : new String[] {
                "Inter-Regular.ttf", "Inter-Medium.ttf", "Inter-SemiBold.ttf", "Inter-Bold.ttf" }) {
            final URL url = StudioApp.class.getResource("fonts/" + face);
            if (url != null) {
                Font.loadFont(url.toExternalForm(), 14);
            } else {
                System.err.println("Studio: bundled font not found: " + face);
            }
        }
    }

    @Override
    public String getBaseResourcePath() {
        return BASE;
    }

    @Override
    public String getConfigTemplateFile() {
        return BASE + "cfg/app.cfg";
    }

    @Override
    public String getBaseConfigTemplateFile() {
        return BASE + "cfg/base.ifc";
    }

    @Override
    public String getInMemoryConfigName() {
        return "ifacex-studio-fx";
    }

    @Override
    public String getAppVersion() {
        return "0.1.0-SNAPSHOT";
    }

    @Override
    public String getAppTitle() {
        return "ifaceX Studio FX";
    }

    @Override
    public String getInternalAppName() {
        return "ifaceXStudioFX";
    }

    @Override
    protected boolean isPortableApp() {
        return true;
    }
}
