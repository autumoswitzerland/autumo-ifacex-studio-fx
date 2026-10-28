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
package ch.autumo.ifacex.studio.ui.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * The file name building blocks of "Save As…": a configuration that was loaded keeps the name of
 * its file, so a converted {@code name.ifacex} is saved as {@code name.ifcx}.
 */
class MainControllerFileNameTest {

    @Test
    void theBaseNameKeepsEverythingBeforeTheLastDot() {
        assertEquals("HubSpot API - Products",
                MainController.baseName(Path.of("/data/converted_templates/HubSpot API - Products.ifacex")));
        assertEquals("002-hubspot-v3-companies",
                MainController.baseName(Path.of("002-hubspot-v3-companies.ifcx")));
        assertEquals("stock.metric", MainController.baseName(Path.of("/x/stock.metric.ifacex")),
                "only the last extension is dropped");
        // A converted template keeps its name: the suggestion is the base name + ".ifcx".
        assertEquals("002-hubspot-v3-companies.ifcx",
                MainController.safeFileName(MainController.baseName(
                        Path.of("/templates/002-hubspot-v3-companies.ifacex"))) + ".ifcx");
    }

    @Test
    void aFileWithoutExtensionKeepsItsName() {
        assertEquals("noext", MainController.baseName(Path.of("/tmp/noext")));
    }

    @Test
    void charactersThatBreakAFileNameAreReplaced() {
        assertEquals("a_b_c__d_e", MainController.safeFileName("a/b:c*?d|e"));
        assertEquals("Zuerich", MainController.safeFileName("  Zuerich  "));
        assertEquals("", MainController.safeFileName(null));
    }
}
