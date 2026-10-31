# 02 — Scope

## 2.1 Goal

Provide ifaceX Studio as a **modern JavaFX desktop application** that lets users
**configure and test** Interface Processor Configurations (IPCs) — functionally
equivalent to the existing Swing Studio, but on a modern, maintainable code base and
with a **new, structured configuration format**.

The application remains a **standalone desktop tool**. It is not a web app and does
not connect to ifaceX Server/Web for operations.

## 2.2 In Scope

- Create, open, save, save-as, and close IPC configurations.
- **New structured configuration format** (replaces the current Java-Properties
  `.ifacex`). See [08-config-format.md](08-config-format.md).
- **Automatic conversion on load:** when a legacy properties file is opened, it is
  converted to the new format, with a clear notice to the user.
- Recent-files list (max 10).
- Visual configuration editor for **readers, writers, entities, and field mapping**,
  including drag & drop mapping.
- Raw editing of the project's configuration.
- Raw editing of ifaceX base/interfaces configuration (`cfg/base.ifc`, `cfg/interfaces.cfg`).
- **Run** a test IPC by launching the external ifaceX engine and streaming its console
  output (with severity highlighting), including a timeout watchdog and kill.
- Debug mode toggle.
- JSON formatting helper.
- Encoder helper for sensitive values (AES/PBE/Blowfish variants).
- Update check against the configured remote version file.
- License registration flow.
- First-run installer wizard (license → language → settings → done).
- Preferences / general settings.
- Internationalization (English, German) with runtime switching.
- Dark theme.
- **Sound feedback** (jingle, finish ok/nok) — retained.
- **Customer extension mechanism:** dropped by decision (2026-10-31) — no plugin API is
  shipped; customer-specific menus stay out of scope.
- Native packaging (Windows, macOS, Linux) via `jpackage`.

## 2.3 Out of Scope

- Any server/web backend or REST/GraphQL API for the Studio itself.
- ifaceX operations/monitoring/alerting (belongs to ifaceX Web).
- Scheduler functionality.
- User/role management.
- Reimplementing the ifaceX engine (the Studio only *launches* the external engine).
- Multi-user / concurrent editing.
- **Backward-compatible writing of the legacy properties format.** Old files are only
  *read* (for conversion); the new format is written from then on.
- Native support of the new format by the ifaceX engine — handled later, outside this scope.

## 2.4 Constraints

| Constraint | Detail |
| --- | --- |
| Reference code | read-only source of truth for behavior; **may be copied/extracted** (owner-authorized) |
| Target directory | `/Users/Mike/Development/git-ext/autumo-ifacex-studio-fx` |
| Language | Java (no Kotlin) |
| UI toolkit | JavaFX |
| JDK | **Java 25** — latest LTS, Azul Zulu builds |
| Version control | fresh local git repo, **no remote** |
| No Electron / no bundled Chromium | hard requirement |
| Reuse | maximize reuse of non-UI Java logic from the reference project |

## 2.5 Non-Functional Requirements

- **Maintainability:** logic separated from UI; testable core; no giant GUI classes.
- **Data-driven config UI:** reader/writer editors described by metadata, not copy-paste.
- **Testability:** automated unit tests for the core; UI smoke tests where practical.
- **Schema-validated config:** the new format is versioned and validated.
- **Cross-platform:** Windows, macOS, Linux.
- **Performance:** responsive UI while an external process streams output.
- **Accessibility/UX:** keyboard navigation, clear dark theme, sensible defaults.

## 2.6 Configuration Format & Migration

- The Studio **defines the new format** from day one (see
  [08-config-format.md](08-config-format.md)); it is versioned so it can evolve.
- **Loading a legacy `.ifacex` file triggers a conversion** to the new format and shows
  a notice. After conversion, saving uses the new format.
- No requirement to keep writing the old format; the engine's native support for the
  new format is a **later, separate task**.
- Existing settings handling and the internal app configuration may be revisited as part
  of this change (see open questions).

## 2.7 Open Scope Decisions

See [07-risks-open-questions.md](07-risks-open-questions.md). Notably the exact choice of
the new serialization format (JSON vs. YAML vs. other), the shape of the plugin API, and
whether existing settings are migrated.
