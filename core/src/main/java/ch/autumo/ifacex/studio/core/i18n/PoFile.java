/*
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
package ch.autumo.ifacex.studio.core.i18n;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/**
 * A minimal gettext <b>PO</b> reader.
 *
 * <p>Understood: {@code msgctxt}, {@code msgid}, {@code msgstr}, continuation lines, the usual
 * C escapes (including octal), comments, references, {@code #, fuzzy} entries and the file
 * header entry. <b>Not</b> understood: plural forms ({@code msgid_plural} / {@code msgstr[n]});
 * the singular translation {@code msgstr[0]} is used if present.</p>
 *
 * <p>The lookup key is {@code msgctxt + '\0' + msgid}, or just the {@code msgid} when there is
 * no context (see {@link #key(String, String)}). Entries with an empty translation or flagged
 * fuzzy are dropped, which is exactly gettext's fallback behaviour: use the source text.</p>
 */
public final class PoFile {

    /** Separator between the optional message context and the message id in a lookup key. */
    public static final char CONTEXT_SEPARATOR = '\u0000';

    private PoFile() {
    }

    /**
     * Parses a PO document.
     *
     * @param text PO document
     * @return lookup key - translation (untranslated and fuzzy entries left out)
     * @throws IOException on a read error
     */
    public static Map<String, String> parse(String text) throws IOException {
        final Map<String, String> out = new HashMap<>();
        final StringBuilder ctx = new StringBuilder();
        final StringBuilder id = new StringBuilder();
        final StringBuilder str = new StringBuilder();

        Target target = Target.NONE;
        boolean fuzzy = false;

        try (BufferedReader r = new BufferedReader(new StringReader(text))) {
            String line;
            while ((line = r.readLine()) != null) {
                final String t = line.strip();
                if (t.isEmpty()) { // end of an entry
                    store(out, ctx, id, str, fuzzy);
                    ctx.setLength(0);
                    id.setLength(0);
                    str.setLength(0);
                    target = Target.NONE;
                    fuzzy = false;
                    continue;
                }
                if (t.startsWith("#")) {
                    // Comments carry metadata; "#, fuzzy" marks an unreliable entry.
                    if (t.startsWith("#,") && t.substring(2).contains("fuzzy")) {
                        fuzzy = true;
                    }
                    continue;
                }
                if (t.startsWith("msgctxt")) {
                    target = Target.CTX;
                    ctx.setLength(0);
                    quoted(ctx, t.substring("msgctxt".length()));
                } else if (t.startsWith("msgid_plural")) {
                    target = Target.NONE; // plurals are not supported, see the class javadoc
                } else if (t.startsWith("msgid")) {
                    target = Target.ID;
                    id.setLength(0);
                    quoted(id, t.substring("msgid".length()));
                } else if (t.startsWith("msgstr[")) {
                    // Only the singular form is used; msgstr[1+] is ignored.
                    final int close = t.indexOf(']');
                    final String index = close < 0 ? "" : t.substring(7, close);
                    target = index.trim().equals("0") ? Target.STR : Target.NONE;
                    if (target == Target.STR) {
                        str.setLength(0);
                        quoted(str, t.substring(close + 1));
                    }
                } else if (t.startsWith("msgstr")) {
                    target = Target.STR;
                    str.setLength(0);
                    quoted(str, t.substring("msgstr".length()));
                } else if (t.startsWith("\"")) {
                    append(target, ctx, id, str, t);
                }
            }
        }
        // A document that does not end with a blank line still has a trailing entry.
        store(out, ctx, id, str, fuzzy);
        return out;
    }

    /** Builds the lookup key for a message, using {@code context} only when it is set. */
    public static String key(String context, String id) {
        return context == null || context.isEmpty() ? id : context + CONTEXT_SEPARATOR + id;
    }

    private static void store(Map<String, String> out, StringBuilder ctx, StringBuilder id,
            StringBuilder str, boolean fuzzy) {
        // An empty msgid is the file header entry, an empty msgstr means "untranslated".
        if (id.length() > 0 && str.length() > 0 && !fuzzy) {
            out.put(key(ctx.toString(), id.toString()), str.toString());
        }
    }

    private static void append(Target target, StringBuilder ctx, StringBuilder id,
            StringBuilder str, String line) {
        switch (target) {
            case CTX -> quoted(ctx, line);
            case ID -> quoted(id, line);
            case STR -> quoted(str, line);
            case NONE -> { /* continuation of something we do not use */ }
        }
    }

    /** Appends the quoted part of {@code line} (decoded) to {@code sb}. */
    private static void quoted(StringBuilder sb, String line) {
        final int start = line.indexOf('"');
        final int end = line.lastIndexOf('"');
        if (start < 0 || end <= start) {
            return;
        }
        unescape(sb, line.substring(start + 1, end));
    }

    /** Decodes the PO/C escape sequences of {@code raw} into {@code sb}. */
    static void unescape(StringBuilder sb, String raw) {
        for (int i = 0; i < raw.length(); i++) {
            final char c = raw.charAt(i);
            if (c != '\\' || i + 1 >= raw.length()) {
                sb.append(c);
                continue;
            }
            final char e = raw.charAt(++i);
            switch (e) {
                case 'n' -> sb.append('\n');
                case 't' -> sb.append('\t');
                case 'r' -> sb.append('\r');
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case '"' -> sb.append('"');
                case '\\' -> sb.append('\\');
                case '0', '1', '2', '3', '4', '5', '6', '7' -> {
                    int value = e - '0';
                    int digits = 1;
                    while (digits < 3 && i + 1 < raw.length()
                            && raw.charAt(i + 1) >= '0' && raw.charAt(i + 1) <= '7') {
                        value = value * 8 + (raw.charAt(++i) - '0');
                        digits++;
                    }
                    sb.append((char) value);
                }
                // Unknown escapes (incl. \a and \v) keep their plain character.
                default -> sb.append(e);
            }
        }
    }

    /** What the next string of a PO entry belongs to. */
    private enum Target {
        NONE, CTX, ID, STR
    }
}
