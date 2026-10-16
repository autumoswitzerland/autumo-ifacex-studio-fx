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

    private String destEntity;
    private final List<FieldMappingRec> fields = new ArrayList<>();

    public String destEntity() {
        return destEntity;
    }

    public void setDestEntity(String destEntity) {
        this.destEntity = destEntity;
    }

    public List<FieldMappingRec> fields() {
        return fields;
    }
}
