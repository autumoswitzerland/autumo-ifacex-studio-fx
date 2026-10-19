/**
 * Copyright 2026 autumo GmbH, Michael Gasche.
 * All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of autumo GmbH The intellectual and technical
 * concepts contained herein are proprietary to autumo GmbH
 * and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from autumo GmbH.
 */
package ch.autumo.ifacex.studio.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import ch.autumo.ifacex.studio.core.io.PropertiesConfigReader;
import ch.autumo.ifacex.studio.core.io.PropertiesConfigWriter;
import ch.autumo.ifacex.studio.core.io.YamlConfigReader;
import ch.autumo.ifacex.studio.core.io.YamlConfigWriter;
import ch.autumo.ifacex.studio.core.model.IpcConfig;

/**
 * Small command line tool for the PoC.
 *
 * <pre>
 *   ConvertTool &lt;in.ifacex&gt; [outDir]   convert one file to .ifcx and re-export .ifacex
 *   ConvertTool --check &lt;dir&gt;          verify lossless round-trips for every *.ifacex in dir
 * </pre>
 */
public final class ConvertTool {

    private ConvertTool() {
    }

    /**
     * Command-line entry point for the PoC converter.
     *
     * @param args {@code <in.ifacex> [outDir]} to convert a single file, or
     *             {@code --check <dir>} to verify lossless round-trips
     * @throws Exception when a file cannot be read or written
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("usage: ConvertTool <in.ifacex> [outDir]");
            System.out.println("       ConvertTool --check <dir>");
            return;
        }
        if ("--check".equals(args[0])) {
            System.exit(check(Path.of(args[1])));
        }
        final Path in = Path.of(args[0]);
        final Path outDir = args.length > 1 ? Path.of(args[1]) : in.getParent();
        convert(in, outDir);
    }

    /**
     * Converts one legacy {@code .ifacex} file into the new {@code .ifcx} YAML format and
     * writes the YAML plus a re-exported {@code .ifacex} (the properties round-trip) into
     * {@code outDir}. A short summary of the job wiring is printed to standard out.
     *
     * @param in     legacy configuration file to read
     * @param outDir target directory; created when it does not exist
     * @throws Exception when the source cannot be read or a target cannot be written
     */
    public static void convert(Path in, Path outDir) throws Exception {
        Files.createDirectories(outDir);
        final IpcConfig cfg = PropertiesConfigReader.read(in);
        final String base = in.getFileName().toString().replaceAll("\\.ifacex$", "");

        final Path yaml = outDir.resolve(base + ".ifcx");
        YamlConfigWriter.write(yaml, cfg);

        final Path props = outDir.resolve(base + ".ifacex");
        PropertiesConfigWriter.write(props, cfg);

        System.out.println("converted: " + in.getFileName());
        System.out.println("  -> " + yaml);
        System.out.println("  -> " + props);
        System.out.println("  source=" + cfg.sourceInterface()
                + " writers=" + cfg.writers().keySet()
                + " entities=" + cfg.processSourceEntities());
    }

    /**
     * Verifies that every legacy configuration in {@code dir} round-trips losslessly: the model
     * must reproduce the original file key for key, and both the properties writer and the YAML
     * writer must survive a full read/write cycle unchanged.
     *
     * @param dir directory that holds the {@code *.ifacex} files (the base config is skipped)
     * @return {@code 0} when all files pass, {@code 1} when at least one fails
     * @throws Exception when the directory cannot be listed
     */
    public static int check(Path dir) throws Exception {
        int ok = 0;
        int bad = 0;
        final List<Path> files;
        try (Stream<Path> s = Files.list(dir)) {
            files = s.filter(p -> p.toString().endsWith(".ifacex"))
                    .filter(p -> !p.getFileName().toString().startsWith("ifacex"))
                    .sorted().toList();
        }
        for (final Path f : files) {
            final String err = checkOne(f);
            if (err == null) {
                ok++;
                System.out.println("PASS  " + f.getFileName());
            } else {
                bad++;
                System.out.println("FAIL  " + f.getFileName());
                System.out.println("      " + err.replace("\n", "\n      "));
            }
        }
        System.out.println();
        System.out.println("checked=" + (ok + bad) + " ok=" + ok + " failed=" + bad);
        return bad == 0 ? 0 : 1;
    }

    static String checkOne(Path file) {
        try {
            final IpcConfig original = PropertiesConfigReader.read(file);
            final Map<String, String> expected = normalize(
                    PropertiesConfigReader.parseRaw(Files.readAllLines(file)));

            // strongest check: the model must reproduce the original file, key for key
            final String rawDiff = diff(expected, normalize(PropertiesConfigWriter.toFlatMap(original)));
            if (rawDiff != null) {
                return "model vs original: " + rawDiff;
            }

            final IpcConfig fromProps =
                    PropertiesConfigReader.read(PropertiesConfigWriter.toText(original).lines().toList());
            final String propsDiff = diff(expected,
                    normalize(PropertiesConfigWriter.toFlatMap(fromProps)));
            if (propsDiff != null) {
                return "properties round-trip: " + propsDiff;
            }

            final IpcConfig fromYaml = YamlConfigReader.read(YamlConfigWriter.toText(original));
            final String yamlDiff = diff(expected,
                    normalize(PropertiesConfigWriter.toFlatMap(fromYaml)));
            if (yamlDiff != null) {
                return "yaml round-trip: " + yamlDiff;
            }
            return null;
        } catch (Exception e) {
            return e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    private static Map<String, String> normalize(Map<String, String> in) {
        final Map<String, String> out = new LinkedHashMap<>();
        in.forEach((k, v) -> {
            if (k.startsWith("meta")) {
                return;
            }
            if (k.endsWith("_fields")) {
                final StringBuilder sb = new StringBuilder();
                for (final String p : v.split(",")) {
                    final String t = p.trim();
                    if (!t.isEmpty()) {
                        if (sb.length() > 0) {
                            sb.append(',');
                        }
                        sb.append(t);
                    }
                }
                out.put(k, sb.toString());
            } else {
                out.put(k, v);
            }
        });
        return out;
    }

    private static String diff(Map<String, String> expected, Map<String, String> actual) {
        final Map<String, String> e = new LinkedHashMap<>(expected);
        final Map<String, String> a = new LinkedHashMap<>(actual);
        final StringBuilder sb = new StringBuilder();
        e.forEach((k, v) -> {
            if (!a.containsKey(k)) {
                sb.append("\n  missing: ").append(k).append('=').append(v);
            } else {
                if (!v.equals(a.get(k))) {
                    sb.append("\n  differs: ").append(k).append(" expected=<").append(v)
                            .append("> actual=<").append(a.get(k)).append('>');
                }
                a.remove(k);
            }
        });
        a.forEach((k, v) -> sb.append("\n  extra: ").append(k).append('=').append(v));
        return sb.length() == 0 ? null : sb.toString().stripLeading();
    }
}
