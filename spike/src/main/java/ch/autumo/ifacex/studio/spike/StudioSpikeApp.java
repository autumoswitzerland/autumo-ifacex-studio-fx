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
package ch.autumo.ifacex.studio.spike;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import ch.autumo.ifacex.studio.core.io.PropertiesConfigWriter;
import ch.autumo.ifacex.studio.core.io.YamlConfigWriter;
import ch.autumo.ifacex.studio.core.model.IpcConfig;
import ch.autumo.ui.fx.BaseApp;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Spike: proves that the Studio can be built on top of the <b>autumo-ui</b> framework.
 *
 * <p>Runs in {@link #isPortableApp() portable mode}: no installer, no licensing, no writes —
 * just the framework lifecycle plus our UI-free {@code core} conversion. This validates the
 * integration (lifecycle, resources, logging/configuration conventions) before committing to
 * the framework as the Studio's shell.</p>
 */
public class StudioSpikeApp extends BaseApp {

    private static final String BASE = "/ch/autumo/ifacex/studio/spike/";

    /** A template used to demonstrate core integration (may be absent on other machines). */
    private static final String SAMPLE = System.getProperty("ifacex.sample",
            "/Users/Mike/Development/git-ext/autumo-ifacex-templates/009-spotify.ifacex");

    private BorderPane root;

    @Override
    protected Pane preInitialize(Stage primaryStage, Properties configuration) {
        root = new BorderPane();
        root.setCenter(buildBody(configuration));
        return root;
    }

    @Override
    protected void initialize(Stage primaryStage, Properties configuration) {
        if (Boolean.getBoolean("ifacex.selftest")) {
            System.out.println("=== SPIKE SELFTEST ===");
            System.out.println(coreProbe());
            System.out.println("=== END ===");
            javafx.application.Platform.exit();
            return;
        }
        primaryStage.setScene(new Scene(root, 900, 620));
        primaryStage.show();
    }

    private Pane buildBody(Properties configuration) {
        final VBox box = new VBox(10);
        box.setPadding(new Insets(14));

        box.getChildren().add(new Label("autumo-ui shell: OK"));
        box.getChildren().add(new Label("app: " + getAppTitle() + " " + getAppVersion()
                + "   (internal: " + getInternalAppName() + ")"));
        box.getChildren().add(new Label("portable: " + isPortableApp()
                + "   installed: " + isInstalled()));
        box.getChildren().add(new Label("cfg keys loaded: " + configuration.size()));

        final TextArea out = new TextArea();
        out.setEditable(false);
        out.setPrefRowCount(18);
        out.setStyle("-fx-font-family: 'Menlo','Consolas',monospace; -fx-font-size: 12px;");
        VBox.setVgrow(out, javafx.scene.layout.Priority.ALWAYS);

        out.setText(coreProbe());
        box.getChildren().add(out);
        return box;
    }

    /** Exercise the UI-free core from within the autumo-ui application to prove linkage. */
    private String coreProbe() {
        final StringBuilder sb = new StringBuilder();
        sb.append("core integration probe\n");
        sb.append("----------------------\n");
        final Path sample = Path.of(SAMPLE);
        if (!Files.exists(sample)) {
            sb.append("sample not found: ").append(sample).append('\n');
            return sb.toString();
        }
        try {
            final IpcConfig cfg = ch.autumo.ifacex.studio.core.io.PropertiesConfigReader.read(sample);
            sb.append("parsed: ").append(sample.getFileName()).append('\n');
            sb.append("  sourceInterface      = ").append(cfg.sourceInterface()).append('\n');
            sb.append("  destinationInterfaces= ").append(cfg.destinationInterfaces()).append('\n');
            sb.append("  processSourceEntities= ").append(cfg.processSourceEntities()).append('\n');
            sb.append("  readers              = ").append(cfg.readers().keySet()).append('\n');
            sb.append("  writers              = ").append(cfg.writers().keySet()).append('\n');
            sb.append('\n');
            sb.append("YAML (.ifcx) preview:\n");
            final String yaml = YamlConfigWriter.toText(cfg);
            sb.append(yaml, 0, Math.min(1200, yaml.length()));
            sb.append('\n');
            sb.append("properties (.ifacex) chars: ")
                    .append(PropertiesConfigWriter.toText(cfg).length()).append('\n');
        } catch (Exception e) {
            sb.append("core probe failed: ").append(e).append('\n');
        }
        return sb.toString();
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
