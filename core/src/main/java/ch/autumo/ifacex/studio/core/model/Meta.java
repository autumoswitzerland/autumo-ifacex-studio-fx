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

/**
 * Metadata of an IPC, rendered as the comment header of a configuration file.
 *
 * <p>Maps to the legacy {@code meta_*} properties and the comment header block
 * (Title, Version, Notes, Author, Date).</p>
 */
public record Meta(String title, String version, String author, String notes, String date) {

    public static final Meta EMPTY = new Meta("", "1.0", "", "-", "");

    public Meta withTitle(String t) {
        return new Meta(t, version, author, notes, date);
    }

    public Meta withVersion(String v) {
        return new Meta(title, v, author, notes, date);
    }

    public Meta withAuthor(String a) {
        return new Meta(title, version, a, notes, date);
    }

    public Meta withNotes(String n) {
        return new Meta(title, version, author, n, date);
    }

    public Meta withDate(String d) {
        return new Meta(title, version, author, notes, d);
    }
}
