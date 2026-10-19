# ifaceX Studio FX - packaging assets

Assets used when the Studio is packaged into a native application with `jpackage`
(roadmap phase 7, see `docs/06-roadmap.md`). Nothing here is needed to run the
`./gradlew :ui:run` development launcher.
## File associations (double-click / "Open with")

`file-associations/` holds one `jpackage` file-association descriptor per
configuration format. They register the Studio as a handler for the two ifaceX
configuration extensions, which is what makes a double-click work:

| Descriptor | Extension | MIME type |
| --- | --- | --- |
| `file-associations/ifcx.properties` | `.ifcx` | `application/x-ifacex` |
| `file-associations/ifacex.properties` | `.ifacex` | `application/x-ifacex-legacy` |

Each descriptor is a plain Java-properties file with the `extension` and
`mime-type` keys that `jpackage --file-associations` expects. Pass both when
building the native package, for example:

```sh
jpackage \
  --type app-image \
  --name "ifaceX Studio FX" \
  --input target/pack \
  --main-jar autumo-ifacex-studio-ui.jar \
  --main-class ch.autumo.ifacex.studio.ui.Launcher \
  --file-associations product/packaging/file-associations/ifcx.properties \
  --file-associations product/packaging/file-associations/ifacex.properties \
  # ... icon, identifier, version, runtime image ...
```

The launcher accepts the file the OS hands it as the **first argument**
(`ch.autumo.ifacex.studio.core.cli.CommandLine`), so no option is required in
the packaged form. The development launcher still uses the explicit
`--open <file>` form.

## Still open (phase 7)

- **Application icon** (`--icon`): a `.icns` (macOS) / `.ico` (Windows) / `.png`
  (Linux) is still needed; the shipped PNGs are toolbar images, not app icons.
- **Bundle identity**: `--mac-package-identifier`, `--vendor`, `--version`.
- **Single-instance "open while running"**: on macOS an already-running app
  receives further double-clicks through the `com.apple.eawt` open-documents
  event. Handling it needs `--add-exports java.desktop/com.apple.eawt=ALL-UNNAMED`
  at launch and is wired together with the real packaging step.
- **Runtime image** (`jlink`) including the JavaFX modules.
