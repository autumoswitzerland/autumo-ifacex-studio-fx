# 06 — Roadmap

Phased plan. Each phase ends with a clear deliverable. Status (2026-10-31): phases 0–6 are
implemented; phase 7 (packaging/CI) is wired but not yet shipped.

## Phase 0 — Proof of Concept (validate the approach)

**Goal:** prove JavaFX works against the real IPC model with minimal effort.

- Set up the multi-module skeleton (`core`, `ui`, parent).
- `core`: take over `IfacexConfiguration` load/save (round-trip on a real `.ifacex`).
- `ui`: a minimal JavaFX window that loads an `.ifacex` and displays key-values in a
  table, then saves it back unchanged.

**Deliverable:** load → display → save round-trip demo; go/no-go on the tech choice.

## Phase 1 — Core Extraction

- Move/extract UI-independent logic into `core`: project wrapper, settings model,
  process runner (arguments, work dir, stream, watchdog), crypto/encoder, license,
  update check, i18n bundles, reader/writer metadata.
- Add JUnit 5 tests for the core (config round-trip, process args, encoder).

**Deliverable:** tested, UI-free `core` module.

## Phase 2 — Application Shell

- JavaFX main window: menu bar, toolbar, status bar, top tabs (console/editor/config).
- GUI state machine + enablement bindings.
- Dark theme (JavaFX CSS), i18n wiring (EN/DE, runtime switch).

**Deliverable:** empty-but-navigable modern shell with the same menu/functions surface.

## Phase 3 — Configuration Editor (main effort)

- Data-driven reader/writer descriptors + generic binder.
- BASE tab and all reader/writer tabs.
- Source-entity management.
- Drag & drop field mapping.
- Field validation and encryption combos.

**Deliverable:** full IPC editing feature parity.

## Phase 4 — Execution & Console

- Run test IPC (external engine), console streaming with severity highlighting.
- Debug toggle, timeout watchdog, kill, post-run JSON formatting.

**Deliverable:** working "configure → run → inspect output" loop.

## Phase 5 — Tools & Dialogs

- Script/config raw editors (`DefaultCodeEditorView` — RSyntaxTextArea embedded via `SwingNode`).
- JSON formatter, Encoder, Preferences, Update, About, Remove, License, Register.

**Deliverable:** complete tool set.

## Phase 6 — Installer, License, Customization

- First-run wizard. *(Wired 2026-10-18: *About/Terms* → **General Settings** → *Done*, run by the
  framework because the Studio is not portable; see doc 07 §7.5.)*
- License acceptance/registration flow.
- Customer customization hook (if kept).

**Deliverable:** end-to-end first-run experience.

## Phase 7 — Packaging & Release

- `jpackage`/`jlink` for Windows/macOS/Linux.
- Register `.ifcx` / `.ifacex` **file associations** so a double-click opens the Studio
  (descriptors ready in `product/packaging/file-associations/`); app icon + bundle identity.
- CI workflows (build + tests + release matrix).

**Deliverable:** installable artifacts per OS.

## Cross-Cutting Workstreams

- **Testing:** grows with each phase (core unit tests from Phase 1; UI smoke tests later).
- **Documentation:** user-facing docs/README for the new app.
- **Compatibility checks:** continuously verify generated `.ifacex` matches the reference.

## Milestone Overview

| Milestone | Marker |
| --- | --- |
| M0 | PoC round-trip succeeds |
| M1 | Core module tested & UI-free |
| M2 | Shell runs with full menu surface |
| M3 | Config editor at feature parity |
| M4 | Run/console loop works |
| M5 | All tools present |
| M6 | First-run/installer complete |
| M7 | Native installers produced |

Estimation, staffing, and dates are intentionally omitted pending the open questions in
document 07.
