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
package ch.autumo.ifacex.studio.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import ch.autumo.ui.utils.PoFile;

/**
 * Tests the autumo-ui PO reader (escapes, contexts, fuzzy entries, header, plurals).
 *
 * <p>The reader now lives in the framework ({@code ch.autumo.ui.utils.PoFile}) because PO support
 * is a framework feature; the Studio keeps the test here because the PO catalogue itself is
 * shipped with the Studio UI.</p>
 */
class PoFileTest {

    @Test
    void parsesSimpleEntry() throws Exception {
        final Map<String, String> po = PoFile.parse("""
                msgid "Save"
                msgstr "Speichern"
                """);
        assertEquals("Speichern", po.get("Save"));
    }

    @Test
    void parsesHeaderAndSkipsIt() throws Exception {
        final Map<String, String> po = PoFile.parse("""
                msgid ""
                msgstr ""
                "Project-Id-Version: 1.0\\n"
                "Content-Type: text/plain; charset=UTF-8\\n"

                msgid "Open"
                msgstr "Öffnen"
                """);
        assertEquals(1, po.size());
        assertEquals("Öffnen", po.get("Open"));
    }

    @Test
    void keyUsesContextOnlyWhenPresent() {
        assertEquals("Save", PoFile.key(null, "Save"));
        assertEquals("Save", PoFile.key("", "Save"));
        assertEquals("menu" + PoFile.CONTEXT_SEPARATOR + "Save", PoFile.key("menu", "Save"));
    }

    @Test
    void parsesContextAndMultiline() throws Exception {
        final Map<String, String> po = PoFile.parse("""
                msgctxt "menu"
                msgid "New"
                msgstr ""
                "Neu"

                msgid ""
                "a very long "
                "message"
                msgstr ""
                "eine sehr lange "
                "Nachricht"
                """);
        assertEquals("Neu", po.get(PoFile.key("menu", "New")));
        assertEquals("eine sehr lange Nachricht", po.get("a very long message"));
    }

    @Test
    void decodesEscapes() throws Exception {
        final Map<String, String> po = PoFile.parse("""
                msgid "line\\nbreak\\ttab\\"quote\\\\slash\\101"
                msgstr "Zeilen\\nbruch"
                """);
        assertEquals("Zeilen\nbruch", po.get("line\nbreak\ttab\"quote\\slashA"));
    }

    @Test
    void skipsFuzzyAndUntranslated() throws Exception {
        final Map<String, String> po = PoFile.parse("""
                #, fuzzy
                msgid "Unreliable"
                msgstr "Unzuverlässig"

                msgid "Untranslated"
                msgstr ""

                msgid "Good"
                msgstr "Gut"
                """);
        assertEquals(1, po.size());
        assertNull(po.get("Unreliable"));
        assertNull(po.get("Untranslated"));
        assertEquals("Gut", po.get("Good"));
    }

    @Test
    void usesSingularPluralFormOnly() throws Exception {
        final Map<String, String> po = PoFile.parse("""
                msgid "{0} entity"
                msgid_plural "{0} entities"
                msgstr[0] "{0} Entität"
                msgstr[1] "{0} Entitäten"
                """);
        assertEquals("{0} Entität", po.get("{0} entity"));
        assertFalse(po.containsKey("{0} entities"));
    }

    @Test
    void toleratesCommentLines() throws Exception {
        final Map<String, String> po = PoFile.parse("""
                # comment
                #: Form.java:12
                #. translator note
                msgid "Mapped"
                msgstr "Gemappt"
                """);
        assertEquals("Gemappt", po.get("Mapped"));
        assertTrue(po.size() == 1);
    }
}
