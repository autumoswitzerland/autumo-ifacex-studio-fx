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

import java.io.IOException;
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
 *   ConvertTool &lt;in.ifacex|in.cfg&gt; [outDir]   convert one file to .ifcx (same base name)
 *   ConvertTool &lt;dir&gt; [outDir]                convert every *.ifacex / *.template in dir
 *   ConvertTool --check &lt;dir&gt;                 verify lossless round-trips for every *.ifacex in dir
 * </pre>
 *
 * <p>The new file is named after the source without its configuration extension:
 * {@code ifacex.cfg} becomes {@code ifacex.ifcx}, {@code 002-hubspot-v3-companies.ifacex} becomes
 * {@code 002-hubspot-v3-companies.ifcx}. The properties re-export is a check and is skipped when
 * it would overwrite the source.</p>
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
            System.out.println("usage: ConvertTool <in.ifacex|in.cfg> [outDir]  convert one file");
            System.out.println("       ConvertTool <dir> [outDir]               convert every *.ifacex / *.template in dir");
            System.out.println("       ConvertTool --check <dir>                verify lossless round-trips");
            return;
        }
        if ("--check".equals(args[0])) {
            System.exit(check(Path.of(args[1])));
        }
        final Path in = Path.of(args[0]);
        if (Files.isDirectory(in)) {
            // Every processor configuration of the directory, the target defaults to the source.
            final Path outDir = args.length > 1 ? Path.of(args[1]) : in;
            System.exit(convertDirectory(in, outDir));
        }
        final Path outDir = args.length > 1 ? Path.of(args[1]) : in.getParent();
        convert(in, outDir);
    }

    /**
     * Converts one legacy configuration ({@code .ifacex} or {@code .cfg}) into the new
     * {@code .ifcx} YAML format and writes the YAML plus a re-exported properties file (the
     * round-trip check) into {@code outDir}. The new file keeps the base name of the source and
     * drops its old extension. A short summary of the job wiring is printed to standard out.
     *
     * @param in     legacy configuration file to read
     * @param outDir target directory; created when it does not exist
     * @throws Exception when the source cannot be read or a target cannot be written
     */
    public static void convert(Path in, Path outDir) throws Exception {
        convert(in, outDir, true);
    }

    /**
     * Converts one file and reports what it did.
     *
     * @param in      legacy configuration file to read
     * @param outDir  target directory; created when it does not exist
     * @param verbose whether to print every written file
     * @return the written {@code .ifcx} file, or {@code null} when it would have overwritten the source
     * @throws Exception when the source cannot be read or a target cannot be written
     */
    private static Path convert(Path in, Path outDir, boolean verbose) throws Exception {
        Files.createDirectories(outDir);
        final String name = in.getFileName().toString();
        if (extension(name).equalsIgnoreCase(".template")) {
            return convertTemplate(in, outDir, verbose);
        }
        final IpcConfig cfg = PropertiesConfigReader.read(in);
        final String base = baseName(name);
        final String ext = extension(name);

        // The old extension is not part of the new name: 'ifacex.cfg' becomes 'ifacex.ifcx',
        // '002-hubspot-v3-companies.ifacex' becomes '002-hubspot-v3-companies.ifcx'.
        final Path yaml = outDir.resolve(base + ".ifcx");
        final Path props = outDir.resolve(base + (ext.isEmpty() ? ".ifacex" : ext));

        Path written = yaml;
        if (verbose) {
            System.out.println("converted: " + name);
        }
        if (isSameFile(yaml, in)) {
            written = null;
            if (verbose) {
                System.out.println("  (skipped " + yaml.getFileName() + ": it is the source file)");
            }
        } else {
            YamlConfigWriter.write(yaml, cfg);
            if (verbose) {
                System.out.println("  -> " + yaml);
            }
        }
        // The properties file is only a round-trip check. It is never allowed to overwrite an
        // existing file: that may be the original configuration, or a hand-edited one.
        if (Files.exists(props)) {
            if (verbose) {
                System.out.println("  (skipped " + props.getFileName()
                        + ": the file exists and is never overwritten)");
            }
        } else {
            PropertiesConfigWriter.write(props, cfg);
            if (verbose) {
                System.out.println("  -> " + props);
            }
        }
        if (verbose) {
            System.out.println("  source=" + cfg.sourceInterface()
                    + " writers=" + cfg.writers().keySet()
                    + " entities=" + cfg.processSourceEntities());
        }
        return written;
    }

    /**
     * Converts a template block ({@code *.template}).
     *
     * <p>The template files in {@code interfaces/} are building blocks: users copy a block into
     * their own configuration, so the <em>name</em> of the file is the name of the block. The
     * converted block therefore keeps the original file name, and the legacy block is kept next
     * to it as {@code old_<name>}. No properties file is written: the block itself is the
     * original.</p>
     *
     * @param in      template block to convert
     * @param outDir  target directory, the source directory when converting in place
     * @param verbose whether to print what was written
     * @return the written block, or {@code null} when the file was already converted
     * @throws Exception when the block cannot be read, renamed or written
     */
    private static Path convertTemplate(Path in, Path outDir, boolean verbose) throws Exception {
        final String name = in.getFileName().toString();
        final List<String> lines = Files.readAllLines(in);
        if (looksConverted(lines)) {
            if (verbose) {
                System.out.println("  (skipped " + name + ": already converted)");
            }
            return null;
        }
        // The same reader as for every other file: the comments above a key belong to the block
        // and are carried into the generated YAML - a template is no exception.
        final IpcConfig cfg = PropertiesConfigReader.read(lines);
        final Path target = outDir.resolve(name);
        final Path backup = in.resolveSibling(TEMPLATE_BACKUP_PREFIX + name);
        if (verbose) {
            System.out.println("converted: " + name);
        }
        // Rename the legacy block first, so the converted block can take its name.
        if (Files.exists(backup) && !isSameFile(backup, in)) {
            if (verbose) {
                System.out.println("  (kept existing " + backup.getFileName() + ")");
            }
        } else if (!isSameFile(in, target) || !Files.exists(target)) {
            Files.move(in, backup);
            if (verbose) {
                System.out.println("  -> " + backup);
            }
        } else {
            // In place: the name has to be free before the converted block is written.
            Files.move(in, backup);
            if (verbose) {
                System.out.println("  -> " + backup);
            }
        }
        YamlConfigWriter.write(target, cfg);
        if (verbose) {
            System.out.println("  -> " + target);
        }
        return target;
    }

    /**
     * Whether a file of a directory is converted: a processor configuration ({@code *.ifacex}) or
     * a template block ({@code *.template}). The backups of converted blocks ({@code old_*}) are
     * not - they are the legacy originals.
     *
     * @param p file of the directory
     * @return {@code true} when the file belongs to the conversion
     */
    static boolean isConvertible(Path p) {
        final String name = p.getFileName().toString();
        final String lower = name.toLowerCase(java.util.Locale.ROOT);
        if (lower.endsWith(".template")) {
            // Only the converted template blocks are skipped; an IPC file may well be called
            // 'old_something.ifacex'.
            return !name.startsWith(TEMPLATE_BACKUP_PREFIX);
        }
        return lower.endsWith(".ifacex");
    }

    /**
     * Whether the lines already are a converted (YAML) block. A legacy block never starts with the
     * format version, so a second run cannot mistake the result for its input.
     *
     * @param lines file lines
     * @return {@code true} when the file holds the new format
     */
    static boolean looksConverted(List<String> lines) {
        for (final String line : lines) {
            final String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            return trimmed.startsWith("formatVersion:");
        }
        return false;
    }

    /**
     * Converts every {@code *.ifacex} and {@code *.template} of a directory. Only the directory
     * itself is read, sub directories are not touched.
     *
     * <p>Without a target directory the files are converted in place. One summary line is
     * printed per file, failures do not stop the run.</p>
     *
     * @param dir    directory that holds the legacy configuration files
     * @param outDir target directory; created when it does not exist
     * @return the number of files that could not be converted
     * @throws Exception when the directory cannot be listed
     */
    public static int convertDirectory(Path dir, Path outDir) throws Exception {
        final List<Path> files;
        try (Stream<Path> s = Files.list(dir)) {
            files = s.filter(Files::isRegularFile)
                    .filter(ConvertTool::isConvertible)
                    .sorted()
                    .toList();
        }
        Files.createDirectories(outDir);
        int failed = 0;
        System.out.println("converting " + files.size() + " file(s): " + dir + " -> " + outDir);
        for (final Path f : files) {
            try {
                final Path written = convert(f, outDir, false);
                System.out.println("  OK    " + f.getFileName() + " -> "
                        + (written == null ? "(skipped)" : written.getFileName()));
            } catch (Exception e) {
                failed++;
                System.out.println("  FAIL  " + f.getFileName() + " : " + e.getMessage());
            }
        }
        System.out.println();
        System.out.println("converted=" + (files.size() - failed) + " failed=" + failed);
        return failed;
    }


    /** Extensions the converter recognises; any of them is dropped from the new file name. */
    private static final List<String> KNOWN_EXTENSIONS =
            List.of(".ifacex", ".ifcx", ".cfg", ".properties", ".template");

    /** Prefix of the backup of a converted template block. */
    static final String TEMPLATE_BACKUP_PREFIX = "old_";

    /**
     * The name of a file without a known configuration extension.
     *
     * @param fileName the file name, e.g. {@code ifacex.cfg}
     * @return the base name, e.g. {@code ifacex}
     */
    static String baseName(String fileName) {
        final String ext = extension(fileName);
        return ext.isEmpty() ? fileName : fileName.substring(0, fileName.length() - ext.length());
    }

    /**
     * The known configuration extension of a file name, or an empty string.
     *
     * @param fileName the file name
     * @return the extension including the dot, e.g. {@code .cfg}
     */
    static String extension(String fileName) {
        final String lower = fileName.toLowerCase(java.util.Locale.ROOT);
        for (final String ext : KNOWN_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                return fileName.substring(fileName.length() - ext.length());
            }
        }
        return "";
    }

    /**
     * Whether both paths point at the same file.
     *
     * @param a first path
     * @param b second path
     * @return {@code true} when the paths resolve to the same file
     */
    private static boolean isSameFile(Path a, Path b) {
        if (a.toAbsolutePath().normalize().equals(b.toAbsolutePath().normalize())) {
            return true;
        }
        try {
            // Catches symbolic links and case-insensitive file systems.
            return Files.exists(a) && Files.exists(b) && Files.isSameFile(a, b);
        } catch (IOException e) {
            return false;
        }
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
