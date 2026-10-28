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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ch.autumo.ifacex.studio.core.model.EntityConfig;
import ch.autumo.ifacex.studio.core.model.FieldMappingRec;
import ch.autumo.ifacex.studio.core.model.IpcConfig;
import ch.autumo.ifacex.studio.core.model.KeyModel;
import ch.autumo.ifacex.studio.core.model.RwConfig;
import ch.autumo.ifacex.studio.core.model.Secret;

/**
 * Writes the model back to the legacy {@code .ifacex} (Java properties) format.
 *
 * <p>Reproduces the legacy Studio layout: comment header, then the fixed sections
 * (Reader, Writers, Source Entities, Overwritten Values) followed by per reader/writer
 * sections in a fixed order with alphabetically sorted keys.</p>
 */
public final class PropertiesConfigWriter {

    private static final List<String> READER_ORDER =
            List.of("csv_in", "json_in", "doc_in", "db_in", "mongo_in", "rest_in");
    private static final List<String> WRITER_ORDER =
            List.of("csv_out", "json_out", "mail_out", "db_out", "rest_out", "mongo_out", "code", "exec");

    private PropertiesConfigWriter() {
    }

    /**
     * Writes the model in the legacy properties format (UTF-8).
     *
     * @param path target file, overwritten when it exists
     * @param cfg  configuration to write
     * @throws IOException when the file cannot be written
     */
    public static void write(Path path, IpcConfig cfg) throws IOException {
        Files.writeString(path, toText(cfg), StandardCharsets.UTF_8);
    }

    /**
     * The value of a parameter as the engine reads it: an encoded secret in its legacy notation
     * ({@code {mode:value}}), a plain value as it is.
     *
     * @param value the value of the model
     * @return the text for the engine
     */
    private static String value(Object value) {
        if (value instanceof Secret secret) {
            return secret.toLegacy();
        }
        return value == null ? "" : value.toString();
    }

    /**
     * Flattens the model back to the canonical key/value map, without the meta keys.
     *
     * @param cfg configuration to flatten
     * @return ordered engine keys and values
     */
    public static Map<String, String> toFlatMap(IpcConfig cfg) {
        final Map<String, String> flat = new LinkedHashMap<>();
        flat.put(KeyModel.SOURCE_INTERFACE, nullToEmpty(cfg.sourceInterface()));
        flat.put(KeyModel.DESTINATION_INTERFACES, String.join(",", cfg.destinationInterfaces()));
        flat.put(KeyModel.PROCESS_SOURCE_ENTITIES, String.join(",", cfg.processSourceEntities()));
        cfg.general().forEach((p, v) -> flat.put(p, value(v)));

        for (final RwConfig rw : cfg.sections()) {
            final String n = rw.name();
            rw.global().forEach((p, v) -> flat.put(n + "_" + p, value(v)));
            rw.wildcard().forEach((p, v) -> flat.put(n + "_" + KeyModel.WILDCARD + "_" + p, value(v)));
            for (final Map.Entry<String, EntityConfig> en : rw.entities().entrySet()) {
                final String entity = en.getKey();
                final EntityConfig ec = en.getValue();
                ec.settings().forEach((p, v) -> {
                    // The engine resolves these two with Class.forName() as soon as the key
                    // exists - an empty value would abort the run, so an unconfigured class
                    // must be omitted instead of written as 'key='.
                    if ((p.equals("data_mapper_class") || p.equals("exclusion_filter_class"))
                            && value(v).isEmpty()) {
                        return;
                    }
                    flat.put(n + "_" + entity + "_" + p, value(v));
                });
                if (!ec.fields().isEmpty()) {
                    flat.put(n + "_" + entity + "_fields", String.join(",", ec.fields()));
                }
                if (ec.hasMapping()) {
                    if (ec.mapping().destEntity() != null) {
                        flat.put(n + "_" + entity + "_mapping_entity", ec.mapping().destEntity());
                    }
                    int stdIdx = 0;
                    int insIdx = 0;
                    for (final FieldMappingRec rec : ec.mapping().fields()) {
                        if (rec.insert()) {
                            flat.put(n + "_" + entity + "_" + KeyModel.PFX_MAPPING_INSERT + pad(insIdx++),
                                    rec.destField() + ":" + rec.formula());
                        } else {
                            flat.put(n + "_" + entity + "_" + KeyModel.PFX_MAPPING_FIELD + pad(stdIdx++),
                                    rec.destField() + ":" + rec.formula());
                        }
                    }
                }
            }
        }
        return flat;
    }

    /**
     * Renders the model as the full legacy file text, including the comment header and the
     * fixed section order.
     *
     * @param cfg configuration to render
     * @return file text
     */
    public static String toText(IpcConfig cfg) {
        final Map<String, String> flat = toFlatMap(cfg);

        final String sourceInterface = nullToEmpty(cfg.sourceInterface());
        final String destinationInterfaces = String.join(",", cfg.destinationInterfaces());
        final String processSourceEntities = String.join(",", cfg.processSourceEntities());

        final Map<String, List<String>> buckets = new LinkedHashMap<>();
        for (final String b : allBuckets()) {
            buckets.put(b, new ArrayList<>());
        }
        buckets.put("overwritten", new ArrayList<>());

        for (final String key : flat.keySet()) {
            if (key.equals(KeyModel.SOURCE_INTERFACE)
                    || key.equals(KeyModel.DESTINATION_INTERFACES)
                    || key.equals(KeyModel.PROCESS_SOURCE_ENTITIES)) {
                continue;
            }
            buckets.get(bucket(key)).add(key);
        }
        buckets.values().forEach(Collections::sort);

        final StringBuilder sb = new StringBuilder();
        sb.append(CommentHeader.render(cfg.meta()));
        sb.append('\n').append('\n').append('\n');

        sb.append("#-------- Reader ---------------------").append('\n');
        sb.append(KeyModel.SOURCE_INTERFACE).append('=').append(sourceInterface).append('\n');
        sb.append('\n').append('\n');

        sb.append("#-------- Writers --------------------").append('\n');
        sb.append(KeyModel.DESTINATION_INTERFACES).append('=').append(destinationInterfaces).append('\n');
        sb.append('\n').append('\n');

        sb.append("#-------- Source Entities ------------").append('\n');
        sb.append(KeyModel.PROCESS_SOURCE_ENTITIES).append('=').append(processSourceEntities).append('\n');
        sb.append('\n').append('\n');

        writeBucket(sb, flat, buckets, "overwritten", "Overwritten Values");

        for (final String rw : READER_ORDER) {
            writeBucket(sb, flat, buckets, rw, rw);
        }
        writeBucket(sb, flat, buckets, "other_in", "<other>_in");

        for (final String rw : WRITER_ORDER) {
            writeBucket(sb, flat, buckets, rw, rw);
        }
        writeBucket(sb, flat, buckets, "other_out", "<other>_out");

        return sb.toString();
    }

    private static void writeBucket(StringBuilder sb, Map<String, String> flat,
            Map<String, List<String>> buckets, String bucket, String label) {
        final List<String> keys = buckets.get(bucket);
        if (keys == null || keys.isEmpty()) {
            return;
        }
        if ("overwritten".equals(bucket)) {
            sb.append("#-------- Overwritten Values ---------").append('\n');
            for (final String key : keys) {
                sb.append(escapeKey(key)).append('=').append(escapeValue(flat.get(key))).append('\n');
            }
        } else {
            writeSection(sb, flat, keys, label);
        }
        sb.append('\n').append('\n');
    }

    private static void writeSection(StringBuilder sb, Map<String, String> flat,
            List<String> keys, String label) {
        final int len = 20 - (label.length() + 1);
        final String end = "-".repeat(Math.max(0, len));
        sb.append("#-------- ").append(label.endsWith("_in") ? "Reader: " : "Writer: ")
                .append(label).append(' ').append(end).append('\n');
        for (final String key : keys) {
            sb.append(escapeKey(key)).append('=').append(escapeValue(flat.get(key))).append('\n');
        }
    }

    /**
     * Escapes a property key so that {@link java.util.Properties} - and with it the engine and
     * {@link PropertiesConfigReader} - reads back exactly the key of the model.
     *
     * <p>Only the characters that would otherwise break the key are escaped; a key without them
     * (the normal case, e.g. {@code csv_out_articles_mapping_entity}) is written unchanged.
     * Entity names may contain colons or spaces, e.g. {@code *:apache:log4j:2.15.0}, which
     * become {@code *\:apache\:log4j\:2.15.0}.</p>
     *
     * @param key key of the model
     * @return key as written to the file
     */
    static String escapeKey(String key) {
        final StringBuilder sb = new StringBuilder(key.length() + 8);
        for (int i = 0; i < key.length(); i++) {
            final char c = key.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case ':' -> sb.append("\\:");
                case '=' -> sb.append("\\=");
                case ' ' -> sb.append("\\ ");
                case '\t' -> sb.append("\\t");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Escapes a property value so that {@link java.util.Properties} reads back exactly the value
     * of the model. Only backslashes and a leading space need escaping: a colon, an equals sign
     * or a hash inside a value is preserved by the parser, so URLs and formulas stay readable.
     *
     * @param value value of the model
     * @return value as written to the file
     */
    static String escapeValue(String value) {
        if (value == null) {
            return "";
        }
        final StringBuilder sb = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '\t' -> sb.append("\\t");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case ' ' -> sb.append(i == 0 ? "\\ " : " ");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String bucket(String key) {
        if (key.startsWith("csv_in_")) {
            return "csv_in";
        }
        if (key.startsWith("json_in_")) {
            return "json_in";
        }
        if (key.startsWith("doc_in_")) {
            return "doc_in";
        }
        if (key.startsWith("db_in_")) {
            return "db_in";
        }
        if (key.startsWith("rest_in_")) {
            return "rest_in";
        }
        if (key.startsWith("mongo_in_")) {
            return "mongo_in";
        }
        if (key.startsWith("csv_out_")) {
            return "csv_out";
        }
        if (key.startsWith("json_out_")) {
            return "json_out";
        }
        if (key.startsWith("mail_out_")) {
            return "mail_out";
        }
        if (key.startsWith("db_out_")) {
            return "db_out";
        }
        if (key.startsWith("rest_out_")) {
            return "rest_out";
        }
        if (key.startsWith("mongo_out_")) {
            return "mongo_out";
        }
        if (key.startsWith("code_")) {
            return "code";
        }
        if (key.startsWith("exec_")) {
            return "exec";
        }
        if (key.contains("_in_")) {
            return "other_in";
        }
        if (key.contains("_out_")) {
            return "other_out";
        }
        return "overwritten";
    }

    private static List<String> allBuckets() {
        final List<String> all = new ArrayList<>(READER_ORDER);
        all.add("other_in");
        all.addAll(WRITER_ORDER);
        all.add("other_out");
        return all;
    }

    private static String pad(int index) {
        return String.format("%03d", index);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
