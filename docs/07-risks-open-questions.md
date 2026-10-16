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
