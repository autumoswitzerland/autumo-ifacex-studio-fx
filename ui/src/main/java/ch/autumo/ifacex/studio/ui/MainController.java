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
import java.util.Objects;

import ch.autumo.ifacex.studio.core.io.PropertiesConfigReader;
import ch.autumo.ifacex.studio.core.io.PropertiesConfigWriter;
import ch.autumo.ifacex.studio.core.io.YamlConfigReader;
import ch.autumo.ifacex.studio.core.io.YamlConfigWriter;
import ch.autumo.ifacex.studio.core.model.IpcConfig;
import ch.autumo.ifacex.studio.core.model.RwConfig;

import ch.autumo.ui.Constants;
import ch.autumo.ui.utils.Helper;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/**
 * Controller for the FXML shell. Static layout comes from {@code main.fxml}; the configuration
 * forms are produced by {@link FormBuilder}.
 */
public class MainController {

    /** Toolbar icons ship with the autumo-ui framework (base resource path). */
    private static final String ICONS = "icons/";

    static final String K_ROOT = "ROOT";
    static final String K_GENERAL = "GENERAL";
    static final String K_SOURCE = "SOURCE";
    static final String K_DEST = "DEST";
    static final String K_ENTITY = "ENTITY";

    /** Style class that renders a navigation entry in bold (see studio.css). */
    private static final String NAV_EMPHASIS = "nav-emphasis";

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
    private ToolBar leftToolBar;
    @FXML
    private ToolBar rightToolBar;
    @FXML
    private MenuItem miNew;
    @FXML
    private MenuItem miOpen;
    @FXML
    private MenuItem miSave;
    @FXML
    private MenuItem miExportIfcx;
    @FXML
    private MenuItem miExportIfacex;

    private final FormBuilder formBuilder = new FormBuilder(this::onModelChanged, this::rebuild);

    private IpcConfig config;
    private Path sourceFile;
    /** Toolbar buttons that only make sense once a configuration is loaded. */
    private Button tbSave;
    private Button tbClose;
    private Button tbConfig;
    private TreeItem<FormBuilder.NavNode> rootItem;

    @FXML
    private void initialize() {
        navTree.setCellFactory(tv -> navCell());
        navTree.getSelectionModel().selectedItemProperty().addListener(
                (o, a, b) -> showForm(b));
        formScroll.setContent(FormBuilder.message("Open a .ifacex or .ifcx file to begin."));
        buildToolBar();
        buildMenuIcons();
        setConfigActionsEnabled(false);
    }

    /**
     * Tree cell that renders a coloured interface-type pill in front of the node label, so the
     * reader/writer type (CSV, JSON, DB, Mongo DB, REST, Mail, …) is visible at a glance — the
     * legacy GUI carried this in the colour-coded tabs. See {@link FormBuilder#typePill(String)}.
     */
    private TreeCell<FormBuilder.NavNode> navCell() {
        return new TreeCell<>() {
            @Override
            protected void updateItem(FormBuilder.NavNode item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    getStyleClass().remove(NAV_EMPHASIS);
                    return;
                }
                setText(item.label());
                setGraphic(item.payload() instanceof RwConfig rw
                        ? FormBuilder.typePill(rw.name()) : null);
                // The global values and the section headers stand out, as asked for.
                final boolean bold = K_GENERAL.equals(item.kind())
                        || K_SOURCE.equals(item.kind())
                        || K_DEST.equals(item.kind());
                if (bold) {
                    if (!getStyleClass().contains(NAV_EMPHASIS)) {
                        getStyleClass().add(NAV_EMPHASIS);
                    }
                } else {
                    getStyleClass().remove(NAV_EMPHASIS);
                }
            }
        };
    }

    /**
     * The toolbar mirrors the original Studio editor: New / Open / Save / Close, then
     * Configuration, Run and External Edit, with the ifaceX mark on the right. Icons come
     * from the autumo-ui framework (48px, label below).
     */
    private void buildToolBar() {
        final Button tbNew = toolButton("project-new-48.png", "New",
                "Create a new, empty configuration", () -> onNew());
        final Button tbOpen = toolButton("project-open-48.png", "Open",
                "Open an .ifacex or .ifcx configuration", () -> onOpen());
        tbSave = toolButton("project-save-48.png", "Save",
                "Save as .ifcx (new format)", () -> onSave());
        tbClose = toolButton("project-close-48.png", "Close",
                "Close the current configuration", () -> onClose());

        tbConfig = toolButton("settings-48.png", "Configuration",
                "Edit file metadata and job wiring (selects the root node)", this::selectRoot);
        final Button tbRun = toolButton("process-48.png", "Run",
                "Run the IPC — engine integration follows later", null);
        final Button tbFolder = toolButton("folder-48.png", "External Edit",
                "Open in an external editor — follows later", null);

        leftToolBar.getItems().addAll(
                tbNew, tbOpen, tbSave, tbClose,
                sep(), tbConfig,
                sep(), tbRun,
                sep(), tbFolder);

        // Right side mirrors the original Studio: the ifaceX Studio title logo and, to its
        // right, the clickable ifaceX mark (label underneath) that opens the website.
        rightToolBar.getItems().addAll(titleLogo(), ifaceXButton());
    }

    /** The ifaceX Studio title logo (banner) as in the original toolbar; not clickable. */
    private HBox titleLogo() {
        final ImageView iv = new ImageView(new Image(
                StudioApp.class.getResource("images/ifacex-studio-reddot2-40.png").toExternalForm()));
        iv.setSmooth(true);
        iv.setPreserveRatio(true);
        iv.setFitHeight(40);
        final HBox wrap = new HBox(iv);
        wrap.setAlignment(Pos.CENTER);
        wrap.setPadding(new Insets(0, 14, 0, 6));
        return wrap;
    }

    /** The ifaceX mark with the label underneath; opens the configured website URL. */
    private Button ifaceXButton() {
        final Button b = new Button("ifaceX", icon("ifaceX48.png"));
        b.setContentDisplay(ContentDisplay.TOP);
        b.getStyleClass().add("toolbar-button");
        b.setTooltip(new Tooltip("Visit the ifaceX website"));
        b.setOnAction(e -> Helper.openURL(
                configUrl(Constants.CONFIG_ENTRY_URL_WEB, Constants.DEF_CONFIG_ENTRY_URL_WEB)));
        return b;
    }

    private String configUrl(String key, String fallback) {
        final StudioApp app = StudioApp.get();
        final String url = app != null ? app.getConfigurationParameter(key) : null;
        return url != null && !url.isBlank() ? url : fallback;
    }

    private void buildMenuIcons() {
        miNew.setGraphic(icon("project-new-16.png"));
        miOpen.setGraphic(icon("project-open-16.png"));
        miSave.setGraphic(icon("project-save-16.png"));
        miExportIfcx.setGraphic(icon("project-save-16.png"));
        miExportIfacex.setGraphic(icon("project-save-16.png"));
    }

    private Button toolButton(String iconFile, String text, String tip, Runnable action) {
        final Button b = new Button(text, icon(iconFile));
        b.setContentDisplay(ContentDisplay.TOP);
        b.getStyleClass().add("toolbar-button");
        b.setTooltip(new Tooltip(tip));
        if (action == null) {
            b.setDisable(true);
        } else {
            b.setOnAction(e -> action.run());
        }
        return b;
    }

    private ImageView icon(String iconFile) {
        return new ImageView(new Image(
                Helper.getBaseResource(ICONS + iconFile), 0, 0, true, true));
    }

    private Separator sep() {
        return new Separator(Orientation.VERTICAL);
    }

    private void setConfigActionsEnabled(boolean enabled) {
        tbSave.setDisable(!enabled);
        tbClose.setDisable(!enabled);
        tbConfig.setDisable(!enabled);
    }

    private void selectRoot() {
        if (rootItem != null) {
            navTree.getSelectionModel().select(rootItem);
        }
    }

    /** Count toolbar buttons whose icon image actually loaded (width &gt; 0). */
    private int toolbarIconsOk() {
        int ok = 0;
        for (final ToolBar bar : new ToolBar[] {leftToolBar, rightToolBar}) {
            for (final Node n : bar.getItems()) {
                if (n instanceof Button b && b.getGraphic() instanceof ImageView iv
                        && iv.getImage() != null && iv.getImage().getWidth() > 0) {
                    ok++;
                }
            }
        }
        return ok;
    }

    private int toolbarItemCount() {
        return leftToolBar.getItems().size() + rightToolBar.getItems().size();
    }

    @FXML
    private void onNew() {
        config = new IpcConfig();
        sourceFile = null;
        rebuild();
        status("New configuration");
    }

    @FXML
    private void onClose() {
        config = null;
        sourceFile = null;
        rootItem = null;
        navTree.setRoot(null);
        rawYaml.clear();
        rawProps.clear();
        formScroll.setContent(FormBuilder.message("Open a .ifacex or .ifcx file to begin."));
        setConfigActionsEnabled(false);
        status("Closed");
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
        final int[] acc = new int[4]; // forms, list views (source fields), tables, dnd buttons
        for (final TreeItem<FormBuilder.NavNode> child : root.getChildren()) {
            countForms(child, acc);
        }
        final var destNode = firstDestinationEntity(root);
        if (destNode != null) {
            showForm(destNode);
        }
        final Node rootForm = formBuilder.build(root.getValue());
        final int rootFormParts = rootForm instanceof javafx.scene.layout.Pane p
                ? p.getChildren().size() : -1;
        return "nodes=" + countNodes(root)
                + " forms=" + acc[0]
                + " rootFormParts=" + rootFormParts
                + " toolbarItems=" + toolbarItemCount()
                + " toolbarIconsOk=" + toolbarIconsOk()
                + " fieldLists=" + acc[1]
                + " mappingTables=" + acc[2]
                + " interFontOk=" + javafx.scene.text.Font.getFamilies().contains("Inter")
                + " yamlChars=" + rawYaml.getText().length()
                + " propsChars=" + rawProps.getText().length()
                + " formClass=" + (formScroll.getContent() == null
                        ? "null" : formScroll.getContent().getClass().getSimpleName())
                + " statusFileOk=" + (statusLabel.getText() != null
                        && statusLabel.getText().startsWith("File: "))
                + " rootLabel=\"" + root.getValue().label() + "\""
                + " title=\"" + (config.meta().title() == null ? "" : config.meta().title()) + "\""
                + " dndButtons=" + acc[3]
                + " statusOk=" + (statusLabel.getText() != null && !statusLabel.getText().isBlank());
    }

    /** Builds every nav node's form and accumulates [forms, listViews, tables, dndButtons]. */
    private void countForms(TreeItem<FormBuilder.NavNode> item, int[] acc) {
        final Node n = formBuilder.build(item.getValue());
        if (n != null) {
            acc[0]++;
            acc[1] += count(n, javafx.scene.control.ListView.class);
            acc[2] += count(n, javafx.scene.control.TableView.class);
            acc[3] += countDnd(n);
        }
        for (final TreeItem<FormBuilder.NavNode> c : item.getChildren()) {
            countForms(c, acc);
        }
    }

    /** Counts the auto-mapping drag-and-drop buttons ({@code .dnd-button}). */
    private static int countDnd(Node node) {
        int n = node.getStyleClass().contains("dnd-button") ? 1 : 0;
        if (node instanceof javafx.scene.Parent p) {
            for (final Node child : p.getChildrenUnmodifiable()) {
                n += countDnd(child);
            }
        }
        return n;
    }

    private static int count(Node node, Class<?> type) {
        int n = type.isInstance(node) ? 1 : 0;
        if (node instanceof javafx.scene.Parent p) {
            for (final Node child : p.getChildrenUnmodifiable()) {
                n += count(child, type);
            }
        }
        return n;
    }

    private int countNodes(TreeItem<FormBuilder.NavNode> item) {
        int n = 1;
        for (final TreeItem<FormBuilder.NavNode> c : item.getChildren()) {
            n += countNodes(c);
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
        final TreeItem<FormBuilder.NavNode> selected = navTree.getSelectionModel().getSelectedItem();
        final FormBuilder.NavNode prev = selected == null ? null : selected.getValue();
        formBuilder.setConfig(config);

        final TreeItem<FormBuilder.NavNode> root = new TreeItem<>(
                new FormBuilder.NavNode(K_ROOT, label(), config, null));
        root.setExpanded(true);

        root.getChildren().add(new TreeItem<>(
                new FormBuilder.NavNode(K_GENERAL, "Overwritten Values", config.general(), null)));

        for (final RwConfig reader : config.readers().values()) {
            root.getChildren().add(sectionItem(reader, K_SOURCE, "Source: " + reader.name()));
        }
        for (final RwConfig writer : config.writers().values()) {
            root.getChildren().add(sectionItem(writer, K_DEST, "Destination: " + writer.name()));
        }

        navTree.setRoot(root);
        rootItem = root;
        final TreeItem<FormBuilder.NavNode> restore = findNode(root, prev);
        navTree.getSelectionModel().select(restore != null ? restore : root);
        setConfigActionsEnabled(true);
        refreshRaw();
    }

    /**
     * Re-find a previously selected node after a structural rebuild (add/remove/rename of a section
     * or entity), so the user stays where they were working.
     */
    private TreeItem<FormBuilder.NavNode> findNode(TreeItem<FormBuilder.NavNode> start,
            FormBuilder.NavNode target) {
        if (target == null) {
            return null;
        }
        final FormBuilder.NavNode v = start.getValue();
        if (v != null && v.kind().equals(target.kind())
                && Objects.equals(v.label(), target.label())
                && Objects.equals(v.payload2(), target.payload2())) {
            return start;
        }
        for (final TreeItem<FormBuilder.NavNode> c : start.getChildren()) {
            final TreeItem<FormBuilder.NavNode> found = findNode(c, target);
            if (found != null) {
                return found;
            }
        }
        return null;
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

    /**
     * The root navigation entry shows the configuration <em>title</em> ({@code meta.title}),
     * falling back to the file name / a default. It is re-derived on every model change so it
     * tracks the title typed in the form.
     */
    private String label() {
        if (config != null) {
            final String title = config.meta().title();
            if (title != null && !title.isBlank()) {
                return title.trim();
            }
        }
        return sourceFile == null ? "Configuration" : sourceFile.getFileName().toString();
    }

    /** Re-derive the root label after a model change, so it follows the title field. */
    private void refreshRootLabel() {
        if (rootItem == null || config == null) {
            return;
        }
        final String next = label();
        final FormBuilder.NavNode value = rootItem.getValue();
        if (value != null && !next.equals(value.label())) {
            rootItem.setValue(new FormBuilder.NavNode(K_ROOT, next, config, null));
        }
    }

    private void showForm(TreeItem<FormBuilder.NavNode> item) {
        if (item == null) {
            return;
        }
        formScroll.setContent(formBuilder.build(item.getValue()));
    }

    private void onModelChanged() {
        refreshRaw();
        refreshRootLabel();
        markModified();
    }

    private void refreshRaw() {
        if (config == null) {
            return;
        }
        rawYaml.setText(YamlConfigWriter.toText(config));
        rawProps.setText(PropertiesConfigWriter.toText(config));
    }

    @FXML
    private void onSave() {
        if (config == null) {
            status("Nothing to save.");
            return;
        }
        // Save always uses the new YAML format (.ifcx); .ifacex is export-only (legacy).
        export(YamlConfigWriter.toText(config), ".ifcx", "ifaceX YAML (new format)",
                "Save as .ifcx");
    }

    @FXML
    private void onExportIfcx() {
        export(YamlConfigWriter.toText(config), ".ifcx", "ifaceX YAML", "Export .ifcx");
    }

    @FXML
    private void onExportIfacex() {
        export(PropertiesConfigWriter.toText(config), ".ifacex", "Legacy properties",
                "Export .ifacex");
    }

    private void export(String text, String extension, String description, String title) {
        if (config == null) {
            status("Nothing to export.");
            return;
        }
        final FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
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
            status("Saved " + out.getName() + " (" + extension + ")");
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
        theme(alert);
        alert.showAndWait();
    }

    /** State shown behind the file: "Loaded", "Converted ...", "Modified", "Saved ...". */
    private String statusState = "";

    /** Sets the state part of the status bar and re-renders it. */
    private void status(String text) {
        statusState = text == null ? "" : text;
        renderStatus();
    }

    /**
     * The status bar reads like the original Studio: {@code File: <absolute path>} first, the
     * state behind it. Without a loaded file it shows {@code File: -}.
     */
    private void renderStatus() {
        final String file = sourceFile == null ? "File: -" : "File: " + sourceFile.toAbsolutePath();
        statusLabel.setText(statusState.isEmpty() ? file : file + "   \u2014   " + statusState);
    }

    /** Flag the configuration as changed, without repeating an existing "Modified". */
    private void markModified() {
        if (statusState.startsWith("Modified")) {
            return;
        }
        status("Modified");
    }

    private void error(String message, Exception e) {
        status(message);
        final Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(message);
        alert.setContentText(String.valueOf(e.getMessage()));
        alert.initOwner(window());
        theme(alert);
        alert.showAndWait();
    }

    /** Apply the autumo-ui dark theme to a dialog so it matches the main window. */
    private void theme(Dialog<?> dialog) {
        dialog.getDialogPane().getStylesheets().add(Helper.getBaseStyle());
    }

    private Window window() {
        return statusLabel.getScene() == null ? null : statusLabel.getScene().getWindow();
    }
}
