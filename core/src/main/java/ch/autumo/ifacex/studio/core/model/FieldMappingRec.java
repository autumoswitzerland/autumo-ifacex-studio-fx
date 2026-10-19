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

/**
 * One destination field mapping: {@code destField} = {@code formula}, either a standard
 * field ({@code insert=false}) or an insert-only value ({@code insert=true}).
 *
 * <p>Legacy: {@code <rw>_<entity>_mapping_field_NNN} / {@code ..._mapping_insert_NNN}
 * with value {@code destField:formula} (split on the first {@code :}).</p>
 *
 * @param destField destination field name
 * @param formula   source field reference or mapping formula
 * @param insert    {@code true} for an insert-only mapping ({@code mapping_insert_NNN}),
 *                  {@code false} for a standard mapping ({@code mapping_field_NNN})
 */
public record FieldMappingRec(String destField, String formula, boolean insert) {
}
