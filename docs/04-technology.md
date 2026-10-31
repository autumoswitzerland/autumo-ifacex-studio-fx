# 04 — Technology & Libraries

## 4.1 Core Stack

| Area | Choice | Notes |
| --- | --- | --- |
| Language | **Java 25** (latest LTS; Azul Zulu builds) | current LTS; JavaFX 25 accordingly |
| UI toolkit | **JavaFX (OpenJFX) 25** | CSS theming, scenes, bindings; no Chromium |
| Build | **Gradle** (multi-module) | like the reference (autumo-ui-demo); jpackage via badass-runtime |
| UI markup | **Hybrid**: FXML for the static shell + programmatic (metadata-driven) forms | decided 2026-10-16; see doc 07 §7.3 and doc 10 |
| App shell / framework | **autumo-ui 1.2.0** (`ch.autumo.commons:autumo-ui`) | lifecycle, first-run installer, licensing/registration, i18n, theming, recent files, update check; **installer active, not portable**: the wizard writes the user `cfg/app.cfg` (see doc 07 §7.5). Baseline **Java 25 + JavaFX 25** (framework bumped, D12) |
| Packaging | `jpackage` / `jlink` | as in the reference |

> **UI foundation (D12, 2026-10-16):** the Studio is built **on top of the autumo-ui framework**
> (itself extracted from the legacy ifaceX Studio Swing code). The framework supplies the
> *application shell and product concerns*; the Studio-specific editor UI and the UI-free
> `core` module remain our own. The framework may be **extended/improved** where it is not well
> implemented (owner-authorized). See doc 07 §7.5. (The original `spike/` module that validated
> this has been removed — its result is the `ui` module.)

## 4.2 Library Mapping (Swing → JavaFX)

### UI libraries that must be replaced

| Reference (Swing) | Purpose | JavaFX replacement |
| --- | --- | --- |
| Swing (`javax.swing`) | whole UI | JavaFX |
| FlatLaf (+ IntelliJ themes) | dark look & feel | JavaFX CSS theme (custom stylesheet) |
| RSyntaxTextArea / RTextScrollPane | code/config editing with syntax highlighting | **kept** — autumo-ui `DefaultCodeEditorView` (RSyntaxTextArea in a `SwingNode`, theme `cfg/edit.xml`) |
| JGoodies Forms | form layouts | JavaFX layout panes (GridPane, etc.) |
| JGoodies Validation | form validation | JavaFX validation + optional **ControlsFX** |
| AbsoluteLayout | absolute positioning | JavaFX layout (avoid absolute positioning) |
| NetBeans GUI Builder output | generated UI | FXML shell + programmatic JavaFX forms |

### Non-UI libraries that carry over

| Reference library | Purpose | Kept? |
| --- | --- | --- |
| `ch.autumo.commons:autumo-ui` | rapid JavaFX framework (app shell, installer, licensing UI, i18n, theming) | ✅ **adopted** (D12) |
| `ch.autumo.commons:autumo-commons` | utils, system, license helpers, crypto usage | ✅ keep (transitive via autumo-ui) |
| `ch.autumo.registration:autumo-registration` | registration/license | ✅ keep (transitive via autumo-ui) |
| Gson (`com.google.code.gson`) | JSON formatting | ✅ keep |
| org.json | JSON handling | ✅ keep |
| Apache HttpClient / httpcore / httpasyncclient / httpmime | HTTPS to autumo servers | ✅ keep |
| Log4j2 (api/core/slf4j-impl) | logging | ✅ keep |
| Apache Commons (lang3, text, codec, compiler) | utilities | ✅ keep |
| Janino | runtime compilation (if needed) | evaluate — keep if used |
| Commons Codec | encoding | ✅ keep |

> Note: Some non-UI libraries are only used by UI code today. The migration must verify
> actual usage in `core` before carrying a dependency over.

## 4.3 New / Additional Libraries (candidates)

| Need | Candidate | Notes |
| --- | --- | --- |
| Code editor | RSyntaxTextArea (`com.fifesoft:rsyntaxtextarea`) via autumo-ui `DefaultCodeEditorView` | the legacy editor itself, embedded with a `SwingNode`; RichTextFX was dropped (caret colour, border/divider, highlighting) |
| Extra UI controls | ControlsFX | validation decoration, notifications, dialogs |
| Unit tests | JUnit 5 | replaces the missing test framework |
| UI tests | JUnit on the JavaFX thread | no TestFX; `StudioUiTest` builds the real shell (see 07 §7.5, 2026-10-18) |
| Settings | plain `Properties` (as today) | keeps compatibility with existing files |

Candidates must be confirmed before adoption (see open questions).

## 4.4 Build & Runtime

- Multi-module Gradle: `core`, `ui`, aggregator root.
- **autumo-ui dependency:** built with **Gradle 9.7 + JDK 25** (Java 25 / JavaFX 25 toolchain) and
  published to `mavenLocal` (`./gradlew publishToMavenLocal`); the Studio consumes
  `ch.autumo.commons:autumo-ui:1.2.0` as a normal dependency (see the validated
  integration in the `ui` module and doc 07 §7.3). `autumo-commons` comes in transitively
  (compile scope); the proprietary `autumo-registration` is declared **explicitly** by the
  Studio (autumo-ui deliberately does not expose it).
- **Dependency scope:** unlike the reference (everything `provided`), prefer normal
  compile/runtime scopes and let `jpackage`/`jlink` assemble the runtime. Revisit the
  `provided` approach deliberately (see open questions).
- **Runtime modules:** include JavaFX modules (`javafx.controls`, `javafx.fxml`,
  `javafx.web`, `javafx.graphics`, etc.); `java.desktop` may no longer be required except for
  `Desktop`-based OS integration. `javafx.web` is required because autumo-ui renders its
  About/license panes in a `WebView`.
- **Java options:** carry over `-Dfile.encoding=UTF-8 -Xms512m` as a starting point.

## 4.5 Testing Strategy

- `core`: JUnit 5 unit tests for config load/save round-trip, key grouping/sorting,
  process-runner argument construction, crypto/encoder, settings.
- `ui`: TestFX smoke tests for startup, opening/binding a config, and run-console flow.
- CI: Gradle build + tests (replaces the current `skipTests=true`).

## 4.6 Continuous Integration

- Adapt the reference GitHub Actions (`.github/workflows/`) for the new module layout — **done**: `.github/workflows/build.yml` (build + tests on every OS; native packages on `workflow_dispatch` with `package=true`).
- Multi-OS matrix (Windows/macOS/Linux) for `jpackage` release artifacts.

## 4.7 Explicit Non-Choices

- ❌ Electron (and any bundled Chromium) — hard requirement against it.
- ❌ Web frontend / server backend for the Studio.
- ❌ Kotlin — Java is retained intentionally for reuse and team familiarity.
