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
import org.snakeyaml.engine.v2.api.lowlevel.Compose;
import org.snakeyaml.engine.v2.comments.CommentLine;
import org.snakeyaml.engine.v2.comments.CommentType;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.NodeTuple;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import org.snakeyaml.engine.v2.nodes.SequenceNode;

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

    /** Composition with comment parsing: the comments above a key belong to that key. */
    private static final Compose COMPOSE =
            new Compose(LoadSettings.builder().setParseComments(true).build());

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
        final IpcConfig cfg =
                PropertiesConfigReader.fromFlat(flat, CommentHeader.parse(text.lines().toList()));
        // The comments that document a parameter: without them a file that was saved once would
        // lose everything the conversion from the legacy format had carried over.
        cfg.comments().putAll(comments(text));
        return cfg;
    }

    /**
     * Reads the comments that document a parameter.
     *
     * <p>A comment block is attached to the key it is written above; it is stored under the same
     * flat engine key the writer uses, e.g. {@code rest_in_host} or {@code csv_out_x_mapping_entity}.
     * The blank line that the writer puts in front of a block is not part of it.</p>
     *
     * @param text complete document text
     * @return comment lines per flat engine key, empty when the document carries none
     */
    static Map<String, List<String>> comments(String text) {
        final Map<String, List<String>> out = new LinkedHashMap<>();
        final Node root = COMPOSE.composeString(text).orElse(null);
        if (!(root instanceof MappingNode map)) {
            return out;
        }
        for (final NodeTuple tuple : map.getValue()) {
            final String key = scalar(tuple.getKeyNode());
            final Node value = tuple.getValueNode();
            if (YamlConfigWriter.K_FORMAT.equals(key)) {
                // NO comment handling here: the file header sits directly above the first key,
                // so its comment block would be taken as the comment of 'formatVersion' and
                // written a second time. The header is owned by CommentHeader.
                continue;
            } else if (YamlConfigWriter.K_GENERAL.equals(key)) {
                // A comment above 'general:' documents the block, not a single parameter.
                comments(out, YamlConfigWriter.K_GENERAL, tuple.getKeyNode());
                if (value instanceof MappingNode general) {
                    for (final NodeTuple entry : general.getValue()) {
                        comments(out, scalar(entry.getKeyNode()), entry.getKeyNode());
                    }
                }
            } else if (YamlConfigWriter.K_SOURCE_INTERFACE.equals(key)) {
                comments(out, KeyModel.SOURCE_INTERFACE, tuple.getKeyNode());
            } else if (YamlConfigWriter.K_DESTINATION_INTERFACES.equals(key)) {
                comments(out, KeyModel.DESTINATION_INTERFACES, tuple.getKeyNode());
            } else if (YamlConfigWriter.K_PROCESS_SOURCE_ENTITIES.equals(key)) {
                comments(out, KeyModel.PROCESS_SOURCE_ENTITIES, tuple.getKeyNode());
            } else if ((YamlConfigWriter.K_SOURCES.equals(key)
                    || YamlConfigWriter.K_DESTINATIONS.equals(key))
                    && value instanceof SequenceNode sections) {
                for (final Node section : sections.getValue()) {
                    if (section instanceof MappingNode sectionMap) {
                        sectionComments(sectionMap, out);
                    }
                }
            }
        }
        return out;
    }

    /**
     * Collects the comments of one reader/writer section.
     *
     * @param section section mapping
     * @param out     target
     */
    private static void sectionComments(MappingNode section, Map<String, List<String>> out) {
        final String name = scalar(valueOf(section, YamlConfigWriter.K_NAME));
        if (name.isEmpty()) {
            return;
        }
        for (final NodeTuple tuple : section.getValue()) {
            final Node value = tuple.getValueNode();
            if (YamlConfigWriter.K_PARAMS.equals(scalar(tuple.getKeyNode()))
                    && value instanceof MappingNode params) {
                for (final NodeTuple entry : params.getValue()) {
                    comments(out, name + "_" + scalar(entry.getKeyNode()), entry.getKeyNode());
                }
            } else if (YamlConfigWriter.K_DEFAULTS.equals(scalar(tuple.getKeyNode()))
                    && value instanceof MappingNode defaults) {
                for (final NodeTuple entry : defaults.getValue()) {
                    comments(out, name + "_" + KeyModel.WILDCARD + "_" + scalar(entry.getKeyNode()),
                            entry.getKeyNode());
                }
            } else if (YamlConfigWriter.K_ENTITIES.equals(scalar(tuple.getKeyNode()))
                    && value instanceof MappingNode entities) {
                for (final NodeTuple entity : entities.getValue()) {
                    if (entity.getValueNode() instanceof MappingNode settings) {
                        entityComments(out, name + "_" + scalar(entity.getKeyNode()) + "_", settings);
                    }
                }
            }
        }
    }

    /**
     * Collects the comments of one entity. Mapping rows are generated from an ordered list and
     * have no key of their own, so they carry no comment.
     *
     * @param out    target
     * @param prefix flat key prefix, e.g. {@code csv_out_companies_}
     * @param map    entity mapping
     */
    private static void entityComments(Map<String, List<String>> out, String prefix, MappingNode map) {
        for (final NodeTuple tuple : map.getValue()) {
            if (YamlConfigWriter.K_MAPPING.equals(scalar(tuple.getKeyNode()))) {
                continue;
            }
            comments(out, prefix + scalar(tuple.getKeyNode()), tuple.getKeyNode());
        }
    }

    /**
     * Stores the comment block of a key.
     *
     * @param out  target
     * @param key  flat engine key
     * @param node the key node the comments are attached to
     */
    private static void comments(Map<String, List<String>> out, String key, Node node) {
        final List<String> lines = lines(node.getBlockComments());
        if (!lines.isEmpty()) {
            out.put(key, lines);
        }
    }

    /**
     * The text of a comment block, one {@code #}-line per entry.
     *
     * @param block comments of a key node, may be {@code null}
     * @return the lines, including their {@code #}
     */
    private static List<String> lines(List<CommentLine> block) {
        final java.util.ArrayList<String> out = new java.util.ArrayList<>();
        if (block == null) {
            return out;
        }
        for (final CommentLine line : block) {
            if (line.getCommentType() == CommentType.BLANK_LINE) {
                // The blank line the writer puts in front of a block.
                continue;
            }
            out.add("#" + line.getValue());
        }
        return out;
    }

    /**
     * The value node of a mapping entry.
     *
     * @param map mapping node
     * @param key key to look up
     * @return the value node, or {@code null} when the key is absent
     */
    private static Node valueOf(MappingNode map, String key) {
        for (final NodeTuple tuple : map.getValue()) {
            if (key.equals(scalar(tuple.getKeyNode()))) {
                return tuple.getValueNode();
            }
        }
        return null;
    }

    /**
     * The scalar value of a node.
     *
     * @param node node
     * @return its value, an empty string for a non-scalar
     */
    private static String scalar(Node node) {
        return node instanceof ScalarNode scalar ? scalar.getValue() : "";
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
