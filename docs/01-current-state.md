# 01 — Current State (As-Is)

Analysis of the existing application in the reference directory
`/Users/Mike/Development/git-ext/autumo-ifacex-studio` (read-only, not modified).

## 1.1 Positioning of ifaceX and the Studio

ifaceX is a platform for data exchange/integration (ETL-like, batch-oriented).
It consists of several components:

| Component | Purpose |
| --- | --- |
| **ifaceX Studio** | Configure/develop IPCs (Interface Processor Configurations) **and run them for testing** |
| ifaceX Server | Scheduler (Quartz), production execution, alarming |
| ifaceX Web | Operations, control, monitoring, roles (Admin/Operator/Controller) |
| Console Tools (CLI) | Command-line execution/crypto |
| Templates | Ready-made IPC templates |

**Important for the rebuild:** The Studio is **not** an operations/monitoring tool.
Its sole purpose is the *configuration* and *test execution* of IPCs. Operations,
monitoring, user management, etc. belong entirely to ifaceX Web/Server and do **not**
need to be rebuilt in the Studio.

## 1.2 Technical Base (As-Is)

| Attribute | Value |
| --- | --- |
| Language / JDK | Java 17 (`<release>17</release>`) |
| Build | Maven, single module |
| Coordinates | `ch.autumo.ifacex.studio:ifaceX-Studio:2.1.0` |
| UI | Java Swing, NetBeans GUI Builder code |
| Look & Feel | FlatLaf Dark + IntelliJ themes, custom color palette |
| Packaging | `jpackage` native installers (`.deb`, `.pkg`, `.exe`, `.msi`) |
| Tests | no test framework (Surefire `skipTests=true`), only manual `main()` helpers |

All dependencies are declared with `<scope>provided</scope>`; at runtime the jars are
collected together (`target/pack`, via `maven-dependency-plugin` +
`maven-resources-plugin`).

## 1.3 Package Structure (As-Is)

```
ch.autumo.ifacex
 ├─ LicIfaceXApp100                       # license application definition
 └─ studio
     ├─ Main / IfaceXStudio / Installer   # entry point & app object
     ├─ Constants / Helper / IfacexConfiguration
     ├─ control/                          # Controller, TaskListener
     ├─ project/                          # Project (1 IPC = 1 project)
     ├─ installer/                        # first-run wizard
     ├─ view/                             # the entire GUI (Swing)
     │   ├─ config/                       # central config editor (12 tabs)
     │   ├─ clients/                      # customer-specific customizations
     │   ├─ event/ / model/               # listeners / view models
     ├─ os/                               # OS-specific helpers
     ├─ elements/ / filter/ / util/ / locale/ / exception/
```

## 1.4 Architecture (As-Is)

Classic desktop MVC with a hand-written controller and a GUI state machine.

- **Model:** `IfacexConfiguration extends Properties` (the IPC), `Project` (project wrapper),
  view models (`EntityFieldsHolder`, `DestinationMappingHolder`, `StringListModel`).
- **View:** `DesignMainFrame` (abstract, generated) → `MainFrame` (concrete). Numerous
  `InternalFrame`/`InternalPanel` sub-windows.
- **Controller:** `control/Controller` (New/Open/Save/SaveAs/Close/Execute/Kill).
- **State machine:** `view/GuiStateMachine` with states
  `STARTED, OPENED, NEW, SAVED, CLOSED, EXECUTION_STARTED, EXECUTION_STOPPED`.

**Interaction with the outside world — exactly two channels:**

1. **External process (core):** `Controller.execute()` launches the *external* ifaceX
   engine via `ProcessBuilder`:
   - Script: `<ifaceXFolder>/bin/run.sh` or `run.bat`
   - Arguments: `config:<path>` `s` `testswitches:ROOTCALL,DEBUG`
   - Working directory: `<ifaceXFolder>/bin`
   - stdout/stderr are merged and streamed line by line to the console
   - `ExternalProcessWatchDog` terminates the process after a timeout
2. **HTTPS to autumo servers** (via `autumo-commons`): license check, update version,
   purchase/documentation URLs.

Plus OS integration: `Desktop.browse/open/edit`, macOS menu handlers, file associations.

## 1.5 The `.ifacex` Data Format (Core)

An IPC is a **`Properties` file** (key-value) with a specially formatted comment header
(title, version, notes, author, date).

Configurable **readers / writers** (prefixes):

| Direction | Types |
| --- | --- |
| Readers | `csv_in`, `json_in`, `doc_in`, `db_in`, `mongo_in`, `rest_in` |
| Writers | `csv_out`, `json_out`, `mail_out`, `db_out`, `mongo_out`, `rest_out`, `code`, `exec` |

Cross-cutting keys: `source_interface`, `destination_interfaces`,
`process_source_entities`. On save, the keys are grouped and sorted
(`store0()` in `IfacexConfiguration`).

**Encryption models** (`ConfigLogicPanel.STR_ENC_MODEL`):
`0: none`, `1: Base64 - SHA3-256 AES`, `2: Base64 - SHA-256 AES`,
`3: Base64 - PBE MD5 DES`, `4: Blowfish`.

## 1.6 The Central Config Editor (As-Is)

Deep inheritance chain spread across several files:

```
JPanel
 └─ ConfigDesignPanel        (generated, ~4950 LOC, 12 tabs)
     └─ ConfigLogicPanel     (state/models/reader-writer, ~1970 LOC)
         └─ ConfigEventPanel (listeners/DnD, ~1390 LOC)
             └─ ConfigLoadPanel  (file → widgets, ~900 LOC)
                 └─ ConfigSavePanel (widgets → file, ~690 LOC)
                     └─ ConfigMainPanel
```

**12 tabs** (`tabbed_pane.addTab(...)`):

`❂ BASE` · `⬇ CSV` · `⬇ JSON` · `⬇ DB` · `⬇ Mongo` · `⬇ REST` ·
`⬆ CSV` · `⬆ JSON` · `⬆ Mail` · `⬆ DB` · `⬆ Mongo` · `⬆ REST`

A mapping of reader/writer name → tab index lives in `readerTabMap`/`writerTabMap`.
Entity/field mapping is done by **drag & drop** (`DNDJList` → `DNDJTextArea`).

## 1.7 Further GUI Features (As-Is)

- **Main window** (`MainFrame`): toolbar (New/Open/Save/Close/Project Settings/Run/Folder),
  three top tabs: **console**, **script editor** (`IPCEditor`), **configuration**.
- **Menus:** File (Open/Recent/New/Save/SaveAs/Close/Quit),
  Configuration (Project Settings, Interface Base Configuration, Interfaces Configuration,
  Encoder, General Settings, Remove App Data),
  Process (Run, Debug Mode), Registration,
  Help (Format JSON, Documentation, Website, Update, About) including language switching.
- **Sub-windows:** About, Preferences, Encoder, JSON formatter, Raw config editor,
  Register, Update, Remove, License.
- **Installer wizard** (`installer/`): license → language → settings → done.
- **License registration** against the autumo license server.
- **Customer customization:** `ClientConfigurator`/`ClientCreaSPA` inject additional menus,
  driven by `client.*` properties in the internal configuration.

## 1.8 Configuration & Settings (As-Is)

- **Internal config** (`/conf/ifacex-studio.cfg`): `url.check`, `url.purchase`,
  `url.website`, `url.documentation`, `url.website.updates`, `url.website.version.file`,
  `email.sales`, `email.support`, `client.*`.
- **User settings** (`ifacex-studio.cfg` in the OS app-data directory):
  `projectRootFolder`, `ifaceXFolder`, `playSounds`, `playJingle`, `seed`, `timeout`,
  `language`, `recent.itemN`, `licenseAccepted`.
- **Project structure:** `interfaces/`, `lic/`, `cfg/base.ifc`, `cfg/interfaces.cfg`,
  `cfg/ifacex.cfg`, `output/`. File extensions: `.ifacex`, `.log`.

## 1.9 Observations / Technical Debt

- **Strong logic↔Swing coupling:** config load/save logic lives inside `JPanel` subclasses.
- **Very large classes** (`ConfigDesignPanel` ~4950 lines) and **deep inheritance**.
- **Lots of copy-paste** across the 12 reader/writer tabs (no data-driven definition).
- **No automated tests** (only manual `main()` helpers).
- **Color/layout constants in Java** instead of a theme/config file.
- **`provided` scope everywhere** — the runtime classpath is assembled manually.

These points are precisely the reasons that justify a structured rebuild.
