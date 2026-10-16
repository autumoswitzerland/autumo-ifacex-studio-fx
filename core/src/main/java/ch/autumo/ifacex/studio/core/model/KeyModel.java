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

    /** Cross-cutting keys. */
    public static final String SOURCE_INTERFACE = "source_interface";
    public static final String DESTINATION_INTERFACES = "destination_interfaces";
    public static final String PROCESS_SOURCE_ENTITIES = "process_source_entities";

    /** Special entity-scoped parameters. */
    public static final String P_FIELDS = "fields";
    public static final String P_MAPPING_ENTITY = "mapping_entity";
    public static final String PFX_MAPPING_FIELD = "mapping_field_";
    public static final String PFX_MAPPING_INSERT = "mapping_insert_";

    /** Wildcard scope marker. */
    public static final String WILDCARD = "*";

    /** Section order used by the legacy properties writer (store0). */
    public static final List<String> READER_ORDER =
            List.of("csv_in", "json_in", "doc_in", "db_in", "mongo_in", "rest_in");
    public static final List<String> WRITER_ORDER =
            List.of("csv_out", "json_out", "mail_out", "db_out", "rest_out", "mongo_out", "code", "exec");

    private static final Pattern RW = Pattern.compile("^([a-z0-9]+)_(in|out)_.*$");

    private KeyModel() {
    }

    /**
     * Return the reader/writer prefix for a key, or {@code null} if the key is not
     * reader/writer scoped.
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

    public static boolean isReaderPrefix(String prefix) {
        return prefix != null && (prefix.endsWith("_in") || prefix.equals("null_in") || prefix.equals("file_in"));
    }

    public static boolean isWriterPrefix(String prefix) {
        return prefix != null && !isReaderPrefix(prefix);
    }

    /** Strip the reader/writer prefix and the leading underscore, returning the remainder. */
    public static String remainder(String key, String prefix) {
        return key.substring(prefix.length() + 1);
    }
}
