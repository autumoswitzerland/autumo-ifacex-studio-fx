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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Catalog of known engine parameters and their form metadata.
 *
 * <p>For the PoC the descriptors live in code (see docs/10-ui-form-design.md). External,
 * customer-extendable descriptors are a later step. Unknown parameters fall back to a
 * best-effort inference so that <em>every</em> field is still shown (no field is dropped).</p>
 */
public final class FieldCatalog {

    private static final Map<String, FieldDescriptor> KNOWN = new LinkedHashMap<>();

    private static final List<String> YES_NO = List.of("yes", "no");

    /** Hint for the parameters that apply to every entity but can be overridden per entity. */
    private static final String PER_ENTITY =
            "Applies to all entities; it can be overridden per entity.";

    static {
        // --- common connection parameters -------------------------------------------------
        text("host", "Host",
                "Host name or IP address of the remote system, without protocol and path.");
        integer("port", "Port",
                "Port of the remote system; empty means the default port of the protocol.");
        select("protocol", "Protocol", "https", "http", "mongodb", "tcp");
        text("user", "User",
                "User name or client id used for the authentication.");
        secret("password", "Password",
                "Password or client secret; it can be stored encoded.");
        secret("api_key", "API key",
                "API key for the authentication; it can be stored encoded.");
        text("api_key_name", "API key name",
                "Name of the header or the parameter that carries the API key.");
        text("url", "URL",
                "Connection URL of the data source, e.g. the JDBC URL.");
        text("db_name", "Database name",
                "Name of the database (MongoDB: the database to use).");
        text("params", "Parameters",
                "Additional connection parameters.");

        // --- REST reader/writer -----------------------------------------------------------
        text("data_path", "Data path",
                "Path to the data array within the JSON response, '.' separates the elements.\n"
                + "Use an empty value (or '.') when the records are at the root, 'FULLARRAY' for an\n"
                + "array that starts at the root.\n"
                + PER_ENTITY);
        text("sub_path", "Sub path",
                "Only for nested records: the path inside a record element that holds the values of\n"
                + "the source fields.\n"
                + "Leave empty in most cases.\n"
                + PER_ENTITY);
        text("entities_url", "Entities URL",
                "URL part after the host for this entity; a trailing '/' must be part of the value.\n"
                + PER_ENTITY);
        bool("field_path_access", "Field path access",
                "Read the source fields as JSON paths instead of plain keys.\n"
                + "Path elements are separated by '.', array access is written as 'path[3]' or\n"
                + "'path[*]' - the '[*]' part must be the second to last element.\n"
                + PER_ENTITY);
        bool("allow_absent_fields", "Allow absent fields",
                "Continue without an error when a source field is missing; the placeholder below is\n"
                + "used instead.\n"
                + PER_ENTITY);
        text("absent_field_placeholder", "Absent field placeholder",
                "Value that replaces a missing source field (may be empty).\n"
                + PER_ENTITY);
        textArea("additional_headers", "Additional headers",
                "Headers for the request, key and value separated by ':', the pairs by '|'.\n"
                + "Encoded values are allowed.\n"
                + "Example: Referer:https://products.autumo.ch\n"
                + PER_ENTITY);
        text("additional_params", "Additional params",
                "Query parameters for the request, key and value separated by '=', the pairs by '&'.\n"
                + "Example: property=firstName&property=lastname\n"
                + "The values are URL-encoded.\n"
                + PER_ENTITY);
        integer("fetchSize", "Fetch size",
                "Number of records requested per call.\n"
                + PER_ENTITY);
        text("pagination_path", "Pagination path",
                "JSON path to the object that holds the pagination details; use '.' when they are at\n"
                + "the root.\n"
                + PER_ENTITY);
        text("pagination_items_per_page", "Pagination items per page",
                "Attribute of the pagination object that holds the number of items per page; it must\n"
                + "match the fetch size.\n"
                + PER_ENTITY);
        text("pagination_items_total", "Pagination items total",
                "Attribute of the pagination object that holds the total amount of records; it is\n"
                + "used to calculate how many calls are needed.\n"
                + PER_ENTITY);
        text("pagination_last_page", "Pagination last page",
                "Attribute of the pagination object that holds the last page; it is used to verify\n"
                + "the calculated last page.\n"
                + "Without it the check is skipped.\n"
                + PER_ENTITY);
        text("pagination_after_id", "Pagination after id",
                "Name of the pointer to the first record of the next batch.\n"
                + "Use it when the page and offset calculations are impossible; then leave the other\n"
                + "pagination fields empty except the path.\n"
                + PER_ENTITY);
        text("param_pageName", "Param page name",
                "Request parameter that carries the page number.\n"
                + PER_ENTITY);
        text("param_offsetName", "Param offset name",
                "Request parameter that carries the offset, for APIs without page numbers.\n"
                + PER_ENTITY);
        text("param_fetchSize", "Param fetch size",
                "Request parameter that carries the fetch size.\n"
                + PER_ENTITY);

        // --- OAuth / auth -----------------------------------------------------------------
        text("auth_host", "Auth host",
                "Host of the authentication service, if it differs from the data host.");
        text("auth_url", "Auth URL",
                "URL part after the authentication host.\n"
                + "A revoke URL for the used token can be appended, separated by ';'.");
        select("auth_method", "Auth method", "none", "basic", "apikey", "digest", "oauth", "oauth-ba");
        textArea("auth_body", "Auth body",
                "Additional body parameters of the authentication request, key and value separated\n"
                + "by ':', the pairs by '|'.\n"
                + "Encoded values are allowed.");
        text("auth_grant_type", "Auth grant type",
                "OAuth grant type of the token request, e.g. 'client_credentials'.");
        bool("auth", "Auth",
                "Use an authenticated mail transport.");

        // --- REST reader: context-specific credentials -------------------------------------
        text("rest_in_user", "Client ID",
                "Client id of the OAuth token request - or the user name when using the basic or\n"
                + "digest authentication.");
        secret("rest_in_password", "Client Secret",
                "Client secret of the OAuth token request - or the password when using the basic or\n"
                + "digest authentication. It can be stored encoded.");
        bool("rest_in_alt_data_conn", "Alternative Connection",
                "Some servers reject the default HTTP client, e.g. with '502 Bad Gateway'.\n"
                + "The alternative connection uses a plain JDK connection for the data calls instead.\n"
                + "Note: authentication headers, additional headers and the REST timeouts do not apply\n"
                + "then; an API key has to be passed as an additional parameter.");
        textArea("rest_in_query", "Query",
                "JSON body of the data call.\n"
                + "As soon as it is set, the reader sends a POST with this body instead of a GET.\n"
                + "Spaces are removed, so write the JSON compactly.");

        // --- Document reader --------------------------------------------------------------
        text("doc_in_extractor_implementation", "Extractor implementation",
                "Fully qualified class name of the DocumentExtractor that reads the documents.");
        bool("doc_in_delete_files_after_processing", "Delete files after processing",
                "Delete the document after it was processed.");
        integer("doc_in_fetchSize", "Fetch size",
                "Number of documents requested per call.\n"
                + PER_ENTITY);
        integer("doc_in_contents_max_length", "Contents max length",
                "Maximum number of characters taken from a document.");

        // --- file reader/writer -----------------------------------------------------------
        path("read_from_folder", "Read from folder",
                "Folder the input files are read from; empty means the configured files folder.\n"
                + "The files must be named '<entity>.csv' / '<entity>.json'.");
        text("read_from_writer", "Read from writer",
                "Read the files that a writer of the same job produced instead of the input folder,\n"
                + "e.g. 'csv_out'.");
        bool("delete_files_after_processing", "Delete files after processing",
                "Delete the processed file, so that it is not read again in the next run.");
        integer("header_size", "Header size",
                "Number of header lines of the CSV file; 0 means no header.\n"
                + "If there is more than one header line, the field names are taken from the last one.");
        path("files_folder", "Files folder",
                "Folder for input and output files; empty means the 'files_folder' of the base\n"
                + "configuration.");
        path("file", "File",
                "File to read.");
        text("encoding", "Encoding",
                "Character encoding of the file, e.g. UTF-8.");

        // --- DB / Mongo -------------------------------------------------------------------
        integer("order_field", "Order field",
                "Field used to order the records of the entity; needed for stable batching.\n"
                + PER_ENTITY);

        // --- mapping / destination --------------------------------------------------------
        list("fields", "Fields",
                "Source fields of the entity, in the order of the data.\n"
                + "With a CSV header size > 0 the CSV header is used instead of this list.");
        list("dest_filter_fields", "Destination filter fields",
                "Destination fields whose values identify an existing record - they decide between\n"
                + "insert and update.\n"
                + PER_ENTITY);
        text("dest_unique_id_field", "Destination unique id field",
                "Destination field that holds the unique id of the record; needed for the update\n"
                + "(upsert) case.\n"
                + PER_ENTITY);
        text("dest_id_path", "Destination id path",
                "JSON path in the REST response that returns the id of the written record.\n"
                + PER_ENTITY);
        text("exclusion_filter_class", "Exclusion filter class",
                "Fully qualified class name of an ExclusionFilter that skips records.\n"
                + "Leave empty for none.");
        text("data_mapper_class", "Data mapper class",
                "Fully qualified class name of a DataMapper that transforms the record before it is\n"
                + "written.\n"
                + "Leave empty for none.");
        bool("ext_insert_mapping", "Extended insert mapping",
                "Allow values such as NOW() or NULL in the insert mappings (database writers only).");
        bool("single_id_filter", "Single id filter",
                "Select the destination record by a single id field (REST writer only).");

        // --- mail writer ------------------------------------------------------------------
        text("subject", "Subject",
                "Subject of the mail.");
        textArea("body", "Body",
                "Body of the mail; line breaks are written as #BR# because the value is a single\n"
                + "line.");
        text("to", "To",
                "Recipients, separated by commas.");
        text("mail_out_from", "From",
                "Sender address of the mail.");
        integer("mail_out_max_size", "Max size",
                "Maximum size of the attachments in megabytes.");
        bool("tls_enable", "TLS enable",
                "Use TLS for the SMTP connection.");

        // --- code writer ------------------------------------------------------------------
        text("code_implementation", "Code implementation",
                "Fully qualified class name of the Code implementation that processes the records.");
        integer("code_proc_pos", "Processing position",
                "Position of the writer in the processing chain; 0 means the first position.");

        // --- exec writer ------------------------------------------------------------------
        text("exec_cmd", "Command",
                "Program or script to execute.");
        text("exec_args", "Arguments",
                "Arguments passed to the program.");
        path("exec_working_dir", "Working directory",
                "Directory the program runs in.");
        integer("exec_timeout", "Timeout",
                "Seconds the program may run before it is stopped.");
        integer("exec_proc_pos", "Processing position",
                "Position of the writer in the processing chain; 0 means the first position.");
        integer("exec_exit_code_success", "Success exit code",
                "Exit code that counts as success; 0 by default.");

        bool("mongo_out_create_collection", "Create collection",
                "Create the collection when it does not exist.\n"
                + PER_ENTITY);
        path("file_out_path", "Output path",
                "Folder the files are written to.");
        bool("file_out_delete_files_after_processing", "Delete files after processing",
                "Delete the file after it was written.");

        // --- files / folders --------------------------------------------------------------

        // --- additional readers/writers (autumo-ifacex-iprw) --------------------------------
        // Labels and help texts are derived from the interface templates
        // (autumo-ifacex-iprw/interfaces/*.template); parameters whose template comment says
        // "can be encrypted" are rendered as secret fields.
        text("url_endpoint", "URL end-point",
                "URL end-point of the service. Can be empty; provide it for faster access.");
        text("access_key", "Access key",
                "Access key of the account.");
        secret("access_key", "Access key",
                "Access key of the account (can be encrypted, use the encoder).");
        secret("secret_key", "Secret key",
                "Secret key of the account (can be encrypted, use the encoder).");
        text("region", "Region",
                "Region of the service; can be empty if the service does not need it.");
        text("bucket_name", "Bucket name",
                "Name of the bucket (only lower-case characters are allowed).");
        text("key_prefix", "Key prefix",
                "Key prefix for the whole key; a '/' is appended and the file name is added.");
        text("bucket", "Bucket",
                "Bucket to write to. It is created if it does not exist.");
        text("storage_class", "Storage class",
                "Storage class used when a new bucket has to be created, e.g. STANDARD.");
        path("credentials_file", "Credentials file",
                "Google credentials file (JSON) of the project.");
        text("project_id", "Project ID", "ID of the cloud project.");
        text("application_name", "Application name",
                "Application name that is connected through OAuth 2.0.");
        text("client_id", "Client ID", "OAuth 2.0 client ID.");
        secret("client_id", "Client ID",
                "OAuth 2.0 client ID (can be encrypted, use the encoder).");
        secret("client_secret", "Client secret",
                "OAuth 2.0 client secret (can be encrypted, use the encoder).");
        text("auth_uri", "Authentication URI", "URI used to authenticate.");
        text("token_uri", "Token URI", "URI used to retrieve the access token.");
        text("auth_provider_x509_cert_url", "Auth provider x509 cert URL",
                "URL of the X.509 certificates of the authentication provider.");
        text("redirect_uris", "Redirect URIs",
                "Redirect URIs, a comma-separated list or empty.");
        text("corpora", "Corpora",
                "Groupings of files to which the query applies; may be empty.");
        text("drive_id", "Drive ID",
                "Necessary if the corpora is set to 'drive', otherwise unused.");
        text("folder", "Folder",
                "Folder to read from or to write to. Can be left empty.");
        text("folder_id_parent", "Parent folder ID",
                "ID of the existing parent folder in which the folder above is created.");
        text("app_name", "App name", "Application name; can be left empty.");
        text("app_version", "App version", "Application version; can be left empty.");
        secret("oauth_access_token", "OAuth access token",
                "Dropbox OAuth 2.0 access token (can be encrypted, use the encoder).");
        path("path", "Path",
                "Remote path; '/' is the root directory and is not supported.");
        bool("ssl", "SSL",
                "Use SSL/TLS for the connection?");
        bool("reusable_ssl", "Reusable SSL",
                "Reusable SSL/TLS connections? Necessary for servers like FileZilla.");
        text("prot", "PROT command",
                "FTP PROT command: C - clear, S - safe, E - confidential, P - private.");
        path("known_hosts", "Known hosts file",
                "Path of the 'known_hosts' file used to verify the server.");
        path("directory", "Directory", "Remote target directory.");
        path("temp_out_path", "Temporary output path",
                "Temporary directory for the files read from the remote service.");
        text("dest_uri", "Destination URI", "Remote directory to upload the files to.");
        bool("allow_delete", "Allow delete",
                "Does the server allow to delete files?");
        integer("version", "Version",
                "Version of the openstack API; use 2 or 3.");
        integer("os_swift_file_in_version", "OpenStack KeyStone version",
                "Version of the OpenStack KeyStone API; use 2 or 3.");
        integer("os_swift_file_out_version", "OpenStack KeyStone version",
                "Version of the OpenStack KeyStone API; use 2 or 3.");
        text("user_id", "User ID",
                "User ID; works only with KeyStone version 3. If blank, the user name is used "
                        + "(can be encrypted, use the encoder).");
        secret("user_id", "User ID",
                "User ID; works only with KeyStone version 3 (can be encrypted, use the encoder).");
        text("project", "Project",
                "Project name; with KeyStone version 2 this is the tenant.");
        text("domain", "Domain",
                "Domain name; 'Default' is used when empty. Only used with KeyStone version 3.");
        text("container", "Container",
                "Container to write to. It is created if it does not exist.");
        secret("connection_string", "Connection string",
                "Azure connection string from 'Access Keys' in the Azure portal. If left blank, "
                        + "the more secure default authentication chain is used.");
        text("account", "Account name", "Name of the Azure storage account.");
        secret("api_key_id", "API key ID",
                "Backblaze B2 API key ID (can be encrypted, use the encoder).");
        secret("api_key", "API key",
                "Backblaze B2 API key (can be encrypted, use the encoder).");
        integer("excel_out_buffer_size", "Buffer size",
                "Excel file streaming buffer size: number of rows kept in memory while writing.");

        // "can be encrypted, use encoder.sh" in the interface templates: the user name of these
        // services is a secret as well, not a plain text field.
        for (final String iface : List.of("webdav_file_in", "webdav_file_out", "ftp_file_in",
                "ftp_file_out", "sftp_file_in", "sftp_file_out", "os_swift_file_in",
                "os_swift_file_out")) {
            secret(iface + "_user", "User",
                    "User name used for the authentication (can be encrypted, use the encoder).");
        }
        text("job_id", "Job id",
                "Identifier of the job, used in log output and reports.");
    }

    private FieldCatalog() {
    }

    /**
     * Returns the descriptor for a parameter, inferring a best-effort one when unknown.
     *
     * @param param engine parameter name
     * @return the matching descriptor, never {@code null}
     */
    public static FieldDescriptor descriptor(String param) {
        final FieldDescriptor d = KNOWN.get(param);
        return d != null ? d : infer(param);
    }

    /**
     * The parameters with a known descriptor. Used by tooling (e.g. the UI tests) to
     * tell a real engine parameter label apart from a plain caption.
     *
     * @return unmodifiable set of parameter names, in declaration order
     */
    public static Set<String> knownParams() {
        return Collections.unmodifiableSet(KNOWN.keySet());
    }

    private static FieldDescriptor infer(String param) {
        final String p = param.toLowerCase();
        final String label = humanize(param);
        if (p.endsWith("_fields") || p.endsWith("_list") || p.endsWith("_columns")) {
            return new FieldDescriptor(param, label, FieldType.LIST);
        }
        if (p.contains("password") || p.contains("secret") || p.contains("token")
                || p.contains("api_key")) {
            return new FieldDescriptor(param, label, FieldType.SECRET);
        }
        if (p.endsWith("_folder") || p.endsWith("_dir") || p.endsWith("_path") && !p.contains("data_path")) {
            return new FieldDescriptor(param, label, FieldType.PATH);
        }
        if (p.endsWith("size") || p.endsWith("port") || p.endsWith("timeout")
                || p.endsWith("interval") || p.endsWith("count") || p.endsWith("limit")) {
            return new FieldDescriptor(param, label, FieldType.INTEGER);
        }
        if (p.startsWith("allow_") || p.startsWith("is_") || p.endsWith("_enable")
                || p.endsWith("_flag") || p.endsWith("_access")) {
            return new FieldDescriptor(param, label, FieldType.BOOLEAN);
        }
        return new FieldDescriptor(param, label, FieldType.TEXT);
    }

    /**
     * Acronyms that are written in capitals in a label.
     *
     * <p>{@code ssl} becomes {@code SSL}, {@code url_endpoint} becomes {@code URL end-point} - a
     * label derived from a key must not produce "Ssl" or "Api Key ID".</p>
     */
    private static final Set<String> ACRONYMS = Set.of(
            "ssl", "tls", "api", "url", "uri", "id", "http", "https", "json", "csv", "db", "ftp",
            "sftp", "ssh", "os", "aws", "oauth", "uuid", "xml", "yaml", "sql", "smtp", "dns", "ip",
            "s3", "b2", "prot", "acl", "jdbc", "tcp", "gm", "utc", "otp", "rest");

    private static String humanize(String param) {
        final StringBuilder sb = new StringBuilder();
        for (final String w : param.split("_")) {
            if (w.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            if (ACRONYMS.contains(w.toLowerCase(Locale.ROOT))) {
                sb.append(w.toUpperCase(Locale.ROOT));
            } else {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
            }
        }
        return sb.toString();
    }

    private static void text(String p, String label) {
        KNOWN.put(p, new FieldDescriptor(p, label, FieldType.TEXT));
    }

    private static void text(String p, String label, String help) {
        KNOWN.put(p, new FieldDescriptor(p, label, FieldType.TEXT, List.of(), help));
    }

    private static void textArea(String p, String label) {
        KNOWN.put(p, new FieldDescriptor(p, label, FieldType.TEXT_AREA));
    }
    private static void textArea(String p, String label, String help) {
        KNOWN.put(p, new FieldDescriptor(p, label, FieldType.TEXT_AREA, List.of(), help));
    }

    private static void path(String p, String label) {
        KNOWN.put(p, new FieldDescriptor(p, label, FieldType.PATH));
    }
    private static void path(String p, String label, String help) {
        KNOWN.put(p, new FieldDescriptor(p, label, FieldType.PATH, List.of(), help));
    }

    private static void secret(String p, String label) {
        KNOWN.put(p, new FieldDescriptor(p, label, FieldType.SECRET));
    }

    private static void secret(String p, String label, String help) {
        KNOWN.put(p, new FieldDescriptor(p, label, FieldType.SECRET, List.of(), help));
    }

    private static void integer(String p, String label) {
        KNOWN.put(p, new FieldDescriptor(p, label, FieldType.INTEGER));
    }
    private static void integer(String p, String label, String help) {
        KNOWN.put(p, new FieldDescriptor(p, label, FieldType.INTEGER, List.of(), help));
    }

    private static void bool(String p, String label) {
        KNOWN.put(p, new FieldDescriptor(p, label, FieldType.BOOLEAN, YES_NO));
    }

    private static void bool(String p, String label, String help) {
        KNOWN.put(p, new FieldDescriptor(p, label, FieldType.BOOLEAN, YES_NO, help));
    }

    private static void list(String p, String label) {
        KNOWN.put(p, new FieldDescriptor(p, label, FieldType.LIST));
    }
    private static void list(String p, String label, String help) {
        KNOWN.put(p, new FieldDescriptor(p, label, FieldType.LIST, List.of(), help));
    }

    private static void select(String p, String label, String... options) {
        KNOWN.put(p, new FieldDescriptor(p, label, FieldType.SELECT, List.of(options)));
    }
}
