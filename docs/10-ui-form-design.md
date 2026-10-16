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

- reader/writer section → collapsible **Parameters** (global), **Defaults (\*, all entities)**
  (wildcard) and one collapsible **Entity** per entity;
- entity → source **fields** (comma separated), **destination entity** + editable **field
  mapping table** (`field` / `formula` / `insert`), and the entity **settings** grid;
- every parameter is shown, even without a descriptor (see `FieldCatalog` inference).

**Metadata.** `core/.../meta/` holds `FieldType`, `FieldDescriptor` and `FieldCatalog`
(known parameters + best-effort inference for unknown ones). It is UI-free and unit-testable;
external, customer-extendable descriptors remain a later step (§10.8, item 1).

**Editing.** Form edits mutate the in-memory model and immediately refresh both raw views;
the model round-trips losslessly through `.ifcx` (YAML) and legacy `.ifacex` (properties).

**Legacy import.** Opening a `.ifacex` converts it in memory and shows
"Converted legacy .ifacex: …" in the status bar; `.ifcx` re-opens directly.

**Per-entity UI (open).** The PoC currently uses collapsible per-entity sections; the
table-vs-tabs evaluation (§10.8, item 2) is still open and only matters for multi-entity IPCs.

**Not yet done.** Typed validation messages, search/filter, secret encryption toggle, theme,
plugin/extensibility hooks.
