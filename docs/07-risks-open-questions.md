# 07 — Risks & Open Questions

## 7.1 Risks

| # | Risk | Impact | Mitigation |
| --- | --- | --- | --- |
| R1 | Logic↔Swing coupling in the reference is deeper than expected | Extraction slower than planned | Phase 0/1 prove extraction early; timebox |
| R2 | New config format (JSON/YAML) is chosen without matching engine semantics | Configs not consumable later / rework | Verify against engine; keep schema evolvable via `formatVersion` |
| R3 | Legacy conversion loses information or reorders/unwraps values incorrectly | Broken configs, silent data loss | Golden-file conversion tests (legacy → new → compare semantics) |
| R4 | JavaFX learning curve for a Swing-experienced team | Slower UI work | Start with PoC; small JavaFX pilot; reuse patterns |
| R5 | JavaFX is not bundled with the JDK | Build/packaging complexity | `jlink`/`jpackage` set up in Phase 0/7; document it |
| R6 | Third-party UI libs (RSyntaxTextArea, JGoodies, FlatLaf) cannot move over | Rewrite effort | Mapping defined (doc 04); use RichTextFX/ControlsFX |
| R7 | Hidden features only reachable via OS-specific code paths | Missing functionality | Inventory macOS/Windows/Linux branches explicitly |
| R8 | Behavior drift vs. reference | User confusion | Keep reference accessible; compare behavior per feature |
| R9 | Scope creep toward web/monitoring features | Oversized project | Scope document (doc 02) explicitly excludes them |
| R10 | Plugin API designed too narrowly (customer needs vary) | Rework / unmet customer needs | Design a general extension point (menus/actions/dialogs/services) |

*(R10 and R11 regarding licensing are resolved: the owner authorizes code reuse and there
is no external remote.)*

## 7.2 Resolved Decisions

| # | Decision |
| --- | --- |
| D1 | Reference project is read-only; new app built in `autumo-ifacex-studio-fx` |
| D2 | Java + JavaFX; no web app; no Electron |
| D3 | Planning documentation in English under `docs/` |
| D4 | **Java 25** (latest LTS, Azul Zulu builds); JavaFX 25 |
| D5 | **Code reuse authorized** — source may be copied/extracted (owner is the rights holder) |
| D6 | **Sound feature retained** (jingle, finish ok/nok) |
| D7 | **CreaSPA-specific hook dropped**, replaced by a **generic, plugin-style extension mechanism** for customer menus/functions |
| D8 | **New structured config format** from day one; legacy properties files are **converted on load with a notice**; engine support later |
| D8a | **Format = YAML** (owner decision); guardrails: YAML 1.2 + safe parser, canonical writer, schema validation |
| D8b | **Dual serialization required:** the engine reads only properties → Studio exports properties for test runs (properties = execution format, YAML = authoring format) |
| D8c | **Engine and templates are read-only references** (`repository/autumo-ifacex`, `git-ext/autumo-ifacex-templates`) |
| D8d | **Generated comment header + section comments** are retained in the new YAML format (requires a custom deterministic emitter). Hand-written comments are **not** preserved (already the case today) — accepted |
| D8e | **Legacy `.ifacex` export** offered as an explicit feature (and used internally for engine test runs) |
| D8f | **File extension:** new YAML format = **`.ifcx`**; `.ifacex` remains for legacy properties |
| D8g | **Base/general config** (`cfg/ifacex.cfg`) gets a **YAML version** too; engine merge stays, properties export retained |
| D8h | **Secrets = structured `{enc, value}`** (Option B), plain values stay plain strings; console encoder gets a `format` option (`properties\|yaml`) |
| D8i | **YAML reading:** SnakeYAML Engine (YAML 1.2, safe); **writing:** custom deterministic emitter; validation lightweight first |
| D8j | **Base/general config** (`ifacex.cfg` / new `ifacex-base.ifcx`) lives in the **test engine's file tree**, is **raw-edited** (not form-generated), and is **per test engine** (required for runs and GUI licensing) |
| D8k | **Reader/writer forms** get a **modern, user-friendly, extensible design** (new fields must be addable without redesign) — see doc 10 |
| D8l | **All current fields are kept** (they are the current standard); grouping/collapsing is presentation only |
| D8m | **Form view + raw view** both retained (the raw view already exists today) |
| D8n | **External field descriptors** (customer-extendable) = **later step**; PoC uses an in-code registry |
| D9 | Fresh **local git repo, no remote** |
| D10 | **Default branch is `master`** |
| D11 | Standard `.gitignore` in place |
| D12 | **UI foundation = the autumo-ui JavaFX framework** (`ch.autumo.commons:autumo-ui:1.2.0`). We build on it for the *application shell / product concerns* (lifecycle, installer, config/log dirs, i18n, theming, recent files, **licensing/registration**, update check). The Studio-specific editor UI and the **UI-free `core`** remain our own code. The framework may be **extended/improved** when something is not well implemented (owner-authorized). Development/PoC runs in the framework's **portable mode** (no installer/licensing). Baseline is **Java 25 + JavaFX 25**; autumo-ui itself was **bumped to Java 25 + JavaFX 25** (Gradle wrapper → 9.7, Java toolchain 25). |

## 7.3 Open Questions

### Config format (see docs 08 & 09)
1. ~~JSON vs. YAML~~ → **YAML chosen** (resolved).
2. ~~File extension~~ → **`.ifcx`** (resolved).
3. ~~Schema tooling~~ → **resolved:** SnakeYAML Engine + custom deterministic emitter + lightweight validation (D8i).
4. ~~Secret representation~~ → **resolved:** structured `{enc, value}` (Option B, D8h).
5. ~~Base config file name~~ → **resolved:** `ifacex-base.ifcx` (new) / `ifacex.cfg` (legacy),
   inside the test engine's tree (see 8.12).
6. ~~Properties export required~~ → **yes** (resolved; until the engine supports the new format).

### Plugin / extension mechanism
5. What exactly should plugins be able to contribute (menus, toolbar actions, dialogs,
   whole views, background services)?
6. Discovery mechanism: `ServiceLoader`, a plugin folder scanned at startup, or both?
7. API stability/versioning of the plugin interface.

### PoC & engine access
8. ~~Developer read access to ifaceX~~ → **granted** (engine + templates as read-only references).
9. Scope of the PoC: **load legacy `.ifacex` → convert → display in JavaFX → save in new
   format** (extended to include conversion)?

### Technical
10. ~~FXML vs. pure Java UI style?~~ → **resolved:** *hybrid*. FXML declares the static
    shell (window, menu bar, toolbar, tab pane, status bar); the metadata-driven
    reader/writer forms are built **programmatically** in Java, because they are generated
    from field descriptors and change with the configuration. See doc 10 §10.9.
10a. ~~Standalone JavaFX vs. the autumo-ui framework?~~ → **resolved:** build on **autumo-ui**
    (D12). App shell/product concerns come from the framework; the Studio editor UI + `core`
    stay ours.
11. ~~Build tool~~ → **resolved:** the Studio stays **Maven**; **autumo-ui** stays a **Gradle**
    project, built and published to `mavenLocal`, and consumed as a normal Maven binary
    (`ch.autumo.commons:autumo-ui:1.2.0`).
11a. ~~autumo-ui baseline~~ → **resolved:** bumped autumo-ui to **Java 25 + JavaFX 25**
    (Gradle wrapper → 9.7, Java toolchain 25) for a single baseline across products; the Studio
    excludes autumo-ui's JavaFX and pins its own 25.x.
11b. **autumo-ui release/governance:** how are framework changes versioned/published, given
    multiple consumers (Studio, Documents UI, demo, …)?
12. **Dependency scope:** keep the reference `provided` approach or normal scopes + `jlink`?
13. **Settings migration:** migrate existing app-data settings automatically, or start fresh?
14. **Editor library:** confirm RichTextFX (or alternative) for script/config editing.
15. **Testing depth:** how much automated UI testing (TestFX) vs. core-only?
15a. ~~Installer/licensing activation~~ → **resolved:** wire the autumo-ui installer +
    registration/licensing UI **later**; development stays in **portable mode**.

### Editor scope / legacy parity (see doc 11)
19. ~~**UI layout:** nav tree or legacy tab-per-type model?~~ **Decided (2026-10-16):** keep the
    **nav tree** and add a **coloured type pill** (blue = base, green = reader, red = writer) on the
    section/nav node — it also scales to plugin/cloud interfaces, which fixed tabs do not.
20. ~~**`meta_*` keys:** write them for parity?~~ **Decided (2026-10-16): NO.** The legacy
    `IfacexConfiguration.store0()` explicitly **excludes** `meta*` from the output
    (`if (!key.startsWith("meta"))`) and persists the metadata only in the **comment header**;
    `loadComments()` keeps `meta_*` in memory only. Our comment-header-only behaviour is therefore
    already the legacy parity.
21. **Interfaces without a legacy editor** (`doc_in`, `file_in`, `file_out`, `null_in`,
    `console_out`, `code`, `exec`, `first_out`, cloud plugins): full editor, read-only, or raw view?
22. **Priority order** for closing the parity gaps in doc 11 §9 (complete per-type parameters,
    destination-mapping editor with STD/INS + drag & drop, new-configuration wizard).
23. **New-configuration workflow:** wizard (reader → writers → entities) or empty model +
    inline "add" actions in the tree?
24. **Menu items deliberately not built yet** (doc 11 §11.1): `Process › Run` and `Debug Mode`
    (need the engine integration), `Registration` (own top-level menu), `Help › Format JSON`
    (formatter), `Configuration › General Settings` (the legacy pane was the Studio's own
    `PrefPanel`). Implement when their feature exists, or drop them for good?

### Process / repository
16. **Versioning scheme** for the new app (e.g. `3.0.0` or `1.0.0`)?
17. **Application name/bundle id:** keep "ifaceX Studio" or mark as FX edition?
18. **Coexistence:** does the old Swing Studio remain available, or is this a full
    replacement (affects settings locations and package names)?

## 7.4 Engine Access (requested input)

To design the new format and its semantics correctly, the following would help — **optional
for the PoC, valuable for the format design**:

- The **reader/writer parameter reference** (required/optional keys, types, defaults) for at
  least `csv`, `json`, `db`, `rest`, `mongo`, `mail`, `doc`.
- How **field mapping / formulas** are represented and evaluated by the engine.
- How **encryption** is applied to values (`enc_*` conventions, the model numbers).
- (Nice to have) the engine's config **parser** source, to guarantee future compatibility.

Access path / location is to be provided by the owner if desired.

## 7.5 Decisions Log

| Date | Decision | Source |
| --- | --- | --- |
| — | Reference read-only; new app in `autumo-ifacex-studio-fx` | user |
| — | Java + JavaFX; no web app; no Electron | user |
| — | Documentation in English under `docs/` | user |
| — | Java 25 (latest LTS, Azul) | user |
| — | Code reuse authorized | user |
| — | Sound retained; CreaSPA hook dropped in favor of a generic plugin mechanism | user |
| — | New config format; legacy converted on load; engine support later | user |
| — | Local git repo, no remote; branch `master` | user |
| 2026-10-16 | Hybrid UI: FXML shell + programmatic metadata-driven forms (D10/10) | user |
| 2026-10-16 | **Build on the autumo-ui framework** for the shell/product concerns; Studio editor UI and `core` stay ours; framework may be improved (D12) | user |
| 2026-10-16 | Spike validated: autumo-ui 1.2.0 built/published to `mavenLocal`; Studio shell subclasses `BaseApp` (portable) and drives `core` on Java 25 + JavaFX 25 | assistant |
| 2026-10-16 | autumo-ui bumped to **Java 25 + JavaFX 25** (Gradle wrapper 9.7, toolchain 25); build.gradle fixes: `autumo-registration` 1.0.0→1.1.0, API-exposed deps `implementation`→`api` | user + assistant |
| 2026-10-16 | Shell folded into the **`ui`** module (standalone `StudioFxApp` replaced by `StudioApp extends BaseApp`, portable mode); `core` stays UI-free; `spike/` kept as reference | user |
| 2026-10-16 | **Root/file form**: clicking the root node edits the comment-header metadata (Title, Version, Author, Description/Notes, Date) plus the job wiring (`source_interface`, `destination_interfaces`, `process_source_entities`) — as in the original Studio. Edits flow into the raw views and exports; writing back to the source file follows later. | user |
| 2026-10-16 | **Dark theme**: the PoC now loads the autumo-ui base stylesheet (`ch/autumo/ui/style/application.css`) before the Studio `studio.css` overrides; dialogs reuse the same base style | user |
| 2026-10-16 | `bin/run.sh [file]` opens a config directly (`--open`); run script forces JDK 25 | assistant |
| 2026-10-16 | **Readability:** base font size raised to 14px with LCD font smoothing; mono (raw views) 13px | user |
| 2026-10-16 | **Toolbar adopted from the original Studio** (icons + labels, ifaceX mark on the right): New · Open · Save · Close · Configuration · Run · External Edit; Run/External Edit are placeholders | user |
| 2026-10-16 | **Typography = Aileron** (the autumo-ui brand font, bundled); also fixed the autumo-ui dialog-header family name (`Aileron-Semibold` → `Aileron SemiBold`, autumo-ui commit `6d3bc79b`) | user |
| 2026-10-16 | **Toolbar right side = original:** title logo (`ifacex-studio-reddot2-40.png`) + clickable ifaceX mark with label `ifaceX` (opens `url.website`). Invented Website/Documentation/Purchase links **removed**. Structure per FX reference `autumo-documents-ui` (`tool-bar-pane` + left/right ToolBar) | user |
| 2026-10-16 | **Save always writes the new `.ifcx` format** (`.ifacex` = export-only) | user |
| 2026-10-16 | **Editor tabs restyled** for the dark theme; **continuous separation line** under the toolbar (also under the tree) | user |
| 2026-10-16 | **Typography = Inter** (bundled, SIL OFL) replaces Aileron: Inter is the modern web-GUI look (React Native Web / system-stack inspired) and is bundled so every machine renders identically. Applied via `studio.css` (`.root` → `Inter`, headings/labels → `Inter SemiBold`); fonts loaded in `StudioApp.loadFonts()` | user |
| 2026-10-16 | **Entity source fields edited as a list** (Add `+` / Remove `−` / move `↑` `↓` / in-place edit) instead of a comma-separated text field — as in the original Studio (`JList` + Add/Remove/Edit/Up/Down). Mapping table uses the same `+` / `−` buttons | user |
| 2026-10-16 | **Form fields have rounded corners** (`-fx-background-radius: 6` for text field/area/password/combo box, `4` for check box, `6` for the table) | user |
| 2026-10-16 | **Text-only forms are padded** (start-up placeholder, "select an element") so the text does not stick to the top-left | user |
| 2026-10-16 | **Entity form split by role**: a reader entity edits the source **fields**, a writer entity edits the **destination mapping** (destination entity + mapping table). The mapping editor is shown for **every** writer entity (previously only when the file already contained a mapping, so none could be created); the mapping record is created lazily — rendering a form never mutates the model | user |
| 2026-10-16 | **The flat key model alone is not enough.** The legacy Swing GUI was analysed in full (doc 11): 12 colour-coded tabs (BASE blue / readers green / writers red), one reader + N writers, `EntityFieldsHolder` (source fields, `order_field`, `uid`/`muid`/`jsonIdPath`, per-writer `dest_filter_fields`) and `DestinationMappingHolder` (**exactly one destination entity per (writer, source entity)**, mapping lines `[STD]`/`[INS] target:{formula}`, drag & drop of a source field into the formula box, auto-map `field:{field}`). The new editor must therefore be **type-driven** with the **destination mapping as a first-class editor**, must be able to **create a new configuration**, and must show the interface **type as a coloured pill** | user |
| 2026-10-16 | **Layout:** keep the nav tree and show the interface **type as a coloured pill** (blue = base, green = reader, red = writer) instead of the legacy fixed tabs — scales to plugin/cloud interfaces | user |
| 2026-10-16 | **No `meta_*` keys are written.** The legacy `IfacexConfiguration.store0()` explicitly excludes `meta*` (`if (!key.startsWith("meta")) …`) and persists the metadata **only in the comment header**; `loadComments()` keeps them in memory only. Our comment-header-only behaviour is already the legacy parity — the earlier "write meta_*" answer was based on a wrong premise and is corrected here | assistant (verified against the legacy source) |
| 2026-10-16 | **Foundation implemented:** core `InterfaceTypes` registry (prefix → kind/display/colour/engine class, legacy colours), coloured **type pills** in the nav tree and on every section/entity form header, **writer-type specific destination fields** (`dest_unique_id_field` for `db_out`/`mongo_out`, `dest_id_path` for `rest_out`, `dest_filter_fields` as a list for db/mongo/rest), reader `order_field` for `db_in`/`mongo_in`, and **up/down reordering** of the mapping rows (STD/INS via the `insert` flag). Building any form does not mutate the model | assistant |
| 2026-10-16 | **Mapping editor laid out as in the original Studio**: source fields **full width at the top** (drag source), the **formula box** (drop target) below them, then the add buttons, then the mapping table. The old "fields left / formula right" side-by-side layout was wrong | user |
| 2026-10-16 | **Mapping buttons use the original wording**, spelled out: `Add standard mapping` (legacy `Add std. mapping`) and `Add insert mapping`. The "All (standard)" button is **replaced by the original auto-mapping control**: the clickable `icons/dnd-48.png` (48×48) with the original Studio's tooltip text (wording preserved, HTML converted to plain text) — see legacy `csv_out_dndPanel` / `ImagePanel` + `generateMappings` | user |
| 2026-10-16 | **Navigation emphasis**: the base section is renamed `General` → **`Overwritten Values`** and rendered in bold, as are the **`Source:`** and **`Destination:`** section entries (`.nav-emphasis` = Inter SemiBold 14.5px) | user |
| 2026-10-16 | **Root navigation entry = the configuration title** (`meta.title`), not the file name; it is re-derived on every model change so it tracks the title typed in the form. Falls back to the file name / "Configuration" when no title is set | user |
| 2026-10-16 | **Status bar reads like the original Studio**: `File: <absolute path>` first (`File: -` without a file), then the state (`Loaded` / `Converted from legacy .ifacex` / `Modified` / `Saved …`). The former reader/writers/entities summary was meaningless there and is gone (`MainFrame.statusLabel.setText("File: …")`) | user |
| 2026-10-16 | **CSS live reload (dev only):** `CssLiveReload` watches `studio.css` and swaps the scene stylesheet (JavaFX caches per URL) so CSS changes are visible without a restart; active only in a `file:`-under-`target/classes` dev layout, `-Difacex.livereload=false` disables it. Java changes still need a rebuild+restart | user |
| 2026-10-16 | **i18n decided: gettext PO files, read at runtime by a small own reader** (`core/.../i18n/PoFile` + `Messages`; no dependency, no build step). **English is the source language** — the msgid *is* the English text, so there is no key catalogue and nothing to translate for English. `messages.pot` is the template, `messages.<lang>.po` the translation (currently `de`, 121 messages). `Messages.setLanguage(...)` also sets `Locale.setDefault(...)`, so **autumo-ui follows the choice instead of the OS locale** (the cause of the German texts). Switch with `-Difacex.lang=de` or `language=de` in the app config (`cfg/app.cfg`); default `en`. See doc 12. **Still English-only:** the per-parameter labels, which come from `FieldCatalog` and get their lookup when that is rebuilt (doc 11 §7) | user |
| 2026-10-16 | **Parameter labels are translatable too: the raw configuration key is the msgid.** The English label derived from the key (`FieldCatalog.humanize`, e.g. `data_path` → "Data path") is only the **fallback** (`Messages.t(id, english)`), so a PO entry translates a parameter by its stable key and everything untranslated stays as-is. Help texts use `<key>.help`. Parameter **keys and interface/type names stay technical and are never translated**. `de` now carries 183 messages; the self-test prints `paramLabels=…` to verify the labels end-to-end. Supersedes the "still English-only" note above | user |
| 2026-10-17 | **Language is switchable at runtime** (`Help › Language › English / Deutsch`, flag icons from autumo-ui; the language names are *not* translated). `MainController.switchLanguage(...)` moves **both** catalogues (`Helper.setLanguage` for the framework — window title, About/license HTML, framework dialogs — and `Messages.setLanguage` for the PO catalogue), then re-runs `applyTexts()`, `buildToolBar()` and `rebuild()`. `buildToolBar()` is now idempotent (clears the bars first). The self-test switches language twice and reports `langItems=2 langSwitchOk=true langRestoredOk=true`. Placement (`Help` menu) is a proposal — the legacy menu only had vestigial `jCheckBoxMenuItem_LANG_*` items | assistant |
| 2026-10-17 | **autumo-ui `1.2.0` was extended (not forked, version unchanged)** so a framework app can switch language live and only has to translate *its own* texts: `Helper.setLanguage/getLanguage/getSupportedLanguages/addLanguageListener` (explicit language state; a region is ignored, `de_CH` → `de`), `getMessageBundle()` now returns a **`FallbackResourceBundle`** (app bundle first, autumo-ui base bundle as parent, so framework keys such as `button.Alright` always resolve and remain overridable), the German framework bundle is `Bundle_de.properties` (was `Bundle_de_CH.properties`, still found for `de_CH`), the framework HTML panes read `Helper.getLanguage()`, `showExceptionDialogExtended` no longer NPEs (it used a non-existent app stylesheet path), and the swapped `getBaseStyleURL()`/`getBaseHTMLStyleURL()` bodies are fixed. **PO support stays in the Studio** — gettext is the translator format and `core` is UI-free. Consequence for the Studio: the `ui` module must ship **`javafx-web`** (the About/license panes are `WebView`s). Changes in `/repository/autumo-ui` are **not committed** (the monorepo also carries unrelated dirty files) | assistant |
| 2026-10-17 | **Menu bar follows the original Studio, but only with items that actually do something** (the full legacy inventory is doc 11 §11). `File` = New / Open / Open Recent / Save / Export .ifcx / Export .ifacex / Close / Exit, `Configuration` = Encoder + Remove App Data, `Help` = Open Online Documentation / Visit Website / Check for Updates / Language / About / License. Accelerators are the legacy ones and are set **programmatically** (`SHORTCUT_DOWN` = ⌘ on macOS) because the FXML builder cannot construct the immutable `KeyCodeCombination`. **Encoder: framework pane `UI.showEncoder(app)`** (user decision) instead of the legacy 32-hex key that had to match the engine; `Remove App Data` → `UI.showRemovePane`, `Check for Updates` → `UI.showUpdatePane`, `License` → `UI.showLicense` (a new item — the legacy reached the licence through About). `Open Recent` reuses the framework (`BaseController.initializeRecentMenu` + `rebuildRecentList`, list kept/persisted by `BaseApp`); the open path is now the framework's `innerOpen(File)`, which `File › Open` and `--open` both use. **Deliberately not built** (no implementation exists, nothing was invented): `Process › Run`/`Debug Mode`, `Registration`, `Help › Format JSON`, `Configuration › Interface Processor/Base/Interfaces Configuration` (that is the navigation tree now) and `General Settings`. The language submenu stays in `Help` (the legacy `jMenu6` was commented-out dead code) | assistant |
