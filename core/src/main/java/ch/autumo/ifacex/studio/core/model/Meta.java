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
 *
 * @param title   job title shown in the header and the navigation tree
 * @param version configuration version string
 * @param author author line of the header
 * @param notes   free-form note text (may span several lines)
 * @param date    date line of the header
 */
public record Meta(String title, String version, String author, String notes, String date) {

    /** Default metadata: empty title and notes, version {@code 1.0}, author {@code -}. */
    public static final Meta EMPTY = new Meta("", "1.0", "", "-", "");

    /**
     * Returns a copy with a different title.
     *
     * @param t new title
     * @return new metadata instance
     */
    public Meta withTitle(String t) {
        return new Meta(t, version, author, notes, date);
    }

    /**
     * Returns a copy with a different version.
     *
     * @param v new version
     * @return new metadata instance
     */
    public Meta withVersion(String v) {
        return new Meta(title, v, author, notes, date);
    }

    /**
     * Returns a copy with a different author.
     *
     * @param a new author
     * @return new metadata instance
     */
    public Meta withAuthor(String a) {
        return new Meta(title, version, a, notes, date);
    }

    /**
     * Returns a copy with different notes.
     *
     * @param n new notes
     * @return new metadata instance
     */
    public Meta withNotes(String n) {
        return new Meta(title, version, author, n, date);
    }

    /**
     * Returns a copy with a different date.
     *
     * @param d new date
     * @return new metadata instance
     */
    public Meta withDate(String d) {
        return new Meta(title, version, author, notes, d);
    }
}
