/*
 * Copyright 2026 autumo GmbH.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
