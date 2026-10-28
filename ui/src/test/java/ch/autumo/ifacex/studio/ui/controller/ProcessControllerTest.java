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
package ch.autumo.ifacex.studio.ui.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * The parts of the external process handling that do not need a running window: the argument the
 * engine receives for the test switches and the decoding of the process output.
 */
class ProcessControllerTest {

    /**
     * The engine only parses the extra argument when it starts with {@code testswitches:}
     * (see {@code App}, step "[2] Test Switches"); without the prefix DEBUG and ROOTCALL are
     * silently ignored.
     */
    @Test
    void testSwitchesCarryThePrefixTheEngineExpects() {
        final ProcessController controller = new ProcessController(null, false);

        assertEquals("testswitches:", controller.consumeSwitchesAsString(),
                "no switches still need the prefix - the engine receives the argument either way");

        controller.addTestSwitch("ROOTCALL");
        assertEquals("testswitches:ROOTCALL", controller.consumeSwitchesAsString());

        controller.addTestSwitch("DEBUG");
        assertEquals("testswitches:ROOTCALL,DEBUG", controller.consumeSwitchesAsString());
    }

    @Test
    void switchesAreNotDuplicated() {
        final ProcessController controller = new ProcessController(null, false);
        controller.addTestSwitch("DEBUG");
        controller.addTestSwitch("DEBUG");

        assertEquals("testswitches:DEBUG", controller.consumeSwitchesAsString());
    }

    @Test
    void utf8OutputIsDecodedAsUtf8() {
        final byte[] bytes = "Grüezi, Zürich".getBytes(StandardCharsets.UTF_8);

        assertEquals("Grüezi, Zürich", ProcessController.decode(bytes));
    }

    @Test
    void plainAsciiOutputIsDecoded() {
        assertEquals("INFO 2026-10-27 hello",
                ProcessController.decode("INFO 2026-10-27 hello".getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    void aTrailingCarriageReturnIsDropped() {
        assertEquals("line", ProcessController.decode("line\r".getBytes(StandardCharsets.US_ASCII)));
    }

    /**
     * Windows JVMs write their console output in the platform code page. Bytes that are not
     * valid UTF-8 must therefore fall back to the platform charset instead of producing
     * replacement characters.
     */
    @Test
    void invalidUtf8FallsBackToThePlatformCharset() {
        // 0xFC is 'ü' in ISO-8859-1/CP1252 and an invalid UTF-8 sequence on its own.
        final byte[] bytes = new byte[] { (byte) 0xFC };

        assertEquals(new String(bytes, Charset.defaultCharset()), ProcessController.decode(bytes),
                "bytes that are not UTF-8 are decoded with the platform charset");
    }

    @Test
    void anErrorLevelAtTheStartOfALineIsAnError() {
        assertTrue(ProcessController.hasErrorLevel("ERROR 20261028-120000 [main] c.a.i.Processor : boom"));
        assertTrue(ProcessController.hasErrorLevel("FATAL something failed"));
    }

    @Test
    void aColouredErrorLineIsAnError() {
        // The engine logs with ANSI colours: the level is not at the very beginning of the bytes.
        assertTrue(ProcessController.hasErrorLevel(
                "\u001B[1;31mERROR\u001B[m \u001B[90m20261028-120000\u001B[m [main] boom"));
    }

    @Test
    void theWordErrorInsideADataRecordIsNoError() {
        // A record ("data") may carry the word in a field - the run succeeded.
        assertFalse(ProcessController.hasErrorLevel(
                "Anna;anna@x.ch;ERROR\nBob;bob@x.ch;OK"));
        assertFalse(ProcessController.hasErrorLevel("status;NO ERROR FOUND"));
        assertFalse(ProcessController.hasErrorLevel("my_ERROR_column;1"));
    }

    @Test
    void emptyOutputHasNoError() {
        assertFalse(ProcessController.hasErrorLevel(null));
        assertFalse(ProcessController.hasErrorLevel(""));
        assertFalse(ProcessController.hasErrorLevel("****  ifaceX interface processor ['x'] succeeded.  ===="));
    }
}
