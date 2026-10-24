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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ch.autumo.ifacex.studio.core.model.EntityConfig;
import ch.autumo.ifacex.studio.core.model.FieldMappingRec;
import ch.autumo.ifacex.studio.core.model.IpcConfig;
import ch.autumo.ifacex.studio.core.model.RwConfig;

/**
 * Tests of the import of a legacy {@code .ifacex} configuration.
 */
class PropertiesConfigReaderTest {

    /**
     * The mapping fields keep the order of the file: the number of a field ({@code _01},
     * {@code _021}, …) is its position, no matter whether the field is inserted or not.
     *
     * @param dir the temporary directory for the test file
     * @throws Exception if the file cannot be written or read
     */
    @Test
    void theMappingFieldsKeepTheOrderOfTheFile(@TempDir Path dir) throws Exception {
        final Path file = dir.resolve("order.ifacex");
        Files.writeString(file, String.join("\n",
                "wb_out_contacts_mapping_entity=adresse",
                "wb_out_contacts_mapping_field_01=kundennummer:{hs_object_id}",
                "wb_out_contacts_mapping_field_02=vorname:{firstname}",
                "wb_out_contacts_mapping_insert_021=typ:herr",
                "wb_out_contacts_mapping_field_155=nachname:{lastname}",
                "wb_out_contacts_mapping_insert_022=marketingsperre:",
                ""));

        final IpcConfig config = PropertiesConfigReader.read(file);

        final RwConfig writer = config.sections().stream()
                .filter(rw -> "wb_out".equals(rw.name())).findFirst().orElseThrow();
        final EntityConfig contacts = writer.entities().get("contacts");
        final List<FieldMappingRec> fields = contacts.mapping().fields();

        assertEquals(List.of("kundennummer", "vorname", "typ", "marketingsperre", "nachname"),
                fields.stream().map(FieldMappingRec::destField).toList(),
                "the fields keep the order of their numbers in the file");
    }
}
