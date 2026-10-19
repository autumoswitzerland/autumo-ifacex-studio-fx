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
package ch.autumo.ifacex.studio.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Destination-mapping syntax and the legacy "generateMappings" behaviour. */
class MappingFormulaTest {

    @Test
    void parsesValidLines() {
        final FieldMappingRec rec = MappingFormula.parse("full-name:{first-name} {last-name}", false);
        assertEquals("full-name", rec.destField());
        assertEquals("{first-name} {last-name}", rec.formula());
        assertFalse(rec.insert());

        final FieldMappingRec ins = MappingFormula.parse("created:{now}", true);
        assertTrue(ins.insert());
    }

    @Test
    void rejectsInvalidLines() {
        assertNull(MappingFormula.parse(null, false));
        assertNull(MappingFormula.parse("   ", false));
        assertNull(MappingFormula.parse("no-colon-here", false));
        assertNull(MappingFormula.parse("bad target:{x}", false), "space in target");
        assertNull(MappingFormula.parse("a:b:c", false), "more than one colon (legacy split)");
        assertNull(MappingFormula.parse(":{x}", false));
        assertFalse(MappingFormula.isValid("a:b:c"));
        assertTrue(MappingFormula.isValid("TARGET-1:{x}"));
    }

    @Test
    void formatsDisplayWithLegacyPrefix() {
        assertEquals("[STD] a:{x}",
                MappingFormula.display(new FieldMappingRec("a", "{x}", false)));
        assertEquals("[INS] a:{x}",
                MappingFormula.display(new FieldMappingRec("a", "{x}", true)));
        assertEquals("a:{x}", MappingFormula.line(new FieldMappingRec("a", "{x}", false)));
    }

    @Test
    void standardForMapsEveryFieldToItselfAndSkipsExisting() {
        final List<FieldMappingRec> out = MappingFormula.standardFor(
                List.of("name", "city"),
                List.of(new FieldMappingRec("name", "{name}", false)));

        assertEquals(1, out.size());
        assertEquals("city", out.get(0).destField());
        assertEquals("{city}", out.get(0).formula());
        assertFalse(out.get(0).insert());
    }

    @Test
    void standardForIsIdempotent() {
        final List<FieldMappingRec> first = MappingFormula.standardFor(List.of("a", "b"), List.of());
        assertEquals(2, first.size());
        assertTrue(MappingFormula.standardFor(List.of("a", "b"), first).isEmpty());
    }
}
