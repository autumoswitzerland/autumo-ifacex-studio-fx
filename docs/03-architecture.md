# 03 — Target Architecture

## 3.1 Principles

1. **Strict separation of concerns:** domain/infrastructure logic has **no** UI dependency.
2. **Data-driven configuration UI:** the reader/writer editors are generated from
   metadata, not hand-copied per tab.
3. **Thin, testable core:** file I/O, config model, process launching, crypto/license
   are plain Java and unit-testable without a UI.
4. **Reuse the existing Java logic** wherever it is UI-independent.
5. **Single responsibility per class**, avoiding the deep inheritance chains of the
   current implementation.

## 3.2 Module Structure (target)

A multi-module Maven build:

```
autumo-ifacex-studio-fx/
├── pom.xml                     (parent / aggregator)
├── core/                       # UI-independent domain + infrastructure (pure Java)
├── ui/                         # JavaFX application (depends on core)
├── docs/                       # this planning documentation
└── product/                    # packaging assets (jpackage, icons, licenses)
```

### `core` module (no JavaFX, no Swing)

Carries over / reimplements the UI-free logic from the reference project:

| Responsibility | Origin in reference |
| --- | --- |
| IPC model (properties, load/save, comment header, grouping/sorting) | `IfacexConfiguration` |
| Project wrapper | `project/Project` |
| Process launching + output streaming + watchdog | `control/Controller` (extracted, UI-agnostic) |
| Settings model (app-data properties) | `IfacexStudio`, `Constants` (keys) |
| Crypto helper | `util` + `CryptUtils` usage (autumo-commons) |
| License handling | `LicIfaceXApp100`, autumo-registration |
| Update check | version-file fetch via HttpClient |
| Internal config access | internal `ifacex-studio.cfg` |
| Reader/writer metadata (tabs, fields, validation rules) | extracted from `Config*Panel` |
| Templates / file layout constants | `Constants` |
| OS helpers | `os/OSHelper`, `os/FileHandler` (non-UI parts) |
| Launcher command-line handling (which config to open; `--open` / bare path for double-click) | new (`cli/CommandLine`) |

### `ui` module (JavaFX)

| Responsibility | Notes |
| --- | --- |
| Application shell (menus, toolbar, tabs, status bar) | replaces `MainFrame`/`DesignMainFrame` |
| Console view with severity highlighting | replaces `ConsoleOutputPane` |
| Script/config raw editor | replaces `IPCEditor` (RSyntaxTextArea) → JavaFX code editor |
| Config editor (BASE + reader/writer tabs) | data-driven; replaces `Config*Panel` chain |
| i18n: gettext PO catalogue (`lang/messages.<lang>.po`) + a thin facade over the autumo-ui framework bundle | the PO reader itself is a framework class, see doc 12 |
| Drag & drop field mapping | replaces `DNDJList`/`DNDJTextArea` |
| Dialogs (Settings, Encoder, JSON, Update, Register, About, Remove, License) | replaces the `InternalPanel`s |
| Installer wizard | replaces `installer/*` |
| GUI state machine | replaces `GuiStateMachine` |

## 3.3 Layering

```
        ┌───────────────────────────┐
        │          ui (JavaFX)       │
        │  views / view-models / FSM │
        └──────────────┬────────────┘
                       │ uses
        ┌──────────────▼────────────┐
        │          core (Java)       │
        │  model / io / process /    │
        │  crypto / license / cli    │
        └──────────────┬────────────┘
                       │ uses
        ┌──────────────▼────────────┐
        │  third-party libraries     │
        │  autumo-commons, gson,     │
        │  httpclient, log4j2, ...   │
        └────────────────────────────┘
```

## 3.4 Process Execution Model (JavaFX)

The core exposes a UI-agnostic process runner:

- `core`: `ProcessRunner` starts `bin/run.sh|run.bat` with the same arguments and
  working directory as today, merges stderr into stdout, and exposes output as a
  stream/callback plus a timeout watchdog callback.
- `ui`: consumes the stream on a **background thread** and marshals lines to the JavaFX
  thread via `Platform.runLater(...)`, appending to the console view.
- Kill/timeout is surfaced as an event that updates the UI state machine.

No UI types leak into `core`.

## 3.5 GUI State Machine

Reimplement `GuiStateMachine` as a UI-layer state model with the same semantic states
(`STARTED, OPENED, NEW, SAVED, CLOSED, EXECUTION_STARTED, EXECUTION_STOPPED`), driving
enablement of menu items/toolbar buttons. Prefer JavaFX properties/bindings over manual
`setEnabled` calls.

## 3.6 Data-Driven Config Editor

Instead of five inherited panel classes and 12 hand-built tabs, describe each reader/
writer with metadata:

```
ReaderWriterDescriptor {
    String id;              // e.g. "csv_in"
    String label;           // e.g. "CSV"
    Direction direction;    // READER | WRITER
    List<FieldDescriptor> fields;
}

FieldDescriptor {
    String key;             // e.g. "csv_in_encoding"
    FieldType type;         // TEXT, PASSWORD, NUMBER, COMBO, CHECKBOX, FOLDER, FILE
    String i18nLabelKey;
    String validationRegex; // from Constants
    boolean encryptable;
}
```

The config editor builds its tabs and fields from these descriptors at runtime. Adding a
reader/writer becomes a metadata entry, not a new panel class. A generic binder maps
descriptor ↔ `IfacexConfiguration` keys.

## 3.7 Persistence & Compatibility

- The **in-memory model is the single source of truth**. It has three serializers:
  1. **YAML reader/writer** — the new authoring format (load/save).
  2. **Properties reader** — import legacy `.ifacex` files (conversion on load).
  3. **Properties writer** — export an `.ifacex` for the external engine when running a
     test IPC, until the engine natively supports the new format.
- The properties writer must reproduce the engine's exact conventions (entity scoping,
  `*` wildcard, `mapping_field_NNN` ordering, `{mode:...}` secrets, base `cfg/ifacex.cfg`
  merge). See [09-engine-config-model.md](09-engine-config-model.md).
- User settings continue to live in the OS app-data directory; a settings-migration
  step may be needed (see open questions).
- See [08-config-format.md](08-config-format.md) for the format decision and versioning.

## 3.8 Packaging

- `jpackage`/`jlink` as today, but the module set changes (JavaFX modules instead of
  Swing/AWT-only). Final runtime image includes OpenJFX.
- `product/` retains per-OS packaging assets; CI workflows adapted accordingly.
