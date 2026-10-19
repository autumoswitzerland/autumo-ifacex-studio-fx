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
