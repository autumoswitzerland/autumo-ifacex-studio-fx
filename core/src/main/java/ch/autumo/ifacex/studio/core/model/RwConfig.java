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
package ch.autumo.ifacex.studio.core.model;

import java.util.LinkedHashMap;
import java.util.Map;

import ch.autumo.ifacex.studio.core.meta.ParameterCatalog;

/**
 * One reader or writer section ({@code <rw>_...}): global parameters, wildcard defaults
 * ({@code <rw>_*_<param>}) and the per-entity configuration.
 */
public final class RwConfig {

    private final String name;
    private final boolean reader;
    private final Map<String, Object> global = new LinkedHashMap<>();
    private final Map<String, Object> wildcard = new LinkedHashMap<>();
    private final Map<String, EntityConfig> entities = new LinkedHashMap<>();

    /**
     * Creates an empty section.
     *
     * @param name   section prefix, e.g. {@code csv_out}
     * @param reader {@code true} for a reader, {@code false} for a writer
     */
    public RwConfig(String name, boolean reader) {
        this.name = name;
        this.reader = reader;
    }

    /**
     * The section prefix.
     *
     * @return section prefix, e.g. {@code rest_in}
     */
    public String name() {
        return name;
    }

    /**
     * Whether this section is a reader.
     *
     * @return {@code true} for a reader, {@code false} for a writer
     */
    public boolean isReader() {
        return reader;
    }

    /**
     * Global section parameters ({@code <rw>_<param>}).
     *
     * @return mutable map of parameter to value
     */
    public Map<String, Object> global() {
        return global;
    }

    /**
     * Wildcard defaults ({@code <rw>_*_<param>}) that apply to every entity of the section.
     *
     * @return mutable map of parameter to value
     */
    public Map<String, Object> wildcard() {
        return wildcard;
    }

    /**
     * The per-entity configuration, in file order.
     *
     * @return mutable map of entity name to configuration
     */
    public Map<String, EntityConfig> entities() {
        return entities;
    }

    /**
     * Returns the configuration for an entity, creating an empty one when necessary.
     * Used by the legacy reader path; does <em>not</em> pre-populate parameters.
     *
     * @param entity entity name
     * @return entity configuration, never {@code null}
     */
    public EntityConfig ensureEntity(String entity) {
        return entities.computeIfAbsent(entity, EntityConfig::new);
    }

    /**
     * Returns the configuration for an entity, creating it when necessary and pre-populating it
     * with all entity-scoped catalogue parameters of the section (see {@link ParameterCatalog}).
     * Used when the user creates a new entity/mapping in the GUI.
     *
     * @param entity entity name
     * @return entity configuration, never {@code null}
     */
    public EntityConfig initializeEntity(String entity) {
        final EntityConfig ec = ensureEntity(entity);
        for (final ParameterCatalog.Entry e : ParameterCatalog.entriesFor(name)) {
            if (e.scope() == ParameterCatalog.Scope.ENTITY) {
                ec.settings().putIfAbsent(e.param(), e.defaultValue());
            }
        }
        return ec;
    }

    /**
     * Whether the section carries no parameters at all (an inert leftover of the legacy file).
     *
     * @return {@code true} when global, wildcard and entity parameters are all empty
     */
    public boolean isEmpty() {
        return global.isEmpty() && wildcard.isEmpty() && entities.isEmpty();
    }
}
