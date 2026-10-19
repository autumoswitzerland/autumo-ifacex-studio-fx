/*
 * Copyright 2026 autumo GmbH.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package ch.autumo.ifacex.studio.ui;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.application.Platform;
import javafx.scene.Scene;

/**
 * Development-only live reload for {@code style/application.css}.
 *
 * <p>JavaFX caches parsed style sheets per URL, so re-adding the same URL has no effect. This
 * watcher therefore writes every change to a <em>newly named</em> copy in a temp directory and
 * swaps the scene's style sheet entry — JavaFX then parses the fresh URL and applies the rules
 * immediately. No restart needed while tweaking padding, fonts or colours.</p>
 *
 * <p>It only activates for a development run: the style sheet must be a {@code file:} URL below
 * {@code target/classes} and the matching {@code src/main/resources} source file must exist. In a
 * packaged application (JAR URL) it does nothing. Disable with {@code -Difacex.livereload=false}.</p>
 *
 * <p>Java code still needs a rebuild and restart; this covers style changes only.</p>
 */
final class CssLiveReload {

    private static final long POLL_MS = 300;

    private CssLiveReload() {
    }

    /**
     * Start watching the source file behind {@code stylesheetUrl} and keep the scene up to date.
     *
     * @param scene         the live scene whose style sheet entry is swapped
     * @param stylesheetUrl the {@code style/application.css} URL that was added to the scene
     */
    static void install(Scene scene, String stylesheetUrl) {
        if (!Boolean.parseBoolean(System.getProperty("ifacex.livereload", "true"))) {
            return;
        }
        final Path source = sourceFileFor(stylesheetUrl);
        if (source == null) {
            return; // packaged build or unknown layout — nothing to watch
        }
        final List<String> stylesheets = scene.getStylesheets();
        final int slot = stylesheets.indexOf(stylesheetUrl);
        if (slot < 0) {
            return;
        }
        final Path tempDir;
        try {
            tempDir = Files.createTempDirectory("ifacex-livereload");
        } catch (IOException e) {
            System.err.println("[dev] CSS live reload unavailable: " + e.getMessage());
            return;
        }
        final Thread watcher = new Thread(() -> watch(scene, slot, source, tempDir), "css-livereload");
        watcher.setDaemon(true);
        watcher.start();
        System.out.println("[dev] CSS live reload: watching " + source);
    }

    /**
     * Map the loaded class-path URL back to the source file. Handles both the Maven layout
     * ({@code .../target/classes/<pkg>/style/application.css}) and the Gradle layout
     * ({@code .../build/resources/main/<pkg>/style/application.css} or
     * {@code .../build/classes/java/main/<pkg>/style/application.css}).
     *
     * @return the source file, or {@code null} when this is not a development layout
     */
    private static Path sourceFileFor(String stylesheetUrl) {
        if (stylesheetUrl == null || !stylesheetUrl.startsWith("file:")) {
            return null;
        }
        try {
            final String path = Path.of(URI.create(stylesheetUrl)).toString();
            for (final String marker : new String[] {
                    "/target/classes/",
                    "/build/resources/main/",
                    "/build/classes/java/main/" }) {
                final int at = path.lastIndexOf(marker);
                if (at < 0) {
                    continue;
                }
                final Path source = Path.of(path.substring(0, at) + "/src/main/resources/"
                        + path.substring(at + marker.length()));
                if (Files.isRegularFile(source)) {
                    return source;
                }
            }
            return null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static void watch(Scene scene, int slot, Path source, Path tempDir) {
        final AtomicInteger version = new AtomicInteger();
        long last = stamp(source);
        while (true) {
            sleep(POLL_MS);
            settle(source);
            final long now = stamp(source);
            if (now == last || now == 0) {
                continue;
            }
            last = now;
            final Path copy = tempDir.resolve("application-" + version.incrementAndGet() + ".css");
            try {
                Files.copy(source, copy, StandardCopyOption.REPLACE_EXISTING);
                final String url = copy.toUri().toString();
                Platform.runLater(() -> {
                    if (slot < scene.getStylesheets().size()) {
                        scene.getStylesheets().set(slot, url);
                    }
                });
                System.out.println("[dev] CSS live reload: applied " + copy.getFileName());
            } catch (IOException e) {
                System.err.println("[dev] CSS live reload failed: " + e.getMessage());
            }
        }
    }

    /** Wait until the file stops changing, so a half-written file is never applied. */
    private static void settle(Path source) {
        long previous = -1;
        for (int i = 0; i < 10; i++) {
            final long now = stamp(source);
            if (now == previous) {
                return;
            }
            previous = now;
            sleep(60);
        }
    }

    private static long stamp(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis() * 1000 + Files.size(path);
        } catch (IOException e) {
            return 0;
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
