/*
 * Copyright 2026 autumo GmbH.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
