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

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ch.autumo.ifacex.studio.core.StudioConstants;
import ch.autumo.ui.utils.CommandLine;

/**
 * Tests for the framework command-line handling ({@code --open} / double-click) with the endings
 * the Studio registers.
 */
class CommandLineTest {

    /** The endings the Studio registers, in the order {@link StudioApp} registers them. */
    private static final Set<String> STUDIO_EXTENSIONS = Set.of(
            StudioConstants.EXT_IFACEX, StudioConstants.EXT_IFCX,
            StudioConstants.EXT_YAML, StudioConstants.EXT_YML);

    @Test
    void recognisesRegisteredExtensions() {
        assertTrue(CommandLine.hasExtension("job.ifacex", STUDIO_EXTENSIONS));
        assertTrue(CommandLine.hasExtension("job.ifcx", STUDIO_EXTENSIONS));
        assertTrue(CommandLine.hasExtension("job.yaml", STUDIO_EXTENSIONS));
        assertTrue(CommandLine.hasExtension("job.YML", STUDIO_EXTENSIONS));
        assertTrue(CommandLine.hasExtension("/tmp/My Job.IFCX", STUDIO_EXTENSIONS));
        assertFalse(CommandLine.hasExtension("readme.txt", STUDIO_EXTENSIONS));
        assertFalse(CommandLine.hasExtension("archive.ifacex.zip", STUDIO_EXTENSIONS));
        assertFalse(CommandLine.hasExtension(null, STUDIO_EXTENSIONS));
    }

    @Test
    void noRegisteredExtensionAcceptsNothing() {
        // The framework knows no endings by itself: an application has to register them.
        assertFalse(CommandLine.hasExtension("job.ifcx", Set.of()));
        assertNull(CommandLine.openTarget(List.of("job.ifcx"), Set.of()));
    }

    @Test
    void acceptsBareFileForDoubleClick() {
        assertEquals(Path.of("/tmp/job.ifcx"),
                CommandLine.openTarget(List.of("/tmp/job.ifcx"), STUDIO_EXTENSIONS));
        assertEquals(Path.of("job.ifacex"),
                CommandLine.openTarget(List.of("job.ifacex"), STUDIO_EXTENSIONS));
    }

    @Test
    void acceptsExplicitOpenOption() {
        assertEquals(Path.of("/tmp/job.ifacex"),
                CommandLine.openTarget(List.of("--open", "/tmp/job.ifacex"), STUDIO_EXTENSIONS));
        // "--open" may name a file of any extension (explicit intent)
        assertEquals(Path.of("/tmp/anything.txt"),
                CommandLine.openTarget(List.of("--open", "/tmp/anything.txt"), STUDIO_EXTENSIONS));
    }

    @Test
    void ignoresNonConfigBareArguments() {
        assertNull(CommandLine.openTarget(List.of("/tmp/readme.txt"), STUDIO_EXTENSIONS));
        assertNull(CommandLine.openTarget(List.of("--some-option"), STUDIO_EXTENSIONS));
    }

    @Test
    void doesNotTreatOwnOptionsAsFiles() {
        assertNull(CommandLine.openTarget(List.of("--verbose", "job.ifacex"), STUDIO_EXTENSIONS));
        assertNull(CommandLine.openTarget(List.of(), STUDIO_EXTENSIONS));
        assertNull(CommandLine.openTarget(null, STUDIO_EXTENSIONS));
    }

    @Test
    void toleratesMissingOpenArgument() {
        assertNull(CommandLine.openTarget(List.of("--open"), STUDIO_EXTENSIONS));
    }
}
