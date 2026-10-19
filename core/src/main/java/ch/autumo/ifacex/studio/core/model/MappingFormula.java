/*
 * Copyright 2026 autumo GmbH.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package ch.autumo.ifacex.studio.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Destination-mapping syntax of the original Studio.
 *
 * <p>A mapping line is {@code target-field:formula}, where the target field must match
 * {@code [A-z0-9$.#\-_]+} and the formula uses source fields enclosed in curly brackets, e.g.
 * {@code full-name:{first-name} {last-name}}. The legacy editor splits the line on {@code :} and
 * requires exactly two parts (so a formula must not contain a colon).</p>
 *
 * <p>The {@code [STD]} / {@code [INS]} prefix distinguishes standard mappings from insert-only
 * values; in the model this is the {@link FieldMappingRec#insert()} flag.</p>
 */
public final class MappingFormula {

    /** Legacy {@code Constants.REGEX_PATTERN_FIELD}. */
    public static final String REGEX_TARGET_FIELD = "[A-z0-9$.#\\-_]+";

    /** Matches a valid destination field name. */
    public static final Pattern TARGET_FIELD = Pattern.compile(REGEX_TARGET_FIELD);

    /** Marker for a standard mapping line (legacy {@code [STD]}). */
    public static final String PREFIX_STD = "[STD]";

    /** Marker for an insert-only mapping line (legacy {@code [INS]}). */
    public static final String PREFIX_INS = "[INS]";

    private MappingFormula() {
    }

    /**
     * Parses a mapping line typed in the formula box.
     *
     * @param text   raw line, e.g. {@code target:{source1}{source2}}
     * @param insert {@code true} for an insert-only mapping, {@code false} for a standard one
     * @return the mapping record, or {@code null} when the line is empty or invalid (the caller
     *         shows the legacy-style warning).
     */
    public static FieldMappingRec parse(String text, boolean insert) {
        if (text == null || text.isBlank()) {
            return null;
        }
        final String line = text.trim();
        // Legacy: split(":") and require exactly two parts => at most one colon.
        final String[] parts = line.split(":");
        if (parts.length != 2 || !TARGET_FIELD.matcher(parts[0]).matches()) {
            return null;
        }
        return new FieldMappingRec(parts[0], parts[1], insert);
    }

    /**
     * Whether {@code text} is a valid mapping line.
     *
     * @param text raw line
     * @return {@code true} when it parses
     */
    public static boolean isValid(String text) {
        return parse(text, false) != null;
    }

    /**
     * The display label of a mapping row, e.g. {@code [STD] target:formula}.
     *
     * @param rec mapping record
     * @return label with the type marker
     */
    public static String display(FieldMappingRec rec) {
        final String prefix = rec.insert() ? PREFIX_INS : PREFIX_STD;
        return prefix + " " + rec.destField() + ":" + rec.formula();
    }

    /**
     * The mapping line without the type marker, as it appears in the formula box.
     *
     * @param rec mapping record
     * @return {@code target:formula}
     */
    public static String line(FieldMappingRec rec) {
        return rec.destField() + ":" + rec.formula();
    }

    /**
     * Legacy {@code generateMappings} ("All"/standard): map every source field to itself as
     * {@code field:{field}} (standard). Fields already mapped are skipped, so pressing the button
     * twice does not duplicate entries.
     *
     * @param sourceFields source fields to map
     * @param existing     mappings already present
     * @return the new standard mappings that are not covered yet
     */
    public static List<FieldMappingRec> standardFor(List<String> sourceFields,
            List<FieldMappingRec> existing) {
        final List<FieldMappingRec> result = new ArrayList<>();
        for (final String field : sourceFields) {
            boolean present = false;
            for (final FieldMappingRec rec : existing) {
                if (rec.destField().equals(field)) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                result.add(new FieldMappingRec(field, "{" + field + "}", false));
            }
        }
        return result;
    }
}
