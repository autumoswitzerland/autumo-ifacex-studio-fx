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
| Menu structure and wording (`File`, `Configuration`, `Help`, legacy item order, ⌘-accelerators) | `DesignMainFrame.jMenu1…jMenu6`, `Bundle_en_US.properties` | `rewrite` — only items that have a real implementation; framework panes come from autumo-ui, the rest follows (see doc 11 §8) |
| Top tabs: console / script editor / config | `MainFrame` | `rewrite` |
| GUI state machine | `GuiStateMachine` | `extract` |
| Dark theme | FlatLaf + `Constants` colors | `rewrite` (JavaFX CSS) |
| i18n (EN/DE, runtime switch) | `Bundle`, `SwingLocaleChanger` | `rewrite` as gettext PO (msgid = English text) plus the autumo-ui bundle; the PO reader is part of the framework (doc 12) |
| Sounds (jingle, finish ok/nok) | `Helper.playFinishSound` | `extract` (decision: keep?) |

## 5.2 Project / File Lifecycle

| Feature | Reference | Migration |
| --- | --- | --- |
| IPC model, load/save, comment header | `IfacexConfiguration` | `reuse` |
| Project wrapper (path, title, saved state) | `project/Project` | `extract` |
| New / Open / Save / Save-As / Close | `Controller` | `extract` + `rewrite` dialogs |
| Recent files (max 10) | `IfaceXStudio.addRecent*`, `MainFrame.rebuildRecentList` | `reuse` (autumo-ui `BaseController.initializeRecentMenu` / `rebuildRecentList`, list kept and persisted by `BaseApp`) |
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
| Script / IPC editor | `IPCEditor` | `reuse` (autumo-ui `DefaultCodeEditorView`, RSyntaxTextArea 1:1) |
| JSON formatter | `JSONPanel` | `rewrite` (Gson `reuse`) |
| Encoder | `EncoderPanel` | `autumo-ui` (`UI.showEncoder`; the framework uses the application's internal key instead of the legacy 32-hex secret) |
| Preferences / general settings | `PrefPanel` | `rewrite` (open) |
| Update check | `UpdatePanel` | `autumo-ui` (`UI.showUpdatePane`; needs a real `url.website.version.file`) |
| About | `AboutPanel` | `autumo-ui` (`UI.showAboutWithLicense`, `html/about_<lang>.html`) |
| Remove app data | `RemovePanel` | `autumo-ui` (`UI.showRemovePane`) — `Configuration › Remove App Data`; the pane's HTML is the **framework default** (`ch/autumo/ui/html/remove_<lang>.html`), the Studio ships none |
| License view | `LicensePanel` | `autumo-ui` (`UI.showLicense`, `html/license_<lang>.html`) — no menu item; shown by the **License** button of the About dialog (`UI.showAboutWithLicense`), as in the legacy Studio |
| Open online documentation / visit website | `jMenuItem_OpenDoco`, `jMenuItem_Visit` | `reuse` (browser via `Helper.openURL`, URLs from `cfg/app.cfg`) |
| Registration | `RegisterPanel` | `rewrite` (open — own top-level menu, as in the original) |
| OS file open/edit | `os/FileHandler` | `extract` |

## 5.6 Installer & License

| Feature | Reference | Migration |
| --- | --- | --- |
| First-run wizard (license→lang→settings→done) | `installer/*` | `rewrite` — **done** (2026-10-18): framework steps *About/Terms* → `StepSettings` → *Done*; settings = `project.root`, `play.sounds`, `play.jingle` (no consumer in the FX Studio yet). The legacy *language* step is dropped — the language follows the desktop and is switched at runtime (`Help › Language`). License acceptance is part of the first step |
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
