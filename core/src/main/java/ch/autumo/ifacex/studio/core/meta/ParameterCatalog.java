/**
 * Copyright 2026 autumo GmbH, Michael Gasche.
 *
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The complete per-type parameter inventory of the ifaceX engine (see docs/11 §7).
 *
 * <p>Every entry names one engine parameter of a reader/writer type ({@code <rw>_<param>}),
 * together with its scope ({@code global} {@code <rw>_<param>}, {@code wildcard}
 * {@code <rw>_*_<param>} or {@code entity} {@code <rw>_<entity>_<param>}) and the field type
 * used to render it.</p>
 *
 * <p>The catalogue is used to pre-populate a newly created reader/writer section with <em>all</em>
 * possible parameters - even the empty ones - so that a saved {@code .ifcx} always carries the
 * full parameter set of every declared interface and the GUI can show every parameter, not only
 * those that happen to be present in a loaded file.</p>
 *
 * <p>The entity source fields ({@code <rw>_<entity>_fields}) are managed separately via the
 * source-field list and are <em>not</em> part of this catalogue.</p>
 */
public final class ParameterCatalog {

    /** Scope of a parameter within a reader/writer section. */
    public enum Scope {
        /** {@code <rw>_<param>} - stored in the section's global parameters. */
        GLOBAL,
        /** {@code <rw>_*_<param>} - stored in the section's wildcard defaults. */
        WILDCARD,
        /** {@code <rw>_<entity>_<param>} - stored in each entity's settings. */
        ENTITY
    }

    /** One catalogue entry. */
    public record Entry(String param, Scope scope, FieldType type, String defaultValue) {
        public Entry {
            if (defaultValue == null) {
                defaultValue = "";
            }
        }
    }

    private ParameterCatalog() {
    }

    /** The reader/writer type prefix extracted from a section name, or {@code null} when unknown. */
    private static String typeOf(String rwName) {
        if (rwName == null || rwName.isEmpty()) {
            return null;
        }
        if (rwName.equals("code") || rwName.equals("exec")) {
            return rwName;
        }
        if (rwName.endsWith("_in")) {
            return rwName.substring(0, rwName.length() - 3);
        }
        if (rwName.endsWith("_out")) {
            return rwName.substring(0, rwName.length() - 4);
        }
        return null;
    }

    /** All catalogue entries for a given reader/writer section name, in stable order. */
    public static List<Entry> entriesFor(String rwName) {
        final String type = typeOf(rwName);
        if (type == null) {
            return List.of();
        }
        final List<Entry> all = new ArrayList<>();
        if (rwName.endsWith("_in")) {
            switch (type) {
                case "csv" -> all.addAll(CSV_READER);
                case "json" -> all.addAll(JSON_READER);
                case "db" -> all.addAll(DB_READER);
                case "mongo" -> all.addAll(MONGO_READER);
                case "rest" -> all.addAll(REST_READER);
                case "doc" -> all.addAll(DOC_READER);
                case "aws_file" -> all.addAll(AWS_FILE_READER);
                case "gdrive_file" -> all.addAll(GDRIVE_FILE_READER);
                case "gstorage_file" -> all.addAll(GSTORAGE_FILE_READER);
                case "dbox_file" -> all.addAll(DBOX_FILE_READER);
                case "webdav_file" -> all.addAll(WEBDAV_FILE_READER);
                case "ftp_file" -> all.addAll(FTP_FILE_READER);
                case "sftp_file" -> all.addAll(SFTP_FILE_READER);
                case "os_swift_file" -> all.addAll(OS_SWIFT_FILE_READER);
                case "azure_file" -> all.addAll(AZURE_FILE_READER);
                case "b2_file" -> all.addAll(B2_FILE_READER);
                default -> {
                }
            }
        } else if (rwName.endsWith("_out")) {
            switch (type) {
                case "csv" -> all.addAll(CSV_WRITER);
                case "json" -> all.addAll(JSON_WRITER);
                case "mail" -> all.addAll(MAIL_WRITER);
                case "db" -> all.addAll(DB_WRITER);
                case "mongo" -> all.addAll(MONGO_WRITER);
                case "rest" -> all.addAll(REST_WRITER);
                case "console" -> all.addAll(CONSOLE_WRITER);
                case "first" -> all.addAll(FIRST_WRITER);
                case "file" -> all.addAll(FILE_WRITER);
                case "aws_file" -> all.addAll(AWS_FILE_WRITER);
                case "gdrive_file" -> all.addAll(GDRIVE_FILE_WRITER);
                case "gstorage_file" -> all.addAll(GSTORAGE_FILE_WRITER);
                case "dbox_file" -> all.addAll(DBOX_FILE_WRITER);
                case "webdav_file" -> all.addAll(WEBDAV_FILE_WRITER);
                case "ftp_file" -> all.addAll(FTP_FILE_WRITER);
                case "sftp_file" -> all.addAll(SFTP_FILE_WRITER);
                case "os_swift_file" -> all.addAll(OS_SWIFT_FILE_WRITER);
                case "azure_file" -> all.addAll(AZURE_FILE_WRITER);
                case "b2_file" -> all.addAll(B2_FILE_WRITER);
                case "excel" -> all.addAll(EXCEL_WRITER);
                default -> {
                }
            }
        }
        // code / exec are writers without the _out suffix; they carry their own parameters.
        if ("code".equals(type)) {
            all.addAll(CODE_WRITER);
        } else if ("exec".equals(type)) {
            all.addAll(EXEC_WRITER);
        }
        return all;
    }

    // ---------------------------------------------------------------- CSV reader
    private static final List<Entry> CSV_READER = List.of(
            entry("header_size", Scope.GLOBAL, FieldType.INTEGER, "1"),
            entry("delete_files_after_processing", Scope.GLOBAL, FieldType.BOOLEAN, "no"),
            entry("read_from_writer", Scope.GLOBAL, FieldType.TEXT, "csv_out"),
            entry("read_from_folder", Scope.GLOBAL, FieldType.PATH, ""));

    // ---------------------------------------------------------------- JSON reader
    private static final List<Entry> JSON_READER = List.of(
            entry("data_path", Scope.WILDCARD, FieldType.TEXT, ""),
            entry("sub_path", Scope.WILDCARD, FieldType.TEXT, ""),
            entry("field_path_access", Scope.WILDCARD, FieldType.BOOLEAN, "no"),
            entry("allow_absent_fields", Scope.WILDCARD, FieldType.BOOLEAN, "no"),
            entry("absent_field_placeholder", Scope.WILDCARD, FieldType.TEXT, ""),
            entry("delete_files_after_processing", Scope.GLOBAL, FieldType.BOOLEAN, "no"),
            entry("read_from_writer", Scope.GLOBAL, FieldType.TEXT, "json_out"),
            entry("read_from_folder", Scope.GLOBAL, FieldType.PATH, ""));

    // ---------------------------------------------------------------- DB reader
    private static final List<Entry> DB_READER = List.of(
            entry("url", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("user", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("password", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("fetchSize", Scope.WILDCARD, FieldType.INTEGER, ""),
            entry("order_field", Scope.ENTITY, FieldType.TEXT, ""));

    // ---------------------------------------------------------------- Mongo reader
    private static final List<Entry> MONGO_READER = List.of(
            entry("host", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("protocol", Scope.GLOBAL, FieldType.SELECT, "mongodb"),
            entry("db_name", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("params", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("user", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("password", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("fetchSize", Scope.WILDCARD, FieldType.INTEGER, ""),
            entry("order_field", Scope.ENTITY, FieldType.TEXT, ""));

    // ---------------------------------------------------------------- REST reader
    private static final List<Entry> REST_READER = List.of(
            entry("host", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("protocol", Scope.GLOBAL, FieldType.SELECT, "https"),
            entry("port", Scope.GLOBAL, FieldType.INTEGER, ""),
            entry("user", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("password", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("api_key", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("api_key_name", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("auth_method", Scope.GLOBAL, FieldType.SELECT, "none"),
            entry("auth_url", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("auth_host", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("auth_grant_type", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("auth_body", Scope.GLOBAL, FieldType.TEXT_AREA, ""),
            entry("alt_data_conn", Scope.GLOBAL, FieldType.BOOLEAN, "no"),
            entry("entities_url", Scope.WILDCARD, FieldType.TEXT, "/"),
            entry("data_path", Scope.WILDCARD, FieldType.TEXT, ""),
            entry("sub_path", Scope.WILDCARD, FieldType.TEXT, ""),
            entry("query", Scope.WILDCARD, FieldType.TEXT_AREA, ""),
            entry("field_path_access", Scope.WILDCARD, FieldType.BOOLEAN, "no"),
            entry("allow_absent_fields", Scope.WILDCARD, FieldType.BOOLEAN, "no"),
            entry("absent_field_placeholder", Scope.WILDCARD, FieldType.TEXT, ""),
            entry("additional_headers", Scope.WILDCARD, FieldType.TEXT_AREA, ""),
            entry("additional_params", Scope.WILDCARD, FieldType.TEXT, ""),
            entry("fetchSize", Scope.WILDCARD, FieldType.INTEGER, ""),
            entry("param_pageName", Scope.WILDCARD, FieldType.TEXT, ""),
            entry("param_offsetName", Scope.WILDCARD, FieldType.TEXT, ""),
            entry("param_fetchSize", Scope.WILDCARD, FieldType.INTEGER, ""),
            entry("pagination_path", Scope.WILDCARD, FieldType.TEXT, ""),
            entry("pagination_items_per_page", Scope.WILDCARD, FieldType.INTEGER, ""),
            entry("pagination_items_total", Scope.WILDCARD, FieldType.INTEGER, ""),
            entry("pagination_last_page", Scope.WILDCARD, FieldType.INTEGER, ""),
            entry("pagination_after_id", Scope.WILDCARD, FieldType.TEXT, ""));

    // ---------------------------------------------------------------- Document reader
    private static final List<Entry> DOC_READER = List.of(
            entry("extractor_implementation", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("delete_files_after_processing", Scope.GLOBAL, FieldType.BOOLEAN, "no"),
            entry("fetchSize", Scope.WILDCARD, FieldType.INTEGER, ""),
            entry("contents_max_length", Scope.GLOBAL, FieldType.INTEGER, ""));

    // ---------------------------------------------------------------- CSV writer
    private static final List<Entry> CSV_WRITER = List.of(
            entry("data_mapper_class", Scope.ENTITY, FieldType.TEXT, ""),
            entry("mapping_entity", Scope.ENTITY, FieldType.TEXT, ""));

    // ---------------------------------------------------------------- JSON writer
    private static final List<Entry> JSON_WRITER = List.of(
            entry("data_mapper_class", Scope.ENTITY, FieldType.TEXT, ""),
            entry("mapping_entity", Scope.ENTITY, FieldType.TEXT, ""));

    // ---------------------------------------------------------------- Mail writer
    private static final List<Entry> MAIL_WRITER = List.of(
            entry("host", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("port", Scope.GLOBAL, FieldType.INTEGER, ""),
            entry("auth", Scope.GLOBAL, FieldType.BOOLEAN, "no"),
            entry("user", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("password", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("subject", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("body", Scope.GLOBAL, FieldType.TEXT_AREA, ""),
            entry("to", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("from", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("max_size", Scope.GLOBAL, FieldType.INTEGER, ""),
            entry("tls_enable", Scope.GLOBAL, FieldType.BOOLEAN, "no"),
            entry("delete_files_after_processing", Scope.GLOBAL, FieldType.BOOLEAN, "no"),
            entry("data_mapper_class", Scope.ENTITY, FieldType.TEXT, ""),
            entry("mapping_entity", Scope.ENTITY, FieldType.TEXT, ""));

    // ---------------------------------------------------------------- DB writer
    private static final List<Entry> DB_WRITER = List.of(
            entry("url", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("user", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("password", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("ext_insert_mapping", Scope.GLOBAL, FieldType.BOOLEAN, "no"),
            entry("data_mapper_class", Scope.ENTITY, FieldType.TEXT, ""),
            entry("mapping_entity", Scope.ENTITY, FieldType.TEXT, ""),
            entry("dest_unique_id_field", Scope.ENTITY, FieldType.TEXT, ""),
            entry("dest_filter_fields", Scope.ENTITY, FieldType.TEXT, ""));

    // ---------------------------------------------------------------- Mongo writer
    private static final List<Entry> MONGO_WRITER = List.of(
            entry("host", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("protocol", Scope.GLOBAL, FieldType.SELECT, "mongodb"),
            entry("db_name", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("params", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("user", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("password", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("create_collection", Scope.WILDCARD, FieldType.BOOLEAN, "no"),
            entry("data_mapper_class", Scope.ENTITY, FieldType.TEXT, ""),
            entry("mapping_entity", Scope.ENTITY, FieldType.TEXT, ""),
            entry("dest_unique_id_field", Scope.ENTITY, FieldType.TEXT, ""),
            entry("dest_filter_fields", Scope.ENTITY, FieldType.TEXT, ""));

    // ---------------------------------------------------------------- REST writer
    private static final List<Entry> REST_WRITER = List.of(
            entry("host", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("protocol", Scope.GLOBAL, FieldType.SELECT, "https"),
            entry("port", Scope.GLOBAL, FieldType.INTEGER, ""),
            entry("user", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("password", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("api_key", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("api_key_name", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("auth_method", Scope.GLOBAL, FieldType.SELECT, "none"),
            entry("auth_url", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("auth_host", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("auth_grant_type", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("auth_body", Scope.GLOBAL, FieldType.TEXT_AREA, ""),
            entry("entities_url", Scope.WILDCARD, FieldType.TEXT, "/"),
            entry("error_code", Scope.WILDCARD, FieldType.TEXT, ""),
            entry("error_code_value", Scope.WILDCARD, FieldType.TEXT, ""),
            entry("single_id_filter", Scope.WILDCARD, FieldType.BOOLEAN, "no"),
            entry("dest_id_path", Scope.ENTITY, FieldType.TEXT, ""),
            entry("dest_filter_fields", Scope.ENTITY, FieldType.TEXT, ""));

    // ---------------------------------------------------------------- Console writer
    private static final List<Entry> CONSOLE_WRITER = List.of();

    // ---------------------------------------------------------------- First batch writer
    private static final List<Entry> FIRST_WRITER = List.of();

    // ---------------------------------------------------------------- File writer
    private static final List<Entry> FILE_WRITER = List.of(
            entry("path", Scope.GLOBAL, FieldType.PATH, ""),
            entry("delete_files_after_processing", Scope.GLOBAL, FieldType.BOOLEAN, "no"));

    // ---------------------------------------------------------------- Code writer
    private static final List<Entry> CODE_WRITER = List.of(
            entry("implementation", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("proc_pos", Scope.GLOBAL, FieldType.INTEGER, "0"));

    // ---------------------------------------------------------------- Exec writer
    private static final List<Entry> EXEC_WRITER = List.of(
            entry("cmd", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("args", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("working_dir", Scope.GLOBAL, FieldType.PATH, ""),
            entry("timeout", Scope.GLOBAL, FieldType.INTEGER, "60"),
            entry("proc_pos", Scope.GLOBAL, FieldType.INTEGER, "0"),
            entry("exit_code_success", Scope.GLOBAL, FieldType.INTEGER, "0"));

    private static Entry entry(String param, Scope scope, FieldType type, String defaultValue) {
        return new Entry(param, scope, type, defaultValue);
    }

    /** Number of catalogue entries per type, for diagnostics. */
    public static int sizeFor(String rwName) {
        return entriesFor(rwName).size();
    }

    /** Map of type -> all catalogue entries (used by tooling/tests). */
    static Map<String, List<Entry>> allByType() {
        final Map<String, List<Entry>> out = new LinkedHashMap<>();
        for (final String rw : List.of(
                "csv_in", "json_in", "db_in", "mongo_in", "rest_in", "doc_in",
                "csv_out", "json_out", "mail_out", "db_out", "mongo_out", "rest_out",
                "console_out", "first_out", "file_out", "code", "exec")) {
            out.put(rw, entriesFor(rw));
        }
        return out;
    }

    // ---------------------------------------------------------------- Additional readers/writers
    // autumo-ifacex-iprw (module sources: mod-*) - parameter names verified against the reader/
    // writer classes, defaults taken from the interface templates (interfaces/*.template) where
    // they are real values (ports, SSL flags, OAuth endpoints, regions) and left empty where the
    // template only shows a placeholder (<YOUR_...>) or a machine specific path.

    // ---------------------------------------------------------------- AWS S3 file
    private static final List<Entry> AWS_FILE_READER = List.of(
            entry("access_key", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("secret_key", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("url_endpoint", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("region", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("temp_out_path", Scope.GLOBAL, FieldType.PATH, ""));

    private static final List<Entry> AWS_FILE_WRITER = List.of(
            entry("access_key", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("secret_key", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("url_endpoint", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("region", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("bucket_name", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("key_prefix", Scope.GLOBAL, FieldType.TEXT, ""));

    // ---------------------------------------------------------------- Google Drive file
    private static final List<Entry> GDRIVE_FILE_COMMON = List.of(
            entry("application_name", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("project_id", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("client_id", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("client_secret", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("auth_uri", Scope.GLOBAL, FieldType.TEXT, "https://accounts.google.com/o/oauth2/auth"),
            entry("token_uri", Scope.GLOBAL, FieldType.TEXT, "https://oauth2.googleapis.com/token"),
            entry("auth_provider_x509_cert_url", Scope.GLOBAL, FieldType.TEXT,
                    "https://www.googleapis.com/oauth2/v1/certs"),
            entry("redirect_uris", Scope.GLOBAL, FieldType.TEXT, ""));

    private static final List<Entry> GDRIVE_FILE_READER = concat(GDRIVE_FILE_COMMON, List.of(
            entry("corpora", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("drive_id", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("folder", Scope.GLOBAL, FieldType.PATH, ""),
            entry("temp_out_path", Scope.GLOBAL, FieldType.PATH, "")));

    private static final List<Entry> GDRIVE_FILE_WRITER = concat(GDRIVE_FILE_COMMON, List.of(
            entry("folder", Scope.GLOBAL, FieldType.PATH, ""),
            entry("folder_id_parent", Scope.GLOBAL, FieldType.TEXT, "")));

    // ---------------------------------------------------------------- Google Storage file
    private static final List<Entry> GSTORAGE_FILE_READER = List.of(
            entry("credentials_file", Scope.GLOBAL, FieldType.PATH, ""),
            entry("project_id", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("temp_out_path", Scope.GLOBAL, FieldType.PATH, ""));

    private static final List<Entry> GSTORAGE_FILE_WRITER = List.of(
            entry("credentials_file", Scope.GLOBAL, FieldType.PATH, ""),
            entry("project_id", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("bucket_name", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("region", Scope.GLOBAL, FieldType.TEXT, "EU"),
            entry("storage_class", Scope.GLOBAL, FieldType.TEXT, "STANDARD"));

    // ---------------------------------------------------------------- Dropbox file
    private static final List<Entry> DBOX_FILE_COMMON = List.of(
            entry("app_name", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("app_version", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("oauth_access_token", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("path", Scope.GLOBAL, FieldType.PATH, ""));

    private static final List<Entry> DBOX_FILE_READER = concat(DBOX_FILE_COMMON, List.of(
            entry("temp_out_path", Scope.GLOBAL, FieldType.PATH, "")));

    private static final List<Entry> DBOX_FILE_WRITER = DBOX_FILE_COMMON;

    // ---------------------------------------------------------------- WebDAV file
    private static final List<Entry> WEBDAV_FILE_READER = List.of(
            entry("url", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("ssl", Scope.GLOBAL, FieldType.BOOLEAN, "yes"),
            entry("user", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("password", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("temp_out_path", Scope.GLOBAL, FieldType.PATH, ""));

    private static final List<Entry> WEBDAV_FILE_WRITER = List.of(
            entry("url", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("ssl", Scope.GLOBAL, FieldType.BOOLEAN, "yes"),
            entry("user", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("password", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("dest_uri", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("allow_delete", Scope.GLOBAL, FieldType.BOOLEAN, "yes"));

    // ---------------------------------------------------------------- FTP file
    private static final List<Entry> FTP_FILE_READER = List.of(
            entry("host", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("port", Scope.GLOBAL, FieldType.INTEGER, "21"),
            entry("user", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("password", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("ssl", Scope.GLOBAL, FieldType.BOOLEAN, "no"),
            entry("reusable_ssl", Scope.GLOBAL, FieldType.BOOLEAN, "yes"),
            entry("prot", Scope.GLOBAL, FieldType.TEXT, "P"),
            entry("temp_out_path", Scope.GLOBAL, FieldType.PATH, ""));

    private static final List<Entry> FTP_FILE_WRITER = List.of(
            entry("host", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("port", Scope.GLOBAL, FieldType.INTEGER, "21"),
            entry("user", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("password", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("ssl", Scope.GLOBAL, FieldType.BOOLEAN, "no"),
            entry("reusable_ssl", Scope.GLOBAL, FieldType.BOOLEAN, "yes"),
            entry("prot", Scope.GLOBAL, FieldType.TEXT, "P"),
            entry("directory", Scope.GLOBAL, FieldType.PATH, ""));

    // ---------------------------------------------------------------- SFTP file
    private static final List<Entry> SFTP_FILE_READER = List.of(
            entry("host", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("port", Scope.GLOBAL, FieldType.INTEGER, "22"),
            entry("user", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("password", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("known_hosts", Scope.GLOBAL, FieldType.PATH, ""),
            entry("temp_out_path", Scope.GLOBAL, FieldType.PATH, ""));

    private static final List<Entry> SFTP_FILE_WRITER = List.of(
            entry("host", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("port", Scope.GLOBAL, FieldType.INTEGER, "22"),
            entry("user", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("password", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("known_hosts", Scope.GLOBAL, FieldType.PATH, ""),
            entry("directory", Scope.GLOBAL, FieldType.PATH, ""));

    // ---------------------------------------------------------------- OpenStack Swift file
    private static final List<Entry> OS_SWIFT_FILE_COMMON = List.of(
            entry("version", Scope.GLOBAL, FieldType.INTEGER, "3"),
            entry("url_endpoint", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("user", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("user_id", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("password", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("project", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("domain", Scope.GLOBAL, FieldType.TEXT, "Default"),
            entry("ssl", Scope.GLOBAL, FieldType.BOOLEAN, "yes"),
            entry("container", Scope.GLOBAL, FieldType.TEXT, ""));

    private static final List<Entry> OS_SWIFT_FILE_READER = concat(OS_SWIFT_FILE_COMMON, List.of(
            entry("temp_out_path", Scope.GLOBAL, FieldType.PATH, "")));

    private static final List<Entry> OS_SWIFT_FILE_WRITER = OS_SWIFT_FILE_COMMON;

    // ---------------------------------------------------------------- Azure Blob file
    private static final List<Entry> AZURE_FILE_READER = List.of(
            entry("connection_string", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("account", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("temp_out_path", Scope.GLOBAL, FieldType.PATH, ""));

    private static final List<Entry> AZURE_FILE_WRITER = List.of(
            entry("connection_string", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("account", Scope.GLOBAL, FieldType.TEXT, ""),
            entry("container", Scope.GLOBAL, FieldType.TEXT, ""));

    // ---------------------------------------------------------------- Excel (iprw-office)
    private static final List<Entry> EXCEL_WRITER = List.of(
            entry("buffer_size", Scope.GLOBAL, FieldType.INTEGER, "1000"));

    // ---------------------------------------------------------------- Backblaze B2 file
    private static final List<Entry> B2_FILE_READER = List.of(
            entry("api_key_id", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("api_key", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("temp_out_path", Scope.GLOBAL, FieldType.PATH, ""));

    private static final List<Entry> B2_FILE_WRITER = List.of(
            entry("api_key_id", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("api_key", Scope.GLOBAL, FieldType.SECRET, ""),
            entry("bucket", Scope.GLOBAL, FieldType.TEXT, ""));

    // ---------------------------------------------------------------- helpers
    /** Concatenates two entry lists, keeping the order. */
    private static List<Entry> concat(List<Entry> first, List<Entry> second) {
        final List<Entry> all = new ArrayList<>(first);
        all.addAll(second);
        return List.copyOf(all);
    }

}
