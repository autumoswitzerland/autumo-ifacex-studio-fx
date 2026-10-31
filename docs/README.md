# ifaceX Studio FX — Planning

Rebuild of **ifaceX Studio** as a modern JavaFX desktop application.

This directory contains **planning documents only** (Markdown). No application code
will be produced until explicitly instructed.

## Ground Rules

| Item | Rule |
| --- | --- |
| Reference project | `/Users/Mike/Development/git-ext/autumo-ifacex-studio` — **read-only**, must not be modified |
| Target project | `/Users/Mike/Development/git-ext/autumo-ifacex-studio-fx` — the new application lives here |
| Technology | **Java 25** (latest LTS, Azul Zulu) + **JavaFX** (no web frontend, no Electron) |
| Language | Java (deliberately not Kotlin) |
| Architecture | Desktop application, no server/web backend |
| Code reuse | Reference code may be taken over (owner-authorized) |
| Config format | New structured format **YAML** (`*.ifcx`) from day one; legacy `.ifacex` (properties) is converted on load; properties export retained for engine test runs |

## Documents

| File | Content |
| --- | --- |
| [01-current-state.md](01-current-state.md) | Analysis of the existing Swing application (as-is) |
| [02-scope.md](02-scope.md) | Goal, scope, boundaries, requirements |
| [03-architecture.md](03-architecture.md) | Target architecture, modules, layers |
| [04-technology.md](04-technology.md) | Technology stack and library mapping (Swing → JavaFX) |
| [05-feature-migration.md](05-feature-migration.md) | Feature inventory and migration matrix |
| [06-roadmap.md](06-roadmap.md) | Phases, milestones, proof of concept |
| [07-risks-open-questions.md](07-risks-open-questions.md) | Risks and open decisions |
| [08-config-format.md](08-config-format.md) | Proposal for the new configuration format (YAML), incl. dual serialization |
| [09-engine-config-model.md](09-engine-config-model.md) | Fact base: the ifaceX engine's configuration model (readers/writers/mapping/encryption) |
| [10-ui-form-design.md](10-ui-form-design.md) | Design of the modern, metadata-driven reader/writer forms |
| [11-legacy-studio-gui-analysis.md](11-legacy-studio-gui-analysis.md) | Source-of-truth analysis of the legacy Swing GUI: tabs, types, colours, per-type parameters, destination mapping, drag & drop, serialization |
| [12-i18n.md](12-i18n.md) | Internationalisation: English as source language, gettext PO files (reader in autumo-ui), language switching |

## Status

> **Phase: implementation complete through phase 6, packaging in progress (3.0.0).**
> The application builds and runs (`./gradlew :ui:run`); native packaging is wired
> (`ui/build.gradle`, `product/packaging/`); CI exists (`.github/workflows/build.yml`).
> Open items: customer plugin API (dropped), TestFX (rejected in favour of JUnit),
> aliases/anchors, version/name/coexistence, new-config wizard vs. inline add,
> interfaces without a legacy editor, and the packaging loose ends in
> `product/packaging/README.md`. See document 07 §7.3.
