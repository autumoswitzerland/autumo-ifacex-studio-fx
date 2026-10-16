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

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
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
 * <p>This milestone runs in {@link #isPortableApp() portable mode} (no installer/licensing) so
 * development does not require a license. The installer and registration/licensing UI are wired
 * in later. See docs 04 and 07 (decision D12).</p>
 */
public class StudioApp extends BaseApp {

    /** Application base resource path (autumo-ui convention). */
    private static final String BASE = "/ch/autumo/ifacex/studio/ui/";

    private Parent root;
    private MainController controller;

    @Override
    protected Pane preInitialize(Stage primaryStage, Properties configuration) {
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
        // Headless self-test: load a config file, exercise the UI, print a summary and exit.
        final List<String> args = getParameters().getRaw();
        if (args.size() >= 2 && "--selftest".equals(args.get(0))) {
            try {
                System.out.println("SELFTEST: " + controller.selfTest(Path.of(args.get(1))));
            } catch (Exception e) {
                System.out.println("SELFTEST-FAILED: " + e);
            }
            Platform.exit();
            return;
        }

        final Scene scene = new Scene(root, 1100, 720);
        scene.getStylesheets().add(
                StudioApp.class.getResource("studio.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void finish(Stage primaryStage) {
        // nothing to clean up in portable mode
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
