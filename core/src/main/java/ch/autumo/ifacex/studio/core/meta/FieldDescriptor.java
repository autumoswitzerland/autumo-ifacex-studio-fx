/*
 * Copyright 2026 autumo GmbH.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
