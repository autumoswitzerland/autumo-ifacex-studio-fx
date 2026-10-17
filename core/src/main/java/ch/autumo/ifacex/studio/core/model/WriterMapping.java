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
