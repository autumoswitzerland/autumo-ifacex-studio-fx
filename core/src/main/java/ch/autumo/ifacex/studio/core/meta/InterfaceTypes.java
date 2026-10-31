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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The engine's reader/writer interface types.
 *
 * <p>An interface name is {@code <prefix>_in} (reader) or {@code <prefix>_out} (writer); the
 * special writers {@code code} and {@code exec} carry no suffix. The <em>type</em> is the
 * prefix ({@code csv}, {@code json}, {@code db}, {@code mongo}, {@code rest}, {@code mail},
 * …). See {@code cfg/interfaces.cfg} of the engine and doc 11 (legacy GUI analysis).</p>
 *
 * <p>The legacy GUI colours the interface roles: BASE blue, readers green, writers red. The
 * same colours are exposed here as hex strings so the UI can render a type <em>pill</em>.</p>
 */
public final class InterfaceTypes {

    /** Role of an interface. */
    public enum Kind {
        /** A source interface ({@code <prefix>_in}). */
        READER,
        /** A destination interface ({@code <prefix>_out}, {@code code}, {@code exec}). */
        WRITER,
        /** The global job/base pseudo interface. */
        BASE,
        /** Not an interface. */
        UNKNOWN
    }

    /**
     * Resolved type information of one interface name.
     *
     * @param rwName      section name, e.g. {@code csv_in}, {@code code} or {@code exec}
     * @param kind        role of the interface
     * @param prefix      engine parameter prefix without the {@code _in}/{@code _out} suffix,
     *                    e.g. {@code csv}
     * @param display     human readable type name, e.g. {@code CSV}
     * @param colorHex    legacy GUI colour used for the type pill
     * @param engineClass fully qualified engine reader/writer class, or {@code null} when unknown
     */
    public record InterfaceType(
            String rwName,
            Kind kind,
            String prefix,
            String display,
            String colorHex,
            String engineClass) {

        /**
         * The engine parameter prefix of this section, e.g. {@code csv_in_}.
         *
         * @return section name plus a trailing underscore
         */
        public String keyPrefix() {
            return rwName + "_";
        }

        /**
         * Whether this type is a source interface.
         *
         * @return {@code true} for a reader
         */
        public boolean isReader() {
            return kind == Kind.READER;
        }

        /**
         * Whether this type is a destination interface.
         *
         * @return {@code true} for a writer
         */
        public boolean isWriter() {
            return kind == Kind.WRITER;
        }
    }

    /** Legacy GUI colour for base/general nodes. */
    public static final String COLOR_BASE = "#48486b";
    /** Legacy GUI colour for readers. */
    public static final String COLOR_READER = "#486b48";
    /** Legacy GUI colour for writers. */
    public static final String COLOR_WRITER = "#6b4848";

    /** prefix -&gt; display name (reader and writer share the display name). */
    private static final Map<String, String> DISPLAY = new LinkedHashMap<>();
    /** prefix -&gt; engine reader class. */
    private static final Map<String, String> READER_CLASS = new LinkedHashMap<>();
    /** prefix -&gt; engine writer class. */
    private static final Map<String, String> WRITER_CLASS = new LinkedHashMap<>();

    static {
        reader("null", "Null", "ch.autumo.ifacex.reader.NullReader");
        reader("csv", "CSV", "ch.autumo.ifacex.reader.file.CSVReader");
        reader("json", "JSON", "ch.autumo.ifacex.reader.file.JSONReader");
        reader("file", "File", "ch.autumo.ifacex.reader.file.FilePathReader");
        reader("doc", "Document", "ch.autumo.ifacex.reader.file.DocumentReader");
        reader("db", "DB", "ch.autumo.ifacex.reader.database.JDBCDatabaseReader");
        reader("mongo", "Mongo DB", "ch.autumo.ifacex.reader.database.MongoDatabaseReader");
        reader("rest", "REST", "ch.autumo.ifacex.reader.rest.RESTReader");

        writer("code", "Code", "ch.autumo.ifacex.writer.CodeRunner");
        writer("exec", "Exec", "ch.autumo.ifacex.writer.ProgramExec");
        writer("first", "First batch", "ch.autumo.ifacex.writer.FirstBatchWriter");
        writer("console", "Console", "ch.autumo.ifacex.writer.ConsoleWriter");
        writer("csv", "CSV", "ch.autumo.ifacex.writer.file.CSVWriter");
        writer("json", "JSON", "ch.autumo.ifacex.writer.file.JSONWriter");
        writer("file", "File", "ch.autumo.ifacex.writer.file.FileWriter");
        writer("mail", "Mail", "ch.autumo.ifacex.writer.mail.MailWriter");
        writer("db", "DB", "ch.autumo.ifacex.writer.database.JDBCDatabaseWriter");
        writer("mongo", "Mongo DB", "ch.autumo.ifacex.writer.database.MongoDatabaseWriter");
        writer("rest", "REST", "ch.autumo.ifacex.writer.rest.RESTWriter");

        // --- Additional readers/writers (module autumo-ifacex-iprw) --------------------------
        // They are registered after the standard ones on purpose: the wizard lists the standard
        // interfaces first and the additional ones below them, in the order of the iprw modules
        // (101 AWS, 111 Google Drive, 112 Google Storage, 121 Dropbox, 131 WebDAV, 141 FTP,
        // 142 SFTP, 151 OpenStack Swift, 161 Azure Blob, 171 Backblaze B2).
        reader("aws_file", "AWS File", "ch.autumo.ifacex.ip.amazon.AWSS3FileReader");
        reader("gdrive_file", "Google Drive File", "ch.autumo.ifacex.ip.google.GoogleDriveReader");
        reader("gstorage_file", "Google Storage File", "ch.autumo.ifacex.ip.google.GoogleStorageReader");
        reader("dbox_file", "Dropbox File", "ch.autumo.ifacex.ip.dropbox.DropboxReader");
        reader("webdav_file", "WebDAV File", "ch.autumo.ifacex.ip.webdav.WebDAVReader");
        reader("ftp_file", "FTP File", "ch.autumo.ifacex.ip.ftp.FTPReader");
        reader("sftp_file", "SFTP File", "ch.autumo.ifacex.ip.sftp.SFTPReader");
        reader("os_swift_file", "OpenStack Swift File", "ch.autumo.ifacex.ip.openstack.OpenStackSwiftReader");
        reader("azure_file", "Azure Blob File", "ch.autumo.ifacex.ip.microsoft.AzureBlobStorageReader");
        reader("b2_file", "Backblaze B2 File", "ch.autumo.ifacex.ip.backblaze.B2StorageReader");

        writer("aws_file", "AWS File", "ch.autumo.ifacex.ip.amazon.AWSS3FileWriter");
        writer("gdrive_file", "Google Drive File", "ch.autumo.ifacex.ip.google.GoogleDriveWriter");
        writer("gstorage_file", "Google Storage File", "ch.autumo.ifacex.ip.google.GoogleStorageWriter");
        writer("dbox_file", "Dropbox File", "ch.autumo.ifacex.ip.dropbox.DropboxWriter");
        writer("webdav_file", "WebDAV File", "ch.autumo.ifacex.ip.webdav.WebDAVWriter");
        writer("ftp_file", "FTP File", "ch.autumo.ifacex.ip.ftp.FTPWriter");
        writer("sftp_file", "SFTP File", "ch.autumo.ifacex.ip.sftp.SFTPWriter");
        writer("os_swift_file", "OpenStack Swift File", "ch.autumo.ifacex.ip.openstack.OpenStackSwiftWriter");
        writer("azure_file", "Azure Blob File", "ch.autumo.ifacex.ip.microsoft.AzureBlobStorageWriter");
        writer("b2_file", "Backblaze B2 File", "ch.autumo.ifacex.ip.backblaze.B2StorageWriter");

        // autumo-ifacex-iprw-office (201 Excel)
        writer("excel", "Excel", "ch.autumo.ifacex.ip.office.excel.ExcelWriter");
    }

    private InterfaceTypes() {
    }

    private static void reader(String prefix, String display, String clazz) {
        DISPLAY.put(prefix, display);
        READER_CLASS.put(prefix, clazz);
    }

    private static void writer(String prefix, String display, String clazz) {
        DISPLAY.putIfAbsent(prefix, display);
        WRITER_CLASS.put(prefix, clazz);
    }

    /**
     * Whether a writer writes the records as they come, without an output mapping.
     *
     * <p>{@code ConsoleWriter} and {@code FirstBatchWriter} ignore a mapping completely; the only
     * entity related parameter they read is {@code <writer>_<entity>_fields}, the column headers
     * of their output.</p>
     *
     * @param rwName writer section name, e.g. {@code console_out}
     * @return {@code true} when the engine ignores an output mapping for this writer
     */
    public static boolean writesUnchanged(String rwName) {
        return "console_out".equals(rwName) || "first_out".equals(rwName);
    }

    /**
     * Resolves the type of an interface name such as {@code csv_in}, {@code rest_out},
     * {@code code} or {@code exec}.
     *
     * @param rwName interface name, may be empty or {@code null}
     * @return resolved type, never {@code null} ({@code Kind.UNKNOWN} for an unknown name)
     */
    public static InterfaceType of(String rwName) {
        if (rwName == null || rwName.isEmpty()) {
            return new InterfaceType("", Kind.UNKNOWN, "", "Other", COLOR_BASE, null);
        }
        final Kind kind;
        final String prefix;
        if (rwName.equals("code") || rwName.equals("exec")) {
            kind = Kind.WRITER;
            prefix = rwName;
        } else if (rwName.endsWith("_in")) {
            kind = Kind.READER;
            prefix = rwName.substring(0, rwName.length() - 3);
        } else if (rwName.endsWith("_out")) {
            kind = Kind.WRITER;
            prefix = rwName.substring(0, rwName.length() - 4);
        } else {
            return new InterfaceType(rwName, Kind.UNKNOWN, rwName, display(prefixOf(rwName)),
                    COLOR_BASE, null);
        }
        return new InterfaceType(rwName, kind, prefix, display(prefix), color(kind),
                kind == Kind.READER ? READER_CLASS.get(prefix) : WRITER_CLASS.get(prefix));
    }

    /**
     * Whether the name is a reader interface ({@code _in}).
     *
     * @param rwName interface name
     * @return {@code true} for a reader
     */
    public static boolean isReader(String rwName) {
        return of(rwName).isReader();
    }

    /**
     * Whether the name is a writer interface ({@code _out}, {@code code}, {@code exec}).
     *
     * @param rwName interface name
     * @return {@code true} for a writer
     */
    public static boolean isWriter(String rwName) {
        return of(rwName).isWriter();
    }

    /**
     * Legacy colour of an interface role.
     *
     * @param kind role
     * @return colour as {@code #rrggbb}
     */
    public static String color(Kind kind) {
        return switch (kind) {
            case READER -> COLOR_READER;
            case WRITER -> COLOR_WRITER;
            default -> COLOR_BASE;
        };
    }

    /**
     * Display name of a prefix; an unknown prefix is simply capitalised.
     *
     * @param prefix interface prefix, e.g. {@code csv}
     * @return human readable type name
     */
    public static String display(String prefix) {
        final String d = DISPLAY.get(prefix);
        if (d != null) {
            return d;
        }
        if (prefix == null || prefix.isEmpty()) {
            return "Other";
        }
        return Character.toUpperCase(prefix.charAt(0)) + prefix.substring(1);
    }

    /**
     * All known reader prefixes, in the legacy GUI order.
     *
     * @return reader prefixes, e.g. {@code csv}
     */
    public static List<String> readerPrefixes() {
        return new ArrayList<>(READER_CLASS.keySet());
    }

    /**
     * All known writer prefixes, in the legacy GUI order.
     *
     * @return writer prefixes, e.g. {@code csv}
     */
    public static List<String> writerPrefixes() {
        return new ArrayList<>(WRITER_CLASS.keySet());
    }

    /**
     * All known reader interface names.
     *
     * @return reader names, e.g. {@code csv_in}
     */
    public static List<String> readerNames() {
        final List<String> names = new ArrayList<>();
        for (final String p : READER_CLASS.keySet()) {
            names.add(p + "_in");
        }
        return names;
    }

    /**
     * All known writer interface names.
     *
     * @return writer names, e.g. {@code csv_out}, {@code code}
     */
    public static List<String> writerNames() {
        final List<String> names = new ArrayList<>();
        for (final String p : WRITER_CLASS.keySet()) {
            names.add(p.equals("code") || p.equals("exec") ? p : p + "_out");
        }
        return names;
    }

    private static String prefixOf(String rwName) {
        final int i = rwName.indexOf('_');
        return i < 0 ? rwName : rwName.substring(0, i);
    }
}
