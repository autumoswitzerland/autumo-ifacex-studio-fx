/*
 * Copyright 2026 autumo GmbH.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
        return fromFlat(parseRaw(lines), CommentHeader.parse(lines));
    }

    /**
     * Parses the raw {@code key=value} lines into an order-preserving map, skipping comments
     * and blank lines. Values are kept verbatim (no escape processing) to mirror the legacy writer.
     *
     * @param lines file lines (without line terminators)
     * @return ordered key/value pairs, meta keys included
     */
    public static Map<String, String> parseRaw(List<String> lines) {
        final Map<String, String> flat = new LinkedHashMap<>();
        for (final String raw : lines) {
            if (raw.isEmpty()) {
                continue;
            }
            final char c0 = raw.charAt(0);
            if (c0 == '#' || c0 == '!') {
                continue;
            }
            final int i = raw.indexOf('=');
            if (i < 0) {
                continue;
            }
            final String key = raw.substring(0, i).trim();
            if (!key.isEmpty()) {
                flat.put(key, raw.substring(i + 1));
            }
        }
        return flat;
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
        final IpcConfig cfg = new IpcConfig();
        cfg.setMeta(meta);

        cfg.setSourceInterface(flat.getOrDefault(KeyModel.SOURCE_INTERFACE, ""));
        cfg.destinationInterfaces().addAll(splitList(flat.get(KeyModel.DESTINATION_INTERFACES)));
        cfg.processSourceEntities().addAll(splitList(flat.get(KeyModel.PROCESS_SOURCE_ENTITIES)));

        final List<String> entities = inferEntities(flat, cfg);

        final Map<String, TreeMap<Integer, FieldMappingRec>> stdFields = new LinkedHashMap<>();
        final Map<String, TreeMap<Integer, FieldMappingRec>> insFields = new LinkedHashMap<>();

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
                cfg.general().put(key, val);
                continue;
            }

            final RwConfig rw = KeyModel.isReaderPrefix(prefix)
                    ? cfg.ensureReader(prefix)
                    : cfg.ensureWriter(prefix);
            final String rest = KeyModel.remainder(key, prefix);

            if (rest.startsWith(KeyModel.WILDCARD + "_")) {
                rw.wildcard().put(rest.substring(2), val);
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
                rw.global().put(rest, val);
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
                (insert ? insFields : stdFields)
                        .computeIfAbsent(scopeKey(prefix, entity), k -> new TreeMap<>())
                        .put(idx, rec);
                ec.ensureMapping();
            } else {
                ec.settings().put(param, val);
            }
        }

        assembleMappings(cfg, stdFields, insFields);
        return cfg;
    }

    private static void assembleMappings(IpcConfig cfg,
            Map<String, TreeMap<Integer, FieldMappingRec>> stdFields,
            Map<String, TreeMap<Integer, FieldMappingRec>> insFields) {
        for (final RwConfig rw : cfg.sections()) {
            for (final Map.Entry<String, EntityConfig> en : rw.entities().entrySet()) {
                final EntityConfig ec = en.getValue();
                if (!ec.hasMapping()) {
                    continue;
                }
                final String k = scopeKey(rw.name(), en.getKey());
                ec.mapping().fields().clear();
                final TreeMap<Integer, FieldMappingRec> std = stdFields.get(k);
                if (std != null) {
                    ec.mapping().fields().addAll(std.values());
                }
                final TreeMap<Integer, FieldMappingRec> ins = insFields.get(k);
                if (ins != null) {
                    ec.mapping().fields().addAll(ins.values());
                }
            }
        }
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
