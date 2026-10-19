/*
 * Copyright 2026 autumo GmbH.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package ch.autumo.ifacex.studio.core.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Entity-scoped part of a reader/writer section: {@code <rw>_<entity>_<param>}.
 *
 * <p>{@code settings} holds all entity-scoped parameters except the source {@code fields}
 * and the destination mapping, which are modelled explicitly.</p>
 */
public final class EntityConfig {

    private final String entity;
    private final Map<String, String> settings = new LinkedHashMap<>();
    private final List<String> fields = new ArrayList<>();
    private WriterMapping mapping;

    /**
     * Creates an empty entity configuration.
     *
     * @param entity entity name
     */
    public EntityConfig(String entity) {
        this.entity = entity;
    }

    /**
     * The entity name.
     *
     * @return entity name
     */
    public String entity() {
        return entity;
    }

    /**
     * Entity-scoped parameters ({@code <rw>_<entity>_<param>}) except the source fields and the
     * destination mapping, which are modelled explicitly.
     *
     * @return mutable map of parameter to value
     */
    public Map<String, String> settings() {
        return settings;
    }

    /**
     * The ordered source fields of this entity. For a reader these are the declared
     * {@code <reader>_<entity>_fields}; writers only reference them.
     *
     * @return mutable list of field names
     */
    public List<String> fields() {
        return fields;
    }

    /**
     * The destination mapping of this entity (writers only).
     *
     * @return mapping, or {@code null} when the entity has none
     */
    public WriterMapping mapping() {
        return mapping;
    }

    /**
     * Whether a destination mapping exists for this entity.
     *
     * @return {@code true} when {@link #mapping()} is not {@code null}
     */
    public boolean hasMapping() {
        return mapping != null;
    }

    /**
     * Returns the destination mapping, creating an empty one when necessary.
     *
     * @return mapping, never {@code null}
     */
    public WriterMapping ensureMapping() {
        if (mapping == null) {
            mapping = new WriterMapping();
        }
        return mapping;
    }
}
