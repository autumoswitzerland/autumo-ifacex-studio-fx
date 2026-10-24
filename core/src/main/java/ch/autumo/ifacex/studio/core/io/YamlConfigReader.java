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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import ch.autumo.ifacex.studio.core.model.IpcConfig;
import ch.autumo.ifacex.studio.core.model.KeyModel;

/**
 * Reads the new {@code .ifcx} (YAML) format into the in-memory model.
 *
 * <p>Reading is safe (SnakeYAML Engine, YAML 1.2) and never instantiates arbitrary types.
 * The comment header carries the metadata; the YAML body is flattened back to the engine's
 * native key/value form and classified by {@link PropertiesConfigReader#fromFlat} so that
 * YAML and properties share exactly one classification implementation.</p>
 */
public final class YamlConfigReader {

    private static final Load LOAD = new Load(LoadSettings.builder().build());

    private YamlConfigReader() {
    }

    /**
     * Reads a {@code .ifcx} file from disk.
     *
     * @param path file to read (UTF-8)
     * @return parsed configuration
     * @throws IOException when the file cannot be read
     */
    public static IpcConfig read(Path path) throws IOException {
        return read(Files.readString(path, StandardCharsets.UTF_8));
    }

    /**
     * Parses the new YAML format from a string (e.g. the in-memory editor buffer). The YAML body
     * is flattened and handed to {@link PropertiesConfigReader#fromFlat} so that both formats
     * share exactly one classification implementation.
     *
     * @param text complete document text, comment header included
     * @return parsed configuration
     */
    public static IpcConfig read(String text) {
        final Object rootObj = LOAD.loadFromString(text);
        final Map<String, String> flat = flatten(asMap(rootObj));
        return PropertiesConfigReader.fromFlat(flat, CommentHeader.parse(text.lines().toList()));
    }

    private static Map<String, String> flatten(Map<String, Object> root) {
        final Map<String, String> flat = new LinkedHashMap<>();
        if (root == null) {
            return flat;
        }

        put(flat, KeyModel.SOURCE_INTERFACE, root.get(YamlConfigWriter.K_SOURCE_INTERFACE));
        putList(flat, KeyModel.DESTINATION_INTERFACES, root.get(YamlConfigWriter.K_DESTINATION_INTERFACES));
        putList(flat, KeyModel.PROCESS_SOURCE_ENTITIES,
                root.get(YamlConfigWriter.K_PROCESS_SOURCE_ENTITIES));

        if (root.get(YamlConfigWriter.K_SOURCES) instanceof List<?> sources) {
            for (final Object o : sources) {
                if (o instanceof Map<?, ?> m) {
                    flattenSection(m, flat);
                }
            }
        }

        if (root.get(YamlConfigWriter.K_DESTINATIONS) instanceof List<?> destinations) {
            for (final Object o : destinations) {
                if (o instanceof Map<?, ?> m) {
                    flattenSection(m, flat);
                }
            }
        }

        if (root.get(YamlConfigWriter.K_GENERAL) instanceof Map<?, ?> general) {
            for (final Map.Entry<?, ?> e : general.entrySet()) {
                flat.put(String.valueOf(e.getKey()), str(e.getValue()));
            }
        }
        return flat;
    }

    private static void flattenSection(Map<?, ?> section, Map<String, String> flat) {
        final String name = str(section.get(YamlConfigWriter.K_NAME));
        if (name.isEmpty()) {
            return;
        }
        if (section.get(YamlConfigWriter.K_PARAMS) instanceof Map<?, ?> params) {
            for (final Map.Entry<?, ?> e : params.entrySet()) {
                flat.put(name + "_" + e.getKey(), str(e.getValue()));
            }
        }
        if (section.get(YamlConfigWriter.K_DEFAULTS) instanceof Map<?, ?> defaults) {
            for (final Map.Entry<?, ?> e : defaults.entrySet()) {
                flat.put(name + "_" + KeyModel.WILDCARD + "_" + e.getKey(), str(e.getValue()));
            }
        }
        if (section.get(YamlConfigWriter.K_ENTITIES) instanceof Map<?, ?> entities) {
            for (final Map.Entry<?, ?> e : entities.entrySet()) {
                if (e.getValue() instanceof Map<?, ?> em) {
                    flattenEntity(name, String.valueOf(e.getKey()), em, flat);
                }
            }
        }
    }

    private static void flattenEntity(String name, String entity, Map<?, ?> em, Map<String, String> flat) {
        final String base = name + "_" + entity + "_";
        int stdIdx = 0;
        int insIdx = 0;
        for (final Map.Entry<?, ?> e : em.entrySet()) {
            final String k = String.valueOf(e.getKey());
            final Object v = e.getValue();
            if (k.equals(YamlConfigWriter.K_FIELDS)) {
                if (v instanceof List<?> l) {
                    flat.put(base + KeyModel.P_FIELDS, join(l));
                } else if (v != null) {
                    flat.put(base + KeyModel.P_FIELDS, str(v));
                }
            } else if (k.equals(YamlConfigWriter.K_MAPPING_ENTITY)) {
                flat.put(base + KeyModel.P_MAPPING_ENTITY, str(v));
            } else if (k.equals(YamlConfigWriter.K_MAPPING)) {
                if (v instanceof List<?> l) {
                    for (final Object o : l) {
                        if (!(o instanceof Map<?, ?> rm)) {
                            continue;
                        }
                        final String field = str(rm.get(YamlConfigWriter.K_FIELD));
                        final String formula = str(rm.get(YamlConfigWriter.K_FORMULA));
                        final boolean insert = Boolean.TRUE.equals(rm.get(YamlConfigWriter.K_INSERT));
                        final String key = base
                                + (insert ? KeyModel.PFX_MAPPING_INSERT : KeyModel.PFX_MAPPING_FIELD)
                                + String.format("%03d", insert ? insIdx++ : stdIdx++);
                        flat.put(key, field + ":" + formula);
                    }
                }
            } else {
                flat.put(base + k, str(v));
            }
        }
    }

    private static void put(Map<String, String> flat, String key, Object value) {
        if (value != null) {
            flat.put(key, str(value));
        }
    }

    private static void putList(Map<String, String> flat, String key, Object value) {
        if (value instanceof List<?> l) {
            flat.put(key, join(l));
        } else if (value instanceof String s && !s.isEmpty()) {
            flat.put(key, s);
        }
    }

    private static String join(List<?> list) {
        final StringBuilder sb = new StringBuilder();
        for (final Object o : list) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(str(o));
        }
        return sb.toString();
    }

    private static String str(Object v) {
        if (v == null) {
            return "";
        }
        if (v instanceof Map<?, ?> map && map.containsKey(YamlConfigWriter.K_ENC)) {
            // An encoded secret in its structured form: the legacy notation is what the reader
            // understands (and what the engine reads).
            final Object mode = map.get(YamlConfigWriter.K_ENC);
            final Object value = map.get(YamlConfigWriter.K_ENC_VALUE);
            if (mode != null && value != null) {
                return "{" + mode + ":" + value + "}";
            }
        }
        return v.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : null;
    }
}
