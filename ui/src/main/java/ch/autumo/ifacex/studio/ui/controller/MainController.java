
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
package ch.autumo.ifacex.studio.ui.controller;


import static ch.autumo.ifacex.studio.ui.StudioHelper.format;
import static ch.autumo.ifacex.studio.ui.StudioHelper.t;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.commons.utils.system.OSUtils;
import ch.autumo.ifacex.studio.core.LicIfaceXApp100;
import ch.autumo.ifacex.studio.core.StudioConstants;
import ch.autumo.ifacex.studio.core.io.PropertiesConfigReader;
import ch.autumo.ifacex.studio.core.io.YamlConfigReader;
import ch.autumo.ifacex.studio.core.io.YamlConfigWriter;
import ch.autumo.ifacex.studio.core.meta.InterfaceTypes;
import ch.autumo.ifacex.studio.core.model.IpcConfig;
import ch.autumo.ifacex.studio.core.model.RwConfig;
import ch.autumo.ifacex.studio.ui.Console;
import ch.autumo.ifacex.studio.ui.FormBuilder;
import ch.autumo.ifacex.studio.ui.RawEditor;
import ch.autumo.ifacex.studio.ui.StudioApp;
import ch.autumo.ifacex.studio.ui.StudioHelper;
import ch.autumo.ifacex.studio.ui.controller.actions.NewConfigAction;
import ch.autumo.ifacex.studio.ui.controller.actions.SettingsActions;
import ch.autumo.ifacex.studio.ui.view.MainView;
import ch.autumo.ui.Constants;
import ch.autumo.ui.fx.BaseApp;
import ch.autumo.ui.fx.control.Icons;
import ch.autumo.ui.fx.controller.BaseController;
import ch.autumo.ui.fx.controller.DefaultCodeEditorController;
import ch.autumo.ui.fx.model.BaseModel;
import ch.autumo.ui.fx.view.DefaultCodeEditorView;
import ch.autumo.ui.utils.Helper;
import ch.autumo.ui.utils.Messages;
import ch.autumo.ui.utils.UI;
import ch.autumo.ui.utils.UI.HyperLinkRedirectListener;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;
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

	/**
	 * Logger.
	 */
	protected static final Logger LOG = LoggerFactory.getLogger(BaseApp.class.getName());
	
    public static final String K_ROOT = "ROOT";
    public static final String K_GENERAL = "GENERAL";
    public static final String K_SOURCE = "SOURCE";
    public static final String K_DEST = "DEST";
    public static final String K_ENTITY = "ENTITY";

    /** Style class that renders a navigation entry in bold (see style/application.css). */
    private static final String NAV_EMPHASIS = "nav-emphasis";

    private String work_folder = null;
    
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

    // The nodes the FXML file fills are package-private (not private): the UI tests of this
    // module live in the same package and assert directly on the built shell, see
    // src/test/java/ch/autumo/ifacex/studio/ui/. Nothing outside the package can reach them.
    @FXML
    TreeView<FormBuilder.NavNode> navTree;
    @FXML
    StackPane navHost;
    @FXML
    SplitPane mainSplit;
    @FXML
    WebView aboutView;
    @FXML
    ScrollPane formScroll;
    @FXML
    Label statusLabel;
    @FXML
    ToolBar leftToolBar;
    @FXML
    ToolBar rightToolBar;
    @FXML
    MenuItem miNew;
    @FXML
    MenuItem miOpen;
    @FXML
    MenuItem miSave;
    @FXML
    MenuItem miSaveAs;
    @FXML
    MenuItem miClose;
    @FXML
    MenuItem miAbout;
    @FXML
    MenuItem miExit;
    @FXML
    MenuBar menuBar;
    @FXML
    Menu menuFile;
    @FXML
    Menu menuRecent;
    @FXML
    Menu menuConfig;
    @FXML
    Menu menuProcess;
    @FXML
    MenuItem miSettings;
    @FXML
    MenuItem miEncoder;
    @FXML
    MenuItem miJsonFormatter;
    @FXML
    MenuItem miBaseInterfaceConfig;
    @FXML
    MenuItem miInterfaceConfig;
    @FXML
    MenuItem miRemoveAppData;
    @FXML
    MenuItem miRun;
    @FXML
    CheckMenuItem miDebugMode;
    @FXML
    MenuItem miRegistration;
    @FXML
    Menu menuRegister;
    @FXML
    MenuItem miDocumentation;
    @FXML
    MenuItem miWebsite;
    @FXML
    MenuItem miUpdate;
    @FXML
    Menu menuHelp;
    @FXML
    Menu menuLanguage;
    @FXML
    Tab tabForm;
    @FXML
    TabPane tabPane;
    @FXML
    Tab tabRaw;
    @FXML
    Tab tabConsole;

    /** Builds the metadata-driven forms; package-private for the UI tests, like the nodes above. */
    final FormBuilder formBuilder = new FormBuilder((StudioApp) app, this::onModelChanged, this::rebuild);
    IpcConfig config;
    Path sourceFile;

    /**
     * Base name of the file the configuration was loaded from, without its extension.
     *
     * <p>"Save As…" proposes this name for a loaded configuration, so a file that was converted
     * from {@code name.ifacex} is saved again as {@code name.ifcx} - and not under the title of
     * the configuration. Only a brand new configuration is named after its title.</p>
     */
    private String loadedBaseName;

    /** Toolbar buttons that only make sense once a configuration is loaded. */
    private Button tbSave;
    private Button tbClose;
    private Button tbConfig;
    private Button tbRun;

    /** Phosphor glyph of the Run button. */
    private static final String RUN_ICON = "play";
    /** Phosphor glyph of the Kill button (a round stop sign). */
    private static final String KILL_ICON = "stop-circle";
    /** Fill of the Kill glyph - the alarm red of the legacy kill icon. */
    private static final String KILL_COLOR = "#e53935";
    /** The controller of the currently running external process, {@code null} when idle. */
    private ProcessController processController;

    /**
     * Set while the navigation is rebuilt programmatically. Selecting a tree item then must not
     * switch the tab: whoever triggered the rebuild (the raw editor, save, run) stays where it is.
     */
    private boolean rebuilding;
    private Button tbFolder;
    private TreeItem<FormBuilder.NavNode> rootItem;

    /** Whether an external ifaceX process is currently running. */
    private boolean processRunning = false;

    /** Fixed width of the navigation pane in pixels - it never changes with the window. */
    private static final double DEFAULT_NAV_WIDTH = 532;

    /** Style class that highlights the navigation pane while a file is dragged over it. */
    private static final String DROP_TARGET = "drop-target";


    /** The raw YAML editor of the "Raw" tab; built in {@link #init}. Package-private for the UI tests. */
    RawEditor rawEditor;
    /** The console for process output. */
    Console console;

	private double splitPosition = DEFAULT_NAV_WIDTH;
	
    private boolean navShown = false;
    
    /**
     * {@inheritDoc}
     *
     * <p>Called after the FXML object graph has been injected and after the framework's macOS
     * decision (menu detach), see {@link BaseController#initialize(URL, ResourceBundle)}.</p>
     */
    @Override
    public void init(URL location, ResourceBundle resources, double toolBarShift) {
        navTree.setCellFactory(_ -> navCell());
        navTree.getSelectionModel().selectedItemProperty().addListener(
                (_, _, b) -> showForm(b));
        applyTexts();
        // The language menu belongs to the framework: one item per supported language, the
        // current one selected, switching through BaseController.switchLanguage().
        initializeLanguageMenu(menuLanguage);
        buildAccelerators();
        buildRecentMenu();
        buildRawEditor();
        buildConsole();
        
        final String sw = app.getConfigurationParameter(StudioConstants.PROP_KEY_SPLIT_WIDTH);
        if (sw == null) {
        	this.splitPosition = DEFAULT_NAV_WIDTH;
        } else {
        	this.splitPosition = Double.valueOf(sw).doubleValue();
        }
        
        keepNavigationWidth();
        // Start with the About page in the navigation pane; the tree appears once a file is loaded.
        showAboutInNav();
        installDropTarget();
        showEmptyForm();
        buildToolBar();
        buildMenuIcons();
        setConfigActionsEnabled(false);
        renderTitle();
    }

	/**
     * Returns the menu bar that may be detached to the macOS system menu bar.
     *
     * <p>The framework sets {@code useSystemMenuBarProperty()} on it when running on macOS; the
     * detach is prevented with {@code disableMacOSMenuDetach=yes} in {@code app.cfg}. On all other
     * platforms this method is never called.</p>
     *
     * @return the menu bar of the main window
     */
    @Override
    public MenuBar getMenuBar() {
        return menuBar;
    }

    /**
     * Returns the toolbar pane that is repositioned on macOS after the menu bar is detached to
     * the system menu bar.
     * 
     * @return toolbar pane (BorderPane with fx:id {@code "tool-bar-pane"})
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
     * {@inheritDoc}
     *
     * <p>The framework already did its part — its bundles, the window title, the stored
     * {@code language} parameter and the language menu selection follow in
     * {@link #switchLanguage(String)} — so only the Studio's own texts are re-applied here: the
     * static FXML shell, the toolbar and the navigation tree plus the open form, so no stale text
     * is left anywhere.</p>
     */
    @Override
    protected void languageChanged() {
        applyTexts();
        buildToolBar();
        buildRecentMenu();
        if (config == null) {
            setConfigActionsEnabled(false);
        } else {
            rebuild();
        }
        renderStatus();
        if (navShown) {
        	showTreeInNav();
        } else {
        	showAboutInNav();
        }
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
        menuProcess.setText(t("Process"));
        menuRegister.setText(t("Registration"));
        menuRecent.setText(t("Open Recent"));
        miNew.setText(t("New"));
        miOpen.setText(t("Open…"));
        miSave.setText(t("Save"));
        miSaveAs.setText(t("Save As…"));
        miClose.setText(t("Close"));
        miExit.setText(t("Exit"));
        miAbout.setText(t("About"));
        miEncoder.setText(t("Encoder"));
        miSettings.setText(t("General Settings"));
        miJsonFormatter.setText(t("JSON Formatter"));
        miBaseInterfaceConfig.setText(t("Base Interface Configuration"));
        miInterfaceConfig.setText(t("Interface Configuration"));
        miRemoveAppData.setText(t("Remove App Data"));
        miRun.setText(t("Run"));
        miDebugMode.setText(t("Debug Mode"));
        miDocumentation.setText(t("Open Online Documentation"));
        miWebsite.setText(t("Visit Website"));
        miUpdate.setText(t("Check for Updates"));
        miRegistration.setText(t("menu.Reg"));
        tabForm.setText(t("Form"));
        tabRaw.setText(t("Raw"));
        if (tabConsole != null) {
            tabConsole.setText(t("Console"));
        }
        renderStatus();
        renderTitle();
    }

    /**
     * Tree cell that renders a coloured interface-type pill in front of the node label, so the
     * reader/writer type (CSV, JSON, DB, Mongo DB, REST, Mail, …) is visible at a glance — the
     * legacy GUI carried this in the colour-coded tabs. See {@link FormBuilder#typePill(String)}.
     */
    private TreeCell<FormBuilder.NavNode> navCell() {
        return new TreeCell<>() {
            /**
             * Renders one navigation node: the coloured type pill (when the node carries a
             * section) followed by the plain text label.
             *
             * @param item  the node to render, or {@code null} for an empty cell
             * @param empty whether the cell is empty
             */
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
                final boolean bold = K_ROOT.equals(item.kind())
                        || K_SOURCE.equals(item.kind())
                        || K_DEST.equals(item.kind());
                if (bold) {
                    if (!getStyleClass().contains(NAV_EMPHASIS)) {
                        getStyleClass().add(NAV_EMPHASIS);
                    }
                } else {
                    getStyleClass().remove(NAV_EMPHASIS);
                }
                // Context menu on rest_in reader nodes: "Root Call".
                if (item.payload() instanceof RwConfig rw && "rest_in".equals(rw.name())) {
                    final ContextMenu cm = new ContextMenu();
                    final MenuItem miRoot = new MenuItem(t("Root Call"));
                    miRoot.setOnAction(_ -> run(true));
                    cm.getItems().add(miRoot);
                    setContextMenu(cm);
                } else {
                    setContextMenu(null);
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
        final Button tbNew = toolButton("file-plus", t("New"),
                t("Create a new, empty configuration"), () -> onNew());
        final Button tbOpen = toolButton("folder-open", t("Open"),
                t("Open an .ifacex or .ifcx configuration"), () -> onOpen());
        tbSave = toolButton("floppy-disk", t("Save"),
                t("Save the configuration (.ifcx)"), () -> onSave());
        tbClose = toolButton("x", t("Close"),
                t("Close the current configuration"), () -> onClose());

        tbConfig = toolButton("gear-six", t("Configuration"),
                t("Edit file metadata and job wiring (selects the root node)"), this::selectRoot);
        // Run and Kill share one button, as in the legacy Studio: while the external process
        // runs, the same button kills it.
        tbRun = toolButton(RUN_ICON, t("Run"),
                t("Run the IPC"), this::toggleRun);
        tbFolder = toolButton("arrow-u-up-right", t("External Edit"),
                t("Open in an external editor"), () -> openExternalEditor());

        leftToolBar.getItems().addAll(
                tbNew, tbOpen, tbSave, tbClose,
                toolSeparator(), tbConfig,
                toolSeparator(), tbRun,
                toolSeparator(), tbFolder
    		);

        // Right side mirrors the original Studio: the ifaceX Studio title logo and, to its
        // right, the clickable ifaceX mark (label underneath) that opens the website.
        rightToolBar.getItems().addAll(titleLogo(), ifaceXButton());
    }

    /** The ifaceX Studio title logo (banner) as in the original toolbar; not clickable. */
    private HBox titleLogo() {
        final ImageView iv = new ImageView(new Image(
                StudioApp.class.getResource("icons/ifacex-studio-reddot2-40.png").toExternalForm()));
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
        b.setGraphicTextGap(2);
        b.setStyle("-fx-padding: 3 10 5 10;");
        b.getStyleClass().add("toolbar-button");
        b.setTooltip(new Tooltip(t("Visit the ifaceX website")));
        b.setOnAction(_ -> Helper.openURL(
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
        menuRecent.setGraphic(Icons.create("clock-counter-clockwise", 16));
        miNew.setGraphic(Icons.create("file-plus", 16));
        miOpen.setGraphic(Icons.create("folder-open", 16));
        miSave.setGraphic(Icons.create("floppy-disk", 16));
        miSaveAs.setGraphic(Icons.create("export", 16));
        miClose.setGraphic(Icons.create("x", 16));
        miExit.setGraphic(Icons.create("sign-out", 16));
        miEncoder.setGraphic(Icons.create("lock-key", 16));
        miJsonFormatter.setGraphic(Icons.create("brackets-curly", 16));
        miSettings.setGraphic(Icons.create("gear-six", 16));
        miBaseInterfaceConfig.setGraphic(Icons.create("gear-six", 16));
        miInterfaceConfig.setGraphic(Icons.create("gear-six", 16));
        miRemoveAppData.setGraphic(Icons.create("trash", 16));
        miRun.setGraphic(Icons.create("person-simple-run", 16));
        miDebugMode.setGraphic(Icons.create("bug-beetle", 16));
        miDocumentation.setGraphic(Icons.create("book-open", 16));
        miWebsite.setGraphic(Icons.create("globe-hemisphere-west", 16));
        miUpdate.setGraphic(Icons.create("download-simple", 16));
        miRegistration.setGraphic(Icons.create("key", 16));
        miAbout.setGraphic(Icons.create("info", 16));
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
        miSaveAs.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN,
                KeyCombination.SHIFT_DOWN));
        miClose.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN));
        miEncoder.setAccelerator(new KeyCodeCombination(KeyCode.E, KeyCombination.SHORTCUT_DOWN));
        miSettings.setAccelerator(new KeyCodeCombination(KeyCode.G, KeyCombination.SHORTCUT_DOWN));
        miBaseInterfaceConfig.setAccelerator(new KeyCodeCombination(KeyCode.B, KeyCombination.SHORTCUT_DOWN));
        miInterfaceConfig.setAccelerator(new KeyCodeCombination(KeyCode.I, KeyCombination.SHORTCUT_DOWN));
        miRemoveAppData.setAccelerator(new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        miRun.setAccelerator(new KeyCodeCombination(KeyCode.X, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        miDebugMode.setAccelerator(new KeyCodeCombination(KeyCode.D, KeyCombination.SHORTCUT_DOWN));
        miRegistration.setAccelerator(new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN));
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

    private void setConfigActionsEnabled(boolean enabled) {
        tbSave.setDisable(!enabled);
        tbClose.setDisable(!enabled);
        tbConfig.setDisable(!enabled);
        // External Edit only makes sense for a configuration that has already been saved to disk.
        tbFolder.setDisable(!enabled || sourceFile == null);
        // Run / External Edit are additionally controlled by setRunButtonsEnabled()
        updateRunButtons();
    }

    /**
     * Updates the Run/Kill button and the actions that must not be used while a process runs.
     *
     * <p>The button itself always stays enabled: while the process runs it is the Kill button.</p>
     */
    private void updateRunButtons() {
        final boolean idle = !processRunning;
        setRunButtonRunning(processRunning);
        tbFolder.setDisable(!idle || sourceFile == null);
        tbConfig.setDisable(!idle);
        miRun.setDisable(!idle);
        navTree.setDisable(!idle);
        // While the IPC runs, the console is the only view: the form and the raw text must not
        // be switched to (the configuration is being processed, editing it makes no sense).
        if (tabForm != null) {
            tabForm.setDisable(!idle);
        }
        if (tabRaw != null) {
            tabRaw.setDisable(!idle);
        }
    }

    /**
     * Switches the toolbar button between Run and Kill.
     *
     * <p>As in the legacy Studio, the icon becomes a stop sign in alarm red while the external
     * ifaceX process is running; the button then calls {@link ProcessController#kill(int)}.</p>
     *
     * @param running {@code true} while the external process runs
     */
    private void setRunButtonRunning(boolean running) {
        if (tbRun == null) {
            return;
        }
        if (tbRun.getGraphic() instanceof StackPane box) {
            final Text icon = Icons.create(running ? KILL_ICON : RUN_ICON, 40);
            if (icon != null) {
                if (running) {
                    icon.setStyle("-fx-fill: " + KILL_COLOR + ";");
                }
                box.getChildren().setAll(icon);
            }
        }
        tbRun.setText(t(running ? "Kill" : "Run"));
        tbRun.setTooltip(new Tooltip(t(running ? "Kill the running process" : "Run the IPC")));
    }

    /** Runs the IPC, or kills the running one - the single action of the toolbar button. */
    private void toggleRun() {
        if (processRunning) {
            killProcess();
        } else {
            onRun();
        }
    }

    /**
     * Kills the running external ifaceX process, the counterpart of the legacy Studio's
     * {@code Kill} button.
     */
    private void killProcess() {
        final ProcessController running = this.processController;
        if (running == null) {
            return;
        }
        status(t("Killing the process..."));
        running.kill();
    }

    /**
     * The view state after a configuration was opened or closed.
     *
     * <p>Invariant of the Studio, in one place so it cannot drift apart:</p>
     * <ul>
     *   <li>a loaded or closed configuration shows the <b>form</b>, never the console of the
     *       previous file,</li>
     *   <li>the <b>console</b> is empty - its output belongs to the file that produced it,</li>
     *   <li>the raw text is refreshed from the model by the caller.</li>
     * </ul>
     */
    private void resetViews() {
        if (console != null) {
            console.clear();
        }
        if (tabForm != null && tabPane != null) {
            tabPane.getSelectionModel().select(tabForm);
        }
    }

    /**
     * Called by {@link ProcessController} when the external process has finished – in every case
     * (normal end, error, kill or watchdog timeout). Re-enables the Run/Kill button and reports
     * the outcome in the status line.
     *
     * @param exitCode the exit code of the external process ({@code 0} means success)
     * @param killed whether the process was stopped on purpose; a killed process ends with a
     *               non-zero exit code, which must not be reported as an error
     */
    public void processFinished(int exitCode, boolean killed) {
        processRunning = false;
        processController = null;
        updateRunButtons();
        // The status line keeps the outcome; the dialog itself is shown by ProcessController.
        if (killed) {
            status(t("The process was stopped."));
        }
    }

    private void selectRoot() {
        if (rootItem != null) {
            navTree.getSelectionModel().select(rootItem);
        }
        if (tabPane != null && tabForm != null) {
            tabPane.getSelectionModel().select(tabForm);
        }
    }

    @FXML
    private void onNew() {
        // A new configuration replaces the loaded one - never drop unsaved changes silently.
        if (!confirmDiscardChanges()) {
            return;
        }
        final StudioApp app = StudioApp.get();
        final boolean wizardEnabled = app == null
                || !"false".equalsIgnoreCase(app.getConfigurationParameter(StudioConstants.PROP_KEY_NEW_CONFIG_WIZARD));
        if (wizardEnabled) {
            final NewConfigAction action = new NewConfigAction();
            UI.showPaneAndWait(app, null, action, t("New configuration"));
            final NewConfigAction.Result r = action.getResult();
            if (r == null) {
                // The wizard was cancelled: nothing was discarded, so the next attempt has to
                // ask again (confirmDiscardChanges() has marked the answer as given).
                discardConfirmed = false;
                return;
            }
            config = new IpcConfig();
            config.setSourceInterface(r.reader);
            for (final String w : r.writers) {
                config.destinationInterfaces().add(w);
            }
            config.initializeReader(config.sourceInterface());
            for (final String w : config.destinationInterfaces()) {
                config.initializeWriter(w);
            }
        } else {
            config = new IpcConfig();
        }
        sourceFile = null;
        loadedBaseName = null;
        rebuild();
        status(t("New configuration"));
        // A freshly created configuration is unsaved from the start, so closing it must
        // prompt for confirmation (see confirmDiscardChanges()).
        modified = true;
        discardConfirmed = false;
    }

    /**
     * {@code File › Close}: clears the loaded configuration. When it holds unsaved changes the
     * user is asked first - closing would throw those changes away.
     */
    @FXML
    void onClose() {
        if (!confirmDiscardChanges()) {
            return;
        }
        closeConfig();
    }

    /**
     * The actual close: drops the configuration and shows the empty start state, without asking
     * (the question belongs to {@link #onClose()}). Package-private for the UI tests, which reset
     * the shell between the tests and must not run into a modal confirmation.
     */
    void closeConfig() {
        config = null;
        sourceFile = null;
        loadedBaseName = null;
        rootItem = null;
        navTree.setRoot(null);
        showAboutInNav();
        setRawText("");
        showEmptyForm();
        setConfigActionsEnabled(false);
        resetViews();
        status(t("Closed"));
    }

    /**
     * Whether it is safe to leave the current configuration: {@code true} when nothing is loaded or
     * the configuration was saved, {@code false} when the user cancels the confirmation dialog shown
     * for a modified configuration.
     *
     * <p>Once the user confirms, the answer is remembered: the close request and the window's
     * {@code showing} listener may both run on the same close, and the question must be asked only
     * once.</p>
     *
     * @return {@code true} when the caller may proceed and discard the state
     */
    @Override
    public boolean confirmDiscardChanges() {
        if (config == null || !modified) {
            return true;
        }
        if (discardConfirmed) {
            return true;
        }
        final Optional<ButtonType> answer = UI.showFineConfirmationDialog(app,
                t("The configuration has unsaved changes.<br>Close anyway and lose them?"));
        if (answer.isPresent() && answer.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            discardConfirmed = true;
            return true;
        }
        return false;
    }

    private void showEmptyForm() {
        formScroll.setContent(FormBuilder.message(t("Open a .ifacex or .ifcx file to begin.")));
        Image image = new Image(StudioApp.class.getResource("icons/ifaceX-reddot-256.png").toExternalForm());
        formScroll.setBackground(new Background(new BackgroundImage(
            image,
            BackgroundRepeat.NO_REPEAT,
            BackgroundRepeat.NO_REPEAT,
            BackgroundPosition.CENTER,
            BackgroundSize.DEFAULT
        )));
    }
    
    /**
     * Shows the About page in the navigation pane (used when no file is loaded).
     */
    private void showAboutInNav() {
        if (aboutView == null || navTree == null) return;
        final String lang = Messages.getLanguage();
        final String resource = "html/about_" + lang + ".html";
        final java.net.URL url = StudioApp.class.getResource(resource);
        if (url != null) {
        	aboutView.getEngine().getLoadWorker().stateProperty().addListener(new HyperLinkRedirectListener(aboutView));
			aboutView.getEngine().setUserStyleSheetLocation(Helper.getBaseHTMLStyle());
            aboutView.getEngine().load(url.toExternalForm());
        } else {
            // fall back to English
            final java.net.URL fallback = StudioApp.class.getResource("html/about_en.html");
            if (fallback != null) aboutView.getEngine().load(fallback.toExternalForm());
        }
        navTree.setVisible(false);
        navTree.setManaged(false);
        aboutView.setVisible(true);
        aboutView.setManaged(true);
    	navShown = false;
    }

    /** Shows the navigation tree (used when a file is loaded). */
    private void showTreeInNav() {
        if (aboutView == null || navTree == null) return;
        aboutView.setVisible(false);
        aboutView.setManaged(false);
        navTree.setVisible(true);
        navTree.setManaged(true);
    	navShown = true;
    }

    @FXML
    private void onOpen() {
        final FileChooser chooser = new FileChooser();
        if (work_folder == null) {
        	work_folder = app.getConfigurationParameter(StudioConstants.PROP_KEY_PROJECT_ROOT);	
        }
        if (work_folder != null && work_folder.length() > 0) {
            chooser.setInitialDirectory(new File(work_folder));
        }
        chooser.setTitle(t("Open configuration"));
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(t("ifaceX configuration"), "*.ifacex", "*.ifcx", "*.yaml", "*.yml"),
                new FileChooser.ExtensionFilter(t("Legacy properties"), "*.ifacex"),
                new FileChooser.ExtensionFilter("YAML", "*.ifcx", "*.yaml", "*.yml"));
        final File file = chooser.showOpenDialog(window());
        if (file != null) {
        	work_folder = file.getParent();
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
    public void openFile(Path path) {
        innerOpen(path.toFile());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Loads the configuration and files it in the framework's recent list, which
     * {@link BaseController#rebuildRecentList()} renders as {@code File › Open Recent}. The recent
     * list is deliberately <em>not</em> touched by {@link #loadConfig(Path)}: a test that loads a
     * configuration directly must not pollute the list that is persisted on exit.</p>
     */
    @Override
    protected void innerOpen(File file) {
        // Every way of opening a file ends here - File › Open, Open Recent, the command line and
        // a drop on the tree, the form, the raw view or the console. The console of the previous
        // file is cleared and the form is shown; deferred, because a drop must not change the
        // scene graph while the drag event is still being handled.
        if (!confirmDiscardChanges()) {
            return;
        }
        javafx.application.Platform.runLater(this::resetViews);
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
        // Keep the name of the (possibly converted) file: it is what "Save As…" proposes.
        loadedBaseName = baseName(path);
        rebuild();
        final boolean legacy = name.endsWith(".ifacex");
        status(legacy ? t("Converted from legacy .ifacex") : t("Loaded"));
        // A freshly loaded file is unmodified: the next action may ask again.
        discardConfirmed = false;
    }

    private void rebuild() {
        final TreeItem<FormBuilder.NavNode> selected = navTree.getSelectionModel().getSelectedItem();
        final FormBuilder.NavNode prev = selected == null ? null : selected.getValue();
        formBuilder.setConfig(config);

        // --- Keep only the readers/writers declared in the job wiring ---
        // A section only belongs in the .ifcx file when it is referenced by
        // source_interface / destination_interfaces. Sections in the model that
        // are not referenced must not show up in the tree and must not be saved.
        final String sourceInterface = config.sourceInterface();
        config.readers().keySet().removeIf(r -> !r.equals(sourceInterface));
        config.writers().keySet().removeIf(w -> !config.destinationInterfaces().contains(w));

        // A declared interface belongs to the job even when the file carries no section for it:
        // the section is created with the complete parameter set of its type, so the user sees
        // what can be configured (defaults included) - these fields cannot be added by hand in
        // the form, only an output mapping can. Existing sections are never touched.
        if (sourceInterface != null && !sourceInterface.isBlank()
                && config.reader(sourceInterface) == null) {
            config.initializeReader(sourceInterface);
        }
        for (final String writer : config.destinationInterfaces()) {
            if (writer != null && !writer.isBlank() && config.writer(writer) == null) {
                config.initializeWriter(writer);
            }
        }
        // Writers that print the records unchanged (console_out, first_out) have no mapping to
        // add, so their entities would never come into existence - and with them the editor for
        // their output headers. Every source entity is therefore prepared in those sections.
        for (final RwConfig writer : config.writers().values()) {
            if (InterfaceTypes.writesUnchanged(writer.name())) {
                for (final String entity : config.sourceEntityNames()) {
                    writer.ensureEntity(entity);
                }
            }
        }

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
        showTreeInNav();
        final TreeItem<FormBuilder.NavNode> restore = findNode(root, prev);
        rebuilding = true;
        try {
            navTree.getSelectionModel().select(restore != null ? restore : root);
        } finally {
            rebuilding = false;
        }
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

    /**
     * Shows the form of a navigation entry. Package-private on purpose: the UI tests build the
     * shell in the same package and walk the navigation tree with it.
     *
     * @param item navigation entry
     */
    void showForm(TreeItem<FormBuilder.NavNode> item) {
        if (item == null) {
            return;
        }
        if (!rebuilding && tabPane != null && tabForm != null) {
            tabPane.getSelectionModel().select(tabForm);
        }
        formScroll.setContent(formBuilder.build(item.getValue()));
        formScroll.setBackground(null);
    }

    private void onModelChanged() {
        refreshRaw();
        refreshRootLabel();
        markModified();
    }

    /**
     * Builds the raw editor of the "Raw" tab and wires the two-way sync: switching to the form
     * tab parses the raw text back into the model; switching to the raw tab refreshes the text
     * from the model.
     */
    private void buildRawEditor() {
        rawEditor = new RawEditor();
        // A change in the raw text marks the configuration as modified - the window title gets
        // its asterisk and the status bar says so, exactly as a change in the form does.
        rawEditor.setOnEdit(this::markModified);
        tabRaw.setContent(rawEditor.node());
        tabRaw.setOnSelectionChanged(_ -> {
            if (!tabRaw.isSelected()) {
                // Left the raw tab: adopt its text into the model.
                applyRawText();
            } else if (config != null) {
                // Entered the raw tab: show the current model and repaint the Swing editor -
                // a SwingNode that became visible does not repaint its content by itself.
                refreshRaw();
                rawEditor.repaintEditor();
            }
        });
    }

    private void buildConsole() {
        console = new Console();
        if (tabConsole != null) {
            tabConsole.setContent(console.node());
            // A SwingNode that becomes visible does not repaint its content by itself.
            tabConsole.setOnSelectionChanged(_ -> {
                if (tabConsole.isSelected()) {
                    console.repaintConsole();
                }
            });
        }
    }

    /** Writes the model into the raw editor (no re-parse of that text). */
    private void refreshRaw() {
        if (config == null) {
            return;
        }
        setRawText(YamlConfigWriter.toText(config));
    }
    /** Sets the raw editor text. */
    private void setRawText(String text) {
        if (rawEditor == null) {
            return;
        }
        rawEditor.setText(text);
    }

    /**
     * Parses the raw editor text and adopts it into the model - the counterpart of
     * {@link #refreshRaw()}. A malformed document is reported and leaves the model untouched, so
     * the form editor never sees a half-broken configuration.
     *
     * @return {@code true} if the text was adopted (or there was nothing to adopt),
     *         {@code false} if it could not be parsed
     */
    private boolean applyRawText() {
        if (config == null || rawEditor == null) {
            return true;
        }
        final String text = rawEditor.getText();
        if (text.equals(YamlConfigWriter.toText(config))) {
            return true;
        }
        final IpcConfig parsed;
        try {
            parsed = YamlConfigReader.read(text);
        } catch (Exception e) {
            error(t("The raw configuration is not valid YAML."), e);
            return false;
        }
        config = parsed;
        rebuild();
        markModified();
        return true;
    }

    /**
     * {@code File › Save}: writes the configuration back to the file it was loaded from. Only a
     * configuration that has no {@code .ifcx} file yet falls back to the "Save As…" dialog; a
     * loaded {@code .ifcx} is overwritten directly. The legacy {@code .ifacex} format is no
     * longer written here - it is a read-only import.
     */
    @FXML
    private void onSave() {
        save();
    }

    /**
     * The save itself: adopts pending raw edits, then overwrites the loaded {@code .ifcx} file
     * or asks for a target file ("Save As…") when there is none.
     *
     * <p>Package-private for the UI tests, which assert the decision
     * ({@link #saveTarget()}) before the write happens.</p>
     */
    boolean save() {
        if (config == null) {
            status(t("Nothing to save."));
            return false;
        }
        if (tabRaw.isSelected()) {
            // The user may have edited the raw text and hit Save without leaving the tab.
            if (!applyRawText()) {
                return false;
            }
        }
        final Path target = saveTarget();
        if (target == null) {
            return saveAs();
        }
        return write(target, YamlConfigWriter.toText(config));
    }

    /**
     * The file {@code File › Save} writes to, or {@code null} when the "Save As…" dialog is
     * needed - a configuration that was never saved as {@code .ifcx} yet.
     *
     * @return the file to overwrite or {@code null}
     */
    Path saveTarget() {
        return sourceFile != null && isIfcx(sourceFile) ? sourceFile : null;
    }

    /** {@code File › Save As…}: always asks for a target file. */
    @FXML
    private void onSaveAs() {
        if (config == null) {
            status(t("Nothing to save."));
            return;
        }
        if (tabRaw.isSelected()) {
            if (!applyRawText()) {
                return;
            }
        }
        saveAs();
    }

    private boolean saveAs() {
        final FileChooser chooser = new FileChooser();
        chooser.setTitle(t("Save as .ifcx"));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(t("ifaceX YAML (new format)"), "*.ifcx"));
        chooser.setInitialFileName(suggestedFileName());
        final File out = chooser.showSaveDialog(window());
        if (out == null) {
            return false;
        }
        return write(out.toPath(), YamlConfigWriter.toText(config));
    }

    /**
     * The file name the "Save As…" dialog proposes: the title defined in the form ({@code meta.title})
     * when there is one, otherwise the name of the loaded file (or a generic default for a brand new
     * configuration). The title is stripped down to a safe file name.
     *
     * @return the suggested file name, always ending in {@code .ifcx}
     */
    String suggestedFileName() {
        // A loaded configuration keeps the name of its file - a file converted from
        // 'name.ifacex' is saved as 'name.ifcx', not under its title.
        if (loadedBaseName != null && !loadedBaseName.isBlank()) {
            return safeFileName(loadedBaseName) + ".ifcx";
        }
        if (config != null) {
            final String title = config.meta().title();
            if (title != null && !title.isBlank()) {
                final String safe = safeFileName(title.trim());
                if (!safe.isBlank()) {
                    return safe + ".ifcx";
                }
            }
        }
        if (sourceFile != null) {
            return safeFileName(baseName(sourceFile)) + ".ifcx";
        }
        return t("untitled") + ".ifcx";
    }

    /**
     * The base name of a file, without its extension: {@code name.ifacex} becomes {@code name}.
     *
     * @param path the file
     * @return the base name, or an empty string when the path has no file name
     */
    static String baseName(Path path) {
        if (path == null || path.getFileName() == null) {
            return "";
        }
        return path.getFileName().toString().replaceAll("\\.[^.]+$", "");
    }

    /**
     * Strips the characters a file name must not contain.
     *
     * @param name the suggested name
     * @return the name as a safe file name
     */
    static String safeFileName(String name) {
        return name == null ? "" : name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    /** Whether the path carries the new {@code .ifcx} format (case-insensitive). */
    private static boolean isIfcx(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ifcx");
    }

    /**
     * Writes the given text to the file, files it in the recent list and updates title/status.
     *
     * @param out  the file to write
     * @param text the content
     */
    boolean write(Path out, String text) {
        try {
            java.nio.file.Files.writeString(out, text);
            sourceFile = out;
            loadedBaseName = baseName(out);
            app.addRecentFile(out.toFile());
            buildRecentMenu();
            status(format("Saved {0}", out.getFileName().toString()));
            renderTitle();
            updateRunButtons();
            return true;
        } catch (Exception e) {
            error(format("Could not write {0}", out.getFileName().toString()), e);
            return false;
        }
    }

    @FXML
    private void onExit() {
        if (!confirmDiscardChanges()) {
            return;
        }
        disposeEditor();
        // The application ends: on macOS hiding the window would only leave the process
        // running in the dock.
        Platform.exit();
    }

    /**
     * Releases the raw editor before the window goes away.
     *
     * <p>The editor is a Swing component: removing it makes macOS call into the native input
     * method, which can block the AWT thread forever. Switching its input methods off first
     * avoids that - see {@link RawEditor#dispose()}.</p>
     */
    public void disposeEditor() {
        if (rawEditor != null) {
            rawEditor.dispose();
        }
        if (console != null) {
            console.dispose();
        }
    }

    /**
     * Opens the currently loaded configuration file in the platform's default external editor.
     * Called from the "External Edit" toolbar button. Disabled when no configuration is loaded.
     */
    private void openExternalEditor() {
        if (sourceFile == null) {
            return;
        }
        final java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
        if (!desktop.isSupported(java.awt.Desktop.Action.OPEN)) {
            UI.showFineWarningDialog(app,
                    t("Opening an external editor is not supported on this platform.<br>"
                            + "File: <b>{0}</b>", sourceFile.toAbsolutePath().toString()));
            return;
        }
        try {
            desktop.open(sourceFile.toFile());
        } catch (IOException e) {
            UI.showFineWarningDialog(app,
                    format("Could not open {0} in an external editor.<br>"
                                    + "File: <b>{1}</b><br>Please open it manually.",
                            sourceFile.getFileName(), sourceFile.toAbsolutePath().toString()));
        }
    }

    /**
     * Shows the About window through the autumo-ui pane, with the license button.
     *
     * <p>The framework builds the dialog header, HTML body, footer) and
     * loads the body from {@code html/about_<lang>.html} plus, behind the license button,
     * {@code html/license_<lang>.html}. That button is the only way to the license — there is no
     * {@code Help › License} item, just as in the original Studio. Both files are resolved
     * relative to {@link StudioApp}, and the
     * language is the one autumo-ui reads from the {@code user.language} system property, which
     * {@link Messages#setLanguage(String)} keeps in sync. The text is therefore a translatable
     * resource and no dialog code changes per language — see {@code docs/12-i18n.md}.</p>
     */
    @FXML
    private void onAbout() {
        UI.showAboutWithLicense(app, "info");
    }

    /**
     * {@code Configuration › Encoder}: opens the framework's encoder pane.
     *
     * <p>The legacy Studio filled the pane from its own {@code STR_ENC_MODEL} list ("none",
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

    /** {@code Configuration › JSON Formatter}: the framework's JSON formatter window (legacy Studio
     *  used to show this pane in the Help menu; the Configuration menu groups all tool panes). */
    @FXML
    private void onJsonFormatter() {
        UI.showJsonFormatter(app);
    }

    /** {@code Configuration › Base Interface Configuration}: shows the ifaceX instance's
     * {@code cfg/ifacex.ifcx} (or legacy {@code cfg/ifacex.cfg}) in a code editor so it can be 
     * viewed and edited. Requires a configured ifaceX test instance; otherwise a fine dialog informs
     * the user and the editor does not open. */
    @FXML
    private void onBaseInterfaceConfig() {
        final String ifacexFolder = StudioHelper.getIfacexFolder(app);
        if (ifacexFolder == null || ifacexFolder.isBlank()) {
            UI.showFineWarningDialog(app,
                    t("No ifaceX test instance configured.<br>Open <b>General Settings</b> and specify the ifaceX instance folder first."));
            return;
        }
        
        boolean yaml = false;
        boolean cfg = false;
        Path validPath = null;        
        Path baseInterfacesYml = java.nio.file.Paths.get(ifacexFolder, "cfg", "ifacex.ifcx");
        Path baseInterfacesCfg = java.nio.file.Paths.get(ifacexFolder, "cfg", "ifacex.cfg");
        if (Files.isReadable(baseInterfacesYml)) {
        	yaml = true;
        	validPath = baseInterfacesYml;
        } else {
            if (Files.isReadable(baseInterfacesCfg)) {
            	cfg = true;
            	validPath = baseInterfacesCfg;
            }
        }

        if (!yaml && !cfg) {
            UI.showFineWarningDialog(app,
                    format("The interfaces configuration <b>{0}</b> or the legacy file <b>{1}</b> could not be read.",
                    		baseInterfacesYml.toString(), baseInterfacesCfg.toString()));
            return;
        }
        
        final Stage stage = new Stage();
        stage.setResizable(true);
        stage.setMinHeight(480);
        stage.setMinWidth(640);

        final DefaultCodeEditorView view = new DefaultCodeEditorView(app, stage, null);
        view.registerSyntaxStyle("ifcx", SyntaxConstants.SYNTAX_STYLE_YAML);
        final DefaultCodeEditorController controller =
                (DefaultCodeEditorController) UI.setup(app, app.getBaseResourcePath(), view, Constants.VIEW_CODE_EDITOR);
        controller.customize(t("Base Interface Configuration"), "gear-six", null);

        final Path currPath = validPath;
        try {
            view.setSourceFile(currPath);
        } catch (IOException e) {
            UI.showFineWarningDialog(app,
                    format("Could not read {0}: {1}", currPath, e.getMessage()));
            return;
        }
        
        final Button btnSave = new Button(t("Save"));
        btnSave.setOnAction(_ -> {
            try {
                Files.writeString(currPath, view.getText());
                UI.showFineInformationDialog(app,
                        t("The base interface configuration was saved."));
            } catch (IOException e) {
                UI.showFineWarningDialog(app,
                        format("Could not save {0}: {1}", currPath, e.getMessage()));
            }
        });

        final Button btnClose = new Button(t("Close"));
        btnClose.setOnAction(_ -> stage.close());
        
        controller.setButtons(List.of(btnSave));
        controller.setHelpText(t(
                "<html>All variables listed here are available for read and write access and can be overwritten in any configuration of the interface processor.<br>Variable “RUN_ENV”: Define your environment.</html>"));
        
        stage.setTitle(t("Base Interface Configuration"));
        for (final java.net.URL url : Helper.getApplicationIcons(app)) {
            stage.getIcons().add(new Image(url.toExternalForm()));
        }
        stage.initModality(javafx.stage.Modality.NONE);
        if (app.getPrimaryStage() != null && app.getPrimaryStage() != stage) {
            stage.initOwner(app.getPrimaryStage());
        }
        stage.show();
        stage.centerOnScreen();
    }
    
    /** {@code Configuration › Interface Configuration}: shows the ifaceX instance's
     * {@code cfg/interfaces.cfg} in a code editor so it can be viewed and edited. Requires a
     * configured ifaceX test instance; otherwise a fine dialog informs the user and the editor
     * does not open. */
    @FXML
    private void onInterfaceConfig() {
        final String ifacexFolder = StudioHelper.getIfacexFolder(app);
        if (ifacexFolder == null || ifacexFolder.isBlank()) {
            UI.showFineWarningDialog(app,
                    t("No ifaceX test instance configured.<br>Open <b>General Settings</b> and specify the ifaceX instance folder first."));
            return;
        }
        final Path interfacesCfg = java.nio.file.Paths.get(ifacexFolder, "cfg", "interfaces.cfg");
        if (!Files.isReadable(interfacesCfg)) {
            UI.showFineWarningDialog(app,
                    format("The interfaces configuration <b>{0}</b> could not be read.",
                            interfacesCfg.toString()));
            return;
        }

        final Stage stage = new Stage();
        stage.setResizable(true);
        stage.setMinHeight(480);
        stage.setMinWidth(640);

        final DefaultCodeEditorView view = new DefaultCodeEditorView(app, stage, null);
        final DefaultCodeEditorController controller =
                (DefaultCodeEditorController) UI.setup(app, app.getBaseResourcePath(), view, Constants.VIEW_CODE_EDITOR);
        controller.customize(t("Interface Configuration"), "gear-six", null);

        try {
            view.setSourceFile(interfacesCfg);
        } catch (IOException e) {
            UI.showFineWarningDialog(app,
                    format("Could not read {0}: {1}", interfacesCfg, e.getMessage()));
            return;
        }

        final Button btnSave = new Button(t("Save"));
        btnSave.setOnAction(_ -> {
            try {
                Files.writeString(interfacesCfg, view.getText());
                UI.showFineInformationDialog(app,
                        t("The interface configuration was saved."));
            } catch (IOException e) {
                UI.showFineWarningDialog(app,
                        format("Could not save {0}: {1}", interfacesCfg, e.getMessage()));
            }
        });

        final Button btnClose = new Button(t("Close"));
        btnClose.setOnAction(_ -> stage.close());
        
        controller.setButtons(List.of(btnSave));
        controller.setHelpText(t(
                "<html>Only change the configuration of these interfaces if you know exactly what you're doing (ifaceX developer).</html>"));
        
        stage.setTitle(t("Interface Configuration"));
        for (final java.net.URL url : Helper.getApplicationIcons(app)) {
            stage.getIcons().add(new Image(url.toExternalForm()));
        }
        stage.initModality(javafx.stage.Modality.NONE);
        if (app.getPrimaryStage() != null && app.getPrimaryStage() != stage) {
            stage.initOwner(app.getPrimaryStage());
        }
        stage.show();
        stage.centerOnScreen();
    }

    /** {@code Configuration › Remove App Data}: the framework's pane (it asks before deleting). */
    @FXML
    private void onRemoveAppData() {
        UI.showRemovePane(app);
    }

    /** {@code Configuration › General Settings}: opens the settings dialog of the Studio. */
    @FXML
    private void onSettings() {
    	UI.showPane(
    			app, 
    			null,
    			new SettingsActions(),
    			Constants.VIEW_DEFAULT_INFO, 
    			t("General Settings"),
    			"gear-six" 
    		);	    	
    }

    @FXML
    private void onRegister() {
        UI.showFineInformationDialog(
        		app, 
        		t("In the following dialog, select the root folder where your purchased autumo ifaceX instance is located (which contains, among other things, the OPERATION.txt file)...") 
        	);
    	
        final StudioApp studioApp = (StudioApp) app;
        
        String folder = StudioHelper.getIfacexFolder(app);
    	String ifacexFolder = Helper.chooseFolder(folder, stage, t("ifaceX Instance Directory"), t("ifaceX Directory"));
    	
    	if (ifacexFolder != null && ifacexFolder.length() != 0) {
	    	ifacexFolder = ifacexFolder.trim();
	    	if (!ifacexFolder.endsWith(OSUtils.FILE_SEPARATOR))
	    		ifacexFolder = ifacexFolder + OSUtils.FILE_SEPARATOR;
    	}
    	if (!StudioHelper.checkIfacexFolder(studioApp, ifacexFolder)) {
    		UI.showFineWarningDialog(app, t("No autumo ifaceX instance was found at the selected location!"));
    		return;
    	}    	
    	final LicIfaceXApp100 licApp = new LicIfaceXApp100();
    	try {
    		licApp.initializeBaseConfig(ifacexFolder);
		} catch (Exception e) {
            LOG.error("Couldn't read base configuration from ifacex instance.", e);
            UI.showFineWarningDialog(app, t("No autumo ifaceX instance was found at the selected location!"));
        	return;
		}
    	// further checks
    	final String licPath = licApp.getLicensePath();
    	final String ifFolder = ifacexFolder + StudioConstants.IFACEX_PATH_REL_INTERFACES;
    	if (!new File(licPath).exists() || !new File(ifFolder).exists()) {
    		UI.showFineWarningDialog(app, t("No autumo ifaceX instance was found at the selected location!"));
        	return;
    	}
    	
    	UI.showRegistrationPane(app, licApp, t("Registration.title"));
    }
    
    @FXML
    private void onRun() {
        run(false);
    }

    private void run(boolean rootCall) {
        if (config == null) {
            status(t("Nothing to run."));
            return;
        }
        // Ensure current file is saved
        if (modified || sourceFile == null || !isIfcx(sourceFile)) {
            if (tabRaw.isSelected()) {
                if (!applyRawText()) {
                    return;
                }
            }
            final Path target = saveTarget();
            // A file that was just converted from the legacy format has no .ifcx yet: it is
            // saved first and then executed. Cancelling the dialog aborts the run.
            final boolean saved = target == null
                    ? saveAs()
                    : write(target, YamlConfigWriter.toText(config));
            if (!saved) {
                return;
            }
        } else {
            if (tabRaw.isSelected()) {
                if (!applyRawText()) {
                    return;
                }
            }
            if (!save()) {
                return;
            }
        }

        final StudioApp studioApp = (StudioApp) app;
        final String ifacexFolder = StudioHelper.getIfacexFolder(app);
        if (!StudioHelper.checkIfacexFolder(studioApp, ifacexFolder)) {
            UI.showFineWarningDialog(app, t("No autumo ifaceX instance was found at the selected location!"));
            return;
        }

        if (console != null) {
            console.setText("");
        }
        if (tabConsole != null) {
            tabPane.getSelectionModel().select(tabConsole);
        }

        final boolean debugMode = miDebugMode != null && miDebugMode.isSelected();
        processController = new ProcessController(this, debugMode);
        processRunning = true;
        updateRunButtons();

        processController.execute(rootCall);
    }

    @FXML
    private void handleDebugModeToggle() {
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

    /** State shown behind the file: "Loaded", "Converted ...", "Modified", "Saved ...". */
    private String statusState = "";

    /** Whether the shown state is the modified one — compared as a flag, not as translated text. */
    private boolean modified = false;

    /** Set once the user agreed to close a modified configuration; reset when the state changes. */
    private boolean discardConfirmed = false;

    /** Sets the state part of the status bar and re-renders it. */
    private void status(String text) {
        statusState = text == null ? "" : text;
        modified = false;
        renderStatus();
        renderTitle();
    }

    /**
     * The status bar reads like the original Studio: {@code File: <absolute path>} first, the
     * state behind it. Without a loaded file it shows {@code File: -}.
     */
    private void renderStatus() {
        if (sourceFile == null) {
            statusLabel.setText(t("Ready"));
            return;
        }
        final String file = t("File") + ": " + sourceFile.toAbsolutePath();
        statusLabel.setText(statusState.isEmpty() ? file : file + "   \u2014   " + statusState);
    }

    /**
     * The window title reads like the original Studio: the application name, then a dash and the
     * loaded file name, and a trailing {@code *} while the configuration holds unsaved changes.
     * Without a loaded file it is the application name alone.
     */
    private void renderTitle() {
        final String base = t("application.name");
        final StringBuilder title = new StringBuilder(base);
        if (sourceFile != null) {
            title.append(" - ").append(sourceFile.getFileName());
            if (modified) {
                title.append(" *");
            }
        }
        stage.setTitle(title.toString());
    }

    /** Flag the configuration as changed, without repeating an existing "Modified". */
    private void markModified() {
        if (modified) {
            return;
        }
        modified = true;
        statusState = t("Modified");
        renderStatus();
        renderTitle();
    }

    private void error(String message, Exception e) {
        status(message);
    	UI.showErrorAndWait(app, e, message);
    }

    public Console getConsole() {
        return console;
    }

    public void appendConsole(String str) {
        if (console == null) {
            return;
        }
        console.appendLine(str);
    }

    public void clearConsole() {
        if (console == null) {
            return;
        }
        console.clear();
    }

    private Window window() {
        return statusLabel.getScene() == null ? null : statusLabel.getScene().getWindow();
    }

    /**
     * Get split width.
     * 
     * @return split width
     */
    public double getSplitWidth() {
        double pos = mainSplit.getDividerPositions()[0];
        return pos * mainSplit.getWidth();
    }
    
    /**
     * Keeps the navigation pane at a fixed width while the window is resized.
     *
     * <p>A {@link SplitPane} distributes a resize proportionally over its items - the divider
     * position is a ratio, so narrowing the window also narrowed the tree. The tree must keep
     * its width and only the right side may shrink (down to the minimum of the navigation
     * pane).</p>
     */
    private void keepNavigationWidth() {
        if (mainSplit == null || navHost == null) {
            return;
        }
        // The split pane must not resize the navigation pane by itself; its width is fixed.
        SplitPane.setResizableWithParent(navHost, Boolean.FALSE);
        keepNavigationWidth(mainSplit.getWidth());
        mainSplit.widthProperty().addListener((_, _, width) -> keepNavigationWidth(width.doubleValue()));
    }

    /**
     * Sets the divider so that the navigation pane is exactly {@link #NAV_WIDTH} pixels wide.
     *
     * <p>The width is fixed: resizing the window only changes the form/raw/console side. The
     * split pane can only be told a ratio, hence the one division by the pane width.</p>
     *
     * @param total width of the split pane in pixels
     */
    private void keepNavigationWidth(double total) {
        if (total <= 0) {
            return;
        }
        mainSplit.setDividerPositions(splitPosition / total);
    }

    /**
     * Makes the navigation pane the drop target for configuration files: a file dragged onto the
     * tree (or onto the About page, as long as no configuration is loaded) is opened.
     *
     * <p>Only the left side accepts a drop - a configuration is opened from the navigation, the
     * right side is where it is edited. The {@code aboutView} is a {@link WebView} and handles
     * drag events itself, so it is registered separately.</p>
     */
    private void installDropTarget() {
        installDropTarget(navHost, null);
        installDropTarget(aboutView, null);
        // The whole right side accepts a drop - the form, the raw view and the console.
        installDropTarget(tabPane, null);
        // The raw editor is a Swing component inside a SwingNode: JavaFX never sees a drop
        // over it, so it gets its own handler.
        if (rawEditor != null) {
            rawEditor.setOnFilesDropped(this::openDroppedFiles);
        }
        if (console != null) {
            console.setOnFilesDropped(this::openDroppedFiles);
        }
    }

    /**
     * Registers the drag-and-drop handlers of a drop target.
     *
     * @param target node that accepts a dropped configuration file
     */
    private void installDropTarget(final Node target, final java.util.function.BooleanSupplier enabled) {
        if (target == null) {
            return;
        }
        target.setOnDragEntered(event -> {
            if (accepts(enabled) && hasConfigurationFiles(event.getDragboard())) {
                if (!target.getStyleClass().contains(DROP_TARGET)) {
                    target.getStyleClass().add(DROP_TARGET);
                }
            }
            event.consume();
        });
        target.setOnDragExited(event -> {
            target.getStyleClass().remove(DROP_TARGET);
            event.consume();
        });
        target.setOnDragOver(event -> {
            if (accepts(enabled) && hasConfigurationFiles(event.getDragboard())) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
        target.setOnDragDropped(event -> {
            target.getStyleClass().remove(DROP_TARGET);
            final boolean done = accepts(enabled) && openDroppedFiles(event.getDragboard().getFiles());
            event.setDropCompleted(done);
            event.consume();
        });
    }

    /**
     * Whether a drop target is active at the moment (e.g. not while the console is shown).
     *
     * @param enabled condition, may be {@code null} for "always"
     * @return {@code true} when a drop is accepted
     */
    private static boolean accepts(java.util.function.BooleanSupplier enabled) {
        return enabled == null || enabled.getAsBoolean();
    }

    /**
     * Opens the first configuration file of a drop - used by the JavaFX drop targets and by the
     * raw editor ({@link RawEditor#setOnFilesDropped}).
     *
     * @param files dropped files, may be {@code null}
     * @return {@code true} when a file was opened
     */
    boolean openDroppedFiles(final List<File> files) {
        if (files == null || files.isEmpty()) {
            return false;
        }
        if (!javafx.application.Platform.isFxApplicationThread()) {
            // A drop onto a Swing component (console, raw editor) is delivered on the AWT
            // thread; opening a file touches the JavaFX scene and must run on the FX thread.
            javafx.application.Platform.runLater(() -> openDroppedFiles(files));
            return true;
        }
        final File file = firstConfigurationFile(files);
        if (file == null) {
            UI.showFineWarningDialog(app, t("Only .ifcx and .ifacex files can be opened."));
            return false;
        }
        if (modified && !confirmDiscardChanges()) {
            return false;
        }
        openFile(file.toPath());
        return true;
    }

    /**
     * Whether the dragboard carries a configuration file.
     *
     * @param board dragboard of the drag event
     * @return {@code true} for a supported file
     */
    private static boolean hasConfigurationFiles(Dragboard board) {
        return firstConfigurationFile(board) != null;
    }

    /**
     * The first supported configuration file of a dragboard.
     *
     * @param board dragboard of the drag event
     * @return the file, or {@code null} when the drag carries none
     */
    private static File firstConfigurationFile(Dragboard board) {
        if (board == null || !board.hasFiles()) {
            return null;
        }
        return firstConfigurationFile(board.getFiles());
    }

    /**
     * The first supported configuration file of a dropped file list.
     *
     * @param files dropped files
     * @return the file, or {@code null} when none is supported
     */
    private static File firstConfigurationFile(List<File> files) {
        if (files == null) {
            return null;
        }
        for (final File file : files) {
            final String name = file.getName().toLowerCase(Locale.ROOT);
            if (name.endsWith(".ifcx") || name.endsWith(".ifacex")
                    || name.endsWith(".yaml") || name.endsWith(".yml")) {
                return file;
            }
        }
        return null;
    }
}
