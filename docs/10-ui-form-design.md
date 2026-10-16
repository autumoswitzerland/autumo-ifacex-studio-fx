# 10 — UI Design for Reader/Writer Forms

## 10.1 Problem (as-is)

The current Swing forms for readers and writers are old-school: fixed fields placed with
absolute coordinates, one large panel per reader/writer, and every parameter hard-coded in
UI code. **Adding or changing a parameter means editing UI code**, and the forms are not
particularly friendly for the variety of parameters the engine supports (see doc 09).

## 10.2 Goals

- **Modern & clean** — consistent layout, spacing, typography and theming (JavaFX + CSS).
- **User-friendly** — clear grouping, inline help, validation, and search/filter.
- **Extensible** — new parameters are added **as data, not UI code**.
- **Consistent with the new config model** — global vs. per-entity settings, wildcard
  defaults, secrets, formulas (see docs 08 & 09).

## 10.3 Core principle: metadata/schema-driven forms

Describe each parameter once, declaratively, and render the UI generically from those
descriptors. A descriptor looks roughly like:

```text
FieldDescriptor
  key        rest_in_host
  label      Host
  type       string | int | bool | enum | list | secret | formula | text
  default    api.example.com
  enum       [...]                 # for type=enum
  scope      global | entity | both
  group      Connection | Auth | Pagination | Files | ...
  help       "..."                 # tooltip / inline help
  required   true|false
  secret     true|false            # renders the encrypted toggle + encoder
```

**Adding a new reader/writer parameter becomes adding a descriptor** — no form redesign.

## 10.4 Entity scoping (important)

**Clarification — the IPC is the global unit.** The IPC-level definitions
(`source_interface`, `destination_interfaces`, `process_source_entities`) and the general
settings apply to the **whole IPC (global)**. The engine *additionally* allows per-entity
overrides, because an IPC can have **several source entities** (e.g. `albums,tracks`) and
the **destination mapping is per source entity** (each source entity maps to one
destination entity per writer — see `DataMapping`: one `sourceEntity` + one `destEntity`).
Most templates use a **single** source entity; with one entity everything is effectively
global.

The engine's parameters are therefore **global** (`<rw>_<param>`), **per-entity**
(`<rw>_<entity>_<param>`), or **wildcard defaults** (`<rw>_*_<param>`). The UI must make
this obvious and easy:

- Show **global settings** and **per-entity overrides** distinctly (e.g. a "Defaults"
  section plus a per-entity editor).
- Make the **wildcard default** vs. explicit entity value visible (which value wins).
- Avoid duplicating every field for every entity.

Candidate layouts (to evaluate in the PoC): a **defaults + overrides table**, or
**tabs** per entity with a "Default" tab.

## 10.5 Layout & UX guidelines

- **Grouped sections** (collapsible) instead of one giant form (e.g. Connection, Auth,
  Pagination, Headers, Files, Mapping).
- **Consistent** label/field grid or card layout; predictable spacing.
- **Inline help** from the descriptor; tooltips for details — carry over the helpful
  explanations the old tooltips already had.
- **Validation** with inline messages, driven by the schema (doc 08.13).
- **Search/filter** fields (there are many parameters).
- **Keyboard navigation** and sensible tab order; accessible labels.
- **Theme**: light/dark.
- **Interface type pill**: every reader/writer section and entity form shows a coloured pill
  with the interface type (`CSV`, `JSON`, `DB`, `Mongo DB`, `REST`, `Mail`, …). The colours
  follow the legacy Studio: **base blue `#48486b`, readers green `#486b48`, writers red
  `#6b4848`** (see `InterfaceTypes`). The same pill is rendered in the navigation tree so the
  type is visible while browsing. This replaces the legacy fixed, colour-coded tabs and scales
  to plugin/cloud interfaces (`aws_file_in`, `excel_out`, …).
- **Writer-type specific destination fields** (doc 11 §6): a writer entity shows only the
  parameters its type actually supports — `dest_unique_id_field` for `db_out`/`mongo_out`,
  `dest_id_path` for `rest_out`, and `dest_filter_fields` (as a list) for `db_out`/`mongo_out`/
  `rest_out`; `csv_out`/`json_out`/`mail_out` show none of them. A DB-like *reader* entity shows
  its `order_field`. The mapping table's `Insert (INS)` flag distinguishes INS from STD, and the
  mapping rows can be reordered (up/down), matching the legacy `[STD]`/`[INS]` model.

## 10.6 Extensibility mechanism

- A **field-descriptor registry** (built-in) drives form rendering, validation and help.
- The registry can optionally be **overridden/extended externally** (resource file) so
  future or customer-specific parameters appear **without recompiling UI code**.
- One descriptor set can feed **form + model + validation + documentation** — single
  source of truth.

## 10.7 Secrets UI

- A per-field **"encrypted" toggle** plus **mode selector** (as today), integrated with the
  built-in **Encoder** (doc 08.5). Stored as `{enc, value}`.
- Show plain vs. encrypted state clearly (e.g. lock icon), never log secrets.

## 10.8 Open Questions (to be decided during the PoC)

Decisions and explanations:

1. **Where do the field descriptors live?** → **Deferred to a later step** (decision).
   The PoC uses an **in-code registry**; an external, customer-extendable override can
   follow later.
2. **Per-entity UI: defaults + overrides table, or tabs?**
   Only relevant when an IPC has **more than one source entity** (see 10.4). Option A: one
   **table** with a "default" column and per-entity overrides. Option B: **tabs**, one per
   entity plus a "Default" tab.
   *Recommendation for PoC:* try the **table** (makes the `*` wildcard visible); fall back
   to tabs if it gets crowded.
3. **Field set** → **all current fields are kept** (decision). The existing field set is the
   current standard; nothing is dropped. Grouping/collapsing ("advanced") is a presentation
   choice only.
4. **Raw-text/YAML view alongside the forms?** → **Yes** (confirmed). This already exists in
   the current Studio (form view + raw view) and is retained.

## 10.9 Implementation (PoC status)

The FXML decision is **resolved: hybrid** (see doc 07 §7.3). The application shell is **built on
the autumo-ui framework** (decision D12), in **portable mode** for development:
`StudioApp extends BaseApp` (autumo-ui) provides lifecycle/resources; it loads our FXML shell.

**Shell (FXML).** `ui/src/main/resources/ch/autumo/ifacex/studio/ui/main.fxml` declares the
static frame: `MenuBar` (File/Help), `ToolBar`, a `SplitPane` with a navigation `TreeView`
on the left and a `TabPane` on the right (tabs: **Form**, **Raw YAML (.ifcx)**,
**Raw Properties (.ifacex)**), and a status bar. Controller: `MainController`.

**Forms (programmatic).** `FormBuilder` renders a form for the selected navigation node
from the model + field metadata:

- **root/file** → **File metadata** (comment header: Title, Version, Author,
  Description/Notes, Date) and **Job wiring** (`source_interface`,
  `destination_interfaces`, `process_source_entities`) — the same configuration-level
  fields the original Studio edits;
- reader/writer section → collapsible **Parameters** (global), **Defaults (\*, all entities)**
  (wildcard) and one collapsible **Entity** per entity;
- entity (reader) → the source **fields** as an editable **list** (Add `+` / Remove `−` /
  move `↑` `↓`, in-place double-click editing — as in the original Studio, not a
  comma-separated field);
- entity (writer) → **Destination entity** + the editable **field mapping table**
  (`destination field` / `formula` / `insert`, with `+` / `−` buttons). The editor is shown for
  **every** writer entity, so a mapping can be created even when the file has none yet; the
  mapping record is created lazily, so merely opening the form does not change the file;
- both → the entity **settings** grid (`dest_filter_fields`, `order_field`, …);
- general → the remaining top-level (non reader/writer) keys;
- every parameter is shown, even without a descriptor (see `FieldCatalog` inference);
- a form that has nothing editable to show (start-up placeholder, "select an element") is
  rendered as a **padded** text body so the text does not stick to the top-left corner.

**Typography.** The UI uses the bundled **Inter** family (SIL OFL, `fonts/Inter-OFL.txt`),
loaded at start-up in `StudioApp` and applied via `studio.css` (`.root` → `Inter`, headings
and field labels → `Inter SemiBold`). Inter is the modern web-GUI typeface (the look that
React Native for Web / the system font stack approximates) and is bundled so rendering is
identical on every machine.

**Theme.** The scene loads the **autumo-ui base stylesheet**
(`ch/autumo/ui/style/application.css`, a modern **dark** palette) first, then the Studio
`studio.css` overrides (navigation tree, collapsible headers, status bar, editor surfaces).
Dialogs reuse the same base stylesheet so they match the main window. `studio.css` also
raises the base font size to **14px** and enables LCD font smoothing for readability on the
dark palette.

**Toolbar.** The toolbar mirrors the original Studio editor (same structure as the FX
reference `autumo-documents-ui`): a `MenuBar` plus a `tool-bar-pane` `BorderPane` holding a
left and a right `ToolBar` (transparent inner bars; each button is a 48px autumo-ui icon with
the label underneath). Left: **New · Open · Save · Close | Configuration | Run | External
Edit**. `Configuration` selects the root node (file metadata + job wiring); `Run` and
`External Edit` are placeholders until engine integration. Right (as in the original): the
**ifaceX Studio title logo** (`ifacex-studio-reddot2-40.png`) and the clickable **ifaceX mark
with the label `ifaceX` underneath**, which opens `url.website`. The same 16px icons are used
in the File menu. **Save always writes the new `.ifcx` format**; `.ifacex` is export-only.

The toolbar carries the `tool-bar-pane` background/gradient and a top+bottom border, giving a
**continuous separation line** across the full width (also under the navigation tree).

**Metadata.** `core/.../meta/` holds `FieldType`, `FieldDescriptor` and `FieldCatalog`
(known parameters + best-effort inference for unknown ones). It is UI-free and unit-testable;
external, customer-extendable descriptors remain a later step (§10.8, item 1).

**Editing.** Form edits mutate the in-memory model and immediately refresh both raw views;
the model round-trips losslessly through `.ifcx` (YAML) and legacy `.ifacex` (properties).

**Legacy import.** Opening a `.ifacex` converts it in memory and shows
"Converted legacy .ifacex: …" in the status bar; `.ifcx` re-opens directly.

**Per-entity UI (open).** The PoC currently uses collapsible per-entity sections; the
table-vs-tabs evaluation (§10.8, item 2) is still open and only matters for multi-entity IPCs.

**Not yet done.** Typed validation messages, search/filter, secret encryption toggle, a
light/dark theme *switch* (the modern dark palette is now the default), plugin/extensibility
hooks.
