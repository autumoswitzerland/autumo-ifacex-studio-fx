# 08 — Configuration Format (Proposal)

> **Status: proposal, open for decision.**
> Replaces the Java-Properties-based `.ifacex` with a structured format.
> Fact base: [09-engine-config-model.md](09-engine-config-model.md).

## 8.1 Why the current format is a problem

An IPC today is a **Java `Properties`** file. Structure is faked into key names
(`<rw>_<entity>_<param>`, `*` = wildcard, `_mapping_field_000` = ordered mapping row),
lists are comma-joined strings, and the comment header is a parsed metadata hack.

Problems:

- **No real structure / no types / no schema** — everything is a string key convention.
- **Order encoded into key indices** (`..._mapping_field_000`).
- **Metadata via comments** that the loader parses back out.
- **Fragile:** every reader/writer needs manual key conventions; renames and refactors are risky.
- **Poor fit for the domain:** an IPC is a pipeline (reader → entities → mapping →
  writers → processing), which deserves a hierarchical model (see 9.3–9.6).

## 8.2 Requirements

1. **Hierarchical** — reader, entities, fields, mappings, writers, processing.
2. **Typed** — strings, numbers, booleans, arrays, objects.
3. **Schema-validatable** and **versioned**.
4. **Diff/VCS-friendly** (configs live in git; templates are hand-authored).
5. **Machine + human friendly**.
6. **Represent wildcard defaults** (`<rw>_*_<param>` → per-reader/writer `defaults`, overridable per entity).
7. **Represent ordered mapping rows** as an array.
8. **Represent secrets** with an explicit encoding marker.
9. **Dual serialization** — the Studio exports legacy properties as an explicit feature
   (ifaceX 3.0.0 reads `.ifcx` itself; see 8.6 / 8.7).

## 8.3 Options

| Format | Pros | Cons |
| --- | --- | --- |
| **YAML** | concise, comments, human-friendly, strong precedent in data/integration tools (K8s, dbt, Airflow, CI), ideal for hand-authored templates | indentation pitfalls, type-coercion footguns (use YAML 1.2 + safe parser) |
| **JSON** | universal, unambiguous, mature JSON-Schema tooling, Gson in stack | no comments, verbose, clunky for hand-edited mappings |
| **TOML** | explicit, comments, typed | awkward for deep nesting / arrays-of-objects (mappings) |
| **HOCON** | properties/JSON superset (smooth migration), includes/comments, JVM-native | less mainstream, quirky semantics |

## 8.4 Recommendation

**Owner decision: YAML.** For a config tool whose configs are hand-authored (templates),
commented, and reviewed in git, YAML is the more natural fit — consistent with modern
data-integration platforms.

To keep YAML safe and deterministic, apply these guardrails:

1. **Parser:** YAML **1.2** with a safe loader (e.g. SnakeYAML Engine, or Jackson-YAML
   configured safely). Avoid YAML 1.1 `yes/no`→boolean surprises.
2. **Canonical writing from the GUI:** stable key order, consistent 2-space indentation,
   quote strings that look like numbers/booleans, one document per file.
3. **Schema validation:** validate the loaded document against a schema (JSON-Schema works
   for the YAML data model) before binding.
4. **No anchors/aliases or tags** in generated YAML (avoid surprising evaluation); allow
   them only if deliberately wanted later.
5. **Explicit `formatVersion`** at the root.

## 8.5 Schema (YAML) — implemented

> **Status: implemented** (PoC). The shape below is what `YamlConfigWriter` / `YamlConfigReader`
> actually produce/consume; it is verified **lossless** against all reference templates (68 in total; 10 ship with the repo under `core/src/test/resources/templates`, the rest live in the templates repository).

Design decisions that shape the schema:

- **Native parameter names.** Parameters keep their **engine spelling** (snake_case, e.g.
  `data_path`, `fetchSize`, `dest_filter_fields`) instead of a camelCase re-mapping — for
  guaranteed **lossless** round-trips and 1:1 traceability to the legacy keys.
- **Explicit interfaces vs. sections.** The declared interfaces are stored explicitly
  (`sourceInterface`, `destinationInterfaces`, `processSourceEntities`) and are **separate**
  from the parameter sections (`sources:`, `destinations:`). A declared interface may
  legitimately have **no keys** (e.g. `console_out`), and a file may contain **inert leftover
  sections** for interfaces that are not currently declared (e.g. `csv_in`). Both must be
  preserved — hence the split.
- **Sections are lists** (`sources:`, `destinations:`), each with a `name`; order and
  multiplicity are explicit and stable.
- **Mapping is structural:** an ordered list of `{field, formula, insert}`.

```yaml
###############################################################################
#
#  Title: Spotify API - Album & Tracks
#  Version: 1.0
#  ...
#  Author: autumo GmbH
#  Date: Thu Oct 31 20:43:02 CET 2024
#
###############################################################################

formatVersion: 1

sourceInterface: rest_in
destinationInterfaces:
  - console_out
processSourceEntities:
  - albums
  - tracks

sources:
  - name: rest_in
    params:                      # <rw>_<param>
      host: api.spotify.com
      protocol: https
      auth_method: oauth-ba
      password: YOUR_OWN_PASSWORD
    defaults:                    # <rw>_*_<param>
      data_path: items
      fetchSize: "20"
      additional_params: market=CH
    entities:                    # <rw>_<entity>_<param>
      tracks:
        entities_url: /v1/albums/5rUgxZ595ROtqxq2NCcytT/
        fields:
          - track_number
          - name
          - duration_ms
          - href

destinations:
  - name: csv_out
    defaults:
      files_folder: output
      encoding: UTF-8
    entities:
      tracks:
        dest_filter_fields: ""
        mapping_entity: tracks     # <rw>_<entity>_mapping_entity
        mapping:                   # ordered; was mapping_field_NNN / mapping_insert_NNN
          - field: track_number
            formula: "{track_number}"
            insert: false
          - field: url
            formula: "https://open.spotify.com/track/{id}"
            insert: true

general: {}
```

### Parameter levels inside a section

- `params:` — global parameters (`<rw>_<param>`);
- `defaults:` — wildcard defaults (`<rw>_*_<param>`), overridable per entity;
- `entities:` — per-entity (`<rw>_<entity>_<param>`), each with:
  - the entity's settings (any parameter other than fields/mapping),
  - `fields:` — the source field list (`<rw>_<entity>_fields`),
  - `mapping:` — the destination mapping of a writer (see below).

`general:` holds the non reader/writer scoped keys. Note the asymmetry above: `console_out`
appears in `destinationInterfaces` although it has **no section**, and `csv_in`/`json_in`
appear as sections although they are **not** the declared source interface — both preserved.

### Mapping: from key syntax to structure (decided)

The legacy mapping is spread over **indexed keys**:

```properties
csv_out_tracks_mapping_entity=tracks
csv_out_tracks_mapping_field_000=track_number:{track_number}
csv_out_tracks_mapping_field_001=name:{name}
csv_out_tracks_mapping_insert_000=url:https://open.spotify.com/track/{id}
```

In the structured format this becomes an **ordered list of fields** per
**(writer, source entity)**:

- **Order = array order** → no more `NNN` indices.
- **`field` and `formula` are separate** → no more "split on the first `:`" ambiguity
  (formulas may themselves contain `:`).
- Insert rows become **`insert: true`**.
- The destination entity becomes **`mapping_entity`** (`<rw>_<entity>_mapping_entity`).
- `dest_filter_fields`, `dest_unique_id_field`, `exclusion_filter_class`, `data_mapper_class`
  become normal (snake_case) entity keys.
- A **missing** mapping entity means **1:1** (no mapping) → simply omit `entities`/`mapping`.

This holds the model described in 10.4: **one reader**, **N source entities**, **N writers**,
and **per writer one output mapping per source entity**.

### Secrets — Option B: structured (decided)

Encoded secrets are stored as a **structured object** with an explicit encryption mode:

```yaml
password: { enc: 3, value: "lua/HUSNx0DMyjk4JJDZUQ==" }
apiToken: { enc: 1, value: "..." }
```

Rules:

- **Plain** values stay **plain strings** (no object): `password: "YOUR_OWN_PASSWORD"`.
  Do **not** use `enc: 0` for "none" — `0` is a real encryption mode (hashing).
- An **encrypted** value is an object `{ enc: <mode>, value: <base64> }`, modes `1..4`.
- The Studio keeps a per-field **"encrypted yes/no"** toggle (as today) and chooses the
  mode; the built-in **Encoder** menu item produces the object.

  **Implemented.** The model holds a secret as a structured value (`Secret`: the mode and the
  encoded value), the import converts the legacy notation `{mode:value}` into it and the
  properties writer converts it back for the engine. The YAML writer writes the object inline
  (`{enc: 3, value: "..."}`), the YAML reader reads it back. The **Encoder** has a *format*
  option: `properties` (the legacy notation, for the engine) or `yaml` (the structured object,
  for the new format).
- **Embedded secrets:** the legacy format can embed a marker *inside* a larger string —
  most notably `rest_in_*_additional_headers` ("Can contain encrypted header values within
  key-value-pairs"). In the structured format such fields become **structured lists**, so
  each value can itself be a secret object:

  ```yaml
  defaults:
    additionalHeaders:
      - { name: "Referer", value: "https://products.autumo.ch" }
      - { name: "Authorization", value: { enc: 3, value: "..." } }
  ```
- **Engine / encoder impact:** the engine still reads `{mode:base64}`; the Studio converts
  to/from the structured form on import/export. The **console encoder** gets a `format`
  option (`properties|yaml`), sharing the **core encode** logic with the Studio encoder.
  When ifaceX later reads YAML natively, it must understand this object form.

## 8.6 Conversion on load (migration flow)

1. User opens an IPC file.
2. Format detection:
   - new YAML → load directly;
   - legacy properties → **convert on the fly** (properties → model → YAML).
3. A **notice** is shown ("Legacy properties file; converting to the new format.").
4. The model is edited in the GUI.
5. On save, **YAML** is written (new extension — see 8.8).

**Engine side (verified 2026-10-31, confirmed by the owner).** ifaceX 3.0.0 reads a
`.ifcx` itself: `ch.autumo.ifacex.utils.Yaml2Properties` maps the document onto the flat
engine keys (`params` → `<rw>_<param>`, `defaults` → `<rw>_*_<param>`,
`entities.<e>.<key>` → `<rw>_<e>_<key>`, `mapping` rows → `mapping_field_NNN` /
`mapping_insert_NNN`, `{enc, value}` → `{mode:value}`, lists → comma separated,
booleans → `yes`/`no`; `data_mapper_class` and `exclusion_filter_class` are omitted when
empty). Cross-checked against the Studio's properties export for all 73 `.ifcx` files of the
3.0.0 installation (`interfaces/`): identical keys and values, no differences. Hand-written
or entity-scoped overrides of general parameters (e.g. `entities_url`) are carried over the
same way, because both sides read the key as `<rw>_<entity>_<param>`.

## 8.7 ⚠ Dual serialization (critical)

The engine of ifaceX 3.0.0 reads **`.ifcx` directly** (see 8.6), so the properties export is
no longer required for test runs; it is kept as the explicit legacy export feature. The
Studio still has **three serializers over one in-memory model**:

| Serializer | Purpose |
| --- | --- |
| **YAML reader/writer** | the new authoring format (load/save) |
| **Properties reader** | import legacy `.ifacex` files (conversion) |
| **Properties writer** | **export** an `.ifacex` (legacy export, and a second view on the model) |

The **in-memory model is the single source of truth**; YAML and properties are just
representations — both writers derive from the same model, which is what keeps the
representations semantically identical.

The properties writer must reproduce the exact conventions (entity scoping, `*` wildcard,
`mapping_field_NNN` indexing, `{mode:...}` secrets, base `cfg/ifacex.cfg` merge).

## 8.8 Versioning & file extension

- Root `formatVersion` (integer); unknown future versions are rejected/handled explicitly.

**Decision (confirmed):**

| Extension | Meaning |
| --- | --- |
| `.ifacex` | **Legacy** properties format — import only, plus explicit "export as legacy" |
| **`.ifcx`** | **New YAML** authoring format (confirmed) |

Rationale: two clean, unambiguous formats with distinct extensions; no "same extension,
two formats" confusion. `.ifacex` remains for legacy import and the optional legacy
export; `.ifcx` is the new format that the engine can bind to once it supports YAML.

Alternatives considered:
- `.ifacex.yaml` — most tooling-friendly (editors detect YAML by suffix), but a composite
  extension.
- `.ipc` — matches the domain term (Interface Processor Configuration) but is generic and
  collision-prone.
- Keeping `.ifacex` for both — rejected (ambiguity).

## 8.9 Generated comment header & section comments

The current Studio writes a **comment header** into `.ifacex` and separates reader/writer
sections with comments. The **header is retained** in the new YAML format; **section comments
are a planned enhancement** (decision D8d).

**Implemented — header.** A generated header block with **Title, Version, Notes, Author,
Date** (`CommentHeader`) is emitted verbatim in front of the YAML body, followed by the
deterministic body:

```yaml
###############################################################################
#  Title: Spotify Album & Tracks
#  Version: 1.0
#  Notes: ...
#  Author: autumo GmbH
#  Date: ...
###############################################################################

formatVersion: 1
sourceInterface: rest_in
destinationInterfaces:
  - csv_out
processSourceEntities:
  - albums
  - tracks
sources:
  - name: rest_in
    params:
      ...
destinations:
  - name: csv_out
    entities:
      albums:
        ...
general: {}
```

**Planned — section comments.** Decision D8d additionally calls for human-readable
**section separators** before the top-level sections (`sources`, `destinations`, `general`)
and before each reader/writer section, e.g. `# -------- Reader: rest_in --------`. The current
emitter (`YamlEmitter`) does **not** emit these yet — only the header plus the body. Adding
them is an isolated change to the emitter and cannot affect parsing (comments are ignored by
the reader).

**Design consequence:** standard YAML serializers do **not** emit/preserve comments, so we
need a **custom, deterministic YAML emitter** (model → text) that:

1. writes the header block (and, later, section comments) from the model,
2. guarantees stable key order and indentation,
3. quotes strings that could be misread as numbers/booleans.

Reading uses a normal **safe YAML 1.2 parser**; only *writing* is custom. No
comment-preserving round-trip parser is required, because the text is always regenerated
from the model (just like the current properties writer regenerates the header and date).

## 8.10 Legacy export (explicit feature)

Besides the automatic conversion on load, the Studio offers a deliberate
**"Export as legacy `.ifacex` (properties)"** action — the same exporter used by
`PropertiesConfigWriter` (see 8.7), exposed as a user feature.

## 8.11 Open Decisions

1. ~~Secret representation~~ → **resolved: Option B, structured `{enc, value}`** (8.5).
2. ~~File extension~~ → **resolved: `.ifcx`** (8.8).
3. **Schema tooling:** SnakeYAML Engine (YAML 1.2, safe) for reading + custom emitter for
   writing — **decided** (D8i); `snakeyaml-engine 2.9` is the dependency.
4. ~~Base/general configuration~~ → **resolved:** lives in the **test engine's** file tree
   (`ifacex.cfg` legacy / `ifacex-base.ifcx` new), raw-edited, **not** form-generated (8.12).
5. **Aliases/anchors:** forbid in generated YAML (recommended) or allow for templating?
6. **Round-trip fidelity:** properties → model → properties must be lossless for
   test-run export (golden-file tests) — **resolved & verified** by `RoundTripTest`.

> **Resolved:** hand-written comments in a config file are **not preserved** by the editor
> (already the case in the current Studio) — **accepted**. Only the generated
> **comment header** and the **section separators** matter (see 8.9).

## 8.12 Base/general configuration (not form-generated)

Unlike IPC configs, the **base/general config is not generated from forms**. It is a file
that **lives in the file structure of the test engine**:

- `cfg/ifacex.cfg` — legacy properties (what existing engine installations ship)
- `cfg/ifacex-base.ifcx` — the new YAML variant

The Studio does **not** assume which one is present: it **checks the test engine's `cfg/`
folder and uses whichever file exists** (the YAML variant is preferred when both are there,
because that is the authoring file — `ifacex.cfg` is the export the engine still reads for test
runs). This also decides where the **encoder seed** comes from: the **`secret_key_seed`** of the
base configuration is the shared crypto seed, and the Studio uses it for password encoding
(`UI.showEncoder(app, false)` → unique security key) exactly as the engine does. Old engine
installations therefore keep working through `ifacex.cfg`; new ones use `ifacex-base.ifcx`.

The Studio merely offers a way to **edit** it, **once a test engine has been defined**
(a test engine is required anyway for test runs and for GUI licensing). The engine merges
the base config dynamically at processing time.

Consequences:

- The base config is a **per-test-engine** artifact — not a project file and not a global
  Studio setting.
- Since the engine of 3.0.0 reads `.ifcx` (8.6), the base config is used **as it is**;
  the legacy `cfg/ifacex.cfg` is only needed for older engine versions.
- Editing is **raw-text** (no forms); this is a technical settings file, not an IPC.

## 8.13 YAML library & schema (plain explanation)

**What is a YAML library?** YAML is just text. To read it in Java you need a library that
parses the text into objects (maps/lists/strings/numbers). Two common choices:

| Library | Notes |
| --- | --- |
| **SnakeYAML Engine** | The modern variant of the classic Java YAML library. Supports **YAML 1.2** (important: avoids the `yes/no`→boolean trap of YAML 1.1) and has a **safe** mode (no arbitrary object construction). |
| **Jackson `jackson-dataformat-yaml`** | Uses Jackson (the JSON library family) to read/write YAML, maps directly to Java objects/records. Convenient, but historically built on YAML 1.1 semantics — needs care. |

**What is schema validation?** A *schema* is a formal description of what the file may
contain: which fields exist, their types, allowed values (e.g. `reader` must be one of
`csv_in|rest_in|db_in|...`). Validating a `.ifcx` against a schema catches typos and
structural errors **before** loading, instead of failing later. *JSON Schema* is a standard
format for this and works for YAML too (same data types).

**Recommendation:**
1. **Read** with **SnakeYAML Engine** in safe/YAML-1.2 mode.
2. **Write** with our **custom deterministic emitter** (needed anyway for the header/section
   comments; see 8.9).
3. **Validate**: start with **lightweight validation in code** (required fields, enum values,
   `formatVersion`). Add a full **JSON Schema** later if the configs get more complex — this
   keeps the PoC simple while leaving the door open.

## 8.14 Standards check (BPEL & alternatives)

**Context:** ifaceX is an **ETL / data-integration engine** between interfaces, with strong
**monitoring and alarming** — smaller in scope than e.g. Apache NiFi, but with its own
qualities. For such engines, standard formats are sometimes mentioned (e.g. BPEL). We
checked the landscape to verify YAML.

| Format | Category | Fit for ifaceX configuration |
| --- | --- | --- |
| **BPEL** | Service/business-process orchestration (XML, bound to SOAP/WSDL) | ❌ Wrong category (orchestration, not data mapping); heavy; largely superseded |
| **BPMN 2.0** | Visual process/workflow modeling (XML) | ❌ Models process flow, not data mapping; heavy |
| **CWL / WDL** | Computational workflow description (YAML/JSON) | ⚠️ Workflow/tool I/O model — overkill and a mismatch |
| **NiFi flow JSON** | Tool-specific (JSON) | ❌ Proprietary to NiFi |
| **Node-RED / n8n / StreamSets** | Tool-specific (JSON) | ❌ Proprietary |
| **Benthos / Redpanda Connect, Vector, dbt, Camel YAML DSL** | Tool-specific (**YAML**) | ✅ Shows YAML is mainstream for pipelines |
| **OpenLineage / OpenTelemetry** | Runtime lineage/telemetry (JSON) | ➕ Complementary for monitoring, **not** a config format |

**Findings:**

- There is **no de-facto standard** for ETL/data-mapping pipeline definitions; established
  tools each use their own **JSON** or **YAML**. Choosing our own is normal and expected.
- **BPEL is the wrong category**: it targets service orchestration (SOAP/WSDL), not data
  transformation/mapping, and is declining. Not recommended.
- **YAML is well-established** in the data/integration space → our decision is **confirmed**.
- **Portability/validation:** add a **JSON Schema**; since YAML is a **superset of JSON**,
  a JSON representation/interchange remains possible if ever needed.
- **Monitoring/alarming** stay their own concern; for cross-tool interoperability one could
  later align with **OpenLineage / OpenTelemetry** (runtime telemetry), not the config.

**Conclusion:** stay with **YAML (`.ifcx`)**.
