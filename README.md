<p align="center" style="margin-bottom:0">
  <img class="logo" src="ui/src/main/resources/ch/autumo/ifacex/studio/ui/icons/ifaceX-reddot-256.png" alt="ifaceX Studio FX — logo" width="256" />
</p>

<h1 align="center" style="font-size:3.5em; margin:0px">autumo UI</h1>

<br />

---

# ifaceX Studio FX

Modern JavaFX rebuild of the **ifaceX Studio** — the desktop tool for configuring and
test-running Interface Processor Configurations (IPCs) of the ifaceX platform.

> **Status:** JavaFX rebuild in progress (version `3.0.0`). The native application is
> built and packaged with `jpackage` (see `product/packaging/README.md`); the build and
> the CI workflow are in `ui/build.gradle` and `.github/workflows/build.yml`.

## What it does

- Create, open, save, save-as and close IPC configurations (`.ifcx` / legacy `.ifacex`).
- Visual, metadata-driven editor for readers, writers, entities and field mapping
  (drag & drop), with a raw YAML/properties view side by side.
- Run a test IPC by launching the external ifaceX engine and streaming its console
  output (severity highlighting, timeout watchdog, kill).
- Tools: Encoder, JSON Formatter, Raw config editors, Remove App Data, About.
- First-run installer, license/registration, update check, recent files.
- Internationalisation (English / German) with a live language switch.
- Dark theme.

The Studio is a **configuration and test tool only** — operations, monitoring and
user management belong to ifaceX Web/Server and are out of scope.

## Technology

- **Java 25** (Azul Zulu) + **JavaFX 25** (OpenJFX), no Electron, no web frontend.
- Multi-module **Gradle** build: `core` (UI-free domain/infrastructure) and `ui`
  (JavaFX application). The application shell is built on the `autumo-ui` framework
  (`ch.autumo.commons:autumo-ui:1.2.0`).
- Configuration format: **YAML** (`.ifcx`); legacy `.ifacex` properties files are
  converted on load. Properties export is retained for engine test runs.

See `docs/04-technology.md` for the full library mapping and `docs/08-config-format.md`
for the format.

## Build & run (development)

```sh
# requires JDK 25 and autumo-ui 1.2.0 published to mavenLocal
./gradlew :ui:run
```

```sh
# tests
./gradlew build
```

## Native packaging

```sh
./gradlew :ui:runtime :ui:jpackageImage
```

Per-OS packaging notes, file associations and the open packaging items are documented
in `product/packaging/README.md`. A GitHub Actions workflow (`.github/workflows/build.yml`)
builds and tests on every OS and, on `workflow_dispatch` with `package=true`, produces
the native installers.

## Documentation

All planning and design documentation lives under `docs/` (English). Start with
`docs/README.md`.

## License

Proprietary — see `LICENSE` and `package/LICENSE-OS-Installer.txt`.