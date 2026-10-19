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

    static {
        // --- common connection parameters -------------------------------------------------
        text("host", "Host");
        integer("port", "Port");
        select("protocol", "Protocol", "https", "http", "mongodb", "tcp");
        text("user", "User");
        secret("password", "Password");
        secret("api_key", "API key");
        text("api_key_name", "API key name");
        text("url", "URL");
        text("db_name", "Database name");
        text("params", "Parameters");

        // --- REST reader/writer -----------------------------------------------------------
        text("data_path", "Data path", "Path to the data array within the JSON response.");
        text("sub_path", "Sub path");
        text("entities_url", "Entities URL");
        bool("field_path_access", "Field path access");
        bool("allow_absent_fields", "Allow absent fields");
        text("absent_field_placeholder", "Absent field placeholder");
        textArea("additional_headers", "Additional headers");
        text("additional_params", "Additional params");
        integer("fetchSize", "Fetch size");
        text("pagination_path", "Pagination path");
        text("pagination_items_per_page", "Pagination items per page");
        text("pagination_items_total", "Pagination items total");
        text("pagination_last_page", "Pagination last page");
        text("pagination_after_id", "Pagination after id");
        text("param_pageName", "Param page name");
        text("param_offsetName", "Param offset name");
        text("param_fetchSize", "Param fetch size");

        // --- OAuth / auth -----------------------------------------------------------------
        text("auth_host", "Auth host");
        text("auth_url", "Auth URL");
        select("auth_method", "Auth method", "none", "basic", "apikey", "digest", "oauth", "oauth-ba");
        textArea("auth_body", "Auth body");
        text("auth_grant_type", "Auth grant type");
        bool("auth", "Auth");

        // --- file reader/writer -----------------------------------------------------------
        path("read_from_folder", "Read from folder");
        text("read_from_writer", "Read from writer");
        bool("delete_files_after_processing", "Delete files after processing");
        integer("header_size", "Header size");
        path("files_folder", "Files folder");
        path("file", "File");
        text("encoding", "Encoding");

        // --- DB / Mongo -------------------------------------------------------------------
        text("fetchSize", "Fetch size");

        // --- mapping / destination --------------------------------------------------------
        list("fields", "Fields");
        list("dest_filter_fields", "Destination filter fields");
        text("dest_unique_id_field", "Destination unique id field");
        text("exclusion_filter_class", "Exclusion filter class");
        text("data_mapper_class", "Data mapper class");
        bool("ext_insert_mapping", "Extended insert mapping");
        bool("single_id_filter", "Single id filter");

        // --- mail writer ------------------------------------------------------------------
        text("subject", "Subject");
        textArea("body", "Body");
        text("to", "To");
        bool("tls_enable", "TLS enable");

        // --- files / folders --------------------------------------------------------------
        text("job_id", "Job id");
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

    private static String humanize(String param) {
        final StringBuilder sb = new StringBuilder();
        for (final String w : param.split("_")) {
            if (w.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
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

    private static void path(String p, String label) {
        KNOWN.put(p, new FieldDescriptor(p, label, FieldType.PATH));
    }

    private static void secret(String p, String label) {
        KNOWN.put(p, new FieldDescriptor(p, label, FieldType.SECRET));
    }

    private static void integer(String p, String label) {
        KNOWN.put(p, new FieldDescriptor(p, label, FieldType.INTEGER));
    }

    private static void bool(String p, String label) {
        KNOWN.put(p, new FieldDescriptor(p, label, FieldType.BOOLEAN, YES_NO));
    }

    private static void list(String p, String label) {
        KNOWN.put(p, new FieldDescriptor(p, label, FieldType.LIST));
    }

    private static void select(String p, String label, String... options) {
        KNOWN.put(p, new FieldDescriptor(p, label, FieldType.SELECT, List.of(options)));
    }
}
