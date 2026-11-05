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
package ch.autumo.ifacex.studio.core.meta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import ch.autumo.ifacex.studio.core.model.KeyModel;

/**
 * The additional readers/writers of autumo-ifacex-iprw must be available in the Studio: listed
 * after the standard interfaces, with their engine class and their parameter catalogue.
 */
class AdditionalInterfacesTest {

    private static final List<String> ADDITIONAL_READERS = List.of(
            "aws_file_in", "gdrive_file_in", "gstorage_file_in", "dbox_file_in", "webdav_file_in",
            "ftp_file_in", "sftp_file_in", "os_swift_file_in", "azure_file_in", "b2_file_in",
            "nifi_in");

    private static final List<String> ADDITIONAL_WRITERS = List.of(
            "aws_file_out", "gdrive_file_out", "gstorage_file_out", "dbox_file_out", "webdav_file_out",
            "ftp_file_out", "sftp_file_out", "os_swift_file_out", "azure_file_out", "b2_file_out",
            "nifi_out", "excel_out");

    @Test
    void readersAreRegisteredAfterTheStandardOnes() {
        final List<String> all = InterfaceTypes.readerNames();
        assertTrue(all.containsAll(ADDITIONAL_READERS), "additional readers missing: " + all);
        assertTrue(all.indexOf("rest_in") < all.indexOf("aws_file_in"),
                "the additional readers must follow the standard ones");
        assertEquals(ADDITIONAL_READERS,
                all.subList(all.size() - ADDITIONAL_READERS.size(), all.size()));
    }

    @Test
    void writersAreRegisteredAfterTheStandardOnes() {
        final List<String> all = InterfaceTypes.writerNames();
        assertTrue(all.containsAll(ADDITIONAL_WRITERS), "additional writers missing: " + all);
        assertTrue(all.indexOf("rest_out") < all.indexOf("aws_file_out"),
                "the additional writers must follow the standard ones");
        assertEquals(ADDITIONAL_WRITERS,
                all.subList(all.size() - ADDITIONAL_WRITERS.size(), all.size()));
    }

    @Test
    void engineClassesAndDisplayNamesAreKnown() {
        assertEquals("ch.autumo.ifacex.ip.amazon.AWSS3FileReader",
                InterfaceTypes.of("aws_file_in").engineClass());
        assertEquals("ch.autumo.ifacex.ip.openstack.OpenStackSwiftWriter",
                InterfaceTypes.of("os_swift_file_out").engineClass());
        assertEquals("Backblaze B2 File", InterfaceTypes.display("b2_file"));
        assertTrue(InterfaceTypes.of("azure_file_in").isReader());
        assertTrue(InterfaceTypes.of("azure_file_out").isWriter());
    }

    @Test
    void parameterCatalogueCoversTheAdditionalInterfaces() {
        final List<ParameterCatalog.Entry> aws = ParameterCatalog.entriesFor("aws_file_in");
        assertFalse(aws.isEmpty(), "aws_file_in must have parameters");
        assertTrue(aws.stream().anyMatch(e -> "access_key".equals(e.param())));
        assertTrue(aws.stream().anyMatch(e -> "temp_out_path".equals(e.param())));

        final List<ParameterCatalog.Entry> azure = ParameterCatalog.entriesFor("azure_file_out");
        assertTrue(azure.stream().anyMatch(e -> "connection_string".equals(e.param())));
        assertTrue(azure.stream().anyMatch(e -> "container".equals(e.param())));

        final List<ParameterCatalog.Entry> ftp = ParameterCatalog.entriesFor("ftp_file_in");
        assertTrue(ftp.stream().anyMatch(e -> "port".equals(e.param())
                && "21".equals(e.defaultValue())));
        assertTrue(ftp.stream().anyMatch(e -> "prot".equals(e.param())
                && "P".equals(e.defaultValue())));

        final List<ParameterCatalog.Entry> gdrive = ParameterCatalog.entriesFor("gdrive_file_in");
        assertTrue(gdrive.stream().anyMatch(e -> "token_uri".equals(e.param())
                && "https://oauth2.googleapis.com/token".equals(e.defaultValue())));
    }

    @Test
    void legacyKeysOfAdditionalInterfacesResolveToTheirSection() {
        assertEquals("aws_file_in", KeyModel.rwPrefix("aws_file_in_access_key"));
        assertEquals("aws_file_in", KeyModel.rwPrefix("aws_file_in_*_region"));
        assertEquals("os_swift_file_in", KeyModel.rwPrefix("os_swift_file_in_container"));
        assertEquals("azure_file_out", KeyModel.rwPrefix("azure_file_out_connection_string"));
        assertEquals("b2_file_out", KeyModel.rwPrefix("b2_file_out_bucket"));
        // standard interfaces keep working
        assertEquals("csv_in", KeyModel.rwPrefix("csv_in_header_size"));
        assertTrue(KeyModel.isReaderPrefix(KeyModel.rwPrefix("sftp_file_in_host")));
        assertNotNull(KeyModel.rwPrefix("webdav_file_out_url"));
    }

    @Test
    void labelsUseAcronymsAndTemplateHelpTexts() {
        final FieldDescriptor ssl = FieldCatalog.descriptor("ssl");
        assertEquals("SSL", ssl.label(), "acronyms are written in capitals");
        assertEquals(FieldType.BOOLEAN, ssl.type(), "SSL is a yes/no parameter");
        assertFalse(ssl.help().isEmpty(), "the SSL parameter carries a help text");

        final FieldDescriptor version = FieldCatalog.descriptor("os_swift_file_in_version");
        assertEquals("OpenStack KeyStone version", version.label());
        assertTrue(version.help().toLowerCase().contains("keystone"), version.help());

        assertEquals("URL end-point", FieldCatalog.descriptor("url_endpoint").label());
        assertEquals("API key ID", FieldCatalog.descriptor("api_key_id").label());
        assertEquals("User ID", FieldCatalog.descriptor("user_id").label());
        assertEquals("Parent folder ID", FieldCatalog.descriptor("folder_id_parent").label());
    }

    @Test
    void encryptedTemplateParametersAreSecretFields() {
        // "can be encrypted, use encoder.sh" in the templates means: a secret field.
        for (final String p : List.of("access_key", "secret_key", "client_id", "client_secret",
                "oauth_access_token", "connection_string", "api_key_id", "api_key",
                "user_id", "password")) {
            assertEquals(FieldType.SECRET, FieldCatalog.descriptor(p).type(), p);
        }
        assertEquals(FieldType.SECRET, FieldCatalog.descriptor("webdav_file_in_user").type(),
                "the WebDAV user name can be encrypted");
        assertEquals(FieldType.SECRET, FieldCatalog.descriptor("os_swift_file_out_user").type(),
                "the OpenStack user name can be encrypted");
        assertEquals(FieldType.SECRET, ParameterCatalog.entriesFor("webdav_file_in").stream()
                .filter(e -> "user".equals(e.param())).findFirst().orElseThrow().type());
        assertEquals(FieldType.SECRET, ParameterCatalog.entriesFor("gdrive_file_out").stream()
                .filter(e -> "client_id".equals(e.param())).findFirst().orElseThrow().type());
        assertEquals(FieldType.SECRET, ParameterCatalog.entriesFor("os_swift_file_in").stream()
                .filter(e -> "user_id".equals(e.param())).findFirst().orElseThrow().type());
    }

    @Test
    void yesNoParametersAreBooleansInTheCatalogue() {
        for (final String iface : List.of("webdav_file_in", "ftp_file_in", "os_swift_file_in")) {
            assertTrue(ParameterCatalog.entriesFor(iface).stream()
                    .anyMatch(e -> "ssl".equals(e.param()) && e.type() == FieldType.BOOLEAN), iface);
        }
        assertTrue(ParameterCatalog.entriesFor("ftp_file_out").stream()
                .anyMatch(e -> "reusable_ssl".equals(e.param()) && e.type() == FieldType.BOOLEAN));
        assertTrue(ParameterCatalog.entriesFor("webdav_file_out").stream()
                .anyMatch(e -> "allow_delete".equals(e.param()) && e.type() == FieldType.BOOLEAN));
    }


    @Test
    void officeExcelWriterIsAvailable() {
        assertTrue(InterfaceTypes.writerNames().contains("excel_out"));
        assertTrue(InterfaceTypes.writerNames().indexOf("excel_out")
                > InterfaceTypes.writerNames().indexOf("rest_out"), "after the standard writers");
        assertEquals("ch.autumo.ifacex.ip.office.excel.ExcelWriter",
                InterfaceTypes.of("excel_out").engineClass());
        assertEquals("Excel", InterfaceTypes.display("excel"));
        assertTrue(ParameterCatalog.entriesFor("excel_out").stream()
                .anyMatch(e -> "buffer_size".equals(e.param())
                        && e.type() == FieldType.INTEGER && "1000".equals(e.defaultValue())));
        assertEquals("Buffer size", FieldCatalog.descriptor("excel_out_buffer_size").label());
        assertFalse(FieldCatalog.descriptor("excel_out_buffer_size").help().isEmpty());
    }

}
