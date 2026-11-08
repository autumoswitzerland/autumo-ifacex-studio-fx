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

import java.util.ArrayList;
import java.util.List;

import ch.autumo.ifacex.studio.core.meta.InterfaceTypes;
import ch.autumo.ifacex.studio.ui.StudioHelper;
import ch.autumo.ui.fx.controller.DefaultInfoController;
import ch.autumo.ui.fx.controller.actions.BaseAction;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Action that builds the "New configuration" wizard inside a framework pane
 * ({@link ch.autumo.ui.utils.UI#showPane}). The user picks one reader and any
 * number of writers by their display name (e.g. "REST Reader", not {@code rest_in});
 * the result is available through {@link #getResult()} after the pane closes.
 */
public class NewConfigAction extends BaseAction {

    /** Result of a successful wizard run. */
    public static final class Result {
        public final String reader;
        public final List<String> writers;
        public Result(String reader, List<String> writers) {
            this.reader = reader;
            this.writers = writers;
        }
    }

    private Result result = null;

    public NewConfigAction() {
        super();
    }

    public Result getResult() {
        return result;
    }

    @Override
    public void execute() {
        final DefaultInfoController ctrl = getController();
        if (ctrl == null) {
            return;
        }

        // --- Reader (single selection) ---
        final ListView<String> readerList = new ListView<>();
        readerList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        final List<String> readerPrefixes = InterfaceTypes.readerPrefixes();
        for (final String prefix : readerPrefixes) {
            readerList.getItems().add(InterfaceTypes.display(prefix) + " Reader");
        }
        if (!readerList.getItems().isEmpty()) {
            readerList.getSelectionModel().select(0);
        }
        readerList.setPrefHeight(160);
        readerList.setPrefWidth(190);

        // --- Writers: available (left) and selected (right) ---
        final ListView<String> availableWriters = new ListView<>();
        availableWriters.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        final List<String> writerPrefixes = InterfaceTypes.writerPrefixes();
        for (final String prefix : writerPrefixes) {
            availableWriters.getItems().add(InterfaceTypes.display(prefix) + " Writer");
        }
        availableWriters.setPrefHeight(160);
        availableWriters.setPrefWidth(190);

        final ListView<String> selectedWriters = new ListView<>();
        selectedWriters.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        selectedWriters.setPrefHeight(160);
        selectedWriters.setPrefWidth(190);

        final Button toSelected = new Button("›");
        final Button toAvailable = new Button("‹");
        toSelected.setOnAction(_ -> {
            final List<String> chosen = new ArrayList<>(availableWriters.getSelectionModel().getSelectedItems());
            for (final String item : chosen) {
                if (!selectedWriters.getItems().contains(item)) {
                    selectedWriters.getItems().add(item);
                }
                availableWriters.getItems().remove(item);
            }
            availableWriters.getSelectionModel().clearSelection();
        });
        toAvailable.setOnAction(_ -> {
            final List<String> chosen = new ArrayList<>(selectedWriters.getSelectionModel().getSelectedItems());
            for (final String item : chosen) {
                if (!availableWriters.getItems().contains(item)) {
                    availableWriters.getItems().add(item);
                }
                selectedWriters.getItems().remove(item);
            }
            selectedWriters.getSelectionModel().clearSelection();
        });

        final DefaultInfoController controller = (DefaultInfoController) ctrl;
        
        final Button ok = new Button(StudioHelper.t("Create"));
        ok.setOnAction(_ -> {
            final int ri = readerList.getSelectionModel().getSelectedIndex();
            if (ri < 0) {
                return;
            }
            final List<String> writerPrefixList = new ArrayList<>();
            for (final String item : selectedWriters.getItems()) {
                for (final String prefix : writerPrefixes) {
                    if ((InterfaceTypes.display(prefix) + " Writer").equals(item)) {
                        writerPrefixList.add(prefix.equals("code") || prefix.equals("exec")
                                ? prefix : prefix + "_out");
                    }
                }
            }
            result = new Result(readerPrefixes.get(ri) + "_in", writerPrefixList);
            controller.close();
        });
        controller.getFooterButton().setOnAction(_ -> ctrl.close());
        
        BorderPane.setMargin(ok, new Insets(0, 8, 0, 0));
        ok.setPrefWidth(100);
        ok.setMinWidth(100);
        ok.getStyleClass().add("default-info-footer-button");
        controller.getFooterButtons().setCenter(ok);
        
        controller.getFooterButton().setText(StudioHelper.t("Cancel"));

        final VBox availableBox = new VBox(5,
    	    new Label(StudioHelper.t("Available writers")),
    	    availableWriters
    	);

    	final VBox selectedBox = new VBox(5,
    	    new Label(StudioHelper.t("Selected writers")),
    	    selectedWriters
    	);

    	HBox.setHgrow(availableBox, Priority.ALWAYS);
    	HBox.setHgrow(selectedBox, Priority.ALWAYS);
    	VBox.setVgrow(availableBox, Priority.ALWAYS);
    	VBox.setVgrow(selectedBox, Priority.ALWAYS);
    	
    	VBox.setVgrow(availableWriters, Priority.ALWAYS);
    	VBox.setVgrow(selectedWriters, Priority.ALWAYS);
    	
    	final VBox buttonBox = new VBox(8, toSelected, toAvailable);
    	buttonBox.setAlignment(Pos.CENTER);
    	buttonBox.setTranslateY(10);
    	
    	final HBox writerLists = new HBox(8,
    	    availableBox,
    	    buttonBox,
    	    selectedBox
    	);
    	writerLists.setMaxWidth(Double.MAX_VALUE);
    	VBox.setVgrow(writerLists, Priority.ALWAYS);
        
        final VBox content = new VBox(10,
                new Label(StudioHelper.t("Reader")),
                readerList,
                writerLists
        );
    	VBox.setVgrow(content, Priority.ALWAYS);
    	VBox.setVgrow(ctrl.getBody(), Priority.ALWAYS);
        
        content.setPadding(new Insets(16));
        ctrl.getBody().setCenter(content);
    }
}
