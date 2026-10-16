# 05 — Feature Inventory & Migration

Migration status legend:
`reuse` = take over from reference with minimal change ·
`extract` = logic exists but must be decoupled from UI ·
`rewrite` = new implementation in JavaFX ·
`new` = not present before.

## 5.1 Application Shell

| Feature | Reference | Migration |
| --- | --- | --- |
| Main window, menu bar, toolbar, status bar | `MainFrame`, `DesignMainFrame` | `rewrite` |
| Top tabs: console / script editor / config | `MainFrame` | `rewrite` |
| GUI state machine | `GuiStateMachine` | `extract` |
| Dark theme | FlatLaf + `Constants` colors | `rewrite` (JavaFX CSS) |
| i18n (EN/DE, runtime switch) | `Bundle`, `SwingLocaleChanger` | `extract` + `rewrite` (bindings) |
| Sounds (jingle, finish ok/nok) | `Helper.playFinishSound` | `extract` (decision: keep?) |

## 5.2 Project / File Lifecycle

| Feature | Reference | Migration |
| --- | --- | --- |
| IPC model, load/save, comment header | `IfacexConfiguration` | `reuse` |
| Project wrapper (path, title, saved state) | `project/Project` | `extract` |
| New / Open / Save / Save-As / Close | `Controller` | `extract` + `rewrite` dialogs |
| Recent files (max 10) | `IfaceXStudio.addRecent*` | `extract` |
| Manual-edit merge into config | `Project.saveFromManualEdit` | `extract` |

## 5.3 Configuration Editor (core feature)

Reference: `ConfigDesignPanel` → `ConfigLogicPanel` → `ConfigEventPanel` →
`ConfigLoadPanel` → `ConfigSavePanel` → `ConfigMainPanel`.

**Target:** one data-driven editor built from reader/writer descriptors (see
architecture doc 3.6). All tabs are `rewrite`.

| Tab | Key prefix | Direction |
| --- | --- | --- |
| BASE | `source_interface`, `destination_interfaces`, `process_source_entities`, `meta_*` | — |
| CSV | `csv_in` / `csv_out` | R / W |
| JSON | `json_in` / `json_out` | R / W |
| DB | `db_in` / `db_out` | R / W |
| Mongo | `mongo_in` / `mongo_out` | R / W |
| REST | `rest_in` / `rest_out` | R / W |
| Mail | `mail_out` | W |

Also: `doc_in`, `code`, `exec` prefixes exist in the format (may have limited/no dedicated
tab today — verify against reference).

| Sub-feature | Reference | Migration |
| --- | --- | --- |
| Reader/writer selection (combo + multi-select writers) | `ConfigLogicPanel` | `rewrite` |
| Tab enable/disable by selection | `readerTabMap`/`writerTabMap` | `extract` (metadata) |
| Source-entity list (add/remove/up/down) | `ConfigEventPanel` | `rewrite` |
| Field mapping drag & drop | `DNDJList`/`DNDJTextArea` | `rewrite` |
| Encryption model combos | `STR_ENC_MODEL` | `extract` |
| Field validation (regex per field) | `Constants.REGEX_*` | `extract` |
| Load config → widgets | `ConfigLoadPanel` | `extract` + `rewrite` |
| Widgets → config | `ConfigSavePanel` | `extract` + `rewrite` |

## 5.4 Execution / Testing

| Feature | Reference | Migration |
| --- | --- | --- |
| Launch external engine (`run.sh`/`run.bat`) | `Controller.execute` | `extract` |
| Test switches | `addTestSwitch` (`ROOTCALL`, `DEBUG`) | `extract` |
| Working directory & args | `Controller` | `extract` |
| Output streaming to console | `MainFrame.addConsoleOutput` | `extract` + `rewrite` |
| Severity highlighting (WARN/ERROR/FATAL) | `MainFrame`, patterns | `extract` + `rewrite` |
| Timeout watchdog / kill | `ExternalProcessWatchDog` | `extract` |
| Debug mode toggle | `MainFrame.isDebugMode` | `extract` |
| Post-run JSON formatting of root call | `MainFrame.formatRootCall` | `extract` |

## 5.5 Tools / Dialogs

| Feature | Reference | Migration |
| --- | --- | --- |
| Raw config editor (base/interfaces) | `ConfigPanel` | `rewrite` |
| Script / IPC editor | `IPCEditor` | `rewrite` (RichTextFX) |
| JSON formatter | `JSONPanel` | `rewrite` (Gson `reuse`) |
| Encoder | `EncoderPanel` | `rewrite` |
| Preferences / general settings | `PrefPanel` | `rewrite` |
| Update check | `UpdatePanel` | `rewrite` (HttpClient `reuse`) |
| About | `AboutPanel` | `rewrite` |
| Remove app data | `RemovePanel` | `rewrite` |
| License view | `LicensePanel` | `rewrite` |
| Registration | `RegisterPanel` | `rewrite` |
| OS file open/edit | `os/FileHandler` | `extract` |

## 5.6 Installer & License

| Feature | Reference | Migration |
| --- | --- | --- |
| First-run wizard (license→lang→settings→done) | `installer/*` | `rewrite` |
| License acceptance flag | `licenseAccepted` | `extract` |
| License server check | `LicenseUtils` (autumo-commons) | `reuse` |
| Registration data handling | `RegisterPanel`, `Registration.*` | `extract` |

## 5.7 Customization

| Feature | Reference | Migration |
| --- | --- | --- |
| Customer extension mechanism (plugin-style: extra menus/toolbar actions/dialogs) | `ClientConfigurator`, `ClientCreaSPA`, `BaseClient` (hardcoded) | `rewrite` as a **generic plugin API** |
| Merged internal config | `/conf/ifacex-studio.cfg` (+ creaspa variant) | `reuse` |

## 5.8 Summary Counts (rough)

- `reuse`: IPC model, license server check, Gson/HttpClient usage, internal config.
- `extract`: project wrapper, controller logic, process runner, i18n, encoder models,
  validation regexes, settings keys, GUI state semantics.
- `rewrite`: entire view layer, config editor, dialogs, installer, theme.

The bulk of engineering effort is in `rewrite` of the **config editor** and the
**application shell**; the domain/infrastructure `reuse`/`extract` is comparatively small
but high-value.
