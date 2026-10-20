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

import static ch.autumo.ifacex.studio.ui.StudioHelper.format;
import static ch.autumo.ifacex.studio.ui.StudioHelper.t;

import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ch.autumo.ifacex.studio.core.meta.FieldCatalog;
import ch.autumo.ifacex.studio.core.meta.FieldDescriptor;
import ch.autumo.ifacex.studio.core.meta.FieldType;
import ch.autumo.ifacex.studio.core.meta.InterfaceTypes;
import ch.autumo.ifacex.studio.core.model.EntityConfig;
import ch.autumo.ifacex.studio.core.model.FieldMappingRec;
import ch.autumo.ifacex.studio.core.model.IpcConfig;
import ch.autumo.ifacex.studio.core.model.MappingFormula;
import ch.autumo.ifacex.studio.core.model.RwConfig;
import ch.autumo.ifacex.studio.core.model.WriterMapping;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.control.cell.TextFieldTableCell;
import ch.autumo.ui.fx.control.Icons;
import javafx.scene.text.Text;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
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
        /**
         * The navigation tree shows the label only.
         *
         * @return the display label
         */
        @Override
        public String toString() {
            return label;
        }
    }

    /** Data format used for dragging a source field into the mapping formula box. */
    private static final DataFormat FIELD_FORMAT = new DataFormat("application/x-ifacex-field");

    private final Runnable onChange;
    private final Runnable onStructureChange;
    private IpcConfig cfg;
    private final ch.autumo.ui.fx.BaseApp app;

    FormBuilder(ch.autumo.ui.fx.BaseApp app, Runnable onChange) {
        this(app, onChange, null);
    }

    FormBuilder(ch.autumo.ui.fx.BaseApp app, Runnable onChange, Runnable onStructureChange) {
        this.app = app;
        this.onChange = onChange;
        this.onStructureChange = onStructureChange;
    }

    /** The current configuration (needed to reach the shared source fields from a writer). */
    void setConfig(IpcConfig cfg) {
        this.cfg = cfg;
    }

    @SuppressWarnings("unchecked")
    Node build(Object selection) {
        if (!(selection instanceof NavNode node)) {
            return message(t("Select an element on the left."));
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
            default -> message(format("No form for: {0}", node.label()));
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
        box.getChildren().add(rootHeader());

        // --- file metadata (comment header) ------------------------------------------------
        final VBox metaBox = new VBox(10);
        metaBox.setPadding(new Insets(4));

        final TextField metaTitle = new TextField(cfg.meta().title());
        metaTitle.textProperty().addListener((_, _, b) -> {
            cfg.setMeta(cfg.meta().withTitle(b));
            changed();
        });
        metaBox.getChildren().add(labeled(t("Title"), metaTitle));

        final TextField metaVersion = new TextField(cfg.meta().version());
        metaVersion.textProperty().addListener((_, _, b) -> {
            cfg.setMeta(cfg.meta().withVersion(b));
            changed();
        });
        metaBox.getChildren().add(labeled(t("Version"), metaVersion));

        final TextField metaAuthor = new TextField(cfg.meta().author());
        metaAuthor.textProperty().addListener((_, _, b) -> {
            cfg.setMeta(cfg.meta().withAuthor(b));
            changed();
        });
        metaBox.getChildren().add(labeled(t("Author"), metaAuthor));

        final TextArea metaNotes = new TextArea(stripTrailingNewline(cfg.meta().notes()));
        metaNotes.setPrefRowCount(3);
        metaNotes.textProperty().addListener((_, _, b) -> {
            cfg.setMeta(cfg.meta().withNotes(b));
            changed();
        });
        metaBox.getChildren().add(labeled(t("Description / Notes"), metaNotes));

        final TextField metaDate = new TextField(cfg.meta().date());
        metaDate.setEditable(false);
        metaBox.getChildren().add(labeled(t("Date (generated)"), metaDate));

        box.getChildren().add(collapsed(t("File metadata (comment header)"), metaBox, true));

        // --- job wiring --------------------------------------------------------------------
        final VBox jobBox = new VBox(10);
        jobBox.setPadding(new Insets(4));

        // Source interface = the reader. Picking one creates the reader section (new configuration).
        final java.util.List<String> readerSuggestions = new java.util.ArrayList<>(InterfaceTypes.readerNames());
        readerSuggestions.add("<other>");
        final ComboBox<String> srcIface = new ComboBox<>(
                FXCollections.observableArrayList(readerSuggestions));
        srcIface.setEditable(true);
        srcIface.setMaxWidth(Double.MAX_VALUE);
        srcIface.setValue(cfg.sourceInterface());
        srcIface.valueProperty().addListener((_, _, b) -> {
            if (b == null || b.isBlank()) {
                return;
            }
            cfg.setSourceInterface(b.trim());
            cfg.ensureReader(b.trim());
            structureChanged();
        });
        final Label srcLabel = new Label(t("Source interface (reader)"));
        srcLabel.getStyleClass().add("field-label");
        srcLabel.setTooltip(new Tooltip(t("The single reader of the job; picking one creates its section.")));
        jobBox.getChildren().add(new VBox(2, srcLabel, srcIface));

        // Destination interfaces = the writers.
        final Label destLabel = new Label(t("Destination interfaces (writers)"));
        destLabel.getStyleClass().add("field-label");
        destLabel.setTooltip(new Tooltip(t("The writers of the job. Add a writer to create its section.")));
        final java.util.List<String> writerSuggestions = new java.util.ArrayList<>(InterfaceTypes.writerNames());
        writerSuggestions.add("<other>");
        jobBox.getChildren().add(new VBox(2, destLabel, stringListEditor(
                caption(""), t("writer (e.g. csv_out)"),
                FXCollections.observableList(cfg.destinationInterfaces()),
                this::syncWritersFromList, writerSuggestions)));

        box.getChildren().add(collapsed(t("Job wiring"), jobBox, true));

        // --- source entities (global; the legacy BASE tab list) ----------------------------
        box.getChildren().add(collapsed(t("Source entities"), sourceEntitiesEditor(cfg), true));
        return box;
    }

    /**
     * The global source-entity registry (the legacy BASE tab). Source entities are shared by the
     * reader (which owns their fields) and every writer (which owns their output mapping).
     */
    private Node sourceEntitiesEditor(IpcConfig cfg) {
        final ObservableList<String> items = FXCollections.observableList(cfg.processSourceEntities());
        final ListView<String> list = new ListView<>(items);
        list.setEditable(true);
        list.setPrefHeight(140);
        list.setPlaceholder(hint("No source entities yet."));
        list.setCellFactory(TextFieldListCell.forListView());
        // ListView.EditEvent has no old value; remember it before the cell commits.
        final String[] renaming = {null};
        list.setOnEditStart(ev -> renaming[0] = ev.getIndex() >= 0 && ev.getIndex() < items.size()
                ? items.get(ev.getIndex()) : null);
        list.setOnEditCommit(ev -> {
            final String old = renaming[0];
            renaming[0] = null;
            final String v = ev.getNewValue() == null ? "" : ev.getNewValue().trim();
            if (old == null || v.isEmpty() || v.equals(old)) {
                list.refresh();
                return;
            }
            final List<String> others = cfg.sourceEntityNames();
            others.removeIf(n -> n.equals(old));
            if (others.contains(v)) {
                warn(list, format("A source entity named {0} already exists.", "'" + v + "'"));
                list.refresh();
                return;
            }
            cfg.renameSourceEntity(old, v);
            list.refresh();
            structureChanged();
        });

        final TextField input = new TextField();
        input.setPromptText(t("New source entity name"));
        HBox.setHgrow(input, Priority.ALWAYS);
        final Runnable add = () -> {
            final String v = input.getText() == null ? "" : input.getText().trim();
            if (cfg.addSourceEntity(v)) {
                input.clear();
                list.getSelectionModel().select(v);
                structureChanged();
            }
        };
        input.setOnAction(_ -> add.run());

        final Button addBtn = toolButton("+", t("Add source entity"), add);
        final Button removeBtn = toolButton("\u2212", t("Remove source entity (fields and mappings)"),
                () -> {
                    final String sel = list.getSelectionModel().getSelectedItem();
                    if (sel != null) {
                        cfg.removeSourceEntity(sel);
                        structureChanged();
                    }
                });
        final Button up = toolButton("\u2191", t("Move up"), () -> {
            final String sel = list.getSelectionModel().getSelectedItem();
            if (sel != null) {
                cfg.moveSourceEntity(sel, -1);
                list.refresh();
                list.getSelectionModel().select(sel);
                structureChanged();
            }
        });
        final Button down = toolButton("\u2193", t("Move down"), () -> {
            final String sel = list.getSelectionModel().getSelectedItem();
            if (sel != null) {
                cfg.moveSourceEntity(sel, 1);
                list.refresh();
                list.getSelectionModel().select(sel);
                structureChanged();
            }
        });

        final Label help = hint(t("Source entities are global: their fields are edited on the reader, "
                + "their output mapping on each writer. Double-click to rename."));
        help.setWrapText(true);
        return new VBox(4, list, new HBox(6, input, addBtn),
                new HBox(6, removeBtn, up, down), help);
    }

    /**
     * Ensure a writer section exists for every name in {@code destination_interfaces} and drop the
     * sections that are no longer listed (the legacy selected-writers list is authoritative).
     */
    private void syncWritersFromList() {
        if (cfg == null) {
            return;
        }
        for (final String name : new ArrayList<>(cfg.destinationInterfaces())) {
            if (!name.isBlank()) {
                cfg.ensureWriter(name.trim());
            }
        }
        cfg.writers().keySet().removeIf(w -> !cfg.destinationInterfaces().contains(w));
        structureChanged();
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
        box.getChildren().add(sectionHeader(rw.name(),
                rw.name() + (rw.isReader() ? "  " + t("(reader)") : "  " + t("(writer)"))));

        if (!rw.global().isEmpty()) {
            box.getChildren().add(collapsed(t("Parameters"), mapGrid(rw.global(), rw), true));
        }
        if (!rw.wildcard().isEmpty()) {
            box.getChildren().add(collapsed(t("Defaults (*, all entities)"),
                    mapGrid(rw.wildcard(), rw), false));
        }
        for (final Map.Entry<String, EntityConfig> e : rw.entities().entrySet()) {
            box.getChildren().add(collapsed(format("Entity: {0}", e.getKey()),
                    entityBody(e.getValue(), rw), false));
        }
        if (!rw.isReader()) {
            final Node addMapping = writerAddMapping(rw);
            if (addMapping != null) {
                box.getChildren().add(addMapping);
            }
        }
        if (box.getChildren().size() == 1) {
            box.getChildren().add(hint(t("No parameters for this interface.")));
        }
        return box;
    }

    /**
     * Writer section helper: create the output mapping for a source entity that does not have one
     * yet (the legacy writer tab selects a source entity from its entity list). The mapping itself
     * is created lazily by the entity form, so nothing is written until the user edits it.
     */
    private Node writerAddMapping(RwConfig rw) {
        if (cfg == null) {
            return null;
        }
        final List<String> unmapped = new ArrayList<>(cfg.sourceEntityNames());
        unmapped.removeAll(rw.entities().keySet());
        if (unmapped.isEmpty()) {
            return null;
        }
        final ComboBox<String> combo = new ComboBox<>(
                FXCollections.observableArrayList(unmapped));
        combo.setPromptText(t("source entity"));
        combo.setValue(unmapped.get(0));
        combo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(combo, Priority.ALWAYS);
        final Button add = toolButton("+", t("Add an output mapping for this source entity"), () -> {
            final String sel = combo.getValue();
            if (sel != null && !sel.isBlank() && !rw.entities().containsKey(sel)) {
                rw.ensureEntity(sel);
                structureChanged();
            }
        });
        final VBox box = new VBox(4,
                caption(t("Add output mapping for a source entity")),
                new HBox(6, combo, add));
        return box;
    }

    private Node entity(RwConfig rw, String entity) {
        final EntityConfig ec = rw.entities().get(entity);
        if (ec == null) {
            return hint(format("Entity not found: {0}", entity));
        }
        final VBox box = new VBox(6);
        box.setPadding(new Insets(8));
        box.getChildren().add(sectionHeader(rw.name(),
                format("Entity: {0}   ({1})", entity, rw.name())));
        box.getChildren().add(entityBody(ec, rw));
        return box;
    }

    /**
     * The body of an entity form.
     *
     * <p>A <em>reader</em> entity owns the source {@code fields} (the one global definition, shared
     * by every writer) and, for DB/Mongo readers, the order field.</p>
     *
     * <p>A <em>writer</em> entity owns the output mapping of one source entity: the destination
     * entity, the writer-type specific destination id/filter fields, and the field mapping. The
     * source fields are read from the reader (drag source) — they are never re-defined here.</p>
     */
    private Node entityBody(EntityConfig ec, RwConfig rw) {
        final VBox box = new VBox(10);
        box.setPadding(new Insets(4));

        if (rw.isReader()) {
            box.getChildren().add(sourceFieldList(ec));
            if (isDbLike(rw)) {
                box.getChildren().add(destField(ec, "order_field", t("Order field")));
            }
        } else {
            box.getChildren().add(destinationMapping(ec, rw));
        }
        final Map<String, String> rest = remainingSettings(ec, rw);
        if (!rest.isEmpty()) {
            box.getChildren().add(mapGrid(rest, rw));
        }
        if (box.getChildren().isEmpty()) {
            box.getChildren().add(hint("(empty)"));
        }
        return box;
    }

    /** DB-like interfaces (db_*, mongo_*) support an entity order field. */
    private static boolean isDbLike(RwConfig rw) {
        final String p = InterfaceTypes.of(rw.name()).prefix();
        return "db".equals(p) || "mongo".equals(p);
    }

    /**
     * Entity settings minus the parameters that are edited by dedicated controls (so they are not
     * rendered twice in the generic settings grid).
     */
    private static Map<String, String> remainingSettings(EntityConfig ec, RwConfig rw) {
        if (ec.settings().isEmpty()) {
            return Map.of();
        }
        final java.util.Set<String> handled = new java.util.LinkedHashSet<>();
        if (rw.isReader()) {
            handled.add("order_field");
        } else {
            handled.add("dest_unique_id_field");
            handled.add("dest_id_path");
            handled.add("dest_filter_fields");
        }
        final Map<String, String> rest = new java.util.TreeMap<>(ec.settings());
        rest.keySet().removeAll(handled);
        return rest;
    }

    /**
     * A single entity-scoped text parameter bound directly to {@code ec.settings()}. Used for the
     * writer-type specific destination id field ({@code dest_unique_id_field}, {@code dest_id_path})
     * and the entity order field.
     */
    private Node destField(EntityConfig ec, String key, String label) {
        final TextField tf = new TextField(ec.settings().getOrDefault(key, ""));
        tf.textProperty().addListener((_, _, b) -> {
            if (b == null || b.isEmpty()) {
                ec.settings().remove(key);
            } else {
                ec.settings().put(key, b);
            }
            changed();
        });
        return labeled(label, tf);
    }

    /**
     * Destination mapping of a writer entity — the heart of the editor, mirroring the original
     * writer tab:
     *
     * <ul>
     *   <li>the destination entity — exactly <em>one</em> per source entity, as in the legacy GUI;</li>
     *   <li>the writer-type specific destination id field ({@code dest_unique_id_field} for
     *       DB/Mongo, {@code dest_id_path} for REST) and the destination filter fields;</li>
     *   <li>the source fields of the selected source entity, in <em>full width at the top</em>,
     *       as the <em>drag source</em> (they are read-only here and edited on the reader);</li>
     *   <li>below them the formula box as <em>drop target</em> (a dropped field becomes
     *       {@code {field}}) with <em>Add standard mapping</em> (legacy {@code Add std. mapping})
     *       and <em>Add insert mapping</em>;</li>
     *   <li>the original Studio's clickable drag-and-drop icon (legacy {@code generateMappings}),
     *       which maps every field to {@code field:{field}};</li>
     *   <li>the mapping table with edit / remove / move up / move down.</li>
     * </ul>
     *
     * The mapping record is created lazily, so merely opening the form does not change the file.
     */
    private Node destinationMapping(EntityConfig ec, RwConfig rw) {
        final String prefix = InterfaceTypes.of(rw.name()).prefix();
        final String entity = ec.entity();

        final VBox box = new VBox(10);

        // --- destination entity (max. one per source entity) --------------------------------
        final String dest = ec.hasMapping() && ec.mapping().destEntity() != null
                ? ec.mapping().destEntity() : "";
        final TextField destEntity = new TextField(dest);
        destEntity.setPromptText(t("destination entity (e.g. COMPANIES)"));
        destEntity.textProperty().addListener((_, _, b) -> {
            if (b.isBlank() && !ec.hasMapping()) {
                return; // nothing to clear yet
            }
            ec.ensureMapping().setDestEntity(b.isBlank() ? null : b);
            changed();
        });
        final Label destLabel = new Label(t("Destination entity"));
        destLabel.getStyleClass().add("field-label");
        destLabel.setTooltip(new Tooltip(t("The legacy GUI allows exactly one destination entity per "
                + "source entity. For a second target, use a view or a second processor configuration.")));
        box.getChildren().add(new VBox(2, destLabel, destEntity));

        // --- writer-type specific destination fields ----------------------------------------
        if ("db".equals(prefix) || "mongo".equals(prefix)) {
            box.getChildren().add(destField(ec, "dest_unique_id_field",
                    t("Destination unique id field")));
        } else if ("rest".equals(prefix)) {
            box.getChildren().add(destField(ec, "dest_id_path", t("Destination id path")));
        }
        if ("db".equals(prefix) || "mongo".equals(prefix) || "rest".equals(prefix)) {
            final Label ffc = caption(t("Destination filter fields"));
            ffc.setTooltip(new Tooltip(t("Only these fields are used to build the destination "
                    + "filter (DB/Mongo) or the id lookup (REST).")));
            box.getChildren().add(stringListEditor(ffc, t("field name"),
                    commaList(ec.settings(), "dest_filter_fields"), () -> { }, null));
        }

        // --- field mapping (source fields -> formula -> mapping table) -----------------------
        box.getChildren().add(caption(format("Field mapping \u2014 source entity {0}",
                "'" + entity + "'")));
        box.getChildren().add(mappingEditor(ec, rw, entity));
        return box;
    }

    /**
     * The drag-and-drop mapping editor. As in the original Studio: the source fields of
     * {@code entity} (owned by the reader) at the top in full width, below them the formula box
     * with the add buttons, and at the bottom the ordered mapping table.
     */
    private Node mappingEditor(EntityConfig ec, RwConfig rw, String entity) {
        final List<String> sourceFields = cfg == null
                ? List.of() : cfg.readerEntityFields(entity);

        final ObservableList<FieldMappingRec> rows = FXCollections.observableArrayList(
                ec.hasMapping() ? new ArrayList<>(ec.mapping().fields()) : new ArrayList<>());
        rows.addListener((javafx.collections.ListChangeListener<FieldMappingRec>) _ -> {
            final WriterMapping m = ec.ensureMapping();
            m.fields().clear();
            m.fields().addAll(rows);
            changed();
        });

        // Source fields on top, in full width: the drag source of this mapping (read-only here,
        // they are shared and edited on the reader).
        final ListView<String> fieldsList = new ListView<>(
                FXCollections.observableArrayList(sourceFields));
        fieldsList.setPrefHeight(200);
        fieldsList.setPlaceholder(hint(format("No source fields for {0}.", "'" + entity + "'")));
        final Label fieldsCaption = caption(format("Source fields ({0})", entity));
        fieldsCaption.setTooltip(new Tooltip(t("Drag a field into the formula box below (or "
                + "double-click it). The fields belong to the source entity and are edited "
                + "on the reader.")));

        // The formula box is the drop target.
        final TextArea formula = new TextArea();
        formula.setPromptText("destination:{source-field} ...");
        formula.setPrefRowCount(3);
        formula.setWrapText(true);
        wireFieldDrag(fieldsList, formula);
        wireFieldDrop(formula);

        final Button addStd = toolButton(t("Add standard mapping"),
                t("Add the formula above as a standard [STD] mapping"),
                () -> addMappingFromBox(ec, rows, formula, false));
        final Button addIns = toolButton(t("Add insert mapping"),
                t("Add the formula above as an insert-only [INS] mapping"),
                () -> addMappingFromBox(ec, rows, formula, true));
        final Button all = dndButton(
                () -> rows.addAll(MappingFormula.standardFor(sourceFields, rows)));

        final HBox buttons = new HBox(8, addStd, addIns, all);
        buttons.setAlignment(Pos.CENTER_LEFT);

        final VBox fieldsBox = new VBox(4, fieldsCaption, fieldsList);
        VBox.setVgrow(fieldsList, Priority.ALWAYS);

        return new VBox(10, fieldsBox,
                labeled(t("Formula (drop source fields here)"), formula),
                buttons,
                mappingTable(rows));
    }

    /** The original Studio's auto-mapping control: a clickable icon that auto-maps every source
     *  field to the destination field with the same name (magic-wand = "do it automatically"). */
    private Button dndButton(Runnable action) {
        final Text view = Icons.create("magic-wand", 40);
        if (view != null) {
            view.setStyle("-fx-fill: #dddddd;");
        }
        final Button b = new Button(null, view);
        b.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        b.getStyleClass().add("dnd-button");
        b.setTooltip(new Tooltip(t(DND_TOOLTIP)));
        b.setOnAction(_ -> action.run());
        return b;
    }

    /** Auto-mapping hint, wording of the original Studio's drag-and-drop panel. */
    private static final String DND_TOOLTIP =
            "Drag single fields from above to destination mapping below.\n\n"
            + "By clicking HERE, default mappings will be created with all source fields\n"
            + "and with target field names equal to source field names:\n\n"
            + "source-field-1:{source-field-1}\n"
            + "source-field-2:{source-field-2}\n"
            + "source-field-3:{source-field-3}\n"
            + "...\n\n"
            + "If there's already a target field name that equals any source field name,\n"
            + "the corresponding source field is not taken over.";

    /** Make a source-field list a drag source for the mapping formula box. */
    private void wireFieldDrag(ListView<String> fieldsList, TextArea formula) {
        fieldsList.setOnDragDetected(ev -> {
            final String field = fieldsList.getSelectionModel().getSelectedItem();
            if (field == null) {
                return;
            }
            final Dragboard db = fieldsList.startDragAndDrop(TransferMode.COPY);
            final ClipboardContent content = new ClipboardContent();
            content.put(FIELD_FORMAT, field);
            content.putString(field);
            db.setContent(content);
            ev.consume();
        });
        // Double-click is a more discoverable alternative to dragging.
        fieldsList.setOnMouseClicked(ev -> {
            final String field = fieldsList.getSelectionModel().getSelectedItem();
            if (ev.getClickCount() == 2 && field != null) {
                insertField(formula, field);
            }
        });
    }

    /** Accept dropped source fields in the formula box and insert {@code {field} }. */
    private void wireFieldDrop(TextArea formula) {
        formula.setOnDragOver(ev -> {
            if (ev.getDragboard().hasContent(FIELD_FORMAT)) {
                ev.acceptTransferModes(TransferMode.COPY);
            }
            ev.consume();
        });
        formula.setOnDragDropped(ev -> {
            final Dragboard db = ev.getDragboard();
            boolean ok = false;
            if (db.hasContent(FIELD_FORMAT)) {
                insertField(formula, String.valueOf(db.getContent(FIELD_FORMAT)));
                ok = true;
            }
            ev.setDropCompleted(ok);
            ev.consume();
        });
    }

    private void insertField(TextArea formula, String field) {
        formula.insertText(formula.getCaretPosition(), "{" + field + "}");
        formula.requestFocus();
    }

    /** Validate the formula box and append the mapping (STD or INS). Mirrors legacy {@code addMapping}. */
    private void addMappingFromBox(EntityConfig ec, ObservableList<FieldMappingRec> rows,
            TextArea formula, boolean insert) {
        final String text = formula.getText();
        final FieldMappingRec rec = MappingFormula.parse(text, insert);
        if (rec == null) {
            // Two messages on purpose: the example contains literal {…} braces, which must not
            // be fed to MessageFormat (it would read them as argument numbers).
            warn(formula, format("The destination mapping {0} has a wrong format!",
                    "'" + (text == null ? "" : text.trim()) + "'") + "\n\n"
                    + t("Example: 'target-field:{source-field-1} and {source-field-4} whatever'.\n"
                    + "First the target field, then ':' and then the value \u2014 one or more source "
                    + "fields enclosed in curly brackets. The line must contain exactly one colon."));
            return;
        }
        rows.add(rec);
        formula.clear();
    }


    /**
     * Editor for the source {@code fields} of an entity. Mirrors the original Studio, which edits
     * them as a list (Add / Remove / Edit / move up / move down) instead of a comma separated text
     * field: type a name and press <em>+</em> (or Enter) to append, double-click a row to edit it.
     */
    private Node sourceFieldList(EntityConfig ec) {
        final ObservableList<String> items = FXCollections.observableList(ec.fields());
        final Label caption = caption(t("Fields (source)"));
        caption.setTooltip(new Tooltip(t("Source entity fields; order matters for CSV/JSON readers.")));
        return stringListEditor(caption, t("New field name"), items, this::changed, null);
    }

    /**
     * A reusable string-list editor: a caption, the list (double-click to rename), an input row with
     * <em>+</em> and the remove / move up / move down tools. {@code onEdit} is run after every
     * mutation that is not already reported by a backing list listener.
     */
    private Node stringListEditor(Label caption, String prompt, ObservableList<String> items,
            Runnable onEdit, List<String> suggestions) {
        final ListView<String> list = new ListView<>(items);
        list.setEditable(true);
        list.setPrefHeight(132);
        if (suggestions != null) {
            // Unknown interface names are shown as the "<other>" placeholder, as in the
            // legacy Studio; known names are shown verbatim.
            list.setCellFactory(_ -> new TextFieldListCell<String>() {
                @Override
                public void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else if (suggestions.contains(item)) {
                        setText(item);
                    } else {
                        setText("<other>");
                    }
                }
            });
        } else {
            list.setCellFactory(TextFieldListCell.forListView());
        }
        list.setOnEditCommit(ev -> {
            final String v = ev.getNewValue() == null ? "" : ev.getNewValue().trim();
            if (v.isEmpty() || items.contains(v)) {
                list.refresh(); // reject empty names and duplicates
            } else {
                items.set(ev.getIndex(), v);
                onEdit.run();
            }
        });

        // Input control: a free-text field by default, or an editable combo box populated
        // with the predefined interface names (+ "<other>") when suggestions are given, so the
        // reader/writer is picked from the legacy Studio's predefined list instead of being
        // typed by hand.
        final javafx.scene.control.Control input;
        final java.util.function.Supplier<String> readInput;
        if (suggestions != null) {
            final ComboBox<String> combo = new ComboBox<>(FXCollections.observableArrayList(suggestions));
            combo.setEditable(true);
            combo.setPromptText(prompt);
            combo.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(combo, Priority.ALWAYS);
            input = combo;
            readInput = () -> combo.getEditor().getText();
        } else {
            final TextField tf = new TextField();
            tf.setPromptText(prompt);
            HBox.setHgrow(tf, Priority.ALWAYS);
            input = tf;
            readInput = tf::getText;
        }
        @SuppressWarnings("unchecked")
		final Runnable addItem = () -> {
            final String v = readInput.get() == null ? "" : readInput.get().trim();
            if (!v.isEmpty() && !"<other>".equals(v) && !items.contains(v)) {
                items.add(v);
                list.getSelectionModel().select(v);
                list.scrollTo(v);
                if (input instanceof TextField) {
                    ((TextField) input).clear();
                } else if (input instanceof ComboBox) {
                    ((ComboBox<?>) input).getEditor().clear();
                    ((ComboBox<String>) input).setValue(null);
                }
                onEdit.run();
            }
        };
        if (input instanceof TextField) {
            ((TextField) input).setOnAction(_ -> addItem.run());
        } else {
            ((ComboBox<?>) input).getEditor().setOnAction(_ -> addItem.run());
        }

        final Button add = toolButton("+", t("Add"), addItem);
        final Button remove = toolButton("\u2212", t("Remove selected"), () -> {
            final int i = list.getSelectionModel().getSelectedIndex();
            if (i >= 0) {
                items.remove(i);
                onEdit.run();
            }
        });
        final Button up = toolButton("\u2191", t("Move up"),
                () -> move(items, list, list.getSelectionModel().getSelectedIndex(), -1, onEdit));
        final Button down = toolButton("\u2193", t("Move down"),
                () -> move(items, list, list.getSelectionModel().getSelectedIndex(), 1, onEdit));

        final HBox inputRow = new HBox(6, input, add);
        final HBox tools = new HBox(6, remove, up, down);
        final VBox box = new VBox(4, list, inputRow, tools);
        if (caption != null && !caption.getText().isEmpty()) {
            box.getChildren().add(0, caption);
        }
        return box;
    }

    /**
     * A live {@link ObservableList} view of a comma separated settings value. Mutations write the
     * comma joined value back into the settings map (and remove the key when the list becomes
     * empty), so the model stays consistent.
     */
    private ObservableList<String> commaList(Map<String, String> settings, String key) {
        final List<String> initial = new ArrayList<>();
        for (final String part : settings.getOrDefault(key, "").split(",")) {
            final String t = part.trim();
            if (!t.isEmpty()) {
                initial.add(t);
            }
        }
        final ObservableList<String> items = FXCollections.observableArrayList(initial);
        items.addListener((javafx.collections.ListChangeListener<String>) _ -> {
            if (items.isEmpty()) {
                settings.remove(key);
            } else {
                settings.put(key, String.join(",", items));
            }
            changed();
        });
        return items;
    }

    private void move(ObservableList<String> items, ListView<String> list, int index, int delta,
            Runnable onEdit) {
        final int target = index + delta;
        if (index < 0 || target < 0 || target >= items.size()) {
            return;
        }
        items.add(target, items.remove(index));
        list.getSelectionModel().select(target);
        onEdit.run();
    }

    private Button toolButton(String text, String tooltip, Runnable action) {
        final Button b = new Button(text);
        b.setTooltip(new Tooltip(tooltip));
        b.setMinWidth(34);
        b.setOnAction(_ -> action.run());
        return b;
    }

    private Node general(Map<String, String> general) {
        final VBox box = new VBox(6);
        box.setPadding(new Insets(8));
        box.getChildren().add(title(t("Overwritten Values")));
        if (general.isEmpty()) {
            box.getChildren().add(hint(t("(none)")));
        } else {
            box.getChildren().add(mapGrid(general, null));
        }
        return box;
    }

    private GridPane mapGrid(Map<String, String> map, RwConfig rw) {
        final GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(6);
        grid.setPadding(new Insets(6, 6, 6, 6));

        int row = 0;
        for (final Map.Entry<String, String> e : new TreeMap<>(map).entrySet()) {
            final String key = e.getKey();
            final FieldDescriptor d = FieldCatalog.descriptor(key);
            // The raw configuration key is the message id; the derived label is the English
            // fallback, so a translation file can translate parameters by their key.
            final Label label = new Label(t(key, d.label()));
            // "field-label" is the shared CSS class, "param-label" marks an engine parameter
            // label (as opposed to a caption) and is queried by the UI tests.
            label.getStyleClass().addAll("field-label", "param-label");
            if (!d.help().isEmpty()) {
                label.setTooltip(new Tooltip(t(key + ".help", d.help())));
            }
            final Node control = control(map, key, d, rw);
            GridPane.setHgrow(control, Priority.ALWAYS);
            grid.add(label, 0, row);
            grid.add(control, 1, row);
            row++;
        }
        return grid;
    }

    private Node control(Map<String, String> map, String key, FieldDescriptor d, RwConfig rw) {
        // MongoDB uses its own protocol values (mongodb, mongodb+srv), not the generic
        // "https/http/mongodb/tcp" list, and only Mongo/REST expose a protocol field.
        if ("protocol".equals(key) && rw != null) {
            final String prefix = InterfaceTypes.of(rw.name()).prefix();
            final List<String> opts;
            if ("mongo".equals(prefix)) {
                opts = List.of("mongodb", "mongodb+srv");
            } else if ("rest".equals(prefix)) {
                opts = List.of("https", "http");
            } else {
                opts = d.options(); // fall back to the descriptor's options
            }
            final FieldDescriptor pd = new FieldDescriptor(d.param(), d.label(), FieldType.SELECT, opts, d.help());
            d = pd;
        }
        final String value = map.getOrDefault(key, "");
        switch (d.type()) {
            case BOOLEAN: {
                final ComboBox<String> cb = new ComboBox<>(
                        FXCollections.observableArrayList("", "yes", "no"));
                cb.setValue(value);
                cb.setMaxWidth(Double.MAX_VALUE);
                cb.valueProperty().addListener((_, _, b) -> {
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
                cb.valueProperty().addListener((_, _, b) -> {
                    map.put(key, b == null ? "" : b);
                    changed();
                });
                return cb;
            }
            case INTEGER: {
                final TextField tf = new TextField(value);
                tf.setTextFormatter(new javafx.scene.control.TextFormatter<>(
                        s -> s.getControlNewText().matches("-?\\d*") ? s : null));
                tf.textProperty().addListener((_, _, b) -> {
                    map.put(key, b);
                    changed();
                });
                return tf;
            }
            case SECRET: {
                final ch.autumo.ui.fx.control.SecretField sf = ch.autumo.ui.fx.control.SecretField.create(app);
                sf.setText(value);
                sf.textProperty().addListener((_, _, b) -> {
                    map.put(key, b == null ? "" : b);
                    changed();
                });
                return sf;
            }
            case TEXT_AREA: {
                final TextArea ta = new TextArea(value);
                ta.setPrefRowCount(2);
                ta.textProperty().addListener((_, _, b) -> {
                    map.put(key, b);
                    changed();
                });
                return ta;
            }
            default: {
                final TextField tf = new TextField(value);
                tf.textProperty().addListener((_, _, b) -> {
                    map.put(key, b);
                    changed();
                });
                return tf;
            }
        }
    }

    /**
     * The ordered mapping table. Standard ({@code [STD]}) and insert ({@code [INS]}) rows share one
     * list; the model follows {@code rows} via the change listener set up in {@link #mappingEditor}.
     */
    private Node mappingTable(ObservableList<FieldMappingRec> rows) {
        final TableView<FieldMappingRec> table = new TableView<>(rows);
        table.setEditable(true);
        table.setPrefHeight(180);
        table.setPlaceholder(hint(t(
                "No mappings yet \u2014 add one with the buttons above (or click the dnd icon).")));

        final TableColumn<FieldMappingRec, String> colField =
                new TableColumn<>(t("Destination field"));
        colField.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().destField()));
        colField.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        colField.setOnEditCommit(ev -> replace(rows, ev.getRowValue(),
                new FieldMappingRec(ev.getNewValue(), ev.getRowValue().formula(), ev.getRowValue().insert())));
        colField.setPrefWidth(240);

        final TableColumn<FieldMappingRec, String> colFormula = new TableColumn<>(t("Formula"));
        colFormula.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().formula()));
        colFormula.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        colFormula.setOnEditCommit(ev -> replace(rows, ev.getRowValue(),
                new FieldMappingRec(ev.getRowValue().destField(), ev.getNewValue(), ev.getRowValue().insert())));
        colFormula.setPrefWidth(320);

        final TableColumn<FieldMappingRec, Boolean> colInsert = new TableColumn<>(t("Insert (INS)"));
        colInsert.setCellValueFactory(c -> new SimpleBooleanProperty(c.getValue().insert()));
        colInsert.setCellFactory(CheckBoxTableCell.forTableColumn(colInsert));
        colInsert.setOnEditCommit(ev -> replace(rows, ev.getRowValue(),
                new FieldMappingRec(ev.getRowValue().destField(), ev.getRowValue().formula(), ev.getNewValue())));
        colInsert.setPrefWidth(90);

        table.getColumns().addAll(List.of(colField, colFormula, colInsert));

        final Button add = toolButton("+", t("Add an empty mapping row"), () -> {
            rows.add(new FieldMappingRec("", "", false));
            table.getSelectionModel().selectLast();
        });
        final Button remove = toolButton("\u2212", t("Remove the selected mapping"), () -> {
            final int i = table.getSelectionModel().getSelectedIndex();
            if (i >= 0) {
                rows.remove(i);
            }
        });
        final Button up = toolButton("\u2191", t("Move mapping up"),
                () -> moveMapping(rows, table.getSelectionModel().getSelectedIndex(), -1));
        final Button down = toolButton("\u2193", t("Move mapping down"),
                () -> moveMapping(rows, table.getSelectionModel().getSelectedIndex(), 1));
        final HBox buttons = new HBox(6, add, remove, up, down);

        final VBox box = new VBox(4, table, buttons);
        VBox.setVgrow(table, Priority.ALWAYS);
        return box;
    }

    /** Reorder a mapping row; STD and INS entries share one ordered list. */
    private void moveMapping(ObservableList<FieldMappingRec> rows, int index, int delta) {
        final int target = index + delta;
        if (index < 0 || target < 0 || target >= rows.size()) {
            return;
        }
        rows.add(target, rows.remove(index));
    }

    private void replace(ObservableList<FieldMappingRec> rows, FieldMappingRec oldRec,
            FieldMappingRec newRec) {
        final int i = rows.indexOf(oldRec);
        if (i >= 0) {
            rows.set(i, newRec);
        }
    }

    private void changed() {
        if (onChange != null) {
            onChange.run();
        }
    }

    /**
     * A change that alters the <em>tree structure</em> (adding/removing/renaming a section, entity
     * or writer) — the navigation tree must be rebuilt.
     */
    private void structureChanged() {
        changed();
        if (onStructureChange != null) {
            onStructureChange.run();
        }
    }

    /** Legacy-style warning dialog for an invalid mapping formula. */
    private void warn(Node source, String message) {
        final Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle(t("Destination mapping"));
        if (source != null && source.getScene() != null) {
            alert.initOwner(source.getScene().getWindow());
        }
        alert.showAndWait();
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

    /**
     * A coloured type pill (legacy GUI colours: readers green, writers red, base blue) showing the
     * interface type (CSV, JSON, DB, Mongo DB, REST, Mail, …). See {@link InterfaceTypes}.
     */
    static Label typePill(String rwName) {
        final InterfaceTypes.InterfaceType t = InterfaceTypes.of(rwName);
        final Label l = new Label(t.display());
        l.getStyleClass().add("type-pill");
        l.setStyle("-fx-background-color: " + t.colorHex() + ";");
        final String cls = t.engineClass() == null ? "" : "\n" + t.engineClass();
        l.setTooltip(new Tooltip(t.display() + "  (" + t.rwName() + ")" + cls));
        return l;
    }

    /** A type pill followed by the section title, so the interface type is visible at a glance. */
    private Node sectionHeader(String rwName, String text) {
        final HBox h = new HBox(8, typePill(rwName), title(text));
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    /** The base/job pseudo interface header, using the legacy BASE colour (blue). */
    private Node rootHeader() {
        final Label pill = new Label("BASE");
        pill.getStyleClass().add("type-pill");
        pill.setStyle("-fx-background-color: " + InterfaceTypes.COLOR_BASE + ";");
        pill.setTooltip(new Tooltip(t("Global job configuration (source_interface, "
                + "destination_interfaces, process_source_entities)")));
        final HBox h = new HBox(8, pill, title(t("Configuration")));
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
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
