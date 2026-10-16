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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ch.autumo.ifacex.studio.core.meta.FieldCatalog;
import ch.autumo.ifacex.studio.core.meta.FieldDescriptor;
import ch.autumo.ifacex.studio.core.meta.FieldType;
import ch.autumo.ifacex.studio.core.model.EntityConfig;
import ch.autumo.ifacex.studio.core.model.FieldMappingRec;
import ch.autumo.ifacex.studio.core.model.RwConfig;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.converter.DefaultStringConverter;

/**
 * Builds configuration forms <em>programmatically</em> from the model and the field metadata
 * ({@link FieldCatalog}). The static shell is FXML; the dynamic parts are Java (see
 * docs/10-ui-form-design.md). Every parameter is shown, even when no descriptor exists.
 */
final class FormBuilder {

    /** Selection carried by a navigation tree item. */
    record NavNode(String kind, String label, Object payload, Object payload2) {
        @Override
        public String toString() {
            return label;
        }
    }

    private final Runnable onChange;

    FormBuilder(Runnable onChange) {
        this.onChange = onChange;
    }

    @SuppressWarnings("unchecked")
    Node build(Object selection) {
        if (!(selection instanceof NavNode node)) {
            return hint("Select an element on the left.");
        }
        return switch (node.kind()) {
            case MainController.K_SOURCE, MainController.K_DEST ->
                    rwSection((RwConfig) node.payload());
            case MainController.K_ENTITY ->
                    entity((RwConfig) node.payload(), (String) node.payload2());
            case MainController.K_GENERAL ->
                    general((Map<String, String>) node.payload());
            default -> hint("No form for: " + node.label());
        };
    }

    private Node rwSection(RwConfig rw) {
        final VBox box = new VBox(6);
        box.setPadding(new Insets(8));
        box.getChildren().add(title(rw.name() + (rw.isReader() ? "  (reader)" : "  (writer)")));

        if (!rw.global().isEmpty()) {
            box.getChildren().add(collapsed("Parameters", mapGrid(rw.global()), true));
        }
        if (!rw.wildcard().isEmpty()) {
            box.getChildren().add(collapsed("Defaults (*, all entities)", mapGrid(rw.wildcard()), false));
        }
        for (final Map.Entry<String, EntityConfig> e : rw.entities().entrySet()) {
            box.getChildren().add(collapsed("Entity: " + e.getKey(), entityBody(e.getValue()), false));
        }
        if (box.getChildren().size() == 1) {
            box.getChildren().add(hint("No parameters for this interface."));
        }
        return box;
    }

    private Node entity(RwConfig rw, String entity) {
        final EntityConfig ec = rw.entities().get(entity);
        if (ec == null) {
            return hint("Entity not found: " + entity);
        }
        final VBox box = new VBox(6);
        box.setPadding(new Insets(8));
        box.getChildren().add(title("Entity: " + entity + "   (" + rw.name() + ")"));
        box.getChildren().add(entityBody(ec));
        return box;
    }

    private Node entityBody(EntityConfig ec) {
        final VBox box = new VBox(10);
        box.setPadding(new Insets(4));

        if (!ec.fields().isEmpty() || ec.hasMapping()) {
            final TextField fields = new TextField(String.join(", ", ec.fields()));
            fields.textProperty().addListener((o, a, b) -> {
                ec.fields().clear();
                for (final String p : b.split(",")) {
                    final String t = p.trim();
                    if (!t.isEmpty()) {
                        ec.fields().add(t);
                    }
                }
                changed();
            });
            box.getChildren().add(labeled("Fields (source, comma separated)", fields));
        }

        if (ec.hasMapping()) {
            final TextField destEntity = new TextField(
                    ec.mapping().destEntity() == null ? "" : ec.mapping().destEntity());
            destEntity.textProperty().addListener((o, a, b) -> {
                ec.mapping().setDestEntity(b.isBlank() ? null : b);
                changed();
            });
            box.getChildren().add(labeled("Destination entity", destEntity));
            box.getChildren().add(new Label("Field mapping"));
            box.getChildren().add(mappingTable(ec));
        }

        if (!ec.settings().isEmpty()) {
            box.getChildren().add(mapGrid(ec.settings()));
        }
        if (box.getChildren().isEmpty()) {
            box.getChildren().add(hint("(empty)"));
        }
        return box;
    }

    private Node general(Map<String, String> general) {
        final VBox box = new VBox(6);
        box.setPadding(new Insets(8));
        box.getChildren().add(title("General / cross-cutting"));
        if (general.isEmpty()) {
            box.getChildren().add(hint("(none)"));
        } else {
            box.getChildren().add(mapGrid(general));
        }
        return box;
    }

    private GridPane mapGrid(Map<String, String> map) {
        final GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(6);
        grid.setPadding(new Insets(6, 6, 6, 6));

        int row = 0;
        for (final Map.Entry<String, String> e : new TreeMap<>(map).entrySet()) {
            final String key = e.getKey();
            final FieldDescriptor d = FieldCatalog.descriptor(key);
            final Label label = new Label(d.label());
            label.getStyleClass().add("field-label");
            if (!d.help().isEmpty()) {
                label.setTooltip(new Tooltip(d.help()));
            }
            final Node control = control(map, key, d);
            GridPane.setHgrow(control, Priority.ALWAYS);
            grid.add(label, 0, row);
            grid.add(control, 1, row);
            row++;
        }
        return grid;
    }

    private Node control(Map<String, String> map, String key, FieldDescriptor d) {
        final String value = map.getOrDefault(key, "");
        switch (d.type()) {
            case BOOLEAN: {
                final ComboBox<String> cb = new ComboBox<>(
                        FXCollections.observableArrayList("", "yes", "no"));
                cb.setValue(value);
                cb.setMaxWidth(Double.MAX_VALUE);
                cb.valueProperty().addListener((o, a, b) -> {
                    map.put(key, b == null ? "" : b);
                    changed();
                });
                return cb;
            }
            case SELECT: {
                final ComboBox<String> cb = new ComboBox<>(
                        FXCollections.observableArrayList(d.options()));
                cb.setEditable(true);
                cb.setValue(value);
                cb.setMaxWidth(Double.MAX_VALUE);
                cb.valueProperty().addListener((o, a, b) -> {
                    map.put(key, b == null ? "" : b);
                    changed();
                });
                return cb;
            }
            case INTEGER: {
                final TextField tf = new TextField(value);
                tf.setTextFormatter(new javafx.scene.control.TextFormatter<>(
                        s -> s.getControlNewText().matches("-?\\d*") ? s : null));
                tf.textProperty().addListener((o, a, b) -> {
                    map.put(key, b);
                    changed();
                });
                return tf;
            }
            case SECRET: {
                final PasswordField pf = new PasswordField();
                pf.setText(value);
                pf.textProperty().addListener((o, a, b) -> {
                    map.put(key, b);
                    changed();
                });
                return pf;
            }
            case TEXT_AREA: {
                final TextArea ta = new TextArea(value);
                ta.setPrefRowCount(2);
                ta.textProperty().addListener((o, a, b) -> {
                    map.put(key, b);
                    changed();
                });
                return ta;
            }
            default: {
                final TextField tf = new TextField(value);
                tf.textProperty().addListener((o, a, b) -> {
                    map.put(key, b);
                    changed();
                });
                return tf;
            }
        }
    }

    private Node mappingTable(EntityConfig ec) {
        final ObservableList<FieldMappingRec> rows =
                FXCollections.observableArrayList(new ArrayList<>(ec.mapping().fields()));
        final TableView<FieldMappingRec> table = new TableView<>(rows);
        table.setEditable(true);
        table.setPrefHeight(180);

        final TableColumn<FieldMappingRec, String> colField = new TableColumn<>("Destination field");
        colField.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().destField()));
        colField.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        colField.setOnEditCommit(ev -> replace(ec, rows, ev.getRowValue(),
                new FieldMappingRec(ev.getNewValue(), ev.getRowValue().formula(), ev.getRowValue().insert())));
        colField.setPrefWidth(240);

        final TableColumn<FieldMappingRec, String> colFormula = new TableColumn<>("Formula");
        colFormula.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().formula()));
        colFormula.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        colFormula.setOnEditCommit(ev -> replace(ec, rows, ev.getRowValue(),
                new FieldMappingRec(ev.getRowValue().destField(), ev.getNewValue(), ev.getRowValue().insert())));
        colFormula.setPrefWidth(320);

        final TableColumn<FieldMappingRec, Boolean> colInsert = new TableColumn<>("Insert");
        colInsert.setCellValueFactory(c -> new SimpleBooleanProperty(c.getValue().insert()));
        colInsert.setCellFactory(CheckBoxTableCell.forTableColumn(colInsert));
        colInsert.setOnEditCommit(ev -> replace(ec, rows, ev.getRowValue(),
                new FieldMappingRec(ev.getRowValue().destField(), ev.getRowValue().formula(), ev.getNewValue())));
        colInsert.setPrefWidth(70);

        table.getColumns().addAll(List.of(colField, colFormula, colInsert));

        final Button add = new Button("Add");
        add.setOnAction(e -> {
            final FieldMappingRec rec = new FieldMappingRec("", "", false);
            ec.mapping().fields().add(rec);
            rows.setAll(ec.mapping().fields());
            changed();
        });
        final Button remove = new Button("Remove");
        remove.setOnAction(e -> {
            final FieldMappingRec sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                ec.mapping().fields().remove(sel);
                rows.setAll(ec.mapping().fields());
                changed();
            }
        });
        final HBox buttons = new HBox(6, add, remove);

        final VBox box = new VBox(4, table, buttons);
        VBox.setVgrow(table, Priority.ALWAYS);
        return box;
    }

    private void replace(EntityConfig ec, ObservableList<FieldMappingRec> rows,
            FieldMappingRec oldRec, FieldMappingRec newRec) {
        final int i = ec.mapping().fields().indexOf(oldRec);
        if (i >= 0) {
            ec.mapping().fields().set(i, newRec);
            rows.set(i, newRec);
            changed();
        }
    }

    private void changed() {
        if (onChange != null) {
            onChange.run();
        }
    }

    private TitledPane collapsed(String title, Node content, boolean expanded) {
        final TitledPane p = new TitledPane(title, content);
        p.setExpanded(expanded);
        return p;
    }

    private Label title(String text) {
        final Label l = new Label(text);
        l.getStyleClass().add("section-title");
        return l;
    }

    private Label hint(String text) {
        final Label l = new Label(text);
        l.getStyleClass().add("field-help");
        return l;
    }

    private Node labeled(String text, Node control) {
        final Label l = new Label(text);
        l.getStyleClass().add("field-label");
        final VBox box = new VBox(2, l, control);
        return box;
    }
}
