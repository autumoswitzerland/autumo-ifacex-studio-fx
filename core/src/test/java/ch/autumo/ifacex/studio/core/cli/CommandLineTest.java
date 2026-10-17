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
package ch.autumo.ifacex.studio.core.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Tests for the launcher command-line handling (double-click / {@code --open}). */
class CommandLineTest {

    @Test
    void recognisesConfigExtensions() {
        assertTrue(CommandLine.isConfigFileName("job.ifacex"));
        assertTrue(CommandLine.isConfigFileName("job.ifcx"));
        assertTrue(CommandLine.isConfigFileName("job.yaml"));
        assertTrue(CommandLine.isConfigFileName("job.YML"));
        assertTrue(CommandLine.isConfigFileName("/tmp/My Job.IFCX"));
        assertFalse(CommandLine.isConfigFileName("readme.txt"));
        assertFalse(CommandLine.isConfigFileName("archive.ifacex.zip"));
        assertFalse(CommandLine.isConfigFileName(null));
    }

    @Test
    void acceptsBareFileForDoubleClick() {
        assertEquals(Path.of("/tmp/job.ifcx"), CommandLine.openTarget(List.of("/tmp/job.ifcx")));
        assertEquals(Path.of("job.ifacex"), CommandLine.openTarget(List.of("job.ifacex")));
    }

    @Test
    void acceptsExplicitOpenOption() {
        assertEquals(Path.of("/tmp/job.ifacex"),
                CommandLine.openTarget(List.of("--open", "/tmp/job.ifacex")));
        // "--open" may name a file of any extension (explicit intent)
        assertEquals(Path.of("/tmp/anything.txt"),
                CommandLine.openTarget(List.of("--open", "/tmp/anything.txt")));
    }

    @Test
    void ignoresNonConfigBareArguments() {
        assertNull(CommandLine.openTarget(List.of("/tmp/readme.txt")));
        assertNull(CommandLine.openTarget(List.of("--some-option")));
    }

    @Test
    void doesNotTreatOwnOptionsAsFiles() {
        assertNull(CommandLine.openTarget(List.of("--selftest", "job.ifacex")));
        assertNull(CommandLine.openTarget(List.of()));
        assertNull(CommandLine.openTarget(null));
    }

    @Test
    void toleratesMissingOpenArgument() {
        assertNull(CommandLine.openTarget(List.of("--open")));
    }
}
