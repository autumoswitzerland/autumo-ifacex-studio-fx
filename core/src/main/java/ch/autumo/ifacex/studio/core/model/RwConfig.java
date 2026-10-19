/*
 * Copyright 2026 autumo GmbH.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package ch.autumo.ifacex.studio.core.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One reader or writer section ({@code <rw>_...}): global parameters, wildcard defaults
 * ({@code <rw>_*_<param>}) and the per-entity configuration.
 */
public final class RwConfig {

    private final String name;
    private final boolean reader;
    private final Map<String, String> global = new LinkedHashMap<>();
    private final Map<String, String> wildcard = new LinkedHashMap<>();
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
    public Map<String, String> global() {
        return global;
    }

    /**
     * Wildcard defaults ({@code <rw>_*_<param>}) that apply to every entity of the section.
     *
     * @return mutable map of parameter to value
     */
    public Map<String, String> wildcard() {
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
     *
     * @param entity entity name
     * @return entity configuration, never {@code null}
     */
    public EntityConfig ensureEntity(String entity) {
        return entities.computeIfAbsent(entity, EntityConfig::new);
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
