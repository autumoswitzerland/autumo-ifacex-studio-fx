/*
 * Copyright 2026 autumo GmbH.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package ch.autumo.ifacex.studio.core.meta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import ch.autumo.ifacex.studio.core.meta.InterfaceTypes.InterfaceType;
import ch.autumo.ifacex.studio.core.meta.InterfaceTypes.Kind;

/** Tests for the reader/writer interface type registry (see doc 11). */
class InterfaceTypesTest {

    @Test
    void resolvesReaders() {
        final InterfaceType csv = InterfaceTypes.of("csv_in");
        assertEquals(Kind.READER, csv.kind());
        assertEquals("csv", csv.prefix());
        assertEquals("CSV", csv.display());
        assertEquals(InterfaceTypes.COLOR_READER, csv.colorHex());
        assertTrue(csv.isReader());
        assertFalse(csv.isWriter());
        assertEquals("csv_in_", csv.keyPrefix());
    }

    @Test
    void resolvesWriters() {
        final InterfaceType db = InterfaceTypes.of("db_out");
        assertEquals(Kind.WRITER, db.kind());
        assertEquals("DB", db.display());
        assertEquals(InterfaceTypes.COLOR_WRITER, db.colorHex());
        assertTrue(db.isWriter());
    }

    @Test
    void handlesSuffixlessWriters() {
        assertEquals(Kind.WRITER, InterfaceTypes.of("code").kind());
        assertEquals("Code", InterfaceTypes.of("code").display());
        assertEquals(Kind.WRITER, InterfaceTypes.of("exec").kind());
        assertEquals(InterfaceTypes.COLOR_WRITER, InterfaceTypes.of("exec").colorHex());
    }

    @Test
    void displaysLegacyNames() {
        assertEquals("Mongo DB", InterfaceTypes.of("mongo_in").display());
        assertEquals("Mongo DB", InterfaceTypes.of("mongo_out").display());
        assertEquals("REST", InterfaceTypes.of("rest_out").display());
        assertEquals("Mail", InterfaceTypes.of("mail_out").display());
    }

    @Test
    void unknownTypesAreReadersOrWritersBySuffix() {
        final InterfaceType t = InterfaceTypes.of("excel_out");
        assertEquals(Kind.WRITER, t.kind());
        assertEquals("Excel", t.display());
        assertEquals(InterfaceTypes.COLOR_WRITER, t.colorHex());
    }

    @Test
    void nullAndEmptyAreUnknown() {
        assertEquals(Kind.UNKNOWN, InterfaceTypes.of(null).kind());
        assertEquals(Kind.UNKNOWN, InterfaceTypes.of("").kind());
    }

    @Test
    void exposesKnownInterfaceNames() {
        assertTrue(InterfaceTypes.readerNames().contains("csv_in"));
        assertTrue(InterfaceTypes.readerNames().contains("rest_in"));
        assertTrue(InterfaceTypes.writerNames().contains("csv_out"));
        assertTrue(InterfaceTypes.writerNames().contains("code"));
        assertTrue(InterfaceTypes.writerNames().contains("exec"));
    }
}
