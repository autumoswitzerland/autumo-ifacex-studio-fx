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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.autumo.commons.configuration.ConfigurationManager;
import ch.autumo.ifacex.studio.core.io.YamlConfigReader;
import ch.autumo.ifacex.studio.core.io.YamlConfigWriter;
import ch.autumo.ifacex.studio.core.meta.FieldCatalog;
import ch.autumo.ifacex.studio.core.model.IpcConfig;
import ch.autumo.ifacex.studio.ui.FormBuilder;
import ch.autumo.ifacex.studio.ui.StudioApp;
import ch.autumo.ifacex.studio.ui.StudioHelper;
import ch.autumo.ifacex.studio.ui.view.MainView;
import ch.autumo.ui.Constants;
import ch.autumo.ui.fx.control.Icons;
import ch.autumo.ui.fx.controller.DefaultInfoController;
import ch.autumo.ui.fx.controller.actions.RemoveAction;
import ch.autumo.ui.utils.Helper;
import ch.autumo.ui.utils.Messages;
import ch.autumo.ui.utils.UI;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * UI tests of the Studio shell.
 *
 * <p>Instead of launching the application, the tests build the real shell the way
 * {@code StudioApp} does it — {@link StudioApp#preInitialize(Stage, Properties)} loads
 * {@code main.fxml} and binds the controller — and assert on it directly: menu bar, toolbar,
 * icons, fonts, the live language switch, the framework's info panes and the metadata-driven
 * forms.</p>
 *
 * <p>Everything runs on the JavaFX application thread ({@link Platform#runLater}); the toolkit is
 * started once. The Gradle test JVM runs with a scratch {@code user.home} (see {@code ui/build.gradle}), so
 * the tests cover the persistence of the language choice without ever touching the developer's own
 * {@code app.cfg}.</p>
 *
 * <p>Until the language menu moved into autumo-ui these checks were a {@code --selftest} mode of
 * {@code MainController}; a test belongs into the test sources, not into the application.</p>
 */
class StudioUiTest {

    /**
     * Sample configuration used by the form tests, taken from the core module's test resources
     * (the working directory of a Gradle test run is the module directory).
     */
    private static final Path SAMPLE = Path.of("..", "core", "src", "test", "resources",
            "templates", "002-hubspot-v3-companies.ifacex");

    /** Language the shell is built in; the tests switch between it and the other one. */
    private static final String START_LANG = Messages.LANG_EN;

    /** The other supported language. */
    private static final String OTHER_LANG = "de";

    /** The user's application configuration of this test run. */
    private static Path appCfg;

    /** Desktop locale of the JVM, restored after the tests. */
    private static Locale desktopLocale;

    private static StudioApp app;
    private static MainController controller;
    private static Stage stage;

    /**
     * Starts the JavaFX toolkit and builds the Studio shell once for all tests.
     *
     * @throws Exception if the toolkit or the shell cannot be built
     */
    @BeforeAll
    static void buildShell() throws Exception {
        desktopLocale = Locale.getDefault();
        final CountDownLatch started = new CountDownLatch(1);
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            started.countDown();
        });
        assertTrue(started.await(30, TimeUnit.SECONDS), "the JavaFX toolkit did not start");

        call(() -> {
            final Properties configuration = new Properties();
            configuration.setProperty(Constants.PROP_KEY_LANGUAGE, START_LANG);
            stage = new Stage();
            app = new StudioApp();
            // BaseApp.start(…) initialises the configuration manager from the template before the
            // view is built; UI.setup(…) reads the macOS menu-detach switch from it.
            Properties props = Helper.loadTemplateConfiguration(app);
            ConfigurationManager.getInstance().initializeWithProperties(props);

            final MainView view = (MainView) app.preInitialize(stage, configuration);
            controller = (MainController) view.getController();
            // The style sheet StudioApp.initialize(…) adds on top of the autumo-ui base style:
            // the application stylesheet of the autumo-ui resource structure. It is resolved the
            // same way production resolves it, so a missing file fails this test immediately.
            stage.getScene().getStylesheets()
                    .add(Helper.getAppResource(app, Constants.STYLESHEET));
            stage.setWidth(1100);
            stage.setHeight(720);
            stage.show();
            return null;
        });

        // The user's application configuration of this test run: autumo-ui resolves it below the
        // scratch 'user.home' of the Gradle test JVM (see ui/build.gradle). It has to exist for a language
        // switch to be stored — which is exactly what the tests verify.
        appCfg = Path.of(Helper.getPropertiesPath(app), "cfg", "app.cfg");
        Files.createDirectories(appCfg.getParent());
        Files.writeString(appCfg, "language=" + START_LANG + "\n");
    }

    /**
     * Closes the shell and restores the desktop locale.
     */
    @AfterAll
    static void closeShell() {
        run(() -> {
            stage.close();
            controller.switchLanguage(START_LANG);
        });
        Locale.setDefault(desktopLocale);
        Platform.exit();
    }

    /**
     * Puts the shell back into its start state before every test: nothing loaded, start language.
     * The tests share one shell (starting the JavaFX toolkit and building the Studio window is
     * expensive), so they may not inherit a loaded configuration from each other.
     */
    @BeforeEach
    void resetShell() {
        run(() -> {
            controller.onClose();
            controller.switchLanguage(START_LANG);
        });
    }

    /** Shell, style sheets, status bar and the framework's language menu. */
    @Test
    void shellIsBuiltWithTheFrameworkLanguageMenu() {
        call(() -> {
            assertTrue(stage.isShowing(), "the shell window is up");
            assertEquals(START_LANG, Messages.getLanguage());
            assertTrue(stage.getScene().getStylesheets().size() >= 2,
                    "autumo-ui base style sheet plus the Studio's application stylesheet");
            assertTrue(stage.getScene().getStylesheets().stream()
                            .anyMatch(sheet -> sheet.endsWith("/style/application.css")),
                    "the application stylesheet must sit at the framework's conventional path "
                            + "style/application.css: " + stage.getScene().getStylesheets());
            assertEquals(Helper.getSupportedLanguages().length, controller.menuLanguage.getItems().size(),
                    "autumo-ui creates one radio item per supported language");
            assertTrue(controller.isLanguageSelected(START_LANG),
                    "the item of the start language must be selected");
            assertFalse(controller.isLanguageSelected(OTHER_LANG));
            assertNotNull(controller.menuLanguage.getGraphic(),
                    "the language menu carries the framework's language icon");
            final String licenseText = StudioHelper.t("License");
            for (final MenuItem helpItem : controller.menuHelp.getItems())
                assertFalse(licenseText.equals(helpItem.getText()),
                        "no separate Help \u203a License item: the license lives behind About");
            assertEquals("File", controller.menuFile.getText());
            assertEquals("Help", controller.menuHelp.getText());
            // Without a configuration the status bar simply reads "Ready.".
            assertEquals(StudioHelper.t("Ready"), controller.statusLabel.getText(),
                    "no configuration loaded: " + controller.statusLabel.getText());
            return null;
        });
    }

    /**
     * The toolbar mirrors the original Studio and every button must carry a loadable icon; a broken
     * icon file name would otherwise only show up as an empty button in the running application.
     */
    @Test
    void toolbarButtonsResolveTheirIcons() {
        call(() -> {
            int buttons = 0;
            int icons = 0;
            for (final ToolBar bar : new ToolBar[] {controller.leftToolBar, controller.rightToolBar}) {
                for (final Node node : bar.getItems()) {
                    if (!(node instanceof Button button)) {
                        continue;
                    }
                    buttons++;
                    final Node graphic = button.getGraphic();
                    if (graphic instanceof ImageView iv && iv.getImage() != null
                            && iv.getImage().getWidth() > 0) {
                        icons++;
                    } else if (graphic instanceof Text t && !t.getText().isBlank()) {
                        icons++;
                    } else if (graphic instanceof javafx.scene.layout.StackPane sp
                            && sp.getChildren().stream().anyMatch(n -> (n instanceof Text t2 && !t2.getText().isBlank())
                                    || (n instanceof ImageView iv2 && iv2.getImage() != null && iv2.getImage().getWidth() > 0))) {
                        icons++;
                    }
                }
            }
            assertTrue(buttons >= 7, "the toolbar offers the Studio's buttons, got " + buttons);
            assertEquals(buttons, icons, "every toolbar button must carry a resolved icon");
            return null;
        });
    }

    /**
     * The application icon on every operating system is the Studio's own icon: the Studio
     * ships the size-specific {@code icons/app-<size>.png} icons derived from its
     * {@code ifaceX.icns}, and autumo-ui must resolve exactly those instead of the
     * framework's default logo.
     */
    @Test
    void applicationIconsComeFromTheStudio() {
        final List<URL> icons = Helper.getApplicationIcons(app);
        assertEquals(Helper.APPLICATION_ICON_SIZES.length, icons.size(),
                "one application icon per size is expected");
        for (int i = 0; i < icons.size(); i++) {
            assertTrue(icons.get(i).toString().endsWith("/icons/app-" + Helper.APPLICATION_ICON_SIZES[i] + ".png"),
                    "the Studio's own application icon must win: " + icons.get(i));
        }
    }

    /**
     * The Phosphor icon font ships with autumo-ui and every Studio toolbar/menu icon name must
     * resolve to a glyph; an unknown name must yield {@code null} rather than a broken button.
     */
    @Test
    void phosphorIconsResolve() {
        call(() -> {
            for (final String name : new String[] {
                    "file-plus", "folder-open", "floppy-disk", "x", "gear-six", "play",
                    "arrow-u-up-right", "clock-counter-clockwise", "export", "sign-out",
                    "lock-key", "trash", "book-open", "globe-hemisphere-west",
                    "download-simple", "info", "translate", "warning", "question"}) {
                final Text icon = Icons.create(name, 16);
                assertNotNull(icon, "Phosphor icon must resolve: " + name);
                assertFalse(icon.getText().isBlank(), "Phosphor icon must carry a glyph: " + name);
            }
            assertNull(Icons.create("no-such-icon"), "unknown icon names must yield null");
            return null;
        });
    }

    /** The Inter font is the Studio's type face and ships with the module. */
    @Test
    void interFontIsLoaded() {
        assertTrue(Font.getFamilies().contains("Inter"), "Inter must be registered as a font family");
    }

    /**
     * Loading a configuration fills the navigation tree and every navigation entry renders a form —
     * source entities with the field list, destination entities with the mapping table and its
     * drag&amp;drop buttons.
     *
     * @throws Exception if the sample configuration cannot be loaded
     */
    @Test
    void everyNavigationEntryBuildsItsForm() throws Exception {
        assertSampleExists();
        final int[] counts = new int[4]; // forms, source field lists, mapping tables, dnd buttons
        final List<String> labels = new ArrayList<>();
        call(() -> {
            controller.loadConfig(SAMPLE);
            final TreeItem<FormBuilder.NavNode> root = controller.navTree.getRoot();
            assertNotNull(root, "the navigation tree must have a root");
            final List<TreeItem<FormBuilder.NavNode>> items = flatten(root);
            assertTrue(items.size() >= 6, "the sample has several navigation entries, got " + items.size());
            for (final TreeItem<FormBuilder.NavNode> item : items) {
                final Node form = controller.formBuilder.build(item.getValue());
                assertNotNull(form, "no form for '" + item.getValue().label() + "'");
                counts[0]++;
                counts[1] += count(form, ListView.class);
                counts[2] += count(form, TableView.class);
                counts[3] += countDnd(form);
                collectLabels(form, labels);
            }
            assertEquals(items.size(), counts[0], "every navigation entry must have built a form");
            return null;
        });
        assertTrue(counts[1] > 0, "source entity forms must list the available fields");
        assertTrue(counts[2] > 0, "a destination entity must show the mapping table");
        assertTrue(counts[3] > 0, "the mapping table must offer the drag&drop buttons");

        // The metadata labels go through the message lookup: key is the msgid, the derived English
        // label the fallback. Only real engine parameters count, captions pollute the sample.
        final Set<String> known = new HashSet<>();
        for (final String param : FieldCatalog.knownParams()) {
            known.add(StudioHelper.t(param, FieldCatalog.descriptor(param).label()));
        }
        final List<String> paramLabels = labels.stream().filter(known::contains).distinct().toList();
        assertTrue(paramLabels.size() >= 10, "expected translated parameter labels, got " + paramLabels);
    }

    /**
     * The raw view shows the loaded configuration as YAML and the status bar names the file.
     *
     * @throws Exception if the sample configuration cannot be loaded
     */
    @Test
    void rawViewsAndStatusShowTheLoadedConfiguration() throws Exception {
        assertSampleExists();
        call(() -> {
            controller.loadConfig(SAMPLE);
            assertTrue(controller.rawEditor.getText().contains("api.hubapi.com"),
                    "the raw view must show the converted configuration as YAML");
            final String status = controller.statusLabel.getText();
            assertTrue(status.startsWith(StudioHelper.t("File") + ": "), "the status bar names the file: " + status);
            assertTrue(status.contains(SAMPLE.toAbsolutePath().toString()),
                    "the status bar must carry the loaded file: " + status);
            return null;
        });
    }

    /**
     * The raw tab and the form editor are two views of the same model: an edit in the raw YAML is
     * adopted into the model when the tab is left, and the form shows it from then on.
     *
     * @throws Exception if the sample configuration cannot be loaded
     */
    @Test
    void rawEditsAreAdoptedByTheFormEditor() throws Exception {
        assertSampleExists();
        call(() -> {
            controller.loadConfig(SAMPLE);

            // The raw editor shows the model as YAML …
            final String yaml = controller.rawEditor.getText();
            assertTrue(yaml.contains("api.hubapi.com"), "the raw view shows the configuration");

            // … the user opens the raw tab and changes the job title in the raw text …
            controller.tabPane.getSelectionModel().select(controller.tabRaw);
            final IpcConfig changed = YamlConfigReader.read(controller.rawEditor.getText());
            changed.setMeta(changed.meta().withTitle("Title from the raw editor"));
            controller.rawEditor.setText(YamlConfigWriter.toText(changed));

            // … and leaving the raw tab adopts it into the model: the navigation root follows
            // the title (see MainController.label()).
            controller.tabPane.getSelectionModel().select(controller.tabForm);
            final TreeItem<FormBuilder.NavNode> root = controller.navTree.getRoot();
            assertNotNull(root, "the navigation tree has a root");
            assertEquals("Title from the raw editor", root.getValue().label(),
                    "the form editor must show the title typed in the raw view");
            return null;
        });
    }

    /**
     * A change in the raw editor is a change of the configuration: the window title gets its
     * asterisk and the status bar says "Modified" - just as a change in the form editor does.
     *
     * @throws Exception if the sample configuration cannot be loaded
     */
    @Test
    void aChangeInTheRawEditorMarksTheConfigurationAsModified() throws Exception {
        assertSampleExists();
        call(() -> {
            controller.loadConfig(SAMPLE);
            assertFalse(stage.getTitle().endsWith(" *"),
                    "a freshly loaded configuration is not modified: " + stage.getTitle());

            // The editor reports the change of the user (the document listener of the editor
            // widget calls it).
            controller.rawEditor.fireEdit();

            assertTrue(stage.getTitle().endsWith(" *"),
                    "a change in the raw editor is marked with an asterisk: " + stage.getTitle());
            assertTrue(controller.statusLabel.getText().contains(StudioHelper.t("Modified")),
                    "the status bar shows the change: " + controller.statusLabel.getText());
            return null;
        });
    }

    /**
     * The window title carries the loaded file name and a trailing {@code *} while the
     * configuration holds unsaved changes - the marker of the original Studio.
     *
     * @throws Exception if the sample configuration cannot be loaded
     */
    @Test
    void titleShowsTheFileNameAndTheModifiedMarker() throws Exception {
        assertSampleExists();
        final Path target = Files.createTempFile("autumo-studio-title", ".ifcx");
        try {
            call(() -> {
                controller.loadConfig(SAMPLE);
                assertEquals("ifaceX Studio FX - " + SAMPLE.getFileName(), stage.getTitle(),
                        "the title shows the application and the loaded file");

                // A change in the raw view marks the configuration as modified …
                controller.tabPane.getSelectionModel().select(controller.tabRaw);
                final IpcConfig changed = YamlConfigReader.read(controller.rawEditor.getText());
                changed.setMeta(changed.meta().withTitle("Modified"));
                controller.rawEditor.setText(YamlConfigWriter.toText(changed));
                controller.tabPane.getSelectionModel().select(controller.tabForm);
                assertTrue(stage.getTitle().endsWith(" *"),
                        "an unsaved change is marked with an asterisk: " + stage.getTitle());

                // … saving takes the marker away again.
                controller.write(target, controller.rawEditor.getText());
                assertFalse(stage.getTitle().endsWith(" *"),
                        "a saved configuration is not marked any more: " + stage.getTitle());
                return null;
            });
        } finally {
            Files.deleteIfExists(target);
        }
    }

    /**
     * {@code File › Save} overwrites a configuration that is already stored as {@code .ifcx} and
     * only asks for a file when there is none - the legacy {@code .ifacex} is an import, not a
     * save target.
     *
     * @throws Exception if the sample configuration cannot be loaded
     */
    @Test
    void saveOverwritesTheLoadedIfcxFileAndAsksOtherwise() throws Exception {
        assertSampleExists();
        final Path ifcx = Files.createTempFile("autumo-studio-save", ".ifcx");
        try {
            call(() -> {
                controller.loadConfig(SAMPLE);
                assertNull(controller.saveTarget(),
                        "a legacy .ifacex file is not a save target - the dialog is needed");

                // The same configuration as .ifcx …
                Files.writeString(ifcx, controller.rawEditor.getText());
                controller.loadConfig(ifcx);
                assertEquals(ifcx, controller.saveTarget(),
                        "an .ifcx file is overwritten directly, without a dialog");

                // … a change in the raw view and a save: the file carries the new title.
                controller.tabPane.getSelectionModel().select(controller.tabRaw);
                final IpcConfig changed = YamlConfigReader.read(controller.rawEditor.getText());
                changed.setMeta(changed.meta().withTitle("Saved title"));
                controller.rawEditor.setText(YamlConfigWriter.toText(changed));
                controller.save();
                assertTrue(Files.readString(ifcx).contains("Saved title"),
                        "the file must carry the saved configuration");
                return null;
            });
        } finally {
            Files.deleteIfExists(ifcx);
        }
    }

    /**
     * The live language switch moves the framework catalogue, the Studio's catalogue, the language
     * menu selection and the Studio's own texts — the mechanism itself lives in autumo-ui
     * ({@code BaseController.switchLanguage}), the Studio only re-applies its texts.
     *
     * @throws Exception if the sample configuration cannot be loaded
     */
    @Test
    void languageSwitchRetranslatesStudioAndFramework() throws Exception {
        assertSampleExists();
        call(() -> {
            controller.loadConfig(SAMPLE);
            try {
                controller.switchLanguage(OTHER_LANG);
                assertEquals(OTHER_LANG, Messages.getLanguage());
                assertEquals(OTHER_LANG, Helper.getLanguage());
                // … the Studio's own catalogue followed …
                assertEquals("Datei", controller.menuFile.getText());
                assertEquals("Schließen", StudioHelper.t("Close"));
                // … and the framework's catalogue, which the Studio's PO does not define.
                assertEquals("Okay", StudioHelper.t("button.Alright"));
                // The Studio's catalogues override the framework's application name; the loaded
                // file name follows behind a dash.
                assertEquals("ifaceX Studio FX - " + SAMPLE.getFileName(), stage.getTitle());
                assertTrue(controller.isLanguageSelected(OTHER_LANG), "the menu shows German as selected");
                assertFalse(controller.isLanguageSelected(START_LANG));
                assertTrue(controller.statusLabel.getText().startsWith("Datei: "),
                        "the status bar was re-rendered: " + controller.statusLabel.getText());
            } finally {
                controller.switchLanguage(START_LANG);
            }
            assertEquals(START_LANG, Messages.getLanguage());
            assertEquals("File", controller.menuFile.getText());
            assertEquals("Alright", StudioHelper.t("button.Alright"));
            assertTrue(controller.isLanguageSelected(START_LANG));
            assertFalse(controller.isLanguageSelected(OTHER_LANG));
            return null;
        });
    }

    /**
     * The language choice is written into the user's application configuration right away, so the
     * next start comes up in it (see {@code BaseController.switchLanguage}).
     *
     * @throws Exception if the configuration file cannot be read
     */
    @Test
    void languageSwitchIsStoredInTheApplicationConfiguration() throws Exception {
        call(() -> {
            controller.switchLanguage(OTHER_LANG);
            return null;
        });
        assertTrue(Files.readString(appCfg).contains("language=" + OTHER_LANG),
                "the choice must reach cfg/app.cfg");
        assertEquals(OTHER_LANG, app.getConfigurationParameter(Constants.PROP_KEY_LANGUAGE));
        call(() -> {
            controller.switchLanguage(START_LANG);
            return null;
        });
        assertTrue(Files.readString(appCfg).contains("language=" + START_LANG));
    }

    /**
     * The framework's info panes are opened the way their menu items do it: they must build (their
     * framework keys must resolve) and actually show a window, which is then closed again. They are
     * checked in both languages, because they carry per-language HTML.
     */
    @Test
    void frameworkPanesOpenInEveryLanguage() {
        for (final String lang : new String[] {START_LANG, OTHER_LANG}) {
            call(() -> {
                controller.switchLanguage(lang);
                try {
                    assertTrue(paneOpens(() -> UI.showAboutWithLicense(app,
                            "info")), "About pane (" + lang + ")");
                    assertTrue(paneOpens(() -> UI.showEncoder(app)), "Encoder pane (" + lang + ")");
                    assertTrue(paneOpens(() -> UI.showLicense(app)), "License pane (" + lang + ")");
                    // The remove pane's HTML is not shipped by the Studio: the framework's default
                    // (autumo-ui html/remove_<lang>.html) is used. Its action runs via
                    // Platform.runLater on the menu path, where a failure would only be printed and
                    // never fail this test - so the action is run once more, synchronously.
                    final DefaultInfoController[] removeInfo = new DefaultInfoController[1];
                    assertTrue(paneOpens(() -> removeInfo[0] = UI.showRemovePane(app)),
                            "Remove pane (" + lang + ")");
                    final RemoveAction removeAction = new RemoveAction();
                    removeAction.setApp(app);
                    removeAction.setController(removeInfo[0]);
                    removeAction.execute();
                    assertNotNull(removeInfo[0].getBody().getCenter(),
                            "the remove pane must have content (" + lang + ")");
                } finally {
                    controller.switchLanguage(START_LANG);
                }
                return null;
            });
        }
        // The per-language HTML panes are resolved from the Studio's own package; a typo would only
        // surface as a WebView error page, so pin them.
        for (final String file : new String[] {"html/about_en.html", "html/about_de.html",
                "html/license_en.html", "html/license_de.html"}) {
            assertNotNull(StudioApp.class.getResource(file), "missing resource: " + file);
        }
    }

    /**
     * Every pane shown through {@code UI.showPane} (and the JSON formatter, which uses the same
     * FXML) is resizable down to the framework minimum 640x480, and only the <b>middle</b> gives
     * way when it shrinks: header and footer keep their 60px height, so the footer buttons are
     * never clipped (they were, before the panes' {@code min/maxHeight} were corrected).
     *
     * <p>The layout is driven directly ({@code resize} + {@code layout}) instead of resizing the
     * window, so the assertion does not depend on the window manager.</p>
     */
    @Test
    void panesShrinkToTheMinimumWithoutClippingHeaderOrFooter() {
        call(() -> {
            assertPaneShrinksKeepingChrome(() -> UI.showAboutWithLicense(app, "info"));
            assertPaneShrinksKeepingChrome(() -> UI.showJsonFormatter(app, null));
            return null;
        });
    }

    /**
     * Opens a pane, pins the minimum window size and checks that a resize to 640x480 keeps
     * header and footer at 60px while the body frame takes the rest.
     *
     * @param opener opens the pane under test
     */
    private static void assertPaneShrinksKeepingChrome(Runnable opener) {
        final List<Window> before = List.copyOf(Window.getWindows());
        opener.run();
        final List<Stage> opened = Window.getWindows().stream()
                .filter(w -> !before.contains(w))
                .filter(Stage.class::isInstance).map(Stage.class::cast).toList();
        try {
            assertEquals(1, opened.size(), "exactly one pane window must open");
            final Stage pane = opened.get(0);
            assertTrue(pane.isResizable(), "the pane must be resizable");
            assertEquals(640, pane.getMinWidth(), 0.01, "the minimum pane width is 640");
            assertEquals(480, pane.getMinHeight(), 0.01, "the minimum pane height is 480");

            final Parent root = pane.getScene().getRoot();
            final BorderPane chrome = (BorderPane) root;
            final Region header = (Region) chrome.getTop();
            final Region body = (Region) chrome.getCenter();
            final Region footer = (Region) chrome.getBottom();
            assertNotNull(header, "the pane has a header");
            assertNotNull(body, "the pane has a body");
            assertNotNull(footer, "the pane has a footer");

            // Shrink to the minimum pane size: header/footer may not move, only the body.
            root.resize(640, 480);
            root.applyCss();
            root.layout();

            assertEquals(60, header.getBoundsInParent().getHeight(), 0.5,
                    "the header keeps its 60px height");
            assertEquals(60, footer.getBoundsInParent().getHeight(), 0.5,
                    "the footer keeps its 60px height");
            assertEquals(480 - 120, body.getBoundsInParent().getHeight(), 0.5,
                    "the body takes everything the header and footer leave");

            // The current footer (main.fxml) is a simple statusBar HBox with one label
            // (statusLabel) and no buttons — there is nothing to clip or overlap here.
        } finally {
            opened.forEach(Stage::close);
        }
    }

    /**
     * The framework's standard dialogs (confirmation, information, warning) are styled like the
     * main window: the autumo-ui base style plus the Studio's own {@code style/application.css}
     * on top ({@code Helper.getApplicationStyleSheets}). Only the Studio sheet alone would drop
     * the dark palette. The confirmation dialog is the one 'Remove App Data' shows before it
     * deletes anything, i.e. the user-visible path of this lookup.
     *
     * <p>The dialogs are modal ({@code showAndWait}), so each one is closed again from inside its
     * own event loop — otherwise the test would hang until someone clicks the button.</p>
     */
    @Test
    void standardDialogsCarryBaseAndApplicationStyles() {
        final List<String> sheets = Helper.getApplicationStyleSheets(app);
        assertEquals(2, sheets.size(), "base style plus the Studio's own style: " + sheets);
        assertTrue(sheets.get(0).endsWith("/ch/autumo/ui/style/application.css"),
                "the autumo-ui base style comes first: " + sheets);
        assertTrue(sheets.get(1).endsWith("/style/application.css"),
                "the Studio's style/application.css follows: " + sheets);
        call(() -> {
            assertNotNull(showDialogAndCloseIt(() -> UI.showConfirmationDialog(app,
                    "Remove the application data?")), "the confirmation dialog must answer");
            showDialogAndCloseIt(() -> {
                UI.showInformationDialog(app, "information");
                return null;
            });
            showDialogAndCloseIt(() -> {
                UI.showWarningDialog(app, "warning");
                return null;
            });
            return null;
        });
    }

    /**
     * Fails with a readable message when the sample configuration is not where the test expects it.
     */
    private static void assertSampleExists() {
        assertTrue(Files.exists(SAMPLE), "sample configuration is missing: " + SAMPLE.toAbsolutePath());
    }

    /**
     * Opens one pane the way its menu item does and closes it again.
     *
     * @param opener the call the menu item performs, e.g. {@code () -> UI.showEncoder(app)}
     * @return {@code true} if a window was created and shown
     */
    private static boolean paneOpens(Runnable opener) {
        final List<Window> before = List.copyOf(Window.getWindows());
        opener.run();
        final List<Stage> opened = Window.getWindows().stream()
                .filter(w -> !before.contains(w))
                .filter(Stage.class::isInstance).map(Stage.class::cast).toList();
        final boolean showing = !opened.isEmpty() && opened.stream().allMatch(Stage::isShowing);
        opened.forEach(Stage::close);
        return showing;
    }

    /**
     * Runs a blocking framework dialog ({@code showAndWait}) and closes it from inside its own
     * event loop.
     *
     * @param dialog the blocking call, e.g. {@code () -> UI.showConfirmationDialog(app, "…")}
     * @param <T> the dialog's result type
     * @return the dialog's result, {@code null} for the void dialogs
     */
    private static <T> T showDialogAndCloseIt(Callable<T> dialog) {
        final List<Window> before = List.copyOf(Window.getWindows());
        // Posted before the dialog is shown: it is queued, so it runs inside the dialog's own
        // nested event loop (once the dialog window exists) and closes every new window. The
        // windows are copied first: hiding one removes it from the live list.
        Platform.runLater(() -> List.copyOf(Window.getWindows()).stream()
                .filter(w -> !before.contains(w))
                .forEach(Window::hide));
        try {
            return dialog.call();
        } catch (Exception e) {
            throw new AssertionError("the dialog must open: " + e, e);
        }
    }

    /**
     * Collects a navigation tree depth-first, root included.
     *
     * @param root root item
     * @return all items
     */
    private static List<TreeItem<FormBuilder.NavNode>> flatten(TreeItem<FormBuilder.NavNode> root) {
        final List<TreeItem<FormBuilder.NavNode>> items = new ArrayList<>();
        items.add(root);
        for (final TreeItem<FormBuilder.NavNode> child : root.getChildren()) {
            items.addAll(flatten(child));
        }
        return items;
    }

    /**
     * Collects the texts of the rendered parameter labels; the metadata-driven forms mark them with
     * the {@code param-label} style class.
     *
     * @param node node to inspect
     * @param out collected labels
     */
    private static void collectLabels(Node node, List<String> out) {
        if (node instanceof Label label && label.getStyleClass().contains("param-label")) {
            out.add(label.getText());
        }
        for (final Node child : children(node)) {
            collectLabels(child, out);
        }
    }

    /**
     * Number of nodes of a type below (and including) a node.
     *
     * @param node node to inspect
     * @param type node type
     * @return number of matching nodes
     */
    private static int count(Node node, Class<?> type) {
        int n = type.isInstance(node) ? 1 : 0;
        for (final Node child : children(node)) {
            n += count(child, type);
        }
        return n;
    }

    /**
     * Number of the drag&amp;drop buttons of a mapping table.
     *
     * @param node node to inspect
     * @return number of elements carrying the {@code dnd-button} style class
     */
    private static int countDnd(Node node) {
        int n = node.getStyleClass().contains("dnd-button") ? 1 : 0;
        for (final Node child : children(node)) {
            n += countDnd(child);
        }
        return n;
    }

    /**
     * Children of a node to walk.
     *
     * <p>A form is built before it is shown, therefore its containers have no skin yet and keep
     * their content outside {@link Parent#getChildrenUnmodifiable()}. The containers the Studio uses
     * are unwrapped explicitly.</p>
     *
     * @param node node to inspect
     * @return children of the node
     */
    private static List<Node> children(Node node) {
        final List<Node> children = new ArrayList<>();
        if (node instanceof TitledPane pane) {
            children.add(pane.getContent());
        } else if (node instanceof ScrollPane scroll) {
            children.add(scroll.getContent());
        } else if (node instanceof TabPane tabs) {
            for (final Tab tab : tabs.getTabs()) {
                children.add(tab.getContent());
            }
        } else if (node instanceof SplitPane split) {
            children.addAll(split.getItems());
        } else if (node instanceof ToolBar bar) {
            children.addAll(bar.getItems());
        } else if (node instanceof Parent parent) {
            children.addAll(parent.getChildrenUnmodifiable());
        }
        children.removeIf(Objects::isNull);
        return children;
    }

    /**
     * Runs an action on the JavaFX application thread.
     *
     * @param action action to run
     */
    private static void run(Runnable action) {
        call(() -> {
            action.run();
            return null;
        });
    }

    /**
     * Runs a callable on the JavaFX application thread and returns its result.
     *
     * <p>A blocked JavaFX thread - a modal dialog the test opened by accident, for example -
     * would otherwise make every following test wait for its timeout, which looks like a hanging
     * test run. The wait is therefore short and the stack of the JavaFX thread is written to the
     * log, so the cause is visible.</p>
     *
     * @param <T> result type
     * @param callable callable to run
     * @return the result of the callable
     * @throws RuntimeException when the callable fails or the JavaFX thread does not answer
     */
    private static <T> T call(Callable<T> callable) {
        final FutureTask<T> task = new FutureTask<>(callable);
        Platform.runLater(task);
        try {
            return task.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            System.err.println("The JavaFX thread did not answer - its stack follows:");
            for (final StackTraceElement element : javafxThread()) {
                System.err.println("    at " + element);
            }
            throw new IllegalStateException("JavaFX call failed: " + e, e);
        } catch (Exception e) {
            throw new IllegalStateException("JavaFX call failed: " + e, e);
        }
    }

    /**
     * The stack of the JavaFX application thread, empty if it cannot be found.
     *
     * @return the stack trace of the JavaFX thread
     */
    private static StackTraceElement[] javafxThread() {
        for (final Thread thread : Thread.getAllStackTraces().keySet()) {
            if ("JavaFX Application Thread".equals(thread.getName())) {
                return thread.getStackTrace();
            }
        }
        return new StackTraceElement[0];
    }
}
