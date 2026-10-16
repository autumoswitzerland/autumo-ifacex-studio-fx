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

    public RwConfig(String name, boolean reader) {
        this.name = name;
        this.reader = reader;
    }

    public String name() {
        return name;
    }

    public boolean isReader() {
        return reader;
    }

    public Map<String, String> global() {
        return global;
    }

    public Map<String, String> wildcard() {
        return wildcard;
    }

    public Map<String, EntityConfig> entities() {
        return entities;
    }

    public EntityConfig ensureEntity(String entity) {
        return entities.computeIfAbsent(entity, EntityConfig::new);
    }

    public boolean isEmpty() {
        return global.isEmpty() && wildcard.isEmpty() && entities.isEmpty();
    }
}
