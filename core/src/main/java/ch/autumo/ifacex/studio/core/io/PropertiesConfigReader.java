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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ch.autumo.ifacex.studio.core.model.EntityConfig;
import ch.autumo.ifacex.studio.core.model.FieldMappingRec;
import ch.autumo.ifacex.studio.core.model.IpcConfig;
import ch.autumo.ifacex.studio.core.model.KeyModel;
import ch.autumo.ifacex.studio.core.model.Meta;
import ch.autumo.ifacex.studio.core.model.RwConfig;
import ch.autumo.ifacex.studio.core.model.Secret;

/**
 * Reads a legacy {@code .ifacex} (Java properties) configuration into the in-memory model.
 *
 * <p>Parsing is deliberately lenient: the file is read as UTF-8, comment lines ({@code #}/{@code !})
 * carry the {@link Meta} header, and all remaining {@code key=value} pairs are classified via
 * {@link KeyModel} into reader/writer sections, wildcard defaults and per-entity settings.</p>
 */
public final class PropertiesConfigReader {

    private static final String S_MAPPING_ENTITY = "_mapping_entity";

    private PropertiesConfigReader() {
    }

    /**
     * Reads a legacy {@code .ifacex} file from disk.
     *
     * @param path file to read (UTF-8)
     * @return parsed configuration
     * @throws IOException when the file cannot be read
     */
    public static IpcConfig read(Path path) throws IOException {
        return read(Files.readAllLines(path, StandardCharsets.UTF_8));
    }

    /**
     * Parses a legacy configuration from already loaded lines, e.g. the lines of an in-memory
     * editor buffer.
     *
     * @param lines file lines (without line terminators)
     * @return parsed configuration
     */
    public static IpcConfig read(List<String> lines) {
        final Map<String, List<String>> comments = new LinkedHashMap<>();
        final Map<String, String> flat = parseRaw(lines, comments);
        // Reading a legacy file is a conversion: the new file is dated today, the date of the
        // legacy header belongs to the old file.
        return fromFlat(flat, CommentHeader.parse(lines).withDate(CommentHeader.now()), comments);
    }

    /**
     * Parses the raw {@code key=value} lines into an order-preserving map, skipping comments
     * and blank lines.
     *
     * <p>The escaping follows {@link java.util.Properties}, because the engine loads the same
     * files with it: {@code \:}, {@code \=}, {@code \ }, {@code \\}, {@code \t}, {@code \n},
     * {@code \r}, {@code \f} and {@code \}{@code uXXXX} are resolved in keys and values. This
     * matters for entity names that contain such characters - e.g. a legacy file may hold
     * {@code csv_out_*\:apache\:log4j\:2.15.0_mapping_entity}, which denotes the entity
     * {@code *:apache:log4j:2.15.0}.</p>
     *
     * @param lines file lines (without line terminators)
     * @return ordered key/value pairs, meta keys included
     */
    public static Map<String, String> parseRaw(List<String> lines) {
        return parseRaw(lines, null);
    }

    /**
     * Parses the raw lines and collects the comments that document a key.
     *
     * <p>A comment block belongs to the key it is written directly above: only a contiguous run
     * of comment lines that is not separated from the key by a blank line is attached. That keeps
     * the documentation of a parameter ({@code # Auth host name} above {@code rest_in_auth_host})
     * and drops the free-standing banners ({@code ####...}, {@code #-- READER ---}) and the file
     * header, which is handled by {@link CommentHeader}.</p>
     *
     * @param lines file lines (without line terminators)
     * @param commentsOut receives the comment lines per key, may be {@code null}
     * @return ordered key/value pairs, meta keys included
     */
    static Map<String, String> parseRaw(List<String> lines, Map<String, List<String>> commentsOut) {
        final Map<String, String> flat = new LinkedHashMap<>();
        final List<String> logical = logicalLines(lines);
        List<String> comments = new ArrayList<>();
        for (final String raw : logical) {
            if (raw.isEmpty()) {
                // A blank line separates the banner comments from the key comments.
                comments = new ArrayList<>();
                continue;
            }
            final char c0 = raw.charAt(0);
            if (c0 == '#' || c0 == '!') {
                comments.add(raw);
                continue;
            }
            final int i = separatorIndex(raw);
            if (i < 0) {
                continue;
            }
            final String key = unescape(raw.substring(0, i).trim());
            if (!key.isEmpty()) {
                flat.put(key, unescape(raw.substring(i + 1).trim()));
                if (commentsOut != null && !comments.isEmpty()) {
                    commentsOut.put(key, comments);
                }
            }
            comments = new ArrayList<>();
        }
        return flat;
    }

    /**
     * Joins physical lines that end with an odd number of backslashes (a property line
     * continuation) into one logical line.
     *
     * @param lines file lines
     * @return logical lines
     */
    private static List<String> logicalLines(List<String> lines) {
        final List<String> logical = new ArrayList<>();
        final StringBuilder current = new StringBuilder();
        for (final String line : lines) {
            current.append(line);
            if (endsWithContinuation(current)) {
                current.setLength(current.length() - 1); // drop the continuation backslash
                continue;
            }
            logical.add(current.toString().stripLeading());
            current.setLength(0);
        }
        if (current.length() > 0) {
            logical.add(current.toString().stripLeading());
        }
        return logical;
    }

    private static boolean endsWithContinuation(CharSequence s) {
        int backslashes = 0;
        for (int i = s.length() - 1; i >= 0 && s.charAt(i) == '\\'; i--) {
            backslashes++;
        }
        return backslashes % 2 == 1;
    }

    /**
     * The index of the first unescaped separator ({@code =} or {@code :}) of a property line.
     *
     * @param line logical property line
     * @return index of the separator, or {@code -1} when there is none
     */
    private static int separatorIndex(String line) {
        for (int i = 0; i < line.length(); i++) {
            final char c = line.charAt(i);
            if (c == '\\') {
                i++; // skip the escaped character
            } else if (c == '=' || c == ':') {
                return i;
            }
        }
        return -1;
    }

    /**
     * Resolves the escape sequences of a property key or value.
     *
     * @param s raw key or value
     * @return unescaped text
     */
    static String unescape(String s) {
        if (s.indexOf('\\') < 0) {
            return s;
        }
        final StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (++i >= s.length()) {
                break;
            }
            final char e = s.charAt(i);
            switch (e) {
                case 't' -> sb.append('\t');
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                case 'f' -> sb.append('\f');
                case 'u' -> {
                    if (i + 4 < s.length()) {
                        try {
                            sb.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
                            i += 4;
                        } catch (NumberFormatException ex) {
                            sb.append(e);
                        }
                    } else {
                        sb.append(e);
                    }
                }
                default -> sb.append(e);
            }
        }
        return sb.toString();
    }

    /**
     * Builds the model from an already parsed, order-preserving key/value map plus metadata.
     * Shared with the YAML reader to guarantee identical classification.
     *
     * @param flat flat engine keys, e.g. {@code csv_in_global_batch_mode}
     * @param meta metadata for the comment header
     * @return the parsed configuration
     */
    public static IpcConfig fromFlat(Map<String, String> flat, Meta meta) {
        return fromFlat(flat, meta, null);
    }

    /**
     * Builds the model from an already parsed key/value map plus metadata and the comments that
     * document the keys.
     *
     * @param flat flat engine keys, e.g. {@code csv_in_global_batch_mode}
     * @param meta metadata for the comment header
     * @param comments comment lines per flat key, may be {@code null}
     * @return the parsed configuration
     */
    public static IpcConfig fromFlat(Map<String, String> flat, Meta meta,
            Map<String, List<String>> comments) {
        final IpcConfig cfg = new IpcConfig();
        cfg.setMeta(meta);
        if (comments != null) {
            comments.forEach((k, v) -> {
                if (flat.containsKey(k)) {
                    cfg.comments().put(k, v);
                }
            });
        }

        cfg.setSourceInterface(flat.getOrDefault(KeyModel.SOURCE_INTERFACE, ""));
        cfg.destinationInterfaces().addAll(splitList(flat.get(KeyModel.DESTINATION_INTERFACES)));
        cfg.processSourceEntities().addAll(splitList(flat.get(KeyModel.PROCESS_SOURCE_ENTITIES)));

        final List<String> entities = inferEntities(flat, cfg);

        // The mapping fields of every scope, ordered by their index: the order of the file is
        // kept, no matter whether a field is inserted or not.
        final Map<String, TreeMap<String, FieldMappingRec>> mapFields = new LinkedHashMap<>();

        for (final Map.Entry<String, String> e : flat.entrySet()) {
            final String key = e.getKey();
            final String val = e.getValue();

            if (key.equals(KeyModel.SOURCE_INTERFACE)
                    || key.equals(KeyModel.DESTINATION_INTERFACES)
                    || key.equals(KeyModel.PROCESS_SOURCE_ENTITIES)) {
                continue;
            }

            final String prefix = KeyModel.rwPrefix(key);
            if (prefix == null) {
                cfg.general().put(key, value(val));
                continue;
            }

            final RwConfig rw = KeyModel.isReaderPrefix(prefix)
                    ? cfg.ensureReader(prefix)
                    : cfg.ensureWriter(prefix);
            final String rest = KeyModel.remainder(key, prefix);

            if (rest.startsWith(KeyModel.WILDCARD + "_")) {
                rw.wildcard().put(rest.substring(2), value(val));
                continue;
            }

            String entity = null;
            String param = rest;
            for (final String candidate : entities) {
                if (rest.length() > candidate.length()
                        && rest.startsWith(candidate)
                        && rest.charAt(candidate.length()) == '_') {
                    entity = candidate;
                    param = rest.substring(candidate.length() + 1);
                    break;
                }
            }

            if (entity == null) {
                rw.global().put(rest, value(val));
                continue;
            }

            final EntityConfig ec = rw.ensureEntity(entity);
            if (param.equals(KeyModel.P_FIELDS)) {
                ec.fields().clear();
                ec.fields().addAll(splitList(val));
            } else if (param.equals(KeyModel.P_MAPPING_ENTITY)) {
                ec.ensureMapping().setDestEntity(val);
            } else if (param.startsWith(KeyModel.PFX_MAPPING_FIELD)
                    || param.startsWith(KeyModel.PFX_MAPPING_INSERT)) {
                final boolean insert = param.startsWith(KeyModel.PFX_MAPPING_INSERT);
                final String idxStr = param.substring(
                        (insert ? KeyModel.PFX_MAPPING_INSERT : KeyModel.PFX_MAPPING_FIELD).length());
                final Integer idx = parseIntOrNull(idxStr);
                if (idx == null) {
                    continue;
                }
                final String[] parts = splitFirstColon(val);
                final FieldMappingRec rec = new FieldMappingRec(parts[0], parts[1], insert);
                mapFields.computeIfAbsent(scopeKey(prefix, entity), _ -> new TreeMap<>())
                        .put(mappingOrderKey(insert, idx), rec);
                ec.ensureMapping();
            } else {
                ec.settings().put(param, value(val));
            }
        }

        assembleMappings(cfg, mapFields);
        if (isBaseConfiguration(flat)) {
            pruneToFile(cfg, flat);
        }
        return cfg;
    }

    /**
     * Whether the file is a general (base) configuration rather than an interface processor
     * configuration: it declares none of the job wiring keys.
     *
     * @param flat flat engine keys
     * @return {@code true} for a base configuration
     */
    private static boolean isBaseConfiguration(Map<String, String> flat) {
        return !flat.containsKey(KeyModel.SOURCE_INTERFACE)
                && !flat.containsKey(KeyModel.DESTINATION_INTERFACES)
                && !flat.containsKey(KeyModel.PROCESS_SOURCE_ENTITIES);
    }

    /**
     * Drops the parameters that were only added by the catalogue pre-population.
     *
     * <p>A converted base configuration keeps exactly what the file contained - the complete
     * parameter catalogue of a reader/writer would only add defaults that the base file never
     * had. Sections that end up empty are removed.</p>
     *
     * @param cfg  the parsed configuration
     * @param flat the keys of the file
     */
    private static void pruneToFile(IpcConfig cfg, Map<String, String> flat) {
        for (final RwConfig rw : cfg.sections()) {
            final String name = rw.name();
            rw.global().keySet().removeIf(p -> !flat.containsKey(name + "_" + p));
            rw.wildcard().keySet().removeIf(p -> !flat.containsKey(name + "_" + KeyModel.WILDCARD + "_" + p));
            for (final Map.Entry<String, EntityConfig> e : rw.entities().entrySet()) {
                final String prefix = name + "_" + e.getKey() + "_";
                e.getValue().settings().keySet().removeIf(p -> !flat.containsKey(prefix + p));
            }
        }
        cfg.readers().values().removeIf(RwConfig::isEmpty);
        cfg.writers().values().removeIf(RwConfig::isEmpty);
    }

    private static void assembleMappings(IpcConfig cfg,
            Map<String, TreeMap<String, FieldMappingRec>> mapFields) {
        for (final RwConfig rw : cfg.sections()) {
            for (final Map.Entry<String, EntityConfig> en : rw.entities().entrySet()) {
                final EntityConfig ec = en.getValue();
                if (!ec.hasMapping()) {
                    continue;
                }
                final String k = scopeKey(rw.name(), en.getKey());
                ec.mapping().fields().clear();
                final TreeMap<String, FieldMappingRec> fields = mapFields.get(k);
                if (fields != null) {
                    ec.mapping().fields().addAll(fields.values());
                }
            }
        }
    }

    /**
     * The value of a parameter as the model holds it: an encoded secret becomes a
     * {@link Secret} (the structured form of the new format), a plain value stays a string.
     *
     * @param val the value of the legacy file
     * @return the value of the model
     */
    private static Object value(String val) {
        final Secret secret = Secret.parseLegacy(val);
        return secret != null ? secret : val;
    }

    /**
     * Entity candidates for classification, longest name first.
     *
     * <p>The authoritative source is {@code process_source_entities}. Entities that appear only in
     * an explicit {@code <rw>_<entity>_mapping_entity} key are inferred as well. We deliberately do
     * <em>not</em> infer from {@code *_fields}, because parameters like {@code dest_filter_fields}
     * end in {@code _fields} too and would otherwise be mistaken for an entity named
     * {@code <entity>_dest_filter}.</p>
     */
    private static List<String> inferEntities(Map<String, String> flat, IpcConfig cfg) {
        final LinkedHashSet<String> set = new LinkedHashSet<>(cfg.processSourceEntities());
        for (final String key : flat.keySet()) {
            final String prefix = KeyModel.rwPrefix(key);
            if (prefix == null) {
                continue;
            }
            final String rest = KeyModel.remainder(key, prefix);
            if (rest.startsWith(KeyModel.WILDCARD + "_")) {
                continue;
            }
            if (rest.endsWith(S_MAPPING_ENTITY)) {
                set.add(rest.substring(0, rest.length() - S_MAPPING_ENTITY.length()));
            }
            // Template blocks document themselves with placeholders: 'csv_in_<entity1>_fields'
            // names the entity '<entity1>', not a parameter '<entity1>_fields'. An angle bracket
            // name is never a real parameter, so this inference is unambiguous.
            final int closing = rest.indexOf('>');
            if (rest.startsWith("<") && closing > 1) {
                set.add(rest.substring(0, closing + 1));
            }
        }
        final List<String> list = new ArrayList<>(set);
        list.removeIf(String::isEmpty);
        // Longest entity name first so that "stock/metric" wins over a hypothetical "stock".
        list.sort(Comparator.comparingInt(String::length).reversed());
        return list;
    }

    private static String scopeKey(String prefix, String entity) {
        return prefix + "\u0000" + entity;
    }

    /**
     * The sort key of one mapping row within its entity.
     *
     * <p>Standard ({@code mapping_field_N}) and insert ({@code mapping_insert_N}) rows are two
     * independent index spaces in the legacy format - a file may well contain both
     * {@code mapping_field_00} and {@code mapping_insert_00}. They must not collide, and they
     * must keep the order the engine applies them in: all standard rows first, then all insert
     * rows, each in ascending index order.</p>
     *
     * @param insert whether the row is an insert-only mapping
     * @param index  the index of the row in its own index space
     * @return sort key
     */
    private static String mappingOrderKey(boolean insert, int index) {
        return (insert ? "1" : "0") + String.format(java.util.Locale.ROOT, "%09d", index);
    }

    static List<String> splitList(String value) {
        final List<String> result = new ArrayList<>();
        if (value == null) {
            return result;
        }
        for (final String part : value.split(",")) {
            final String t = part.trim();
            if (!t.isEmpty()) {
                result.add(t);
            }
        }
        return result;
    }

    private static String[] splitFirstColon(String value) {
        final int i = value.indexOf(':');
        if (i < 0) {
            return new String[] { value.trim(), "" };
        }
        return new String[] { value.substring(0, i).trim(), value.substring(i + 1) };
    }

    private static Integer parseIntOrNull(String s) {
        try {
            return Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
