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
package ch.autumo.ifacex.studio.core.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ch.autumo.ifacex.studio.core.model.IpcConfig;

/**
 * The legacy to {@code .ifcx} conversion keeps the comments that document a parameter: the block
 * that is written directly above a key travels to the generated YAML, the free-standing banners
 * and the file header do not.
 */
class CommentConversionTest {

    private static final String LEGACY = String.join("\n",
            "###############################################################################",
            "#",
            "#  Title: commented",
            "#  Version: 2.1",
            "#",
            "#------------------------------------------------------------------------------",
            "#",
            "#  Author: me",
            "#  Date: 2026-10-27",
            "#",
            "###############################################################################",
            "",
            "",
            "###############################################################################",
            "# Generic REST reader configuration",
            "###############################################################################",
            "",
            "",
            "source_interface=rest_in",
            "",
            "",
            "#",
            "# Host name",
            "#",
            "rest_in_host=api.example.com",
            "",
            "",
            "# Auth method; none, apikey, basic, digest, oauth",
            "rest_in_auth_method=none",
            "",
            "", "");

    private static IpcConfig read(@TempDir Path dir) throws Exception {
        final Path file = dir.resolve("commented.ifacex");
        Files.writeString(file, LEGACY);
        return PropertiesConfigReader.read(file);
    }

    @Test
    void theCommentBlockAboveAKeyIsRemembered(@TempDir Path dir) throws Exception {
        final IpcConfig cfg = read(dir);

        assertEquals(List.of("#", "# Host name", "#"), cfg.comments().get("rest_in_host"));
        assertEquals(List.of("# Auth method; none, apikey, basic, digest, oauth"),
                cfg.comments().get("rest_in_auth_method"));
    }

    @Test
    void bannersAndTheFileHeaderAreNotAttachedToAKey(@TempDir Path dir) throws Exception {
        final IpcConfig cfg = read(dir);

        // The header is a block of its own, separated from the keys by blank lines.
        assertNull(cfg.comments().get("source_interface"));
        assertEquals("commented", cfg.meta().title());
        assertEquals("2.1", cfg.meta().version());
        // The legacy date belongs to the legacy file: a conversion creates a new file.
        assertNotEquals("2026-10-27", cfg.meta().date(), "the converted file is dated today");
        assertFalse(cfg.meta().date().isBlank(), "the converted file carries a date");
        // The "Generic REST reader configuration" banner is separated from the key by blanks.
        assertFalse(String.join("\n", cfg.comments().getOrDefault("rest_in_host", List.of()))
                .contains("Generic REST reader"), "the banner must not become the key comment");
    }

    @Test
    void theGeneratedYamlCarriesTheCommentBlockAboveItsKey(@TempDir Path dir) throws Exception {
        final String yaml = YamlConfigWriter.toText(read(dir));

        final int comment = yaml.indexOf("# Host name");
        final int key = yaml.indexOf("host: api.example.com");
        assertTrue(comment > 0, "comment written:\n" + yaml);
        assertTrue(key > comment, "comment above its key:\n" + yaml);
        // One blank line in front of the block, the block glued to the key.
        assertTrue(yaml.contains("params:\n\n      #\n      # Host name\n      #\n      host: api.example.com"),
                "readable block layout:\n" + yaml);
    }

    @Test
    void theYamlStillReadsBackTheSameValues(@TempDir Path dir) throws Exception {
        final IpcConfig original = read(dir);
        final IpcConfig reparsed = YamlConfigReader.read(YamlConfigWriter.toText(original));

        assertNotNull(reparsed.reader());
        assertEquals("api.example.com", reparsed.reader().global().get("host"));
        assertEquals("none", reparsed.reader().global().get("auth_method"));
        assertEquals("rest_in", reparsed.sourceInterface());
    }
}
