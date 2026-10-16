# 09 — Engine Configuration Model (Fact Base)

Derived from a read-only analysis of:

- Engine: `/Users/Mike/Development/git/repository/autumo-ifacex`
- Templates: `/Users/Mike/Development/git-ext/autumo-ifacex-templates` (80 `.ifacex` files)

Relevant engine classes: `IPC`, `Configuration`, `RWC`/`RWConfig`, `DataMapping`,
`FieldMapping`, `WriterMapping`, `SourceEntity`, `security/Encoder`, plus the
reader/writer packages.

## 9.1 The IPC = base config + specific config

An IPC is **not** a single file at runtime. It is the **merge** of:

1. the base configuration `cfg/ifacex.cfg` (general parameters valid for every IPC), and
2. the IPC-specific file (e.g. `interfaces/xyz.ifacex`).

Specific values **overwrite** base values with the same key. Internally the engine builds
one `RWConfig` for the reader and one per writer.

## 9.2 Reader / Writer identifiers

Readers and writers are identified by a **prefix name** used in every key:

| Kind | Names |
| --- | --- |
| Readers | `null_in`, `csv_in`, `json_in`, `doc_in`, `file_in`, `db_in`, `mongo_in`, `rest_in` |
| Writers | `csv_out`, `json_out`, `file_out`, `mail_out`, `db_out`, `mongo_out`, `rest_out`, `console_out`, `first_out`, `code`, `exec` |

Cross-cutting keys in the IPC file:

- `source_interface` — one reader prefix
- `destination_interfaces` — comma-separated writer prefixes (1..n)
- `process_source_entities` — comma-separated source entities
- `files_folder` — overwrite of the base file folder

## 9.3 Key naming model (3 levels + wildcard)

Every reader/writer parameter key follows:

```
<rw>_<param>                      global for that reader/writer
<rw>_<entity>_<param>            scoped to one source entity
<rw>_*_<param>                   wildcard default for ALL entities
```

Resolution: entity-specific value, else `*` value, else default. Examples:

```
rest_in_host=api.spotify.com                     # global (applies to reader)
rest_in_*_data_path=items                        # wildcard default for all entities
rest_in_albums_entities_url=/v1/artists/.../     # entity 'albums' only
rest_in_tracks_fields=track_number,name,href     # field list for entity 'tracks'
```

Entity names may contain characters such as `/` (e.g. `stock/metric`) and `.` — see
`019-finnhub-basic-financials.ifacex`.

## 9.4 Fields

```
<rw>_<entity>_fields=a,b,c
```

Comma-separated list of source fields (JSON paths / column names). Only relevant for
readers/writers that map data (CSV, REST, DB, JSON, …).

## 9.5 Destination mapping (writers)

The mapping is encoded as **indexed, ordered keys** (order preserved by the `NNN` index):

```
<rw>_<entity>_mapping_entity=<destEntity>
<rw>_<entity>_mapping_field_000=<destField>:<sourceFormula>
<rw>_<entity>_mapping_field_001=<destField>:<sourceFormula>
...
<rw>_<entity>_mapping_insert_NNN=<destField>:<sourceFormula>   # insert-only (NOT NULL) values
```

- If `_mapping_entity` is **absent**, the source entity is mapped **1:1** to the
  destination (no field mapping, no data mapper).
- `<destField>` and `<sourceFormula>` are split on the **first** `:` only
  (`split(":", 2)`), so formulas may themselves contain `:`.
- Mapping types: `TYPE_FIELD` (standard) and `TYPE_INSERT` (values required on insert).

Related writer keys (entity-scoped):

| Key | Meaning |
| --- | --- |
| `<rw>_<entity>_dest_filter_fields` | fields whose values uniquely identify an existing record (update/insert decision) |
| `<rw>_<entity>_dest_unique_id_field` | unique id field for upsert logic |
| `<rw>_<entity>_exclusion_filter_class` | FQCN of an exclusion filter |
| `<rw>_<entity>_data_mapper_class` | FQCN of a custom `DataMapper` |
| `<rw>_ext_insert_mapping` | yes/no external insert mapping |
| `<rw>_single_id_filter` | yes/no single-id filter (REST) |

Concrete example (HubSpot → CSV):

```properties
csv_out_companies_mapping_entity=companies
csv_out_companies_mapping_field_000=properties.hs_object_id:{properties.hs_object_id}
csv_out_companies_mapping_field_001=properties.name:{properties.name}
csv_out_companies_mapping_field_002=properties.website:{properties.website}
```

> **Structural representation:** the target YAML stores this as an **ordered list of
> fields** per **(writer, source entity)** instead of indexed keys — see
> [08-config-format.md](08-config-format.md) §8.5 ("Mapping: from key syntax to structure").
> Mapping is inherently **per source entity**: one reader, N source entities, N writers,
> each writer with one output mapping per source entity.

## 9.6 Formula / transformation syntax

`sourceFormula` is a **template string with `{sourceField}` placeholders**:

- `{properties.name}` → replaced by the value of that source field.
- Multiple placeholders and static text allowed: `{first}-{last}`, `Mr. {last}` etc.
- Empty formula → direct field passthrough.
- Reverse resolution exists (mapping a dest filter field back to a source field) with a
  clear error if no source field can be associated.
- Additional mapping helpers in base config: `map_country2iso`,
  `map_source_country_fields`, `map_country_lang`, `map_default_iso`.

## 9.7 Encryption / secrets

- Sensitive values (passwords, API keys, users) can be stored **encoded**.
- **Encoding marker:** the value is enclosed in **curly braces**:
  `{<mode>:<base64>}` — e.g. `rest_in_api_key={3:lua/HUSNx0DMyjk4JJDZUQ==}`.
- **Plain** values have **no** braces.
- Modes (per `security/Encoder` help text):
  - `0` — hashing with the configured hash provider
  - `1` — Base64, SHA3-256, AES
  - `2` — Base64, SHA-256, AES
  - `3` — Base64, PBE, MD5, DES
  - `4` — Blowfish
- Decoding at runtime via `CryptUtils.decodeIfNecessary(...)`.
- The key seed `secret_key_seed` (32-char hex) in `cfg/ifacex.cfg` must be identical in the
  engine, the server, and the **Studio**. Hashing uses `hash_implementation`
  (default `Argon2HashProvider`).
- The Studio already ships an **Encoder** dialog and a CLI encoder
  (`encoder.sh|.bat -e|-d`).

## 9.8 General / base parameters (`cfg/ifacex.cfg`)

Observed base parameters (not exhaustive; the engine file is authoritative):

| Key | Purpose / values |
| --- | --- |
| `one_error_stop` | atomic chain stops on first error (yes/no) |
| `proc_timeout` | processor timeout (s) for parallel mode |
| `rest_setup_timeout`, `rest_timeout` | HTTP timeouts (s) |
| `batch_mode` | `serial` \| `parallel` (atomic forces serial) |
| `files_folder` | default input/output folder |
| `general_batch_size` | default batch/fetch size |
| `write_header` | write CSV header (yes/no) |
| `value_delimiter` | value delimiter (1 char, default `;`) |
| `value_enclosure` | value enclosure (1 char, default `"`) |
| `replace_char`, `old_char`, `new_char` | output character replacement |
| `cleanNullValues` | turn `null`-like values into `""` (yes/no) |
| `map_country2iso`, `map_source_country_fields`, `map_country_lang`, `map_default_iso` | country mapping |
| `mail_out_from`, `mail_out_max_size` | mail defaults |
| `logging_implementation` | logging class |
| `console_colors` | ANSI colors (yes/no) |
| `secret_key_seed` | shared crypto seed (32-char hex) |
| `hash_implementation` | password hash provider class |

## 9.9 Processing, modes, test switches

- **Batch mode:** `serial` / `parallel`; **atomic** chain processing for multiple IPs.
- **Test switches** passed by the Studio when running: `ROOTCALL`, `DEBUG`.
- The Studio launches `bin/run.sh|run.bat` with `config:<path> s testswitches:...`.

## 9.10 Key inventory across templates (aggregate)

80 templates, **536 distinct keys**. Distribution by reader/writer prefix:

`rest_in` 110 · `rest_out` 96 · `mongo_out` 86 · `db_out` 84 · `mongo_in` 51 ·
`db_in` 48 · `csv_out` 27 · `mail_out` 10 · `json_in` 8 · `csv_in` 4 · plus `exec_*`
and general keys.

Distinct **non-entity** keys include `source_interface`, `destination_interfaces`,
`process_source_entities`, `files_folder`, `batch_mode`, and `exec_*`
(`cmd`, `args`, `working_dir`, `timeout`, `exit_code_success`, `proc_pos`).

> Note: the count is inflated because entity names appear inside keys
> (`rest_in_<entity>_...`). The naming model (9.3) is what matters.

## 9.11 Implications for the new format

1. **The model is hierarchical:** reader/writer → (global | per-entity) → parameter,
   plus per-entity **fields** and ordered **mapping rows**. A structured format maps onto
   this naturally (entity objects with their own settings, fields, and mappings).
2. **Wildcard defaults are first-class:** the `*` scope must be representable (e.g. a
   `defaults` block per reader/writer, overridden per entity).
3. **Mapping is ordered:** an array preserves order without `NNN` indices.
4. **Formulas are strings with `{field}` placeholders:** store as-is.
5. **Secrets need an explicit marker:** encoded values must be distinguishable from plain
   ones (e.g. `{mode:base64}` kept, or a structured `{ "enc": 3, "value": "..." }`).
6. **⚠ Dual serialization is required (critical):** the engine currently reads **only
   properties**. Until ifaceX natively supports the new format, the Studio must be able to
   **export the in-memory model to legacy properties** for test runs. The properties
   format therefore becomes an *execution/export format*; the new format is the *authoring*
   format. This must be designed in from day one.
7. **Base vs. specific split:** general parameters come from the base config; the new
   format must decide whether to embed general settings, reference them, or treat them as
   global app settings.
