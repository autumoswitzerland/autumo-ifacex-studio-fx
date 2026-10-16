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

import java.io.File;
import java.nio.file.Path;
import java.util.Locale;

import ch.autumo.ifacex.studio.core.io.PropertiesConfigReader;
import ch.autumo.ifacex.studio.core.io.PropertiesConfigWriter;
import ch.autumo.ifacex.studio.core.io.YamlConfigReader;
import ch.autumo.ifacex.studio.core.io.YamlConfigWriter;
import ch.autumo.ifacex.studio.core.model.IpcConfig;
import ch.autumo.ifacex.studio.core.model.RwConfig;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/**
 * Controller for the FXML shell. Static layout comes from {@code main.fxml}; the configuration
 * forms are produced by {@link FormBuilder}.
 */
public class MainController {

    static final String K_GENERAL = "GENERAL";
    static final String K_SOURCE = "SOURCE";
    static final String K_DEST = "DEST";
    static final String K_ENTITY = "ENTITY";

    @FXML
    private TreeView<FormBuilder.NavNode> navTree;
    @FXML
    private ScrollPane formScroll;
    @FXML
    private TextArea rawYaml;
    @FXML
    private TextArea rawProps;
    @FXML
    private Label statusLabel;
    @FXML
    private Button exportIfcxButton;
    @FXML
    private Button exportIfacexButton;

    private final FormBuilder formBuilder = new FormBuilder(this::onModelChanged);

    private IpcConfig config;
    private Path sourceFile;

    @FXML
    private void initialize() {
        navTree.getSelectionModel().selectedItemProperty().addListener(
                (o, a, b) -> showForm(b));
        formScroll.setContent(new Label("Open a .ifacex or .ifcx file to begin."));
    }

    @FXML
    private void onOpen() {
        final FileChooser chooser = new FileChooser();
        chooser.setTitle("Open configuration");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("ifaceX configuration", "*.ifacex", "*.ifcx", "*.yaml", "*.yml"),
                new FileChooser.ExtensionFilter("Legacy properties", "*.ifacex"),
                new FileChooser.ExtensionFilter("YAML", "*.ifcx", "*.yaml", "*.yml"));
        final File file = chooser.showOpenDialog(window());
        if (file != null) {
            openFile(file.toPath());
        }
    }

    private void openFile(Path path) {
        try {
            loadConfig(path);
        } catch (Exception e) {
            error("Could not read " + path.getFileName(), e);
        }
    }

    /** Parse and load a configuration, rebuilding the navigation, forms and raw views. */
    void loadConfig(Path path) throws Exception {
        final String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        config = name.endsWith(".ifacex")
                ? PropertiesConfigReader.read(path)
                : YamlConfigReader.read(path);
        sourceFile = path;
        rebuild();
        final boolean legacy = name.endsWith(".ifacex");
        status((legacy ? "Converted legacy .ifacex: " : "Loaded ") + path.getFileName()
                + "  \u2014  reader=" + (config.reader() == null ? "-" : config.reader().name())
                + ", writers=" + config.writers().keySet()
                + ", entities=" + config.processSourceEntities());
    }

    /** Headless self-test support: load a file and describe what the UI would show. */
    String selfTest(Path path) throws Exception {
        loadConfig(path);
        final TreeItem<FormBuilder.NavNode> root = navTree.getRoot();
        // exercise the form builder for every nav node
        int forms = 0;
        for (final TreeItem<FormBuilder.NavNode> child : root.getChildren()) {
            forms += countForms(child);
        }
        final var destNode = firstDestinationEntity(root);
        if (destNode != null) {
            showForm(destNode);
        }
        return "nodes=" + countNodes(root)
                + " forms=" + forms
                + " yamlChars=" + rawYaml.getText().length()
                + " propsChars=" + rawProps.getText().length()
                + " formClass=" + (formScroll.getContent() == null
                        ? "null" : formScroll.getContent().getClass().getSimpleName())
                + " statusOk=" + (statusLabel.getText() != null && !statusLabel.getText().isBlank());
    }

    private int countNodes(TreeItem<FormBuilder.NavNode> item) {
        int n = 1;
        for (final TreeItem<FormBuilder.NavNode> c : item.getChildren()) {
            n += countNodes(c);
        }
        return n;
    }

    private int countForms(TreeItem<FormBuilder.NavNode> item) {
        int n = formBuilder.build(item.getValue()) != null ? 1 : 0;
        for (final TreeItem<FormBuilder.NavNode> c : item.getChildren()) {
            n += countForms(c);
        }
        return n;
    }

    private TreeItem<FormBuilder.NavNode> firstDestinationEntity(TreeItem<FormBuilder.NavNode> root) {
        for (final TreeItem<FormBuilder.NavNode> child : root.getChildren()) {
            if (child.getValue() != null && K_DEST.equals(child.getValue().kind())
                    && !child.getChildren().isEmpty()) {
                return child.getChildren().get(0);
            }
        }
        return null;
    }

    private void rebuild() {
        final TreeItem<FormBuilder.NavNode> root = new TreeItem<>(
                new FormBuilder.NavNode("ROOT", label(), config, null));
        root.setExpanded(true);

        root.getChildren().add(new TreeItem<>(
                new FormBuilder.NavNode(K_GENERAL, "General", config.general(), null)));

        for (final RwConfig reader : config.readers().values()) {
            root.getChildren().add(sectionItem(reader, K_SOURCE, "Source: " + reader.name()));
        }
        for (final RwConfig writer : config.writers().values()) {
            root.getChildren().add(sectionItem(writer, K_DEST, "Destination: " + writer.name()));
        }

        navTree.setRoot(root);
        if (!root.getChildren().isEmpty()) {
            navTree.getSelectionModel().select(root.getChildren().get(0));
        }
        exportIfcxButton.setDisable(false);
        exportIfacexButton.setDisable(false);
        refreshRaw();
    }

    private TreeItem<FormBuilder.NavNode> sectionItem(RwConfig rw, String kind, String label) {
        final TreeItem<FormBuilder.NavNode> item = new TreeItem<>(
                new FormBuilder.NavNode(kind, label, rw, null));
        item.setExpanded(true);
        for (final String entity : rw.entities().keySet()) {
            item.getChildren().add(new TreeItem<>(
                    new FormBuilder.NavNode(K_ENTITY, entity, rw, entity)));
        }
        return item;
    }

    private String label() {
        if (sourceFile == null) {
            return "Configuration";
        }
        return sourceFile.getFileName().toString();
    }

    private void showForm(TreeItem<FormBuilder.NavNode> item) {
        if (item == null) {
            return;
        }
        formScroll.setContent(formBuilder.build(item.getValue()));
    }

    private void onModelChanged() {
        refreshRaw();
        if (statusLabel.getText() != null && !statusLabel.getText().startsWith("Modified")) {
            statusLabel.setText("Modified — " + statusLabel.getText());
        }
    }

    private void refreshRaw() {
        if (config == null) {
            return;
        }
        rawYaml.setText(YamlConfigWriter.toText(config));
        rawProps.setText(PropertiesConfigWriter.toText(config));
    }

    @FXML
    private void onExportIfcx() {
        export(YamlConfigWriter.toText(config), ".ifcx", "ifaceX YAML");
    }

    @FXML
    private void onExportIfacex() {
        export(PropertiesConfigWriter.toText(config), ".ifacex", "Legacy properties");
    }

    private void export(String text, String extension, String description) {
        if (config == null) {
            status("Nothing to export.");
            return;
        }
        final FileChooser chooser = new FileChooser();
        chooser.setTitle("Export " + extension);
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(description, "*" + extension));
        if (sourceFile != null) {
            final String base = sourceFile.getFileName().toString().replaceAll("\\.[^.]+$", "");
            chooser.setInitialFileName(base + extension);
        }
        final File out = chooser.showSaveDialog(window());
        if (out == null) {
            return;
        }
        try {
            java.nio.file.Files.writeString(out.toPath(), text);
            status("Exported " + out.getName());
        } catch (Exception e) {
            error("Could not write " + out.getName(), e);
        }
    }

    @FXML
    private void onExit() {
        window().hide();
    }

    @FXML
    private void onAbout() {
        final Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("ifaceX Studio FX — PoC");
        alert.setContentText("Metadata-driven configuration editor.\n"
                + "Shell: FXML.  Forms: programmatic.\n"
                + "\u00A9 2026 autumo GmbH");
        alert.initOwner(window());
        alert.showAndWait();
    }

    private void status(String text) {
        statusLabel.setText(text);
    }

    private void error(String message, Exception e) {
        status(message);
        final Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(message);
        alert.setContentText(String.valueOf(e.getMessage()));
        alert.initOwner(window());
        alert.showAndWait();
    }

    private Window window() {
        return statusLabel.getScene() == null ? null : statusLabel.getScene().getWindow();
    }
}
