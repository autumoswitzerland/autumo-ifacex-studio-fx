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
import ch.autumo.ifacex.studio.core.model.IpcConfig;
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
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldListCell;
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
            return message("Select an element on the left.");
        }
        return switch (node.kind()) {
            case MainController.K_ROOT ->
                    root((IpcConfig) node.payload());
            case MainController.K_SOURCE, MainController.K_DEST ->
                    rwSection((RwConfig) node.payload());
            case MainController.K_ENTITY ->
                    entity((RwConfig) node.payload(), (String) node.payload2());
            case MainController.K_GENERAL ->
                    general((Map<String, String>) node.payload());
            default -> message("No form for: " + node.label());
        };
    }

    /**
     * The root/file node: the comment-header metadata (Title, Version, Author, Notes/Description,
     * Date) that the original Studio edits on the configuration level, plus the job wiring
     * ({@code source_interface}, {@code destination_interfaces}, {@code process_source_entities}).
     * Edits mutate the model and are reflected in the raw views / exports.
     */
    private Node root(IpcConfig cfg) {
        final VBox box = new VBox(10);
        box.setPadding(new Insets(8));
        box.getChildren().add(title("Configuration"));

        // --- file metadata (comment header) ------------------------------------------------
        final VBox metaBox = new VBox(10);
        metaBox.setPadding(new Insets(4));

        final TextField metaTitle = new TextField(cfg.meta().title());
        metaTitle.textProperty().addListener((o, a, b) -> {
            cfg.setMeta(cfg.meta().withTitle(b));
            changed();
        });
        metaBox.getChildren().add(labeled("Title", metaTitle));

        final TextField metaVersion = new TextField(cfg.meta().version());
        metaVersion.textProperty().addListener((o, a, b) -> {
            cfg.setMeta(cfg.meta().withVersion(b));
            changed();
        });
        metaBox.getChildren().add(labeled("Version", metaVersion));

        final TextField metaAuthor = new TextField(cfg.meta().author());
        metaAuthor.textProperty().addListener((o, a, b) -> {
            cfg.setMeta(cfg.meta().withAuthor(b));
            changed();
        });
        metaBox.getChildren().add(labeled("Author", metaAuthor));

        final TextArea metaNotes = new TextArea(stripTrailingNewline(cfg.meta().notes()));
        metaNotes.setPrefRowCount(3);
        metaNotes.textProperty().addListener((o, a, b) -> {
            cfg.setMeta(cfg.meta().withNotes(b));
            changed();
        });
        metaBox.getChildren().add(labeled("Description / Notes", metaNotes));

        final TextField metaDate = new TextField(cfg.meta().date());
        metaDate.setEditable(false);
        metaBox.getChildren().add(labeled("Date (generated)", metaDate));

        box.getChildren().add(collapsed("File metadata (comment header)", metaBox, true));

        // --- job wiring --------------------------------------------------------------------
        final VBox jobBox = new VBox(10);
        jobBox.setPadding(new Insets(4));

        final TextField srcIface = new TextField(cfg.sourceInterface());
        srcIface.textProperty().addListener((o, a, b) -> {
            cfg.setSourceInterface(b);
            changed();
        });
        jobBox.getChildren().add(labeled("Source interface (reader)", srcIface));

        final TextField destIfaces = new TextField(String.join(", ", cfg.destinationInterfaces()));
        destIfaces.textProperty().addListener((o, a, b) -> {
            setList(cfg.destinationInterfaces(), b);
            changed();
        });
        jobBox.getChildren().add(labeled("Destination interfaces (writers, comma separated)", destIfaces));

        final TextField srcEntities = new TextField(String.join(", ", cfg.processSourceEntities()));
        srcEntities.textProperty().addListener((o, a, b) -> {
            setList(cfg.processSourceEntities(), b);
            changed();
        });
        jobBox.getChildren().add(labeled("Process source entities (comma separated)", srcEntities));

        box.getChildren().add(collapsed("Job wiring", jobBox, true));
        return box;
    }

    private static void setList(List<String> target, String commaSeparated) {
        target.clear();
        for (final String part : commaSeparated.split(",")) {
            final String t = part.trim();
            if (!t.isEmpty()) {
                target.add(t);
            }
        }
    }

    private static String stripTrailingNewline(String s) {
        if (s == null) {
            return "";
        }
        return s.endsWith("\n") ? s.substring(0, s.length() - 1) : s;
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
            box.getChildren().add(collapsed("Entity: " + e.getKey(),
                    entityBody(e.getValue(), rw.isReader()), false));
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
        box.getChildren().add(entityBody(ec, rw.isReader()));
        return box;
    }

    /**
     * The body of an entity form. A <em>reader</em> entity defines the source {@code fields}; a
     * <em>writer</em> entity defines the destination mapping ({@code mapping_entity} +
     * {@code mapping_field_NNN}). The mapping editor is shown for every writer entity, so a
     * mapping can be created even when the file does not contain one yet.
     */
    private Node entityBody(EntityConfig ec, boolean reader) {
        final VBox box = new VBox(10);
        box.setPadding(new Insets(4));

        if (reader || !ec.fields().isEmpty()) {
            box.getChildren().add(sourceFieldList(ec));
        }
        if (!reader) {
            box.getChildren().add(destinationMapping(ec));
        }
        if (!ec.settings().isEmpty()) {
            box.getChildren().add(mapGrid(ec.settings()));
        }
        if (box.getChildren().isEmpty()) {
            box.getChildren().add(hint("(empty)"));
        }
        return box;
    }

    /**
     * Destination mapping of a writer entity: the destination entity name plus the field mapping
     * table. The mapping record is created lazily, so merely opening the form does not change the
     * configuration.
     */
    private Node destinationMapping(EntityConfig ec) {
        final String dest = ec.hasMapping() && ec.mapping().destEntity() != null
                ? ec.mapping().destEntity() : "";
        final TextField destEntity = new TextField(dest);
        destEntity.textProperty().addListener((o, a, b) -> {
            if (b.isBlank() && !ec.hasMapping()) {
                return; // nothing to clear yet
            }
            ec.ensureMapping().setDestEntity(b.isBlank() ? null : b);
            changed();
        });

        final VBox box = new VBox(6);
        box.getChildren().add(labeled("Destination entity", destEntity));
        box.getChildren().add(caption("Field mapping (destination field \u2190 formula)"));
        box.getChildren().add(mappingTable(ec));
        return box;
    }

    /**
     * Editor for the source {@code fields} of an entity. Mirrors the original Studio, which edits
     * them as a list (Add / Remove / Edit / move up / move down) instead of a comma separated text
     * field: type a name and press <em>+</em> (or Enter) to append, double-click a row to edit it.
     */
    private Node sourceFieldList(EntityConfig ec) {
        final ObservableList<String> items = FXCollections.observableList(ec.fields());

        final ListView<String> list = new ListView<>(items);
        list.setEditable(true);
        list.setPrefHeight(132);
        list.setCellFactory(TextFieldListCell.forListView());
        list.setOnEditCommit(ev -> {
            final String v = ev.getNewValue() == null ? "" : ev.getNewValue().trim();
            if (v.isEmpty() || items.contains(v)) {
                list.refresh(); // reject empty names and duplicates
            } else {
                items.set(ev.getIndex(), v);
                changed();
            }
        });

        final TextField input = new TextField();
        input.setPromptText("New field name");
        HBox.setHgrow(input, Priority.ALWAYS);
        final Runnable addField = () -> {
            final String v = input.getText() == null ? "" : input.getText().trim();
            if (!v.isEmpty() && !items.contains(v)) {
                items.add(v);
                list.getSelectionModel().select(v);
                list.scrollTo(v);
                input.clear();
                changed();
            }
        };
        input.setOnAction(e -> addField.run());

        final Button add = toolButton("+", "Add field", addField);
        final Button remove = toolButton("\u2212", "Remove selected field", () -> {
            final int i = list.getSelectionModel().getSelectedIndex();
            if (i >= 0) {
                items.remove(i);
                changed();
            }
        });
        final Button up = toolButton("\u2191", "Move up",
                () -> move(items, list, list.getSelectionModel().getSelectedIndex(), -1));
        final Button down = toolButton("\u2193", "Move down",
                () -> move(items, list, list.getSelectionModel().getSelectedIndex(), 1));

        final Label caption = caption("Fields (source)");
        caption.setTooltip(new Tooltip("Source entity fields; order matters for CSV/JSON readers."));

        final HBox inputRow = new HBox(6, input, add);
        final HBox tools = new HBox(6, remove, up, down);
        return new VBox(4, caption, list, inputRow, tools);
    }

    private void move(ObservableList<String> items, ListView<String> list, int index, int delta) {
        final int target = index + delta;
        if (index < 0 || target < 0 || target >= items.size()) {
            return;
        }
        items.add(target, items.remove(index));
        list.getSelectionModel().select(target);
        changed();
    }

    private Button toolButton(String text, String tooltip, Runnable action) {
        final Button b = new Button(text);
        b.setTooltip(new Tooltip(tooltip));
        b.setMinWidth(34);
        b.setOnAction(e -> action.run());
        return b;
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
        final ObservableList<FieldMappingRec> rows = FXCollections.observableArrayList(
                ec.hasMapping() ? new ArrayList<>(ec.mapping().fields()) : new ArrayList<>());
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

        final Button add = toolButton("+", "Add mapping", () -> {
            ec.ensureMapping().fields().add(new FieldMappingRec("", "", false));
            rows.setAll(ec.mapping().fields());
            changed();
        });
        final Button remove = toolButton("\u2212", "Remove selected mapping", () -> {
            final FieldMappingRec sel = table.getSelectionModel().getSelectedItem();
            if (sel != null && ec.hasMapping()) {
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
        if (!ec.hasMapping()) {
            return;
        }
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

    private Label caption(String text) {
        final Label l = new Label(text);
        l.getStyleClass().add("field-label");
        return l;
    }

    private Label hint(String text) {
        final Label l = new Label(text);
        l.getStyleClass().add("field-help");
        return l;
    }

    /**
     * A padded, text-only body used when there is nothing editable to show (start-up placeholder,
     * "select an element" hint). Without the padding such a lone label would stick to the top-left.
     */
    static Node message(String text) {
        final VBox box = new VBox();
        box.setPadding(new Insets(12, 14, 12, 14));
        final Label l = new Label(text);
        l.getStyleClass().add("field-help");
        l.setWrapText(true);
        box.getChildren().add(l);
        return box;
    }

    private Node labeled(String text, Node control) {
        final Label l = new Label(text);
        l.getStyleClass().add("field-label");
        final VBox box = new VBox(2, l, control);
        return box;
    }
}
