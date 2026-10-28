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

import java.util.ArrayList;
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

    /** Date pattern of the legacy header, e.g. {@code Fri Apr 10 04:48:42 CEST 2020}. */
    private static final String DATE_PATTERN = "EEE MMM dd HH:mm:ss zzz yyyy";

    /** Width of a note line, the {@code "#   "} prefix included. */
    private static final int NOTE_WIDTH = 80;
    /** Prefix of a note line. */
    private static final String NOTE_PREFIX = "#   ";

    private CommentHeader() {
    }

    /**
     * The current date in the format of the header.
     *
     * <p>A conversion creates a new file, so its header carries the date of the conversion and
     * not the date of the legacy configuration it was made from.</p>
     *
     * @return the current date, e.g. {@code Tue Oct 27 22:05:12 CET 2026}
     */
    public static String now() {
        return new java.text.SimpleDateFormat(DATE_PATTERN, java.util.Locale.ENGLISH)
                .format(new java.util.Date());
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
        // writeComments() terminates the last line itself: trailing line breaks would produce
        // an empty line between the notes and the closing '#' line.
        String notes = wrapNotes(meta.notes()).stripTrailing();
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
        final List<String> header = new ArrayList<>();
        for (final String line : lines) {
            // The header is the first comment block without a blank line in it; everything
            // after the blank line documents the parameters and belongs to their keys.
            // Leading blank lines are skipped, files may start with one.
            if (line.isBlank()) {
                if (!header.isEmpty()) {
                    break;
                }
                continue;
            }
            header.add(line);
            if (line.startsWith("#  Title: ")) {
                title = line.substring(10);
            } else if (line.startsWith("#  Author: ")) {
                author = line.substring(11);
            } else if (line.startsWith("#  Version: ")) {
                version = line.substring(12);
            } else if (line.startsWith("#  Date: ")) {
                date = line.substring(9);
            }
        }

        // The notes start after '#  Notes:' and end at the dashed separator. A note line is
        // written as '#   <text>'; an *empty* note line is either '#   ' (what the legacy writer
        // produced) or just '#' (hand-written files) - both must stay an empty line.
        final List<String> notes = new ArrayList<>();
        boolean inNotes = false;
        for (final String line : header) {
            if (line.startsWith("#  Notes:")) {
                inNotes = true;
                continue;
            }
            if (!inNotes) {
                continue;
            }
            if (line.startsWith("#---") || line.startsWith("###")) {
                break;
            }
            if (line.startsWith("#   ")) {
                notes.add(line.substring(4));
            } else if (line.equals("#") || line.startsWith("# ")) {
                notes.add("");
            } else if (line.startsWith("#  ")) {
                continue; // another field (Author/Date, ...) ends the notes
            } else {
                break;
            }
        }
        // The '#' that closes the block is the last one before the separator - it is not a note.
        while (!notes.isEmpty() && notes.get(notes.size() - 1).isEmpty()) {
            notes.remove(notes.size() - 1);
        }

        // A line break inside a paragraph is only the wrapping of the file; the paragraph is
        // one text in the form. An empty note line separates paragraphs.
        final StringBuilder text = new StringBuilder();
        final StringBuilder paragraph = new StringBuilder();
        for (final String note : notes) {
            if (note.isEmpty()) {
                if (paragraph.length() > 0) {
                    text.append(paragraph).append('\n');
                    paragraph.setLength(0);
                }
                text.append('\n');
                continue;
            }
            if (paragraph.length() > 0) {
                paragraph.append(' ');
            }
            paragraph.append(note.strip());
        }
        if (paragraph.length() > 0) {
            text.append(paragraph).append('\n');
        }
        if (text.length() == 0) {
            text.append('-').append('\n');
        }
        if (version.isEmpty()) {
            version = "1.0";
        }
        return new Meta(title, version, author, text.toString(), date);
    }


    /**
     * Wraps the notes so that no note line is wider than {@link #NOTE_WIDTH} characters with its
     * prefix. A paragraph is wrapped at word boundaries; an empty line stays an empty line.
     *
     * @param notes the notes of the metadata
     * @return the wrapped notes, every line terminated by a newline
     */
    static String wrapNotes(String notes) {
        final int width = NOTE_WIDTH - NOTE_PREFIX.length();
        final StringBuilder sb = new StringBuilder();
        for (final String line : notes.split("\n", -1)) {
            if (line.isEmpty()) {
                sb.append('\n');
                continue;
            }
            String rest = line.strip();
            while (rest.length() > width) {
                int cut = rest.lastIndexOf(' ', width);
                if (cut <= 0) {
                    // No space to break at: keep the word (e.g. a long URL) in one piece, a
                    // broken URL would be joined with a space when the file is read again.
                    break;
                }
                sb.append(rest, 0, cut).append('\n');
                rest = rest.substring(cut).stripLeading();
            }
            sb.append(rest).append('\n');
        }
        return sb.toString();
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
