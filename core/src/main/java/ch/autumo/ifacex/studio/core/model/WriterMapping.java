/*
 * Copyright 2026 autumo GmbH.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package ch.autumo.ifacex.studio.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Destination output mapping of one (writer, source entity): the destination entity plus
 * the ordered field mappings.
 */
public final class WriterMapping {

    /** Creates an empty mapping with no destination entity. */
    public WriterMapping() {
    }

    private String destEntity;
    private final List<FieldMappingRec> fields = new ArrayList<>();

    /**
     * The destination entity this mapping writes to (legacy {@code mapping_entity}).
     *
     * @return destination entity name, or {@code null} when unset
     */
    public String destEntity() {
        return destEntity;
    }

    /**
     * Sets the destination entity.
     *
     * @param destEntity destination entity name
     */
    public void setDestEntity(String destEntity) {
        this.destEntity = destEntity;
    }

    /**
     * The ordered destination mappings: destination field to source formula.
     *
     * @return mutable list of mapping records (standard and insert-marker entries together)
     */
    public List<FieldMappingRec> fields() {
        return fields;
    }
}
