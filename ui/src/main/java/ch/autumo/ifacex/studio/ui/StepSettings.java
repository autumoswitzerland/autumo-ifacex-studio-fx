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

import java.io.File;

import ch.autumo.ui.fx.controller.actions.installer.BaseStep;
import ch.autumo.ui.utils.Helper;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;

/**
 * Installer step: general settings.
 *
 * <p>The step asks for the settings the legacy Swing Studio collected in its installer: the root
 * folder of the configuration files ("project root") and whether the Studio plays sounds and its
 * start-up jingle. The folder is chosen with a directory chooser and defaults to
 * {@code <user home>/ifaceXProjects}, like the legacy default
 * ({@code OSHelper.getDefaultProjectRootFolder()}).</p>
 *
 * <p>The values are stored in the application configuration of the user's application data
 * directory ({@link StudioConstants}), which the last installer step writes
 * ({@link ch.autumo.ui.fx.BaseApp#completed(ProcessException)}). Until then
 * {@link ch.autumo.ui.fx.BaseApp#setConfigurationParameter(String, String)} keeps them in memory,
 * so going back and forth in the installer shows the chosen values again.</p>
 *
 * <p>The step is listed in {@code cfg/installer-steps.cfg} and instantiated by the framework, so
 * it needs a public no-argument constructor.</p>
 */
public class StepSettings extends BaseStep {

    /**
     * Creates the step; the framework calls {@link #execute()} when the step is shown.
     */
    public StepSettings() {
    }

    @Override
    public void execute() {

        final BorderPane pane = controller.getBody();
        // Individual insets, depending on the element that is placed!
        BorderPane.setMargin(pane, new Insets(0, 10, 0, 10));

        final GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(14);
        grid.setPadding(new Insets(18, 0, 0, 0));

        final String rootFolder = readProjectRootFolder();

        final TextField rootField = new TextField(rootFolder);
        rootField.setEditable(false);
        rootField.setMinWidth(420);
        rootField.setPrefWidth(420);
        GridPane.setHgrow(rootField, Priority.ALWAYS);

        final Button browse = new Button("...");
        browse.setOnAction(e -> {
            final DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle(Messages.t("Choose root configuration folder…"));
            final File current = new File(rootField.getText());
            chooser.setInitialDirectory(current.isDirectory()
                    ? current : new File(System.getProperty("user.home")));
            final File selected = chooser.showDialog(getStage());
            if (selected != null)
                rootField.setText(selected.getAbsolutePath());
        });

        final CheckBox playSounds = new CheckBox(Messages.t("Play sounds?"));
        playSounds.setSelected(readBooleanParameter(StudioConstants.PROP_KEY_PLAY_SOUNDS, true));
        final CheckBox playJingle = new CheckBox(Messages.t("Play jingle?"));
        playJingle.setSelected(readBooleanParameter(StudioConstants.PROP_KEY_PLAY_JINGLE, true));

        grid.add(new Label(Messages.t("Configuration root folder:")), 0, 0);
        grid.add(rootField, 1, 0);
        grid.add(browse, 2, 0);
        grid.add(playSounds, 1, 1);
        grid.add(playJingle, 1, 2);
        pane.setCenter(grid);

        final Button next = controller.getFooterButton();
        next.setText(Helper.getTranslatedText(app, "Installer.next"));
        next.setOnAction(e -> {
            storeSettings(rootField.getText(), playSounds.isSelected(), playJingle.isSelected());
            ic.next();
        });

        final BorderPane buttons = controller.getFooterButtons();
        final Button back = new Button(Helper.getTranslatedText(app, "Installer.back"));
        BorderPane.setMargin(back, new Insets(0, 8, 0, 0));
        back.setPrefWidth(100);
        back.setMinWidth(100);
        back.getStyleClass().add("default-info-footer-button");
        buttons.setCenter(back);
        back.setOnAction(e -> ic.back());
    }

    @Override
    public String getTitle() {
        return Messages.t("General Settings");
    }

    /**
     * Reads the configured project root folder; without one the legacy default
     * {@code <user home>/ifaceXProjects} is used.
     *
     * @return the project root folder
     */
    private String readProjectRootFolder() {
        final String configured = app.getConfigurationParameter(StudioConstants.PROP_KEY_PROJECT_ROOT);
        if (configured != null && !configured.trim().isEmpty())
            return configured.trim();
        return System.getProperty("user.home") + File.separator
                + StudioConstants.DEFAULT_PROJECT_ROOT_FOLDER;
    }

    /**
     * Reads a boolean configuration parameter.
     *
     * @param key          parameter key
     * @param defaultValue value used when the parameter is not set
     * @return the configured value
     */
    private boolean readBooleanParameter(String key, boolean defaultValue) {
        final String value = app.getConfigurationParameter(key);
        if (value == null || value.trim().isEmpty())
            return defaultValue;
        return Boolean.parseBoolean(value.trim());
    }

    /**
     * Stores the settings of this step in the application configuration. The project root folder
     * is created if it does not exist yet - it is the folder the Studio works in, and the legacy
     * Studio created it as well.
     *
     * @param rootFolder project root folder
     * @param playSounds play UI sounds
     * @param playJingle play the start-up jingle
     */
    private void storeSettings(String rootFolder, boolean playSounds, boolean playJingle) {
        final File folder = new File(rootFolder);
        folder.mkdirs();
        app.setConfigurationParameter(StudioConstants.PROP_KEY_PROJECT_ROOT, folder.getAbsolutePath());
        app.setConfigurationParameter(StudioConstants.PROP_KEY_PLAY_SOUNDS, String.valueOf(playSounds));
        app.setConfigurationParameter(StudioConstants.PROP_KEY_PLAY_JINGLE, String.valueOf(playJingle));
    }

}
