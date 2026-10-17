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
package ch.autumo.ifacex.studio.core.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Root of the in-memory configuration model: one IPC job.
 *
 * <p>Model shape (see docs/09-engine-config-model.md): one reader, N source entities
 * (declared by {@link #processSourceEntities()}), N writers, and per writer one output
 * mapping per source entity.</p>
 */
public final class IpcConfig {

    /** Creates an empty job with the default metadata. */
    public IpcConfig() {
    }

    private Meta meta = Meta.EMPTY;
    private String sourceInterface = "";
    private final List<String> destinationInterfaces = new ArrayList<>();
    private final List<String> processSourceEntities = new ArrayList<>();

    /** Reader sections by prefix, e.g. {@code rest_in}. Usually exactly one. */
    private final Map<String, RwConfig> readers = new LinkedHashMap<>();
    /** Writer sections by prefix, e.g. {@code csv_out}. */
    private final Map<String, RwConfig> writers = new LinkedHashMap<>();

    /** General (non reader/writer scoped) keys, e.g. {@code batch_mode}, {@code files_folder}. */
    private final Map<String, String> general = new LinkedHashMap<>();

    /**
     * The metadata (comment header) of this configuration.
     *
     * @return metadata, never {@code null}
     */
    public Meta meta() {
        return meta;
    }

    /**
     * Sets the metadata (comment header).
     *
     * @param meta metadata to set
     */
    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    /**
     * The single source interface prefix, e.g. {@code rest_in}.
     *
     * @return source interface prefix, empty when unset
     */
    public String sourceInterface() {
        return sourceInterface;
    }

    /**
     * Sets the source interface prefix.
     *
     * @param sourceInterface source interface prefix, e.g. {@code rest_in}
     */
    public void setSourceInterface(String sourceInterface) {
        this.sourceInterface = sourceInterface;
    }

    /**
     * The destination interface prefixes in declaration order, e.g. {@code [csv_out, mail_out]}.
     * A declared interface need not have a section of its own (e.g. {@code console_out}).
     *
     * @return mutable list of destination interface prefixes
     */
    public List<String> destinationInterfaces() {
        return destinationInterfaces;
    }

    /**
     * The globally declared source entities (legacy {@code process_source_entities}).
     *
     * @return mutable list of source entity names
     */
    public List<String> processSourceEntities() {
        return processSourceEntities;
    }

    /**
     * The reader sections by prefix, in file order.
     *
     * @return mutable map of reader prefix to section
     */
    public Map<String, RwConfig> readers() {
        return readers;
    }

    /**
     * The writer sections by prefix, in file order.
     *
     * @return mutable map of writer prefix to section
     */
    public Map<String, RwConfig> writers() {
        return writers;
    }

    /**
     * Keys that are not bound to a reader or writer section, e.g. {@code batch_mode} or
     * {@code files_folder}.
     *
     * @return mutable map of general key to value
     */
    public Map<String, String> general() {
        return general;
    }

    /**
     * The first (and usually only) reader section.
     *
     * @return reader section, or {@code null} when the configuration has none
     */
    public RwConfig reader() {
        return readers.values().stream().findFirst().orElse(null);
    }

    /**
     * Looks up a reader section by prefix.
     *
     * @param name reader prefix, e.g. {@code rest_in}
     * @return reader section, or {@code null} when absent
     */
    public RwConfig reader(String name) {
        return readers.get(name);
    }

    /**
     * Looks up a writer section by prefix.
     *
     * @param name writer prefix, e.g. {@code csv_out}
     * @return writer section, or {@code null} when absent
     */
    public RwConfig writer(String name) {
        return writers.get(name);
    }

    /**
     * All reader/writer sections, readers first, each group in insertion order.
     *
     * @return new list of sections
     */
    public List<RwConfig> sections() {
        final List<RwConfig> all = new ArrayList<>();
        all.addAll(readers.values());
        all.addAll(writers.values());
        return all;
    }

    /**
     * Returns the reader section with the given prefix, creating it when necessary. Every globally
     * declared source entity is reconciled into it, so its fields stay editable.
     *
     * @param name reader prefix
     * @return reader section, never {@code null}
     */
    public RwConfig ensureReader(String name) {
        final RwConfig r = readers.computeIfAbsent(name, n -> new RwConfig(n, true));
        // Reconcile: every declared source entity must exist in the reader, so its fields are editable.
        for (final String entity : processSourceEntities) {
            r.ensureEntity(entity);
        }
        return r;
    }

    /**
     * Returns the writer section with the given prefix, creating it when necessary.
     *
     * @param name writer prefix
     * @return writer section, never {@code null}
     */
    public RwConfig ensureWriter(String name) {
        return writers.computeIfAbsent(name, n -> new RwConfig(n, false));
    }

    // ---------------------------------------------------------------- source entities

    /**
     * Ordered global source-entity names (the legacy {@code process_source_entities}), merged with
     * any entity that only exists in the reader section. This is the canonical list shown in the
     * BASE tab and mirrored by every reader and writer — source entities are <em>global</em> in the
     * original Studio (one {@code EntityFieldsHolder} list shared by all tabs).
     *
     * @return ordered source entity names
     */
    public List<String> sourceEntityNames() {
        final Set<String> names = new LinkedHashSet<>(processSourceEntities);
        final RwConfig r = reader();
        if (r != null) {
            names.addAll(r.entities().keySet());
        }
        return new ArrayList<>(names);
    }

    /**
     * Adds a global source entity (legacy BASE "add source entity") and reconciles it into the
     * reader section, so the fields become editable.
     *
     * @param name entity name; {@code null} or blank is ignored
     * @return {@code false} when the entity already exists or the name is unusable
     */
    public boolean addSourceEntity(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        final String n = name.trim();
        if (processSourceEntities.contains(n)) {
            return false;
        }
        processSourceEntities.add(n);
        final RwConfig r = reader();
        if (r != null) {
            r.ensureEntity(n);
        }
        return true;
    }

    /**
     * Removes a global source entity together with all its fields and mappings.
     *
     * @param name entity name
     */
    public void removeSourceEntity(String name) {
        processSourceEntities.remove(name);
        for (final RwConfig rw : readers.values()) {
            rw.entities().remove(name);
        }
        for (final RwConfig rw : writers.values()) {
            rw.entities().remove(name);
        }
    }

    /**
     * Renames a global source entity in the entity list, the reader fields and every writer
     * mapping, preserving positions.
     *
     * @param oldName current entity name
     * @param newName new entity name; a blank or unchanged name is ignored
     */
    public void renameSourceEntity(String oldName, String newName) {
        if (oldName == null || newName == null || newName.isBlank()
                || oldName.equals(newName.trim())) {
            return;
        }
        final String n = newName.trim();
        final int i = processSourceEntities.indexOf(oldName);
        if (i >= 0) {
            processSourceEntities.set(i, n);
        }
        for (final RwConfig rw : readers.values()) {
            renameEntity(rw, oldName, n);
        }
        for (final RwConfig rw : writers.values()) {
            renameEntity(rw, oldName, n);
        }
    }

    /**
     * Moves a source entity in the global order and mirrors the move into the reader section.
     *
     * @param name  entity to move
     * @param delta {@code -1} up, {@code 1} down
     */
    public void moveSourceEntity(String name, int delta) {
        final int i = processSourceEntities.indexOf(name);
        final int t = i + delta;
        if (i < 0 || t < 0 || t >= processSourceEntities.size()) {
            return;
        }
        processSourceEntities.add(t, processSourceEntities.remove(i));
        final RwConfig r = reader();
        if (r != null && r.entities().containsKey(name)) {
            reorderEntity(r, name, t);
        }
    }

    /** Move an entity to a new position inside a section, keeping the insertion order. */
    private static void reorderEntity(RwConfig rw, String name, int target) {
        final List<Map.Entry<String, EntityConfig>> entries =
                new ArrayList<>(rw.entities().entrySet());
        int from = -1;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getKey().equals(name)) {
                from = i;
                break;
            }
        }
        if (from < 0 || target < 0 || target >= entries.size() || from == target) {
            return;
        }
        entries.add(target, entries.remove(from));
        rw.entities().clear();
        for (final Map.Entry<String, EntityConfig> e : entries) {
            rw.entities().put(e.getKey(), e.getValue());
        }
    }

    /**
     * The live source-field list of a source entity. The fields are stored on the reader section
     * (legacy {@code <reader>_<entity>_fields}); writers only reference them. Returns an empty,
     * read-only list when the reader or the entity is absent (e.g. a mapping to an unknown entity).
     *
     * @param entity source entity name
     * @return the entity's source fields, or an empty list
     */
    public List<String> readerEntityFields(String entity) {
        final RwConfig r = reader();
        if (r == null || entity == null) {
            return List.of();
        }
        final EntityConfig ec = r.entities().get(entity);
        return ec == null ? List.of() : ec.fields();
    }

    /** Rename an entity key inside a section, preserving its position and content. */
    private static void renameEntity(RwConfig rw, String oldName, String newName) {
        final EntityConfig ec = rw.entities().get(oldName);
        if (ec == null || rw.entities().containsKey(newName)) {
            return;
        }
        final EntityConfig fresh = new EntityConfig(newName);
        fresh.settings().putAll(ec.settings());
        fresh.fields().addAll(ec.fields());
        if (ec.hasMapping()) {
            final WriterMapping m = fresh.ensureMapping();
            m.setDestEntity(ec.mapping().destEntity());
            m.fields().addAll(ec.mapping().fields());
        }
        final Map<String, EntityConfig> copy = new LinkedHashMap<>();
        for (final Map.Entry<String, EntityConfig> e : rw.entities().entrySet()) {
            if (e.getKey().equals(oldName)) {
                copy.put(newName, fresh);
            } else {
                copy.put(e.getKey(), e.getValue());
            }
        }
        rw.entities().clear();
        rw.entities().putAll(copy);
    }
}
