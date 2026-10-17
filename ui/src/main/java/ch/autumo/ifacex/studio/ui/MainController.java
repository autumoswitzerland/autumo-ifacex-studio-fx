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

import static ch.autumo.ifacex.studio.core.i18n.Messages.format;
import static ch.autumo.ifacex.studio.core.i18n.Messages.t;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;

import ch.autumo.ifacex.studio.core.i18n.Messages;
import ch.autumo.ifacex.studio.core.io.PropertiesConfigReader;
import ch.autumo.ifacex.studio.core.io.PropertiesConfigWriter;
import ch.autumo.ifacex.studio.core.io.YamlConfigReader;
import ch.autumo.ifacex.studio.core.io.YamlConfigWriter;
import ch.autumo.ifacex.studio.core.meta.FieldCatalog;
import ch.autumo.ifacex.studio.core.model.IpcConfig;
import ch.autumo.ifacex.studio.core.model.RwConfig;

import ch.autumo.ui.Constants;
import ch.autumo.ui.fx.BaseApp;
import ch.autumo.ui.fx.controller.BaseController;
import ch.autumo.ui.fx.model.BaseModel;
import ch.autumo.ui.utils.Helper;
import ch.autumo.ui.utils.UI;

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
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Controller of the main window.
 *
 * <p>The static shell comes from {@code main.fxml}; every configuration form is produced by
 * {@link FormBuilder}. The controller extends the autumo-ui {@link BaseController}, so the
 * framework supplies the standard behaviour: the HTML info panes (About, …), the macOS menu
 * detach when the menu bar is overwritten by {@link #getMenuBar()}, and the keyboard shortcuts
 * installed by {@code ch.autumo.ui.utils.UI#setup}.</p>
 *
 * <p>Lifecycle: the FXMLLoader injects the {@code @FXML} fields and then calls
 * {@link BaseController#initialize(URL, ResourceBundle)}}, which handles macOS and finally
 * delegates to {@link #init(URL, ResourceBundle, double)}.</p>
 *
 * @see MainView
 * @see StudioApp#preInitialize(Stage, java.util.Properties)
 */
public class MainController extends BaseController {

    /** Toolbar icons ship with the autumo-ui framework (base resource path). */
    private static final String ICONS = "icons/";

    static final String K_ROOT = "ROOT";
    static final String K_GENERAL = "GENERAL";
    static final String K_SOURCE = "SOURCE";
    static final String K_DEST = "DEST";
    static final String K_ENTITY = "ENTITY";

    /** Style class that renders a navigation entry in bold (see studio.css). */
    private static final String NAV_EMPHASIS = "nav-emphasis";

    /**
     * Creates the controller of the main view.
     *
     * <p>The instance is created by hand and handed to {@code UI.setup(…)} — the FXML file no
     * longer declares {@code fx:controller}.</p>
     *
     * @param app   the application
     * @param stage the primary stage
     * @param model the main model
     * @param view  the main view
     */
    public MainController(BaseApp app, Stage stage, BaseModel model, Pane view) {
        super(app, stage, model, view);
    }

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
    @FXML
    private MenuItem miClose;
    @FXML
    private MenuItem miAbout;
    @FXML
    private MenuItem miLicense;
    @FXML
    private MenuItem miExit;
    @FXML
    private MenuBar menuBar;
    @FXML
    private Menu menuFile;
    @FXML
    private Menu menuRecent;
    @FXML
    private Menu menuConfig;
    @FXML
    private MenuItem miEncoder;
    @FXML
    private MenuItem miRemoveAppData;
    @FXML
    private MenuItem miDocumentation;
    @FXML
    private MenuItem miWebsite;
    @FXML
    private MenuItem miUpdate;
    @FXML
    private Menu menuHelp;
    @FXML
    private Menu menuLanguage;
    @FXML
    private RadioMenuItem miLangEn;
    @FXML
    private RadioMenuItem miLangDe;
    @FXML
    private Tab tabForm;
    @FXML
    private Tab tabRawYaml;
    @FXML
    private Tab tabRawProps;

    private final FormBuilder formBuilder = new FormBuilder(app, this::onModelChanged, this::rebuild);

    private IpcConfig config;
    private Path sourceFile;
    /** Toolbar buttons that only make sense once a configuration is loaded. */
    private Button tbSave;
    private Button tbClose;
    private Button tbConfig;
    private TreeItem<FormBuilder.NavNode> rootItem;

    /**
     * {@inheritDoc}
     *
     * <p>Called after the FXML object graph has been injected and after the framework's macOS
     * decision (menu detach), see {@link BaseController#initialize(URL, ResourceBundle)}.</p>
     */
    @Override
    public void init(URL location, ResourceBundle resources, double toolBarShift) {
        navTree.setCellFactory(tv -> navCell());
        navTree.getSelectionModel().selectedItemProperty().addListener(
                (o, a, b) -> showForm(b));
        applyTexts();
        buildLanguageMenu();
        buildAccelerators();
        buildRecentMenu();
        formScroll.setContent(FormBuilder.message(t("Open a .ifacex or .ifcx file to begin.")));
        buildToolBar();
        buildMenuIcons();
        setConfigActionsEnabled(false);
    }

    /**
     * The menu bar that may be detached to the macOS system menu bar.
     *
     * <p>The framework sets {@code useSystemMenuBar} on it when running on macOS; the detach is
     * prevented with {@code disableMacOSMenuDetach=yes} in {@code app.cfg}. On all other
     * platforms this method is never called.</p>
     *
     * @return the menu bar of the main window
     */
    @Override
    public MenuBar getMenuBar() {
        return menuBar;
    }

    /**
     * The toolbar pane that is repositioned on macOS after the menu bar is
     * detached to the system menu bar.
     * 
     * @return toolbar pane (BorderPane with fx:id "tool-bar-pane")
     */
    @Override
    public javafx.scene.layout.Pane getToolBar() {
        javafx.scene.Node top = menuBar.getParent();
        if (top instanceof javafx.scene.layout.AnchorPane ap) {
            for (javafx.scene.Node n : ap.getChildren()) {
                if (n.getId() != null && "tool-bar-pane".equals(n.getId())) {
                    return (javafx.scene.layout.Pane) n;
                }
                if (n instanceof javafx.scene.layout.BorderPane bp && bp.getStyleClass().contains("tool-bar-pane")) {
                    return bp;
                }
            }
        }
        return null;
    }

    /**
     * Builds the {@code Help › Language} sub-menu: one radio item per supported language, each
     * with the framework's flag icon. Picking an item switches the language live, see
     * {@link #switchLanguage(String)}.
     *
     * <p>The language names are deliberately <em>not</em> translated — a language list reads
     * best in the language itself ("English", "Deutsch").</p>
     */
    private void buildLanguageMenu() {
        final ToggleGroup group = new ToggleGroup();
        miLangEn.setToggleGroup(group);
        miLangDe.setToggleGroup(group);
        miLangEn.setGraphic(icon("lang_en.png"));
        miLangDe.setGraphic(icon("lang_de.png"));
        selectLanguageItem(Messages.getLanguage());
    }

    /**
     * Marks the radio item of the given language as selected <em>without</em> triggering a switch;
     * a programmatic selection does not fire {@code onAction} in JavaFX.
     *
     * @param lang language code, e.g. {@code de}
     */
    private void selectLanguageItem(String lang) {
        if (Messages.LANG_DE.equals(lang)) {
            miLangDe.setSelected(true);
        } else {
            miLangEn.setSelected(true);
        }
    }

    /**
     * {@code Help › Language}: switches the UI language live.
     */
    @FXML
    private void onLanguage() {
        switchLanguage(miLangDe.isSelected() ? Messages.LANG_DE : Messages.LANG_EN);
    }

    /**
     * Switches the whole UI to the given language at runtime.
     *
     * <p>Two catalogues have to follow: the <b>autumo-ui framework</b> resources (window title,
     * About/license HTML panes, framework dialogs) via {@link Helper#setLanguage(String)}, and the
     * <b>application</b> texts via {@link Messages#setLanguage(String)}. Afterwards the static FXML
     * shell and the toolbar are re-texted and the navigation tree plus the open form are rebuilt,
     * so no stale text is left anywhere.</p>
     *
     * @param lang language code, e.g. {@code de}
     */
    private void switchLanguage(String lang) {
        if (lang == null || lang.equals(Messages.getLanguage())) {
            return;
        }
        Helper.setLanguage(lang);
        Messages.setLanguage(lang);
        applyTexts();
        buildToolBar();
        buildRecentMenu();
        if (config == null) {
            setConfigActionsEnabled(false);
        } else {
            rebuild();
        }
        selectLanguageItem(lang);
        renderStatus();
    }

    /**
     * Applies the translated texts of the static FXML shell. FXML cannot read PO files, so the
     * labels that live in {@code main.fxml} are (re-)set here from {@link Messages}.
     */
    private void applyTexts() {
        menuFile.setText(t("File"));
        menuHelp.setText(t("Help"));
        menuLanguage.setText(t("Language"));
        menuConfig.setText(t("Configuration"));
        menuRecent.setText(t("Open Recent"));
        miNew.setText(t("New"));
        miOpen.setText(t("Open…"));
        miSave.setText(t("Save As… (.ifcx)"));
        miExportIfcx.setText(t("Export .ifcx"));
        miExportIfacex.setText(t("Export .ifacex"));
        miClose.setText(t("Close"));
        miExit.setText(t("Exit"));
        miAbout.setText(t("About"));
        miEncoder.setText(t("Encoder"));
        miRemoveAppData.setText(t("Remove App Data"));
        miDocumentation.setText(t("Open Online Documentation"));
        miWebsite.setText(t("Visit Website"));
        miUpdate.setText(t("Check for Updates"));
        miLicense.setText(t("License"));
        tabForm.setText(t("Form"));
        tabRawYaml.setText(t("Raw YAML (.ifcx)"));
        tabRawProps.setText(t("Raw Properties (.ifacex)"));
        statusLabel.setText(t("Ready"));
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
        // Idempotent: also called again on a live language switch, so drop the old items first.
        leftToolBar.getItems().clear();
        rightToolBar.getItems().clear();
        final Button tbNew = toolButton("project-new-48.png", t("New"),
                t("Create a new, empty configuration"), () -> onNew());
        final Button tbOpen = toolButton("project-open-48.png", t("Open"),
                t("Open an .ifacex or .ifcx configuration"), () -> onOpen());
        tbSave = toolButton("project-save-48.png", t("Save"),
                t("Save as .ifcx (new format)"), () -> onSave());
        tbClose = toolButton("project-close-48.png", t("Close"),
                t("Close the current configuration"), () -> onClose());

        tbConfig = toolButton("settings-48.png", t("Configuration"),
                t("Edit file metadata and job wiring (selects the root node)"), this::selectRoot);
        final Button tbRun = toolButton("process-48.png", t("Run"),
                t("Run the IPC — engine integration follows later"), null);
        final Button tbFolder = toolButton("folder-48.png", t("External Edit"),
                t("Open in an external editor — follows later"), null);

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
        b.setTooltip(new Tooltip(t("Visit the ifaceX website")));
        b.setOnAction(e -> Helper.openURL(
                configUrl(Constants.CONFIG_ENTRY_URL_WEB, Constants.DEF_CONFIG_ENTRY_URL_WEB)));
        return b;
    }

    private String configUrl(String key, String fallback) {
        final StudioApp app = StudioApp.get();
        final String url = app != null ? app.getConfigurationParameter(key) : null;
        return url != null && !url.isBlank() ? url : fallback;
    }

    /**
     * Puts the original Studio's 16px menu icons on the menu items (they come from autumo-ui, the
     * same set the toolbar uses). Icons are language independent and are set once at start-up.
     */
    private void buildMenuIcons() {
        menuRecent.setGraphic(icon("project-open-16.png"));
        miNew.setGraphic(icon("project-new-16.png"));
        miOpen.setGraphic(icon("project-open-16.png"));
        miSave.setGraphic(icon("project-save-16.png"));
        miExportIfcx.setGraphic(icon("project-save-16.png"));
        miExportIfacex.setGraphic(icon("project-save-16.png"));
        miClose.setGraphic(icon("project-close-16.png"));
        miExit.setGraphic(icon("exit-16.png"));
        miEncoder.setGraphic(icon("config-16.png"));
        miRemoveAppData.setGraphic(icon("remove-config-16.png"));
        miDocumentation.setGraphic(icon("help-16.png"));
        miWebsite.setGraphic(icon("ifaceX16.png"));
        miUpdate.setGraphic(icon("update-16.png"));
        miAbout.setGraphic(icon("about-16.png"));
        miLicense.setGraphic(icon("about-16.png"));
    }

    /**
     * Puts the original Studio's keyboard shortcuts on the menu items.
     *
     * <p>{@link KeyCombination#SHORTCUT_DOWN} is the platform's menu modifier (Command on macOS,
     * Control elsewhere), so the original ⌘-shortcuts survive without hard-coding the modifier.
     * They are set here and not in the FXML: {@code KeyCodeCombination} is immutable and the FXML
     * builder cannot construct it from a plain set of attributes.</p>
     */
    private void buildAccelerators() {
        miNew.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN));
        miOpen.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN));
        miSave.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));
        miClose.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN));
        miEncoder.setAccelerator(new KeyCodeCombination(KeyCode.E, KeyCombination.SHORTCUT_DOWN));
        miRemoveAppData.setAccelerator(
                new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN,
                        KeyCombination.SHIFT_DOWN));
        miDocumentation.setAccelerator(new KeyCodeCombination(KeyCode.F1));
        miWebsite.setAccelerator(new KeyCodeCombination(KeyCode.F2));
        miUpdate.setAccelerator(new KeyCodeCombination(KeyCode.U, KeyCombination.SHORTCUT_DOWN));
    }

    /**
     * Builds {@code File › Open Recent}. The list itself lives in the framework
     * ({@link BaseController#initializeRecentMenu(Menu, MenuItem)} wires the {@code Clear Entries}
     * item, {@link BaseController#rebuildRecentList()} renders the entries, {@code BaseApp} keeps
     * the file list, drops vanished files and persists it on exit), so the Studio only has to hook
     * its own open into {@link #innerOpen(File)}.
     *
     * <p>The clear item is passed in so that <em>all</em> menu labels come from one catalogue (our
     * PO file, {@link Messages#t(String)}); it is rebuilt whenever the language changes.</p>
     */
    private void buildRecentMenu() {
        initializeRecentMenu(menuRecent, new MenuItem(t("Clear Entries")));
        rebuildRecentList();
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
        status(t("New configuration"));
    }

    @FXML
    private void onClose() {
        config = null;
        sourceFile = null;
        rootItem = null;
        navTree.setRoot(null);
        rawYaml.clear();
        rawProps.clear();
        formScroll.setContent(FormBuilder.message(t("Open a .ifacex or .ifcx file to begin.")));
        setConfigActionsEnabled(false);
        status(t("Closed"));
    }

    @FXML
    private void onOpen() {
        final FileChooser chooser = new FileChooser();
        chooser.setTitle(t("Open configuration"));
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(t("ifaceX configuration"), "*.ifacex", "*.ifcx", "*.yaml", "*.yml"),
                new FileChooser.ExtensionFilter(t("Legacy properties"), "*.ifacex"),
                new FileChooser.ExtensionFilter("YAML", "*.ifcx", "*.yaml", "*.yml"));
        final File file = chooser.showOpenDialog(window());
        if (file != null) {
            openFile(file.toPath());
        }
    }

    /**
     * Opens a file by path — used by {@code File › Open} and the {@code --open} command line
     * option; goes through the framework's {@link #innerOpen(File)} so the file is filed in the
     * recent list.
     *
     * @param path file to open
     */
    void openFile(Path path) {
        innerOpen(path.toFile());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Loads the configuration and files it in the framework's recent list, which
     * {@link BaseController#rebuildRecentList()} renders as {@code File › Open Recent}. The recent
     * list is deliberately <em>not</em> touched by {@link #loadConfig(Path)}: a head-less self-test
     * run must not pollute the list that is persisted on exit.</p>
     */
    @Override
    protected void innerOpen(File file) {
        try {
            loadConfig(file.toPath());
            app.addRecentFile(file);
            buildRecentMenu();
        } catch (Exception e) {
            error(format("Could not read {0}", file.getName()), e);
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
        status(legacy ? t("Converted from legacy .ifacex") : t("Loaded"));
    }

    /** Headless self-test support: load a file and describe what the UI would show. */
    String selfTest(Path path) throws Exception {
        loadConfig(path);
        final String startLang = Messages.getLanguage();
        final TreeItem<FormBuilder.NavNode> root = navTree.getRoot();
        // exercise the form builder for every nav node
        final int[] acc = new int[4]; // forms, list views (source fields), tables, dnd buttons
        final List<String> fieldLabels = new ArrayList<>();
        for (final TreeItem<FormBuilder.NavNode> child : root.getChildren()) {
            countForms(child, acc, fieldLabels);
        }
        final var destNode = firstDestinationEntity(root);
        if (destNode != null) {
            showForm(destNode);
        }
        final Node rootForm = formBuilder.build(root.getValue());
        final int rootFormParts = rootForm instanceof javafx.scene.layout.Pane p
                ? p.getChildren().size() : -1;
        // Parameter labels are rendered through the message lookup; keep only texts that match
        // a known engine parameter so captions do not pollute the sample.
        final Set<String> knownLabels = new HashSet<>();
        for (final String param : FieldCatalog.knownParams()) {
            knownLabels.add(t(param, FieldCatalog.descriptor(param).label()));
        }
        final List<String> paramLabels = fieldLabels.stream().filter(knownLabels::contains)
                .distinct().sorted().toList();
        final String metrics = "nodes=" + countNodes(root)
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
                        && statusLabel.getText().startsWith(t("File") + ": "))
                + " rootLabel=\"" + root.getValue().label() + "\""
                + " title=\"" + (config.meta().title() == null ? "" : config.meta().title()) + "\""
                + " dndButtons=" + acc[3]
                // proves the metadata parameter labels go through the message lookup (key =
                // msgid, derived English label = fallback)
                + " paramLabels=" + paramLabels.size() + " \"" + paramLabels.stream().limit(6)
                        .collect(java.util.stream.Collectors.joining(", ")) + "\""
                + " nav=\"" + root.getChildren().stream()
                        .map(c -> c.getValue().label()).collect(java.util.stream.Collectors.joining(" | "))
                        + "\""
                + " statusText=\"" + (statusLabel.getText() == null ? "" : statusLabel.getText()) + "\""
                + " statusOk=" + (statusLabel.getText() != null && !statusLabel.getText().isBlank())
                // the framework's HTML about/license panes resolve these per language; a typo in
                // the package/resource name would only show up as a WebView error page, so pin it
                + " aboutHtmlOk=" + (getClass().getResource("html/about_en.html") != null
                        && getClass().getResource("html/about_de.html") != null
                        && getClass().getResource("html/license_en.html") != null
                        && getClass().getResource("html/license_de.html") != null)
                + " aboutPaneOk=" + frameworkPaneSelfTest(() -> UI.showAboutWithLicense(app, "/ch/autumo/ifacex/studio/ui/images/about-48.png"))
                + " encoderPaneOk=" + frameworkPaneSelfTest(() -> UI.showEncoder(app))
                + " licensePaneOk=" + frameworkPaneSelfTest(() -> UI.showLicense(app))
                + " recentItems=" + menuRecent.getItems().size()
                + " langItems=" + menuLanguage.getItems().size()
                + " langSwitchOk=" + languageSwitchSelfTest(startLang)
                + " langRestoredOk=" + Messages.getLanguage().equals(startLang);
        return metrics;
    }

    /**
     * Drives one full live language switch — to the "other" language and back — and reports whether
     * the shell text, the framework language and the radio selection all followed. Used by
     * {@link #selfTest(Path)} to prove the {@code Help › Language} switch works headlessly.
     *
     * @param startLang language the self-test started in
     * @return {@code true} if the switch and the restore behaved as expected
     */
    private boolean languageSwitchSelfTest(String startLang) {
        final String other = Messages.LANG_DE.equals(startLang) ? Messages.LANG_EN : Messages.LANG_DE;
        switchLanguage(other);
        final boolean ok = other.equals(Messages.getLanguage())
                && other.equals(Helper.getLanguage())
                && menuFile.getText().equals(t("File"))
                && (Messages.LANG_DE.equals(other) ? miLangDe.isSelected() : miLangEn.isSelected());
        switchLanguage(startLang);
        return ok;
    }

    /**
     * Opens one of the framework panes exactly like its menu item does, verifies that a window was
     * actually shown and closes it again.
     *
     * <p>This exercises the whole framework-pane path headlessly: the framework bundle keys
     * ({@code About.title}, {@code Encoder.*}, {@code License.title}, {@code button.License} …) must
     * resolve and the pane must build — a missing key would otherwise only surface as an exception
     * when the user clicks the menu item.</p>
     *
     * @param opener the same call the menu item makes, e.g. {@code () -> UI.showEncoder(app)}
     * @return {@code true} if the pane was created and shown
     */
    private boolean frameworkPaneSelfTest(Runnable opener) {
        opener.run();
        final List<Stage> extra = Window.getWindows().stream()
                .filter(w -> w instanceof Stage).map(w -> (Stage) w)
                .filter(s -> s != getStage()).toList();
        final boolean showing = !extra.isEmpty() && extra.stream().allMatch(Stage::isShowing);
        extra.forEach(Stage::close);
        return showing;
    }

    /** Builds every nav node's form and accumulates [forms, listViews, tables, dndButtons]. */
    private void countForms(TreeItem<FormBuilder.NavNode> item, int[] acc, List<String> fieldLabels) {
        final Node n = formBuilder.build(item.getValue());
        if (n != null) {
            acc[0]++;
            acc[1] += count(n, javafx.scene.control.ListView.class);
            acc[2] += count(n, javafx.scene.control.TableView.class);
            acc[3] += countDnd(n);
            collectFieldLabels(n, fieldLabels);
        }
        for (final TreeItem<FormBuilder.NavNode> c : item.getChildren()) {
            countForms(c, acc, fieldLabels);
        }
    }

    /**
     * Collects the text of all rendered parameter labels ({@code .param-label}). The walk
     * descends into {@link javafx.scene.control.TitledPane} content because a control's children
     * only exist once its skin is built.
     */
    private static void collectFieldLabels(Node node, List<String> out) {
        if (node == null) {
            return;
        }
        if (node instanceof javafx.scene.control.Label l
                && l.getStyleClass().contains("param-label")) {
            out.add(l.getText());
        }
        if (node instanceof javafx.scene.control.TitledPane tp) {
            collectFieldLabels(tp.getContent(), out);
        } else if (node instanceof javafx.scene.control.ScrollPane sp) {
            collectFieldLabels(sp.getContent(), out);
        } else if (node instanceof javafx.scene.Parent p) {
            for (final Node child : p.getChildrenUnmodifiable()) {
                collectFieldLabels(child, out);
            }
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

        root.getChildren().add(new TreeItem<>(new FormBuilder.NavNode(K_GENERAL,
                t("Overwritten Values"), config.general(), null)));

        for (final RwConfig reader : config.readers().values()) {
            root.getChildren().add(sectionItem(reader, K_SOURCE,
                    t("Source") + ": " + reader.name()));
        }
        for (final RwConfig writer : config.writers().values()) {
            root.getChildren().add(sectionItem(writer, K_DEST,
                    t("Destination") + ": " + writer.name()));
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
        return sourceFile == null ? t("Configuration") : sourceFile.getFileName().toString();
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
            status(t("Nothing to save."));
            return;
        }
        // Save always uses the new YAML format (.ifcx); .ifacex is export-only (legacy).
        export(YamlConfigWriter.toText(config), ".ifcx", t("ifaceX YAML (new format)"),
                t("Save as .ifcx"));
    }

    @FXML
    private void onExportIfcx() {
        export(YamlConfigWriter.toText(config), ".ifcx", t("ifaceX YAML"), t("Export .ifcx"));
    }

    @FXML
    private void onExportIfacex() {
        export(PropertiesConfigWriter.toText(config), ".ifacex", t("Legacy properties"),
                t("Export .ifacex"));
    }

    private void export(String text, String extension, String description, String title) {
        if (config == null) {
            status(t("Nothing to export."));
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
            app.addRecentFile(out);
            buildRecentMenu();
            status(format("Saved {0} ({1})", out.getName(), extension));
        } catch (Exception e) {
            error(format("Could not write {0}", out.getName()), e);
        }
    }

    @FXML
    private void onExit() {
        window().hide();
    }

    /**
     * Shows the About window through the autumo-ui pane, with the license button.
     *
     * <p>The framework builds the dialog ({@code about-48.png} header, HTML body, footer) and
     * loads the body from {@code html/about_<lang>.html} plus, behind the license button,
     * {@code html/license_<lang>.html}. Both are resolved relative to {@link StudioApp}, and the
     * language is the one autumo-ui reads from the {@code user.language} system property, which
     * {@link Messages#setLanguage(String)} keeps in sync. The text is therefore a translatable
     * resource and no dialog code changes per language — see {@code docs/12-i18n.md}.</p>
     */
    @FXML
    private void onAbout() {
        UI.showAboutWithLicense(app, "/ch/autumo/ifacex/studio/ui/images/about-48.png");
    }

    /**
     * {@code Configuration › Encoder}: opens the framework's encoder pane.
     *
     * <p>The original Studio filled the pane from its own {@code STR_ENC_MODEL} list ("none",
     * "Base64 - SHA3-256 AES", …) and required a 32 character key from the general settings that had
     * to match the engine's secret. The framework pane is used with {@code internalEncryption=false}
     * so it reads the application's <em>unique</em> security key ({@code secret_key_seed}) from the
     * configuration instead of the internal framework key. When a test instance is configured later,
     * that seed will be taken from the instance's base config ({@code cfg/ifacex.cfg} or
     * {@code cfg/ifacex-base.ifcx}, whichever exists). See {@code docs/07-risks-open-questions.md}.</p>
     */
    @FXML
    private void onEncoder() {
        UI.showEncoder(app, false);
    }

    /** {@code Configuration › Remove App Data}: the framework's pane (it asks before deleting). */
    @FXML
    private void onRemoveAppData() {
        UI.showRemovePane(app);
    }

    /** {@code Help › Open Online Documentation}: opens {@code url.documentation} from app.cfg. */
    @FXML
    private void onDocumentation() {
        Helper.openURL(configUrl(Constants.CONFIG_ENTRY_URL_DOC,
                Constants.DEF_CONFIG_ENTRY_URL_DOC));
    }

    /** {@code Help › Visit Website}: opens {@code url.website} from app.cfg. */
    @FXML
    private void onWebsite() {
        Helper.openURL(configUrl(Constants.CONFIG_ENTRY_URL_WEB, Constants.DEF_CONFIG_ENTRY_URL_WEB));
    }

    /** {@code Help › Check for Updates}: the framework's update pane. */
    @FXML
    private void onUpdate() {
        UI.showUpdatePane(app);
    }

    /** {@code Help › License}: the framework's license pane (same HTML as the About button). */
    @FXML
    private void onLicense() {
        UI.showLicense(app);
    }

    /** State shown behind the file: "Loaded", "Converted ...", "Modified", "Saved ...". */
    private String statusState = "";

    /** Whether the shown state is the modified one — compared as a flag, not as translated text. */
    private boolean modified = false;

    /** Sets the state part of the status bar and re-renders it. */
    private void status(String text) {
        statusState = text == null ? "" : text;
        modified = false;
        renderStatus();
    }

    /**
     * The status bar reads like the original Studio: {@code File: <absolute path>} first, the
     * state behind it. Without a loaded file it shows {@code File: -}.
     */
    private void renderStatus() {
        final String file = sourceFile == null
                ? t("File") + " -" : t("File") + ": " + sourceFile.toAbsolutePath();
        statusLabel.setText(statusState.isEmpty() ? file : file + "   \u2014   " + statusState);
    }

    /** Flag the configuration as changed, without repeating an existing "Modified". */
    private void markModified() {
        if (modified) {
            return;
        }
        modified = true;
        statusState = t("Modified");
        renderStatus();
    }

    private void error(String message, Exception e) {
        status(message);
        final Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(t("Error"));
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
