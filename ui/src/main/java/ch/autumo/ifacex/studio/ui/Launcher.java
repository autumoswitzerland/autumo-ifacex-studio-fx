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
package ch.autumo.ifacex.studio.ui;

import javafx.application.Application;

/**
 * Plain launcher that does <em>not</em> extend {@link Application}.
 *
 * <p>Launching through a separate class keeps JavaFX happy when the JavaFX modules are on the
 * classpath instead of the module path (which is how the PoC runs).</p>
 */
public final class Launcher {

    private Launcher() {
    }

    /**
     * Launches the Studio. Command-line arguments are forwarded to the JavaFX application, which
     * resolves them via {@link StudioApp#getParameters()}.
     *
     * @param args command-line arguments (a configuration file to open)
     */
    public static void main(String[] args) {
        Application.launch(StudioApp.class, args);
    }
}
