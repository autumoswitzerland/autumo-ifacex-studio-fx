# 11 – Legacy Studio GUI Analysis (source of truth for the editor)

Status: analysis, 2026-10-16. Scope: how the **old Swing Studio** (`autumo-ifacex-studio`)
configures readers/writers and how it assigns the destination mapping. This document is the
specification the new JavaFX editor must implement — the flat key model alone is **not** enough.

All statements below were verified against the legacy sources:

- `view/config/ConfigDesignPanel.form` / `.java` (layout)
- `view/config/ConfigLogicPanel.java` (model + operations)
- `view/config/ConfigEventPanel.java` (events, wiring)
- `view/config/ConfigSavePanel.java` / `ConfigLoadPanel.java` (serialization)
- `view/model/EntityFieldsHolder.java`, `view/model/DestinationMappingHolder.java`
- `view/config/DNDJList.java`, `DNDJTextArea.java`
- engine `cfg/interfaces.cfg` (reader/writer type registry)

---

## 1. Overall screen

One `JTabbedPane` with **12 tabs**, coloured by role:

| # | Tab | Role | Tab background |
|---|-----|------|----------------|
| 0 | `⊗ BASE` | global job | `(72,72,107)` blue |
| 1 | `↓ CSV` | reader | `(72,107,72)` green |
| 2 | `↓ JSON` | reader | green |
| 3 | `↓ DB` | reader | green |
| 4 | `↓ Mongo` | reader | green |
| 5 | `↓ REST` | reader | green |
| 6 | `↑ CSV` | writer | `(107,72,72)` red |
| 7 | `↑ JSON` | writer | red |
| 8 | `↑ Mail` | writer | red |
| 9 | `↑ DB` | writer | red |
| 10 | `↑ Mongo` | writer | red |
| 11 | `↑ REST` | writer | red |

`↓` = reader (source), `↑` = writer (destination). Only the tabs of the **selected** reader and
of the **selected** writers are enabled (`activateReaderTabs()` / `activateWriterTabs()`).

In the new GUI the same information is carried by a **coloured type pill/badge**
(blue = base, green = reader, red = writer) on the section/nav node.

---

## 2. Interface type registry

The *type* is the prefix of the interface name (`<type>_in` / `<type>_out`).

Engine `cfg/interfaces.cfg`:

| Type | Reader prefix | Reader class |
|------|---------------|--------------|
| Null | `null_in` | `NullReader` |
| CSV | `csv_in` | `csv.file.CSVReader` |
| JSON | `json_in` | `file.JSONReader` |
| File | `file_in` | `file.FilePathReader` |
| Document | `doc_in` | `file.DocumentReader` |
| DB | `db_in` | `database.JDBCDatabaseReader` |
| Mongo | `mongo_in` | `database.MongoDatabaseReader` |
| REST | `rest_in` | `rest.RESTReader` |

| Type | Writer prefix | Writer class |
|------|---------------|--------------|
| Code | `code` | `CodeRunner` |
| Exec | `exec` | `ProgramExec` |
| First batch | `first_out` | `FirstBatchWriter` |
| Console | `console_out` | `ConsoleWriter` |
| CSV | `csv_out` | `file.CSVWriter` |
| JSON | `json_out` | `file.JSONWriter` |
| File | `file_out` | `file.FileWriter` |
| Mail | `mail_out` | `mail.MailWriter` |
| DB | `db_out` | `database.JDBCDatabaseWriter` |
| Mongo | `mongo_out` | `database.MongoDatabaseWriter` |
| REST | `rest_out` | `rest.RESTWriter` |

Additionally, plugin readers/writers exist (`aws_file_in`, `gdrive_file_in`, `dbox_file_in`,
`webdav_file_in`, `ftp_file_in`, `sftp_file_in`, `os_swift_file_in`, `azure_file_in`,
`b2_file_in`, `gstorage_file_in`, `excel_out`, …). These are **not** in the GUI model lists but
can be typed as `<Other>`.

GUI model lists (`ConfigLogicPanel`):

```java
READER_MODEL = { "CSV", "JSON", "Document", "DB", "Mongo DB", "REST", "Null", "<Other>" }
WRITER_MODEL = { "CSV", "JSON", "Mail", "DB", "Mongo DB", "REST", "Console", "Code", "Exec", "<Other>" }
```

Display name → prefix: CSV→`csv`, JSON→`json`, Mail→`mail`, DB→`db`, Mongo DB→`mongo`,
REST→`rest`, Document→`doc`, Null→`null`, Console→`console`, Code→`code`, Exec→`exec`,
`<Other>`→ free prefix.

Note: the GUI has **dedicated tabs only** for CSV, JSON, DB, Mongo, REST (+ Mail on the writer
side). `Document`, `Null`, `Console`, `Code`, `Exec`, `File` have **no field editor**.

---

## 3. BASE tab (global job)

Components: `l_source_entities`, `tf_source_entity`, `l_source_entities_list`,
`b_add_source_entity`, `b_remove_source_entity`, `b_base_edit_entity`, `b_base_up`,
`b_base_down`, `b_base_manual_edit`, `l_reader`, `c_reader`, `l_writers`,
`l_writers_selected`, `b_writer_add`, `b_writer_remove`, `l_writers_deselected`,
`l_title`/`t_title`, `l_author`/`t_author`, `l_version`/`t_version`, `l_notes`/`ta_notes`.

| Field | Key | Notes |
|-------|-----|-------|
| Title | `meta_title` | metadata |
| Author | `meta_author` | metadata |
| Version | `meta_version` | metadata |
| Notes | `meta_notes` | multiline |
| Reader | `source_interface` | **exactly one** (combo box) |
| Writers | `destination_interfaces` | comma list, from selected/deselected lists |
| Source entities | `process_source_entities` | comma list, ordered (up/down) |

`<Other>` lets the user type a custom reader/writer prefix; existing keys with unknown prefixes
are preserved on save (`keepStartsWith`/`keepEndsWith` + `saveMap`/`putBackAll`).

---

## 4. Data model (the important part)

### 4.1 `EntityFieldsHolder` — one per **source entity**, shared across all writer tabs

| Field | Meaning | Serialized as |
|-------|---------|---------------|
| `sourceFields` | ordered list of the entity's source fields (+ up/down/edit/add/remove) | `<reader>_<entity>_fields` (comma list) |
| `orderFieldName` | order field (DB/Mongo reader) | `<reader>_<entity>_order_field` |
| `uid` | destination unique id field (DB writer) | `<writer>_<entity>_dest_unique_id_field` |
| `muid` | destination unique id field (Mongo writer) | `<writer>_<entity>_dest_unique_id_field` |
| `jsonIdPath` | destination id path (REST writer) | `<writer>_<entity>_dest_id_path` |
| `filterFields` | per-writer list stored as `writer:field` | `<writer>_<entity>_dest_filter_fields` (comma list) |

Key insight: **source fields and the uid/muid/jsonIdPath/filter fields live on the *source
entity*, not on the writer.** The writer only *references* them through the mapping.

### 4.2 `DestinationMappingHolder` — one per **(writer, source entity)**

| Field | Meaning |
|-------|---------|
| `writer` | e.g. `csv_out` |
| `sourceEntity` | e.g. `transactions` |
| `entityName` | **destination** entity name (exactly one per source entity) |
| `mappings` | ordered list of `"[STD] targetField:formula"` / `"[INS] targetField:formula"` |

`MAPPING_PREFIX_STD = "[STD]"`, `MAPPING_PREFIX_INS = "[INS]"`.

Identity: `writer + "_" + sourceEntity + "_" + entityName`; `matches()` compares only
writer + sourceEntity. The code explicitly states:

> `// we only allow one destination entity per source entity at this moment !`

`add(standard, targetField, sourceFormula)` refuses duplicates by target field.

---

## 5. Mapping editor (identical block in every writer tab)

Repeated components (replace `csv_out` by `json_out`, `db_out`, `mongo_out`, `rest_out`,
`mail_out`):

| Component | Purpose |
|-----------|---------|
| `l_<w>_entities_list` | source entities (from `process_source_entities`) |
| `l_<w>_fields_list` | **source fields of the selected entity** — **drag source** |
| `ta_<w>_mapping` | **formula input** — **drop target**; receives `{field} ` on drop |
| `l_<w>_mappings_list` | the `[STD]`/`[INS]` mapping lines of the selected dest entity |
| `t_<w>_dest_entity` + `l_<w>_dest_entities_list` | destination entity (max 1) |
| `b_<w>_add_std_mapping` / `b_<w>_add_ins_mapping` | add the text from `ta_<w>_mapping` as STD/INS |
| `b_<w>_edit_mapping` / `b_<w>_remove_mapping` / `b_<w>_up` / `b_<w>_down` | edit/remove/reorder mappings |
| `b_<w>_add_dest_entity` / `b_<w>_remove_dest_entity` / `b_<w>_edit_dest_entity` | manage the destination entity |
| `<w>_dndPanelMain` / `<w>_dndPanel` | panel whose **mouse click** triggers auto-mapping |

### 5.1 Formula format and validation

`addMapping()` validates: split on `:` must yield exactly **2** parts and the first part must
match `PATTERN_TARGET_FIELD`; otherwise a dialog explains:

```
target-field: {source-field-1} and {source-field-4} whatever
```

So a mapping is `<destField>:<free text with {sourceField} placeholders>`.

### 5.2 Drag & drop

- `DNDJList` (= `l_<w>_fields_list`) starts a `StringSelection` drag with the selected field.
- `DNDJTextArea` (= `ta_<w>_mapping`) accepts the drop and does
  `setText(getText() + "{" + str + "} ")` — i.e. inserts `{field} ` at the end.

### 5.3 Auto-mapping (click on the DND panel)

`csv_out_dndPanelMouseClicked` → `generateMappings(...)`: for **every** source field of the
selected entity it adds

```
<field>:{<field>}
```

as a **STD** mapping. That is the "map all fields 1:1" convenience.

### 5.4 Destination entity rule

`addDestinationEntity()` refuses a second destination entity for the same source entity:

> "Currently only one destination entity is allowed per source entity! For DB's use a view or
> create a second ifacex processor configuration."

---

## 6. Serialization (GUI model → flat keys)

### Writer mapping (`writeMappingConfig`)

```
<writer>_<sourceEntity>_mapping_entity      = <destEntity>
<writer>_<sourceEntity>_mapping_field_NNN   = <destField>:<formula>     (STD, counter per STD)
<writer>_<sourceEntity>_mapping_insert_NNN  = <destField>:<formula>     (INS, counter per INS)
```
`NNN` = `%03d`-style counter (`FIELD_COUNTER_FORMAT`). STD and INS have **separate** counters.

### Writer id/filter (`writeIDAndFilterFields`)

```
db_out_<entity>_dest_unique_id_field   = <uid>
mongo_out_<entity>_dest_unique_id_field= <muid>
rest_out_<entity>_dest_id_path         = <jsonIdPath>
<writer>_<entity>_dest_filter_fields   = f1,f2,...
```

### Reader entities

```
<reader>_<entity>_fields       = f1,f2,...
<reader>_<entity>_order_field  = <field>        (db_in / mongo_in)
```

### Load side (`fillDestEntityAndFields`, `fillDestFilterFields`, `fillID`, `fillSourceFields`)

Mirror of the above; mapping keys are sorted (`Collections.sort`) and classified by prefix
`..._mapping_field_` / `..._mapping_insert_`.

---

## 7. Per-type parameter inventory (authoritative, as written by the GUI)

`*` in a key means it is written as a **wildcard** (`<rw>_*_<param>`), otherwise it is
**reader/writer-global** (`<rw>_<param>`). `E` = entity-scoped.

### BASE
`meta_title`, `meta_author`, `meta_version`, `meta_notes`, `source_interface`,
`destination_interfaces`, `process_source_entities`.

### CSV reader (`csv_in`)
| Key | GUI field |
|-----|-----------|
| `csv_in_header_size` | Header size |
| `csv_in_delete_files_after_processing` | Delete after |
| `csv_in_read_from_writer` | Read from writer (`csv_out` or empty) |
| `csv_in_read_from_folder` | Read from folder (path) |
| `csv_in_E_fields` | Fields (E) |

### JSON reader (`json_in`)
`json_in_*_data_path`, `json_in_*_sub_path`, `json_in_*_field_path_access`,
`json_in_*_allow_absent_fields`, `json_in_*_absent_field_placeholder`,
`json_in_delete_files_after_processing`, `json_in_read_from_writer`,
`json_in_read_from_folder`, `json_in_E_fields`.

### DB reader (`db_in`)
`db_in_url`, `db_in_user`, `db_in_password`, `db_in_*_fetchSize`,
`db_in_E_fields`, `db_in_E_order_field`.

### Mongo reader (`mongo_in`)
`mongo_in_host`, `mongo_in_protocol`, `mongo_in_db_name`, `mongo_in_params`,
`mongo_in_user`, `mongo_in_password`, `mongo_in_*_fetchSize`,
`mongo_in_E_fields`, `mongo_in_E_order_field`.

### REST reader (`rest_in`)
`rest_in_host`, `rest_in_protocol`, `rest_in_port`, `rest_in_user`, `rest_in_password`,
`rest_in_api_key`, `rest_in_api_key_name`, `rest_in_auth_method`, `rest_in_auth_url`,
`rest_in_auth_host`, `rest_in_auth_grant_type`, `rest_in_auth_body`,
`rest_in_alt_data_conn`, `rest_in_*_entities_url`, `rest_in_*_data_path`, `rest_in_*_sub_path`,
`rest_in_*_field_path_access`, `rest_in_*_allow_absent_fields`,
`rest_in_*_absent_field_placeholder`, `rest_in_*_additional_headers`,
`rest_in_*_additional_params`, `rest_in_*_fetchSize`, `rest_in_*_param_pageName`,
`rest_in_*_param_offsetName`, `rest_in_*_param_fetchSize`, `rest_in_*_pagination_path`,
`rest_in_*_pagination_items_per_page`, `rest_in_*_pagination_items_total`,
`rest_in_*_pagination_last_page`, `rest_in_*_pagination_after_id`, `rest_in_E_fields`.
(global: `rest_timeout`, `rest_setup_timeout`)

### CSV writer (`csv_out`)
`files_folder` (Output folder — global!), plus `csv_out_E_mapping_entity`,
`csv_out_E_mapping_field_NNN`, `csv_out_E_mapping_insert_NNN`.

### JSON writer (`json_out`)
`files_folder`, plus the `json_out_E_mapping_*` block.

### Mail writer (`mail_out`)
`mail_out_host`, `mail_out_port`, `mail_out_auth`, `mail_out_user`, `mail_out_password`,
`mail_out_subject`, `mail_out_body` (newlines encoded as `#BR#`), `mail_out_to`,
`mail_out_from`, `mail_out_max_size`, `mail_out_tls_enable`,
`mail_out_delete_files_after_processing`, plus the `mail_out_E_mapping_*` block.

### DB writer (`db_out`)
`db_out_url`, `db_out_user`, `db_out_password`, `db_out_ext_insert_mapping`,
plus the `db_out_E_mapping_*` block, `db_out_E_dest_unique_id_field`,
`db_out_E_dest_filter_fields`.

### Mongo writer (`mongo_out`)
`mongo_out_host`, `mongo_out_protocol`, `mongo_out_db_name`, `mongo_out_params`,
`mongo_out_user`, `mongo_out_password`, plus the `mongo_out_E_mapping_*` block,
`mongo_out_E_dest_unique_id_field`, `mongo_out_E_dest_filter_fields`.

### REST writer (`rest_out`)
`rest_out_host`, `rest_out_protocol`, `rest_out_port`, `rest_out_user`, `rest_out_password`,
`rest_out_api_key`, `rest_out_api_key_name`, `rest_out_auth_method`, `rest_out_auth_url`,
`rest_out_auth_host`, `rest_out_auth_grant_type`, `rest_out_auth_body`,
`rest_out_*_entities_url`, `rest_out_*_error_code`, `rest_out_*_error_code_value`,
`rest_out_single_id_filter`, plus the `rest_out_E_mapping_*` block,
`rest_out_E_dest_id_path`, `rest_out_E_dest_filter_fields`.

### Special value conventions

| Convention | Meaning |
|-----------|---------|
| `{|...}` / `{...}` around user/password | value is **encoded/secret** |
| `#BR#` | line break inside a single-line value (mail body) |
| `yes` / `no` | booleans |
| `;` / `,` | list separators (GUI uses `,` for config lists; `;` is CSV separator) |

---

## 8. New configuration flow

1. New project → empty model, `writerTabMap` etc. reset (`newProject()`).
2. BASE: add source entities, pick **one** reader (`c_reader`), move writers from
   *deselected* to *selected*.
3. `activateReaderTabs()` / `activateWriterTabs()` enable the tabs of the chosen types.
4. Per entity: enter source fields.
5. Per writer tab: select the source entity, add one destination entity, build the mapping
   (manually via the formula box + STD/INS, or via drag & drop, or auto-map all).
6. Save → flat keys (`ConfigSavePanel`).

A source entity that is not listed in `process_source_entities` cannot be configured; a writer
not in `destination_interfaces` cannot be configured.

---

## 9. Gap analysis vs. the current `-fx` PoC

| # | Legacy capability | `-fx` PoC status |
|---|-------------------|------------------|
| 1 | Interface **type** (CSV/JSON/DB/Mongo/REST/Mail/…) visible + colour-coded | missing |
| 2 | One reader + N writers as *selected* interfaces | shows all prefixes present in the file |
| 3 | Create a **new** configuration in the GUI | missing (open/convert only) |
| 4 | Source fields **per entity**, shared across writers | present (reader/writer split) |
| 5 | **Destination entity** per (writer, entity), max 1 | present (text field) |
| 6 | **STD/INS** mapping list with add/edit/remove/up/down | partially (add/remove, no STD/INS flag, no edit/up/down) |
| 7 | Mapping formula validation (`dest:…{field}…`) | missing |
| 8 | **Drag & drop** field → formula box | missing |
| 9 | Auto-map all fields (`field:{field}`) | missing |
| 10 | `uid`/`muid`/`jsonIdPath` per entity+writer | missing (only `dest_unique_id_field` in catalog) |
| 11 | `dest_filter_fields` per writer | present as plain text setting |
| 12 | `order_field` per entity (DB/Mongo reader) | partially (raw setting) |
| 13 | Full per-type parameter sets (see §7) | incomplete catalog |
| 14 | `files_folder`, `meta_*`, `rest_timeout`, … globals | partially |
| 15 | Writer "read from writer" round-trip (`csv_in_read_from_writer`) | missing |
| 16 | Secret/encoding convention `{…}` | missing |
| 17 | `<Other>` custom reader/writer prefix + key preservation | partially (unknown keys kept raw) |
| 18 | Reordering of source entities | missing in UI |

---

## 10. Consequences for the new design

- The editor must be **type-driven**: a registry `prefix → (type, kind, display, colour)` and a
  **per-type descriptor list** (scope: global / wildcard / entity / mapping / id / filter).
- The **destination mapping is a first-class editor**, not a generic settings grid.
- Source fields belong to the **entity**; the writer tabs only *reference* them.
- The mapping formula needs a **monospace editor with D&D drop** and a validation rule.
- "New configuration" is a required workflow (choose reader + writers + entities), not an
  afterthought.
- Unknown/`<Other>` parameters must always be preserved.

## 11. Menu bar and menu items (authoritative)

The bar is built in `DesignMainFrame.initialize()`; almost all labels come from
`Bundle_en_US.properties` (`ifacex.jMenu*.text`, `ifacex.jMenuItem_*.text`), a few are hard-coded in
the layout code (marked below). Menu and toolbar share the dark `Color(102, 102, 102)`. The texts are
the *shipped English* ones — the legacy Studio has no German bundle.

| Menu | Item | Label as shipped | Accelerator | Icon | Reference |
| --- | --- | --- | --- | --- | --- |
| File | Open | `Open` | ⌘O | `project-open-16.png` | `jMenuItem_Open` |
| File | Open Recent | `Open Recent` | ⇧⌘O on the first entry | `project-open-16.png` | `jMenu_Recent`, `MainFrame.rebuildRecentList` |
| File | New | `New Configuration` | ⌘N | `project-new-16.png` | `jMenuItem_New` |
| File | Save | `Save` | ⌘S | `project-save-16.png` | `jMenuItem_Save` |
| File | Save As | `Save As` | ⇧⌘S | `project-save-16.png` | `jMenuItem_SaveAs` |
| File | Close | `Close` | ⌘W | `project-close-16.png` | `jMenuItem_Close` |
| File | Quit (only off macOS — `OSHelper.isMac()` skips it) | `Quit` | ⌘Q (Windows: Alt+F4) | `exit-16.png` | `jMenuItem_Quit` |
| Configuration | Interface Processor Configuration | `Interface Processor Configuration` | ⇧⌘P | `settings-16.png` | `jMenuItem_ProjectSettings` |
| Configuration | Interface Base Configuration | *hard-coded* | ⌘B | `settings-16.png` | `jMenuItem_BaseConfig` |
| Configuration | Interfaces Configuration | *hard-coded* | ⌘I | `settings-16.png` | `jMenuItem_Interfaces` |
| Configuration | Encoder | *hard-coded* | ⌘E | `config-16.png` | `jMenuItem_Encoder` |
| Configuration | General Settings | `General Settings` | ⌘G | `config-16.png` | `jMenuItem_GeneralSettings` |
| Configuration | Remove App Data | `Remove App Data` | ⇧⌘R | `remove-config-16.png` | `jMenuItem_RemoveAppData` |
| Process | Run | *hard-coded* | ⇧⌘X | `process-16.png` | `jMenuItem_Execute` |
| Process | Debug Mode (**check box**, checked by default) | *hard-coded* | ⌘D | `debug.png` | `jCheckBoxItem_Debug` |
| Registration | Register an ifaceX instance | `Register an ifaceX instance` | ⌘R | `register-16.png` | `jMenuItem_Registration` |
| Help | Format JSON | *hard-coded* | ⌘F | `json-16.png` | `jMenuItem_Format` |
| Help | Open Online Documentation | `Open Online Documentation` | F1 | `help-16.png` | `jMenuItem_OpenDoco` |
| Help | Visit Website | `Visit Website` | F2 | `ifaceX16.png` | `jMenuItem_Visit` |
| Help | Check for Updates | `Check for Updates` | ⌘U | `update-16.png` | `jMenuItem_Update` |
| Help | About | `About` | F3 | `about-16.png` | `jMenuItem_About` |

**There is no language menu.** `Bundle_en_US.properties` still holds `ifacex.jMenu6.text=Language`
and `ifacex.jCheckBoxMenuItem_LANG_EN/DE.text`, but the field `jMenu6` is commented out in the
generated layout and the two check-box items are never added to the bar — they are dead code. The
legacy Studio set its language once at start-up (`SwingLocaleChanger`), not at runtime.

### 11.1 What the new Studio does with it

| Legacy item | `-fx` Studio |
| --- | --- |
| File: Open / Open Recent / New / Save / Close / Quit | wired (Quit lives in the macOS app menu) |
| File: Save As | not needed — `Save` always writes a fresh `.ifcx` (there is no "current path" for the new format yet) |
| File: Export .ifcx / .ifacex | **new** items: the YAML format is primary, `.ifacex` is import/export only |
| Configuration: Interface Processor / Base / Interfaces Configuration | the navigation tree + raw views (no separate editors) |
| Configuration: Encoder | `UI.showEncoder(app)` (framework) |
| Configuration: General Settings | open — the legacy pane was the Studio's own `PrefPanel` |
| Configuration: Remove App Data | `UI.showRemovePane(app)` (framework) |
| Process: Run | toolbar `Run` button (placeholder until the engine integration) |
| Process: Debug Mode | open — needs the engine run |
| Registration | open — own top-level menu later, as in the original |
| Help: Format JSON | open — the formatter is not rebuilt yet |
| Help: Open Online Documentation / Visit Website | wired, URLs from `cfg/app.cfg` |
| Help: Check for Updates | `UI.showUpdatePane(app)` (framework) |
| Help: About | `UI.showAboutWithLicense(app)` |
| Help: License | `UI.showLicense(app)` — **new item**; the legacy Studio had the license behind About |
| Language | `Help › Language` (English/Deutsch) — **new**, the legacy had no language menu (see above) |

The accelerators are set in code (`MainController.buildAccelerators()`) with
`KeyCombination.SHORTCUT_DOWN`, so macOS keeps the ⌘ shortcuts of the original without hard-coding
the modifier. `About` deliberately gets no F3 — macOS uses F3 for Mission Control.
