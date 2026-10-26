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
package ch.autumo.ifacex.studio.core.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ch.autumo.ifacex.studio.core.meta.ParameterCatalog;
import ch.autumo.ifacex.studio.core.meta.ParameterCatalog.Scope;
import ch.autumo.ifacex.studio.core.model.EntityConfig;
import ch.autumo.ifacex.studio.core.model.IpcConfig;
import ch.autumo.ifacex.studio.core.model.RwConfig;

/**
 * Verifies that a newly created reader/writer section is pre-populated with <em>all</em> possible
 * parameters of its type (see docs/11 §7), so the saved {@code .ifcx} carries the full parameter
 * set even when nothing is filled in, and that loading such a YAML restores every parameter.
 */
class ParameterCatalogTest {

    @Test
    void catalogueListsAllKnownTypes() {
        assertFalse(ParameterCatalog.entriesFor("rest_in").isEmpty());
        assertFalse(ParameterCatalog.entriesFor("db_out").isEmpty());
        assertFalse(ParameterCatalog.entriesFor("mail_out").isEmpty());
        assertFalse(ParameterCatalog.entriesFor("code").isEmpty());
        assertFalse(ParameterCatalog.entriesFor("exec").isEmpty());
        assertTrue(ParameterCatalog.entriesFor("unknown_type").isEmpty());
    }

    @Test
    void restInReaderHasExpectedParameters() {
        final List<ParameterCatalog.Entry> entries = ParameterCatalog.entriesFor("rest_in");
        final Map<String, ParameterCatalog.Entry> byParam = entries.stream()
                .collect(java.util.stream.Collectors.toMap(
                        ParameterCatalog.Entry::param, e -> e));
        assertNotNull(byParam.get("host"));
        assertNotNull(byParam.get("password"));
        assertNotNull(byParam.get("entities_url"));
        assertEquals(Scope.WILDCARD, byParam.get("entities_url").scope());
        assertEquals(Scope.GLOBAL, byParam.get("host").scope());
        assertEquals(Scope.ENTITY, byParam.get("fields").scope());
    }

    @Test
    void dbOutWriterHasExpectedParameters() {
        final List<ParameterCatalog.Entry> entries = ParameterCatalog.entriesFor("db_out");
        final Map<String, ParameterCatalog.Entry> byParam = entries.stream()
                .collect(java.util.stream.Collectors.toMap(
                        ParameterCatalog.Entry::param, e -> e));
        assertNotNull(byParam.get("url"));
        assertNotNull(byParam.get("password"));
        assertNotNull(byParam.get("dest_unique_id_field"));
        assertEquals(Scope.ENTITY, byParam.get("dest_unique_id_field").scope());
    }

    @Test
    void newReaderSectionIsPrePopulatedWithAllParameters() {
        final IpcConfig cfg = new IpcConfig();
        final RwConfig reader = cfg.initializeReader("rest_in");

        final List<ParameterCatalog.Entry> expected = ParameterCatalog.entriesFor("rest_in");
        for (final ParameterCatalog.Entry e : expected) {
            final Map<String, Object> target;
            switch (e.scope()) {
                case GLOBAL -> target = reader.global();
                case WILDCARD -> target = reader.wildcard();
                case ENTITY -> target = null; // no entity yet
                default -> target = null;
            }
            if (target != null) {
                assertEquals(e.defaultValue(), target.get(e.param()),
                        "missing pre-populated parameter " + e.param());
            }
        }
    }

    @Test
    void newWriterSectionIsPrePopulatedWithAllParameters() {
        final IpcConfig cfg = new IpcConfig();
        final RwConfig writer = cfg.initializeWriter("db_out");

        final List<ParameterCatalog.Entry> expected = ParameterCatalog.entriesFor("db_out");
        for (final ParameterCatalog.Entry e : expected) {
            final Map<String, Object> target;
            switch (e.scope()) {
                case GLOBAL -> target = writer.global();
                case WILDCARD -> target = writer.wildcard();
                case ENTITY -> target = null; // no entity yet
                default -> target = null;
            }
            if (target != null) {
                assertEquals(e.defaultValue(), target.get(e.param()),
                        "missing pre-populated parameter " + e.param());
            }
        }
    }

    @Test
    void newWriterEntityIsPrePopulatedWithEntityParameters() {
        final IpcConfig cfg = new IpcConfig();
        final RwConfig writer = cfg.initializeWriter("db_out");
        final EntityConfig ec = writer.initializeEntity("companies");

        final List<ParameterCatalog.Entry> entityParams = ParameterCatalog.entriesFor("db_out")
                .stream().filter(e -> e.scope() == Scope.ENTITY).toList();
        for (final ParameterCatalog.Entry e : entityParams) {
            assertEquals(e.defaultValue(), ec.settings().get(e.param()),
                    "missing entity-scoped parameter " + e.param());
        }
    }

    @Test
    void yamlOutputContainsAllParametersOfANewSection() {
        final IpcConfig cfg = new IpcConfig();
        cfg.initializeWriter("db_out");
        cfg.initializeWriter("rest_out");

        final String yaml = YamlConfigWriter.toText(cfg);

        assertTrue(yaml.contains("name: db_out"), "db_out section present");
        assertTrue(yaml.contains("name: rest_out"), "rest_out section present");
        // Every global parameter of each section must appear as a YAML key.
        for (final ParameterCatalog.Entry e : ParameterCatalog.entriesFor("db_out")) {
            if (e.scope() == ParameterCatalog.Scope.GLOBAL) {
                assertTrue(yaml.contains(e.param() + ":"),
                        "db_out global param " + e.param() + " in YAML:\n" + yaml);
            }
        }
        for (final ParameterCatalog.Entry e : ParameterCatalog.entriesFor("rest_out")) {
            if (e.scope() == ParameterCatalog.Scope.GLOBAL) {
                assertTrue(yaml.contains(e.param() + ":"),
                        "rest_out global param " + e.param() + " in YAML:\n" + yaml);
            }
        }
    }

    @Test
    void loadingYamlRestoresAllParametersOfEachSection() {
        final IpcConfig original = new IpcConfig();
        original.initializeWriter("db_out");
        original.initializeWriter("rest_out");
        final String yaml = YamlConfigWriter.toText(original);

        final IpcConfig reparsed = YamlConfigReader.read(yaml);

        final RwConfig dbOut = reparsed.writer("db_out");
        assertNotNull(dbOut, "db_out writer section restored");
        for (final ParameterCatalog.Entry e : ParameterCatalog.entriesFor("db_out")) {
            if (e.scope() == ParameterCatalog.Scope.GLOBAL) {
                assertEquals(e.defaultValue(), dbOut.global().get(e.param()),
                        "db_out global param " + e.param() + " restored from YAML");
            }
        }
        final RwConfig restOut = reparsed.writer("rest_out");
        assertNotNull(restOut, "rest_out writer section restored");
        for (final ParameterCatalog.Entry e : ParameterCatalog.entriesFor("rest_out")) {
            if (e.scope() == ParameterCatalog.Scope.GLOBAL) {
                assertEquals(e.defaultValue(), restOut.global().get(e.param()),
                        "rest_out global param " + e.param() + " restored from YAML");
            }
        }
    }
}
