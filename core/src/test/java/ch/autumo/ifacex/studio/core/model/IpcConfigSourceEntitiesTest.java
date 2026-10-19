/*
 * Copyright 2026 autumo GmbH.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package ch.autumo.ifacex.studio.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Global source-entity registry (legacy BASE tab / {@code process_source_entities}). */
class IpcConfigSourceEntitiesTest {

    @Test
    void addSourceEntityAddsToListAndReader() {
        final IpcConfig cfg = new IpcConfig();
        cfg.setSourceInterface("csv_in");
        cfg.ensureReader("csv_in");

        assertTrue(cfg.addSourceEntity("companies"));
        assertTrue(cfg.addSourceEntity("contacts"));
        assertFalse(cfg.addSourceEntity("companies"), "duplicates are rejected");

        assertEquals(List.of("companies", "contacts"), cfg.processSourceEntities());
        assertEquals(List.of("companies", "contacts"), cfg.sourceEntityNames());
        assertTrue(cfg.reader().entities().containsKey("companies"));
        assertTrue(cfg.reader().entities().containsKey("contacts"));
    }

    @Test
    void ensureReaderReconcilesDeclaredEntities() {
        final IpcConfig cfg = new IpcConfig();
        cfg.addSourceEntity("companies");
        cfg.addSourceEntity("contacts");

        // No reader yet -> adding entities only touched the canonical list.
        assertTrue(cfg.reader() == null || cfg.reader().entities().isEmpty());
        assertTrue(cfg.reader() == null);

        cfg.ensureReader("rest_in");
        assertEquals(List.of("companies", "contacts"),
                List.copyOf(cfg.reader().entities().keySet()));
    }

    @Test
    void removeSourceEntityRemovesFieldsAndMappingsEverywhere() {
        final IpcConfig cfg = new IpcConfig();
        cfg.ensureReader("csv_in");
        cfg.addSourceEntity("companies");
        cfg.readerEntityFields("companies").add("name");
        final RwConfig out = cfg.ensureWriter("csv_out");
        out.ensureEntity("companies").ensureMapping().setDestEntity("C");

        cfg.removeSourceEntity("companies");

        assertTrue(cfg.processSourceEntities().isEmpty());
        assertFalse(cfg.reader().entities().containsKey("companies"));
        assertFalse(out.entities().containsKey("companies"));
    }

    @Test
    void renameSourceEntityMovesFieldsAndMappingsKeepingOrder() {
        final IpcConfig cfg = new IpcConfig();
        cfg.ensureReader("csv_in");
        cfg.addSourceEntity("companies");
        cfg.addSourceEntity("contacts");
        cfg.readerEntityFields("companies").addAll(List.of("name", "city"));

        final RwConfig out = cfg.ensureWriter("csv_out");
        final WriterMapping m = out.ensureEntity("companies").ensureMapping();
        m.setDestEntity("C");
        m.fields().add(new FieldMappingRec("NAME", "{name}", false));

        cfg.renameSourceEntity("companies", "firms");

        assertEquals(List.of("firms", "contacts"), cfg.processSourceEntities());
        assertEquals(List.of("firms", "contacts"),
                List.copyOf(cfg.reader().entities().keySet()));
        assertEquals(List.of("firms"), List.copyOf(out.entities().keySet()));
        assertEquals(List.of("name", "city"), cfg.readerEntityFields("firms"));
        assertEquals("C", out.entities().get("firms").mapping().destEntity());
        assertEquals("{name}", out.entities().get("firms").mapping().fields().get(0).formula());
    }

    @Test
    void moveSourceEntityReordersTheCanonicalList() {
        final IpcConfig cfg = new IpcConfig();
        cfg.addSourceEntity("a");
        cfg.addSourceEntity("b");
        cfg.addSourceEntity("c");

        cfg.moveSourceEntity("c", -1);
        assertEquals(List.of("a", "c", "b"), cfg.processSourceEntities());
        cfg.moveSourceEntity("a", 1);
        assertEquals(List.of("c", "a", "b"), cfg.processSourceEntities());
    }
}
