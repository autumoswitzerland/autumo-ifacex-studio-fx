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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ch.autumo.ifacex.studio.core.model.EntityConfig;
import ch.autumo.ifacex.studio.core.model.FieldMappingRec;
import ch.autumo.ifacex.studio.core.model.IpcConfig;
import ch.autumo.ifacex.studio.core.model.RwConfig;
import ch.autumo.ifacex.studio.core.model.Secret;

/**
 * Writes the model to the new {@code .ifcx} (YAML) format.
 *
 * <p>The metadata/comment header is preserved verbatim in front of the YAML body. The body keeps
 * two kinds of information strictly apart, which is required for lossless round-trips:</p>
 * <ul>
 *   <li>the declared job wiring ({@code sourceInterface}, {@code destinationInterfaces},
 *       {@code processSourceEntities}); a declared interface may have no parameters at all
 *       (e.g. {@code console_out}),</li>
 *   <li>the actual reader/writer sections that carry parameters ({@code sources}, {@code destinations}),
 *       which may include inert leftovers.</li>
 * </ul>
 *
 * <p>See docs/08-config-format.md. Values keep the engine's native parameter names to remain lossless.</p>
 */
public final class YamlConfigWriter {

    /** Version of the new {@code .ifcx} YAML format written by this class. */
    public static final int FORMAT_VERSION = 1;

    /** Document key that carries {@link #FORMAT_VERSION}. */
    public static final String K_FORMAT = "formatVersion";

    static final String K_SOURCE_INTERFACE = "sourceInterface";
    static final String K_DESTINATION_INTERFACES = "destinationInterfaces";
    static final String K_PROCESS_SOURCE_ENTITIES = "processSourceEntities";
    static final String K_SOURCES = "sources";
    static final String K_DESTINATIONS = "destinations";
    static final String K_GENERAL = "general";
    /** The key of the encryption mode of a secret in the structured form. */
    static final String K_ENC = "enc";
    /** The key of the encoded value of a secret in the structured form. */
    static final String K_ENC_VALUE = "value";

    static final String K_NAME = "name";
    static final String K_PARAMS = "params";
    static final String K_DEFAULTS = "defaults";
    static final String K_ENTITIES = "entities";

    static final String K_FIELDS = "fields";
    static final String K_MAPPING_ENTITY = "mapping_entity";
    static final String K_MAPPING = "mapping";
    static final String K_FIELD = "field";
    static final String K_FORMULA = "formula";
    static final String K_INSERT = "insert";

    private YamlConfigWriter() {
    }

    /**
     * Writes the model in the new YAML format (UTF-8).
     *
     * @param path target file, overwritten when it exists
     * @param cfg  configuration to write
     * @throws IOException when the file cannot be written
     */
    public static void write(Path path, IpcConfig cfg) throws IOException {
        Files.writeString(path, toText(cfg), StandardCharsets.UTF_8);
    }

    /**
     * Renders the model as the full {@code .ifcx} text: the metadata comment header followed by
     * the YAML body.
     *
     * @param cfg configuration to write
     * @return file text
     */
    public static String toText(IpcConfig cfg) {
        final StringBuilder sb = new StringBuilder();
        sb.append(CommentHeader.render(cfg.meta()));
        sb.append('\n');
        sb.append(YamlEmitter.dump(toTree(cfg)));
        return sb.toString();
    }

    /**
     * Builds the ordered YAML tree for the model. Kept public so tests can assert the structure
     * independently of the emitter's formatting.
     *
     * @param cfg configuration to convert
     * @return ordered map that mirrors the document layout
     */
    public static Map<String, Object> toTree(IpcConfig cfg) {
        final Map<String, Object> root = new LinkedHashMap<>();
        root.put(K_FORMAT, FORMAT_VERSION);
        // The global parameters come first: they concern the whole configuration.
        root.put(K_GENERAL, values(cfg.general()));
        root.put(K_SOURCE_INTERFACE, cfg.sourceInterface());
        root.put(K_DESTINATION_INTERFACES, new ArrayList<>(cfg.destinationInterfaces()));
        root.put(K_PROCESS_SOURCE_ENTITIES, new ArrayList<>(cfg.processSourceEntities()));

        final List<Object> sources = new ArrayList<>();
        for (final RwConfig rw : cfg.readers().values()) {
            sources.add(section(rw));
        }
        root.put(K_SOURCES, sources);

        final List<Object> destinations = new ArrayList<>();
        for (final RwConfig rw : cfg.writers().values()) {
            destinations.add(section(rw));
        }
        root.put(K_DESTINATIONS, destinations);

        return root;
    }

    /**
     * The parameters of a section as the YAML tree holds them: an encoded secret becomes the
     * structured object of the new format ({@code {enc: 3, value: "..." }}), a plain value
     * stays a string.
     *
     * @param params the parameters of the model
     * @return the parameters for the YAML tree
     */
    private static Map<String, Object> values(Map<String, Object> params) {
        final Map<String, Object> out = new LinkedHashMap<>();
        for (final Map.Entry<String, Object> p : params.entrySet()) {
            out.put(p.getKey(), value(p.getValue()));
        }
        return out;
    }

    /**
     * The value of a parameter as the YAML tree holds it.
     *
     * @param value the value of the model
     * @return the value for the YAML tree
     */
    private static Object value(Object value) {
        if (value instanceof Secret secret) {
            final YamlEmitter.InlineMap out = new YamlEmitter.InlineMap();
            out.put(K_ENC, secret.mode());
            out.put(K_ENC_VALUE, secret.value());
            return out;
        }
        return value;
    }

    private static Map<String, Object> section(RwConfig rw) {
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put(K_NAME, rw.name());
        if (!rw.global().isEmpty()) {
            m.put(K_PARAMS, values(rw.global()));
        }
        if (!rw.wildcard().isEmpty()) {
            m.put(K_DEFAULTS, values(rw.wildcard()));
        }
        if (!rw.entities().isEmpty()) {
            final Map<String, Object> entities = new LinkedHashMap<>();
            for (final Map.Entry<String, EntityConfig> e : rw.entities().entrySet()) {
                entities.put(e.getKey(), entity(e.getValue()));
            }
            m.put(K_ENTITIES, entities);
        }
        return m;
    }

    private static Map<String, Object> entity(EntityConfig ec) {
        final Map<String, Object> m = values(ec.settings());
        if (!ec.fields().isEmpty()) {
            m.put(K_FIELDS, new ArrayList<>(ec.fields()));
        }
        if (ec.hasMapping()) {
            if (ec.mapping().destEntity() != null) {
                m.put(K_MAPPING_ENTITY, ec.mapping().destEntity());
            }
            final List<Object> fields = new ArrayList<>();
            for (final FieldMappingRec rec : ec.mapping().fields()) {
                final Map<String, Object> r = new LinkedHashMap<>();
                r.put(K_FIELD, rec.destField());
                r.put(K_FORMULA, rec.formula());
                r.put(K_INSERT, rec.insert());
                fields.add(r);
            }
            m.put(K_MAPPING, fields);
        }
        return m;
    }
}
