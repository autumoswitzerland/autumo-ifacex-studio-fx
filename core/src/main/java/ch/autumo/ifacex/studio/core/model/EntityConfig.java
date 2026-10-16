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

    public EntityConfig(String entity) {
        this.entity = entity;
    }

    public String entity() {
        return entity;
    }

    public Map<String, String> settings() {
        return settings;
    }

    public List<String> fields() {
        return fields;
    }

    public WriterMapping mapping() {
        return mapping;
    }

    public boolean hasMapping() {
        return mapping != null;
    }

    public WriterMapping ensureMapping() {
        if (mapping == null) {
            mapping = new WriterMapping();
        }
        return mapping;
    }
}
