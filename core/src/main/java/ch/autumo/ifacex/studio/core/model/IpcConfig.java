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
import java.util.List;
import java.util.Map;

/**
 * Root of the in-memory configuration model: one IPC job.
 *
 * <p>Model shape (see docs/09-engine-config-model.md): one reader, N source entities
 * (declared by {@link #processSourceEntities()}), N writers, and per writer one output
 * mapping per source entity.</p>
 */
public final class IpcConfig {

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

    public Meta meta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    public String sourceInterface() {
        return sourceInterface;
    }

    public void setSourceInterface(String sourceInterface) {
        this.sourceInterface = sourceInterface;
    }

    public List<String> destinationInterfaces() {
        return destinationInterfaces;
    }

    public List<String> processSourceEntities() {
        return processSourceEntities;
    }

    public Map<String, RwConfig> readers() {
        return readers;
    }

    public Map<String, RwConfig> writers() {
        return writers;
    }

    public Map<String, String> general() {
        return general;
    }

    public RwConfig reader() {
        return readers.values().stream().findFirst().orElse(null);
    }

    public RwConfig reader(String name) {
        return readers.get(name);
    }

    public RwConfig writer(String name) {
        return writers.get(name);
    }

    /** All reader/writer sections in insertion order. */
    public List<RwConfig> sections() {
        final List<RwConfig> all = new ArrayList<>();
        all.addAll(readers.values());
        all.addAll(writers.values());
        return all;
    }

    public RwConfig ensureReader(String name) {
        return readers.computeIfAbsent(name, n -> new RwConfig(n, true));
    }

    public RwConfig ensureWriter(String name) {
        return writers.computeIfAbsent(name, n -> new RwConfig(n, false));
    }
}
