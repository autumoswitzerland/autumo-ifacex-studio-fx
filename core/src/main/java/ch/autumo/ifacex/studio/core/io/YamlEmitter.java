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
package ch.autumo.ifacex.studio.core.io;

import java.util.List;
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
            sb.append(indent(indent)).append(key(String.valueOf(e.getKey()))).append(':');
            final Object v = e.getValue();
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

    private static void dumpList(StringBuilder sb, List<?> list, int indent) {
        for (final Object item : list) {
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
            if (!first) {
                sb.append(indent(indent));
            }
            first = false;
            sb.append(key(String.valueOf(e.getKey()))).append(':');
            final Object v = e.getValue();
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
