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

import ch.autumo.ifacex.studio.core.model.Meta;

/**
 * Renders and parses the comment header block (Title, Version, Notes, Author, Date).
 *
 * <p>Byte-compatible with the legacy Studio's {@code store0}/{@code writeComments}:
 * a 79-character {@code #} ruler, a dashed separator, and note lines prefixed with
 * {@code "#   "}. Non-ASCII characters are escaped as {@code \\uXXXX}.</p>
 */
public final class CommentHeader {

    private static final String RULER = "#".repeat(79);
    private static final String DASHES = "#" + "-".repeat(78);

    private CommentHeader() {
    }

    /**
     * Renders the header block, ending with a newline after the closing ruler.
     *
     * @param meta metadata to render
     * @return the comment header text
     */
    public static String render(Meta meta) {
        final StringBuilder sb = new StringBuilder();
        sb.append(RULER).append('\n');
        sb.append('#').append('\n');
        writeComments(sb, "Title", meta.title(), false);
        writeComments(sb, "Version", meta.version(), false);
        sb.append('#').append('\n');
        // The legacy GUI stores notes without a trailing newline; parse() appends one per line,
        // so drop exactly one here to reproduce the original header byte-for-byte.
        String notes = meta.notes();
        if (notes.endsWith("\n")) {
            notes = notes.substring(0, notes.length() - 1);
        }
        writeComments(sb, "Notes", notes, true);
        sb.append('#').append('\n');
        sb.append(DASHES).append('\n');
        sb.append('#').append('\n');
        writeComments(sb, "Author", meta.author(), false);
        writeComments(sb, "Date", meta.date(), false);
        sb.append('#').append('\n');
        sb.append(RULER).append('\n');
        return sb.toString();
    }

    /**
     * Parses the metadata out of a configuration file's comment lines.
     *
     * @param lines all lines of the file, comment header included
     * @return parsed metadata; missing fields keep their default values
     */
    public static Meta parse(List<String> lines) {
        String title = "";
        String author = "";
        String version = "";
        String date = "";
        final StringBuilder notes = new StringBuilder();
        for (final String line : lines) {
            if (line.startsWith("#  Title: ")) {
                title = line.substring(10);
            } else if (line.startsWith("#  Author: ")) {
                author = line.substring(11);
            } else if (line.startsWith("#  Version: ")) {
                version = line.substring(12);
            } else if (line.startsWith("#  Date: ")) {
                date = line.substring(9);
            } else if (line.startsWith("#   ")) {
                notes.append(line.substring(4)).append('\n');
            }
        }
        if (version.isEmpty()) {
            version = "1.0";
        }
        if (notes.length() == 0) {
            notes.append('-').append('\n');
        }
        return new Meta(title, version, author, notes.toString(), date);
    }

    private static void writeComments(StringBuilder sb, String id, String comments, boolean newline) {
        sb.append("#  ");
        final String all = newline ? (id + ":\n" + comments) : (id + ": " + comments);
        final int len = all.length();
        int current = 0;
        int last = 0;
        while (current < len) {
            final char c = all.charAt(current);
            if (c > '\u00ff' || c == '\n' || c == '\r') {
                if (last != current) {
                    sb.append(all, last, current);
                }
                if (c > '\u00ff') {
                    sb.append(String.format("\\u%04X", (int) c));
                } else {
                    sb.append('\n');
                    if (c == '\r' && current != len - 1 && all.charAt(current + 1) == '\n') {
                        current++;
                    }
                    if (current == len - 1
                            || (all.charAt(current + 1) != '#' && all.charAt(current + 1) != '!')) {
                        sb.append("#   ");
                    }
                }
                last = current + 1;
            }
            current++;
        }
        if (last != current) {
            sb.append(all, last, current);
        }
        sb.append('\n');
    }
}
