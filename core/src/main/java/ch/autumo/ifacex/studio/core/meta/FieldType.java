/*
 * Copyright 2026 autumo GmbH.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package ch.autumo.ifacex.studio.core.meta;

/**
 * Rendering/validation type of a configuration field, used to build forms from metadata.
 */
public enum FieldType {

    /** Free text (single line). */
    TEXT,
    /** Multi-line text. */
    TEXT_AREA,
    /** Boolean rendered as yes/no (engine convention). */
    BOOLEAN,
    /** Integer number. */
    INTEGER,
    /** Comma separated list of values. */
    LIST,
    /** Fixed set of choices; see {@link FieldDescriptor#options()}. */
    SELECT,
    /** File system path. */
    PATH,
    /** Credential that may be plain or encoded ({enc,mode,value}). */
    SECRET
}
