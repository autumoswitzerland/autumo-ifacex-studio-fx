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
 * 
 */
package ch.autumo.ifacex.studio.core.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Command-line handling for the launcher, kept in {@code core} so it is UI-free and testable.
 *
 * <p>The Studio accepts a configuration file in two shapes:</p>
 * <ul>
 *   <li>{@code --open <file>} — the explicit form used by the development launcher
 *       ({@code bin/run.sh some.ifacex});</li>
 *   <li>{@code <file>} — the bare file path a packaged ({@code jpackage}) launcher receives when
 *       the user double-clicks a registered {@code .ifcx}/{@code .ifacex} file or uses
 *       <em>Open With</em>. The OS hands the selected file straight to the app as the first
 *       argument, so no option is present.</li>
 * </ul>
 *
 * <p>A bare argument is only treated as a file when its name carries a known configuration
 * extension; this prevents unrelated arguments (JavaFX or JVM options) from being mistaken for a
 * configuration. The association itself is declared at packaging time
 * ({@code product/packaging/}, doc 06).</p>
 */
public final class CommandLine {

    /** Option that explicitly names the file to open. */
    public static final String OPEN_OPTION = "--open";

    /** Configuration file extensions the Studio can open (legacy properties and new YAML). */
    private static final String[] EXTENSIONS = { ".ifacex", ".ifcx", ".yaml", ".yml" };

    private CommandLine() {
    }

    /**
     * Whether a file name carries one of the configuration extensions the Studio can open — the
     * legacy {@code .ifacex}, the new {@code .ifcx}, and the plain YAML spellings the open dialog
     * also offers.
     *
     * @param name file name or path to inspect (may be {@code null})
     * @return {@code true} if it looks like an ifaceX configuration file
     */
    public static boolean isConfigFileName(String name) {
        if (name == null) {
            return false;
        }
        final String lower = name.toLowerCase(Locale.ROOT);
        for (final String ext : EXTENSIONS) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Picks the configuration file to open from the raw command line arguments.
     *
     * <p>Recognises {@code --open <file>} and a bare {@code <file>} (see the class comment). Any
     * other argument — notably one starting with {@code -} such as {@code --selftest} — yields
     * {@code null}, so the caller can keep handling its own options.</p>
     *
     * @param args raw command line arguments (may be {@code null} or empty)
     * @return the file to open, or {@code null} when the command line carries none
     */
    public static Path openTarget(List<String> args) {
        if (args == null || args.isEmpty()) {
            return null;
        }
        final boolean explicit = OPEN_OPTION.equals(args.get(0));
        final int idx = explicit ? 1 : 0;
        if (args.size() <= idx) {
            return null;
        }
        final String candidate = args.get(idx);
        if (candidate == null || candidate.isEmpty() || candidate.startsWith("-")) {
            return null;
        }
        if (!explicit && !isConfigFileName(candidate)) {
            return null;
        }
        return Path.of(candidate);
    }
}
