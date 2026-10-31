# Changelog

All notable changes to **ifaceX Studio FX** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.0.0] - 2026-10-29

### Added

- Initial release of the JavaFX rebuild of ifaceX Studio.
- Multi-module Gradle build (`core` + `ui`), Java 25 + JavaFX 25, no Electron.
- New structured configuration format **YAML (`.ifcx`)**, with lossless conversion of
  legacy `.ifacex` (properties) files on load and properties export for engine test runs.
- Data-driven, metadata-based configuration editor: BASE/reader/writer tabs, entities,
  source-field lists, destination field mapping with drag & drop, validation and encryption
  combos.
- Run of a test IPC by launching the external ifaceX engine, with console streaming,
  severity highlighting, timeout watchdog and kill; debug mode toggle.
- Tools: Encoder, JSON Formatter, raw config editor, base/interfaces config editor,
  Remove App Data, About, registration, update check.
- First-run installer wizard (license → language → settings → done), recent files.
- Internationalisation (English / German) with a live language switch.
- Dark theme, sounds (jingle, finish ok/nok), splash screen.
- Phosphor icon font (2.1.2) for toolbar/menu icons.
- Native packaging via `jpackage`/`jlink` (macOS, Windows, Linux) and file associations
  for `.ifcx` / `.ifacex`.

### Changed

- UI rebuilt from Swing to JavaFX on top of the `autumo-ui` framework (1.2.0).
- Raw config editor uses RSyntaxTextArea 1:1 (theme `cfg/edit.xml`) instead of RichTextFX.
- Code editor and configuration files use the Inter typeface (bundled).

### Removed

- Swing / FlatLaf / NetBeans GUI Builder UI layer.
- Legacy `.ifacex` is now an import/export format only; the new format is written from
  then on.