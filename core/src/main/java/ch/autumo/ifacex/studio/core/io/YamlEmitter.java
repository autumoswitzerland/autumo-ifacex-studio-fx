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
package ch.autumo.ifacex.studio.core.io;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal, deterministic YAML emitter (block style) for the {@code .ifcx} writer.
 *
 * <p>Only the subset needed by the configuration model is supported: maps, lists and
 * string/boolean/integer scalars. All model values are strings, so they are quoted only
 * when the plain representation would be ambiguous. This emitter is intentionally small
 * and stable; reading is done with SnakeYAML Engine.</p>
 */
public final class YamlEmitter {

    private static final String SPECIALS = ":{}[],#&*!|>'\"%@`";

    private YamlEmitter() {
    }

    /**
     * A map that is written in one line: {@code { enc: 3, value: "..." }}.
     *
     * <p>The structured form of an encoded secret is short and belongs to its parameter - the
     * specification shows it inline (see docs/08-config-format.md, 8.5).</p>
     */
    public static final class InlineMap extends LinkedHashMap<String, Object> {

        /**
         * Creates an empty inline map.
         */
        public InlineMap() {
            super();
        }

        /**
         * Creates an inline map with the given entries.
         *
         * @param entries the entries of the map
         */
        public InlineMap(Map<String, Object> entries) {
            super(entries);
        }
    }

    /**
     * A value with the comment lines that document it.
     *
     * <p>YAML has no place for comments in a data model, so they travel next to the value and
     * are written by the emitter directly above the key. The comment block gets one blank line
     * in front of it, which keeps the generated file readable; the block itself stays glued to
     * its key.</p>
     *
     * @param value    the actual value
     * @param comments the comment lines, each including its {@code #}
     */
    public record Commented(Object value, List<String> comments) {
    }

    /**
     * A value that starts a new block and therefore gets a blank line in front of it.
     *
     * <p>Top level sections ({@code sourceInterface}, {@code sources}, ...) and sub sections
     * ({@code params}, {@code entities}, {@code mapping}, ...) are separated by a blank line;
     * without it a long configuration reads like one dense list of keys.</p>
     *
     * @param value the actual value
     */
    public record Spaced(Object value) {
    }

    /**
     * Serialises a nested {@code Map}/{@code List} tree (with scalar leaves) to YAML text.
     *
     * @param root tree root, typically a {@link java.util.Map}
     * @return YAML document text, terminated by a newline
     */
    public static String dump(Object root) {
        final StringBuilder sb = new StringBuilder();
        if (root instanceof Map<?, ?> map) {
            dumpMap(sb, map, 0);
        } else if (root instanceof List<?> list) {
            dumpList(sb, list, 0);
        } else {
            sb.append(scalar(root)).append('\n');
        }
        return sb.toString();
    }

    private static void dumpMap(StringBuilder sb, Map<?, ?> map, int indent) {
        for (final Map.Entry<?, ?> e : map.entrySet()) {
            final Object v = decorated(sb, e.getValue(), indent, false);
            sb.append(indent(indent)).append(key(String.valueOf(e.getKey()))).append(':');
            if (v instanceof InlineMap m) {
                sb.append(' ').append(inline(m)).append('\n');
            } else if (v instanceof Map<?, ?> m) {
                if (m.isEmpty()) {
                    sb.append(" {}\n");
                } else {
                    sb.append('\n');
                    dumpMap(sb, m, indent + 2);
                }
            } else if (v instanceof List<?> l) {
                if (l.isEmpty()) {
                    sb.append(" []\n");
                } else {
                    sb.append('\n');
                    dumpList(sb, l, indent + 2);
                }
            } else {
                sb.append(' ').append(scalar(v)).append('\n');
            }
        }
    }

    /** A map in one line: {@code { enc: 3, value: "..." }}. */
    private static String inline(Map<String, Object> map) {
        final StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (final Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(key(e.getKey())).append(": ").append(scalar(e.getValue()));
        }
        return sb.append('}').toString();
    }

    private static void dumpList(StringBuilder sb, List<?> list, int indent) {
        boolean first = true;
        for (final Object element : list) {
            // Sections of a list are separated by a blank line, plain value lists are not.
            Object item = element;
            boolean blank = false;
            while (item instanceof Spaced spaced) {
                blank = true;
                item = spaced.value();
            }
            if (blank && !first) {
                sb.append('\n');
            }
            first = false;
            if (item instanceof Map<?, ?> m) {
                sb.append(indent(indent)).append("- ");
                dumpMapInline(sb, m, indent + 2);
            } else if (item instanceof List<?> l) {
                sb.append(indent(indent)).append("-\n");
                dumpList(sb, l, indent + 2);
            } else {
                sb.append(indent(indent)).append("- ").append(scalar(item)).append('\n');
            }
        }
    }

    /** Like {@link #dumpMap} but the first entry is written on the current line. */
    private static void dumpMapInline(StringBuilder sb, Map<?, ?> map, int indent) {
        boolean first = true;
        for (final Map.Entry<?, ?> e : map.entrySet()) {
            // The first entry is written behind the list marker: no indent, no blank line.
            final boolean isFirst = first;
            first = false;
            // The decoration (blank line, comments) comes first, the indentation after it.
            final Object v = decorated(sb, e.getValue(), indent, isFirst);
            if (!isFirst) {
                sb.append(indent(indent));
            }
            sb.append(key(String.valueOf(e.getKey()))).append(':');
            if (v instanceof Map<?, ?> m) {
                if (m.isEmpty()) {
                    sb.append(" {}\n");
                } else {
                    sb.append('\n');
                    dumpMap(sb, m, indent + 2);
                }
            } else if (v instanceof List<?> l) {
                if (l.isEmpty()) {
                    sb.append(" []\n");
                } else {
                    sb.append('\n');
                    dumpList(sb, l, indent + 2);
                }
            } else {
                sb.append(' ').append(scalar(v)).append('\n');
            }
        }
    }

    /**
     * Writes the decorations of an entry - a blank line for a {@link Spaced} value and the
     * comment block of a {@link Commented} one - and returns the value itself.
     *
     * @param sb         target
     * @param value      the entry value, possibly decorated
     * @param indent     indentation of the key the decoration belongs to
     * @param firstEntry whether this is the first entry of a list item ({@code - name: ...}):
     *                   it is written on the marker line and gets neither indent nor blank line
     * @return the unwrapped value
     */
    private static Object decorated(StringBuilder sb, Object value, int indent, boolean firstEntry) {
        Object v = value;
        boolean blank = false;
        while (v instanceof Spaced spaced) {
            blank = true;
            v = spaced.value();
        }
        final List<String> notes = v instanceof Commented commented ? commented.comments() : List.of();
        // Exactly one blank line: either before the comment block or, when there is none,
        // before the key of the new section.
        if (!notes.isEmpty()) {
            sb.append('\n');
            for (final String line : notes) {
                sb.append(indent(indent)).append(line.stripTrailing()).append('\n');
            }
        } else if (blank && !firstEntry) {
            sb.append('\n');
        }
        if (v instanceof Commented commented) {
            v = commented.value();
        }
        return v;
    }

    private static String indent(int n) {
        return " ".repeat(n);
    }

    private static String key(String s) {
        if (s.matches("[A-Za-z0-9_./-]+") && !s.startsWith("-")) {
            return s;
        }
        return quote(s);
    }

    private static String scalar(Object v) {
        if (v == null) {
            return "\"\"";
        }
        if (v instanceof Boolean || v instanceof Integer || v instanceof Long) {
            return v.toString();
        }
        final String s = v.toString();
        return needsQuote(s) ? quote(s) : s;
    }

    private static boolean needsQuote(String s) {
        if (s.isEmpty()) {
            return true;
        }
        final String lower = s.toLowerCase();
        if (lower.equals("null") || lower.equals("~") || lower.equals("true") || lower.equals("false")
                || lower.equals("yes") || lower.equals("no") || lower.equals("on") || lower.equals("off")) {
            return true;
        }
        if (s.matches("[-+]?\\d+(\\.\\d+)?([eE][-+]?\\d+)?")) {
            return true;
        }
        if (s.startsWith(" ") || s.endsWith(" ")) {
            return true;
        }
        if (s.endsWith("=")) {
            // A base64 value: quoted, the way the specification shows it.
            return true;
        }
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c == '\n' || c == '\t' || c == '\r' || c < 0x20) {
                return true;
            }
            if (SPECIALS.indexOf(c) >= 0) {
                return true;
            }
        }
        return false;
    }

    private static String quote(String s) {
        final StringBuilder sb = new StringBuilder(s.length() + 2);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
