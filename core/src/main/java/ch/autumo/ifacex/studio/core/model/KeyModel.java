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
package ch.autumo.ifacex.studio.core.model;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Knowledge about the engine's flat key naming model (see docs/09-engine-config-model.md).
 *
 * <p>Keys are {@code <rw>_<param>} (global), {@code <rw>_<entity>_<param>} (entity) or
 * {@code <rw>_*_<param>} (wildcard default for all entities). This class detects the
 * reader/writer prefix and the fixed section order used by the legacy writer.</p>
 */
public final class KeyModel {

    /** Job-level key: the single source interface, e.g. {@code rest_in}. */
    public static final String SOURCE_INTERFACE = "source_interface";
    /** Job-level key: comma-separated destination interfaces. */
    public static final String DESTINATION_INTERFACES = "destination_interfaces";
    /** Job-level key: comma-separated global source entities. */
    public static final String PROCESS_SOURCE_ENTITIES = "process_source_entities";

    /** Entity-scoped key: the source field list. */
    public static final String P_FIELDS = "fields";
    /** Entity-scoped key: the destination entity of a writer mapping. */
    public static final String P_MAPPING_ENTITY = "mapping_entity";
    /** Entity-scoped key prefix: a standard mapping record. */
    public static final String PFX_MAPPING_FIELD = "mapping_field_";
    /** Entity-scoped key prefix: an insert-only mapping record. */
    public static final String PFX_MAPPING_INSERT = "mapping_insert_";

    /** Wildcard scope marker. */
    public static final String WILDCARD = "*";

    /** Reader section order used by the legacy properties writer (store0). */
    public static final List<String> READER_ORDER =
            List.of("csv_in", "json_in", "doc_in", "db_in", "mongo_in", "rest_in");
    /** Writer section order used by the legacy properties writer (store0). */
    public static final List<String> WRITER_ORDER =
            List.of("csv_out", "json_out", "mail_out", "db_out", "rest_out", "mongo_out", "code", "exec");

    private static final Pattern RW = Pattern.compile("^([a-z0-9]+)_(in|out)_.*$");

    private KeyModel() {
    }

    /**
     * Returns the reader/writer prefix for a key.
     *
     * @param key flat engine key, e.g. {@code csv_in_global_x}
     * @return prefix such as {@code csv_in}, {@code code} or {@code exec}; {@code null} when the
     *         key is not reader/writer scoped
     */
    public static String rwPrefix(String key) {
        final Matcher m = RW.matcher(key);
        if (m.matches()) {
            return m.group(1) + "_" + m.group(2);
        }
        if (key.startsWith("exec_")) {
            return "exec";
        }
        if (key.startsWith("code_")) {
            return "code";
        }
        return null;
    }

    /**
     * Whether a reader/writer prefix denotes a reader ({@code *_in}, and the special
     * {@code null_in} / {@code file_in}).
     *
     * @param prefix prefix from {@link #rwPrefix(String)}
     * @return {@code true} for a reader prefix
     */
    public static boolean isReaderPrefix(String prefix) {
        return prefix != null && (prefix.endsWith("_in") || prefix.equals("null_in") || prefix.equals("file_in"));
    }

    /**
     * Whether a reader/writer prefix denotes a writer. Every prefix that is not a reader prefix
     * counts as a writer.
     *
     * @param prefix prefix from {@link #rwPrefix(String)}
     * @return {@code true} for a writer prefix
     */
    public static boolean isWriterPrefix(String prefix) {
        return prefix != null && !isReaderPrefix(prefix);
    }

    /**
     * Strips the reader/writer prefix and the separating underscore.
     *
     * @param key    full key, e.g. {@code csv_in_ent_fields}
     * @param prefix prefix from {@link #rwPrefix(String)}, e.g. {@code csv_in}
     * @return the remainder, e.g. {@code ent_fields}
     */
    public static String remainder(String key, String prefix) {
        return key.substring(prefix.length() + 1);
    }
}
