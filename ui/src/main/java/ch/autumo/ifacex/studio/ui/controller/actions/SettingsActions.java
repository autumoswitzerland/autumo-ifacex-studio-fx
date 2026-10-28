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
package ch.autumo.ifacex.studio.ui.controller.actions;

import static ch.autumo.ifacex.studio.ui.StudioHelper.format;
import static ch.autumo.ifacex.studio.ui.StudioHelper.t;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.commons.Constants;
import ch.autumo.ifacex.studio.core.LicIfaceXApp100;
import ch.autumo.ifacex.studio.core.StudioConstants;
import ch.autumo.ifacex.studio.ui.StudioApp;
import ch.autumo.ui.fx.controller.actions.BaseAction;
import ch.autumo.ui.utils.UI;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;


/**
 * Settings action.
 */
public class SettingsActions extends BaseAction {

	/** Logger. */
	private static final Logger LOG = LoggerFactory.getLogger(SettingsActions.class.getName());
	
    /** Timeout the watchdog uses when the configuration carries none (seconds). */
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    private final TextField projectRootField = new TextField();
    private final TextField ifacexFolderField = new TextField();
    private final TextField seedField = new TextField();
    private final TextField timeoutField = new TextField();
    private final CheckBox playSoundsCheck = new CheckBox();
    private final CheckBox playJingleCheck = new CheckBox();

    
	/**
	 * Default constructor.
	 */
	public SettingsActions() {
	}
    
	@Override
	public void execute() {

		final BorderPane pane = controller.getBody();
		
        projectRootField.setEditable(false);
        projectRootField.setPrefWidth(420);
        projectRootField.setMaxWidth(Double.MAX_VALUE);

        final Button projectRootButton = new Button("...");
        projectRootButton.setPrefWidth(32);
        projectRootButton.setOnAction(_ -> chooseFolder(projectRootField, t("Project Root Folder")));

        ifacexFolderField.setEditable(false);
        ifacexFolderField.setPrefWidth(420);
        ifacexFolderField.setMaxWidth(Double.MAX_VALUE);

        final Button ifacexFolderButton = new Button("...");
        ifacexFolderButton.setPrefWidth(32);
        ifacexFolderButton.setOnAction(_ -> chooseIfcXFolder());

        seedField.setPrefWidth(420);
        seedField.setMaxWidth(Double.MAX_VALUE);
        seedField.setTooltip(new Tooltip(
                t("Seed (32 hex characters) - must match the secret_key_seed of the ifaceX instance.")));
        // Only hex digits, max 32 characters. newV may be null when the text is cleared.
        seedField.textProperty().addListener((_, _, newV) -> {
            if (newV == null) {
                return;
            }
            String filtered = newV.replaceAll("[^0-9a-fA-F]", "");
            if (filtered.length() > 32) {
                filtered = filtered.substring(0, 32);
            }
            if (!filtered.equals(newV)) {
                seedField.setText(filtered);
            }
        });

        timeoutField.setPrefWidth(420);
        timeoutField.setMaxWidth(Double.MAX_VALUE);
        timeoutField.setTooltip(new Tooltip(
                t("Seconds the external ifaceX process may run before the Studio stops it.")));
        // Seconds only.
        timeoutField.textProperty().addListener((_, _, newV) -> {
            if (newV == null) {
                return;
            }
            final String filtered = newV.replaceAll("[^0-9]", "");
            if (filtered.length() > 6) {
                timeoutField.setText(filtered.substring(0, 6));
            } else if (!filtered.equals(newV)) {
                timeoutField.setText(filtered);
            }
        });

        playSoundsCheck.setText(t("Play sounds"));
        playJingleCheck.setText(t("Play jingle"));

        final GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(24, 24, 16, 24));
        // The dialog may be widened: the value column takes the extra width.
        grid.setMaxWidth(Double.MAX_VALUE);
        final ColumnConstraints labelColumn = new ColumnConstraints();
        final ColumnConstraints valueColumn = new ColumnConstraints();
        valueColumn.setHgrow(Priority.ALWAYS);
        valueColumn.setFillWidth(true);
        grid.getColumnConstraints().addAll(labelColumn, valueColumn);

        grid.add(new Label(t("Project Root Folder")), 0, 0);
        grid.add(rootBox(projectRootField, projectRootButton), 1, 0);
        grid.add(new Label(t("ifaceX Test Instance")), 0, 1);
        grid.add(rootBox(ifacexFolderField, ifacexFolderButton), 1, 1);
        grid.add(new Label(t("Seed (for encoder)")), 0, 2);
        grid.add(seedField, 1, 2);
        grid.add(new Label(t("Process timeout (s)")), 0, 3);
        grid.add(timeoutField, 1, 3);
        grid.add(playSoundsCheck, 1, 4);
        grid.add(playJingleCheck, 1, 5);

        GridPane.setHgrow(projectRootField, Priority.ALWAYS);
        GridPane.setHgrow(ifacexFolderField, Priority.ALWAYS);
        GridPane.setHgrow(seedField, Priority.ALWAYS);
        GridPane.setHgrow(timeoutField, Priority.ALWAYS);

        pane.setCenter(grid);
        
        
        final Button doneButton = controller.getFooterButton();
        doneButton.setText(t("Done"));
        doneButton.setPrefWidth(100);
        doneButton.setOnAction(_ -> saveAndClose());
        
        loadValues();
	}

    /** Loads the current values from the application configuration. */
    private void loadValues() {
        projectRootField.setText(app.getConfigurationParameter(StudioConstants.PROP_KEY_PROJECT_ROOT));
        ifacexFolderField.setText(app.getConfigurationParameter(StudioConstants.PROP_KEY_IFACEX_FOLDER));
        seedField.setText(app.getConfigurationParameter(Constants.SEC_KEY_SEED));
        final String timeout = app.getConfigurationParameter(StudioConstants.PROP_KEY_TIMEOUT);
        timeoutField.setText(timeout == null || timeout.isBlank()
                ? String.valueOf(DEFAULT_TIMEOUT_SECONDS) : timeout.trim());
        playSoundsCheck.setSelected(booleanParameter(StudioConstants.PROP_KEY_PLAY_SOUNDS, true));
        playJingleCheck.setSelected(booleanParameter(StudioConstants.PROP_KEY_PLAY_JINGLE, false));
    }
    
    /** Saves the values and closes the dialog. */
    private void saveAndClose() {
    	
    	final String projectRoot = projectRootField.getText() == null ? "" : projectRootField.getText().trim();
        app.setConfigurationParameter(StudioConstants.PROP_KEY_PROJECT_ROOT, projectRoot);
        
        final String seed = seedField.getText() == null ? "" : seedField.getText().trim();
        if (!seed.isEmpty() && seed.length() != 32) {
            UI.showFineWarningDialog(app,
                    format("The seed must be 32 hex characters (it is {0} now).", seed.length()));
            return;
        }
        
        final LicIfaceXApp100 ifacexApp = ((StudioApp) app).getIfacexApp();
        
        app.setConfigurationParameter(Constants.SEC_KEY_SEED, seed);
        ifacexApp.setSeed(seed);
        
        final String ifacexFolder = ifacexFolderField.getText() == null ? "" : ifacexFolderField.getText().trim();
        if (!ifacexFolder.isEmpty()) {
            app.setConfigurationParameter(StudioConstants.PROP_KEY_IFACEX_FOLDER, ifacexFolder.trim());
            try {
            	ifacexApp.initializeBaseConfig(ifacexFolder.trim());
			} catch (Exception e) {
				LOG.error("Couldn't initialize ifaceX test instance!", e);
				UI.showError(app, e);
			}
        }
        
        final String timeout = timeoutField.getText() == null ? "" : timeoutField.getText().trim();
        if (timeout.isEmpty() || Integer.parseInt(timeout) <= 0) {
            UI.showFineWarningDialog(app,
                    t("The process timeout must be a number of seconds greater than zero."));
            return;
        }
        app.setConfigurationParameter(StudioConstants.PROP_KEY_TIMEOUT, timeout);

        app.setConfigurationParameter(StudioConstants.PROP_KEY_PLAY_SOUNDS,
                String.valueOf(playSoundsCheck.isSelected()));
        
        app.setConfigurationParameter(StudioConstants.PROP_KEY_PLAY_JINGLE,
                String.valueOf(playJingleCheck.isSelected()));

        getStage().hide();
    }
    
    private static HBox rootBox(TextField field, Button button) {
        final HBox box = new HBox(8, field, button);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(field, Priority.ALWAYS);
        return box;
    }
    
    /** Chooses a folder for the given text field. */
    private void chooseFolder(TextField field, String title) {
        final DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(title);
        final String current = field.getText();
        if (current != null && !current.isBlank()) {
            chooser.setInitialDirectory(new File(current));
        }
        final File selected = chooser.showDialog(getStage());
        if (selected != null) {
            field.setText(selected.getAbsolutePath());
        }
    }

    /** Chooses the ifaceX test instance folder and reads its seed automatically. */
    private void chooseIfcXFolder() {
        final DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(t("ifaceX Instance Directory"));
        final String current = ifacexFolderField.getText();
        if (current != null && !current.isBlank()) {
            chooser.setInitialDirectory(new File(current));
        }
        final File selected = chooser.showDialog(getStage());
        if (selected == null) {
            return;
        }

        final String folder = selected.getAbsolutePath();
        final String instanceSeed = readBaseConfigSeed(folder);
        if (instanceSeed != null) {
            seedField.setText(instanceSeed);
        } else {
            UI.showFineWarningDialog(app,
                    format("No secret_key_seed found in {0}/cfg/ifacex.cfg.", folder));
        }
        ifacexFolderField.setText(folder);
    }
    
    /** Reads the {@code secret_key_seed} from {@code <folder>/cfg/ifacex.cfg}. */
    private static String readBaseConfigSeed(String folder) {
        final File file = new File(folder, "cfg" + java.io.File.separator + "ifacex.cfg");
        if (!file.exists()) {
            return null;
        }
        final Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(file)) {
            props.load(fis);
        } catch (IOException e) {
            return null;
        }
        return props.getProperty("secret_key_seed");
    }

    /** Reads a boolean parameter of the application configuration. */
    private boolean booleanParameter(String key, boolean defaultValue) {
        final String value = app.getConfigurationParameter(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }
    
}
