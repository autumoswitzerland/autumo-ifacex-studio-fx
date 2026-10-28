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
package ch.autumo.ifacex.studio.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The converted file keeps the base name of the source; the old configuration extension is
 * dropped, so {@code ifacex.cfg} becomes {@code ifacex.ifcx} and not {@code ifacex.cfg.ifcx}.
 */
class ConvertToolNameTest {

    @Test
    void theKnownExtensionIsDropped() {
        assertEquals("ifacex", ConvertTool.baseName("ifacex.cfg"));
        assertEquals("002-hubspot-v3-companies", ConvertTool.baseName("002-hubspot-v3-companies.ifacex"));
        assertEquals("stock.metric", ConvertTool.baseName("stock.metric.ifacex"),
                "only the configuration extension is dropped");
        assertEquals("noext", ConvertTool.baseName("noext"));
    }

    @Test
    void theExtensionIsOnlyRecognisedForConfigurationFiles() {
        assertEquals(".cfg", ConvertTool.extension("ifacex.cfg"));
        assertEquals(".ifacex", ConvertTool.extension("a.ifacex"));
        assertEquals("", ConvertTool.extension("a.txt"));
        assertEquals("", ConvertTool.extension("noext"));
    }

    @Test
    void aBaseConfigurationIsConvertedToThePlainBaseName(@TempDir Path dir) throws Exception {
        final Path in = dir.resolve("ifacex.cfg");
        Files.writeString(in, String.join("\n",
                "source_interface=csv_in",
                "one_error_stop=yes",
                ""));

        final Path out = dir.resolve("out");
        ConvertTool.convert(in, out);

        assertTrue(Files.isRegularFile(out.resolve("ifacex.ifcx")), "ifacex.ifcx expected");
        assertTrue(Files.isRegularFile(out.resolve("ifacex.cfg")), "re-export keeps the extension");
    }

    @Test
    void aTemplateBlockKeepsItsNameAndTheOriginalIsKept(@TempDir Path dir) throws Exception {
        final Path block = dir.resolve("01_csv_in.template");
        final String legacy = String.join("\n",
                "#",
                "# Header size, 0 = no header",
                "#",
                "csv_in_header_size=1",
                "csv_in_<entity1>_fields=",
                "");
        Files.writeString(block, legacy);

        ConvertTool.convertDirectory(dir, dir);

        final String converted = Files.readString(block);
        assertTrue(converted.contains("formatVersion:"), "the block is converted:\n" + converted);
        assertTrue(converted.contains("\"<entity1>\": {}"),
                "a placeholder is an entity, not a parameter:\n" + converted);
        assertEquals(legacy, Files.readString(dir.resolve("old_01_csv_in.template")),
                "the legacy block is kept as old_<name>");
        assertTrue(converted.contains("# Header size, 0 = no header"),
                "a template carries its comments like every other file:\n" + converted);
    }

    @Test
    void aSecondRunDoesNotConvertTheBlocksAgain(@TempDir Path dir) throws Exception {
        final Path block = dir.resolve("10_file_in.template");
        Files.writeString(block, "file_in_path=\n");

        ConvertTool.convertDirectory(dir, dir);
        final String first = Files.readString(block);
        assertEquals(0, ConvertTool.convertDirectory(dir, dir));

        assertEquals(first, Files.readString(block), "the converted block stays as it is");
        assertTrue(Files.notExists(dir.resolve("old_old_10_file_in.template")),
                "the backup of a backup must not appear");
    }

    @Test
    void aDirectoryIsConvertedFileByFile(@TempDir Path dir) throws Exception {
        final Path src = dir.resolve("src");
        Files.createDirectories(src);
        Files.writeString(src.resolve("job-a.ifacex"), "source_interface=csv_in\n");
        Files.writeString(src.resolve("job-b.ifacex"), "source_interface=db_in\n");
        Files.writeString(src.resolve("notes.txt"), "ignored\n");
        final Path out = dir.resolve("out");

        final int failed = ConvertTool.convertDirectory(src, out);

        assertEquals(0, failed);
        assertTrue(Files.isRegularFile(out.resolve("job-a.ifcx")));
        assertTrue(Files.isRegularFile(out.resolve("job-b.ifcx")));
        assertTrue(Files.notExists(out.resolve("notes.ifcx")), "only *.ifacex is converted");
        assertEquals("source_interface=csv_in\n", Files.readString(src.resolve("job-a.ifacex")),
                "the source files stay unchanged");
    }

    @Test
    void anExistingPropertiesFileIsNeverOverwritten(@TempDir Path dir) throws Exception {
        final Path in = dir.resolve("src").resolve("ifacex.cfg");
        Files.createDirectories(in.getParent());
        Files.writeString(in, "source_interface=csv_in\n");
        final Path out = dir.resolve("out");
        Files.createDirectories(out);
        // An original configuration that already exists at the target: it must stay untouched.
        final Path existing = out.resolve("ifacex.cfg");
        Files.writeString(existing, "ORIGINAL\n");

        ConvertTool.convert(in, out);

        assertEquals("ORIGINAL\n", Files.readString(existing));
        assertTrue(Files.isRegularFile(out.resolve("ifacex.ifcx")));
    }

    @Test
    void theSourceIsNeverOverwritten(@TempDir Path dir) throws Exception {
        final Path in = dir.resolve("job.ifacex");
        Files.writeString(in, "source_interface=csv_in\n");
        final String before = Files.readString(in);

        // outDir == the source directory: the re-export must not touch the source.
        ConvertTool.convert(in, dir);

        assertEquals(before, Files.readString(in), "the source file stays unchanged");
        assertTrue(Files.isRegularFile(dir.resolve("job.ifcx")), "job.ifcx expected");
    }
}
