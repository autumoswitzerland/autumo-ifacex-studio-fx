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
package ch.autumo.ifacex.studio.core.meta;

import java.util.List;

/**
 * Metadata for one engine parameter, used to render a form field.
 *
 * @param param   engine parameter name, e.g. {@code data_path}
 * @param label   human readable label
 * @param type    rendering/validation type
 * @param options allowed values for {@link FieldType#SELECT} (may be empty)
 * @param help    short help text (may be empty)
 */
public record FieldDescriptor(String param, String label, FieldType type,
        List<String> options, String help) {

    /**
     * Creates a descriptor without select options and without help text.
     *
     * @param param engine parameter name
     * @param label human readable label
     * @param type  rendering/validation type
     */
    public FieldDescriptor(String param, String label, FieldType type) {
        this(param, label, type, List.of(), "");
    }

    /**
     * Creates a descriptor with fixed selectable values and no help text.
     *
     * @param param   engine parameter name
     * @param label   human readable label
     * @param type    rendering/validation type
     * @param options allowed values for a select field
     */
    public FieldDescriptor(String param, String label, FieldType type, List<String> options) {
        this(param, label, type, options, "");
    }
}
