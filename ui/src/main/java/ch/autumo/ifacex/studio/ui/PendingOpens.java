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

import java.awt.Desktop;
import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Files that the operating system hands to the Studio ("open with", double-click).
 *
 * <p>On macOS a double-click does <b>not</b> pass the file as an argument: the system sends an
 * open-file event to the application, and it may arrive <em>before</em> the JavaFX application is
 * up. The handler is therefore installed as early as possible ({@link Launcher#main}) and the
 * files are queued until the shell exists; then {@link StudioApp} drains the queue.</p>
 */
final class PendingOpens {

    /** Files received before the shell was ready. */
    private static final List<File> QUEUE = new CopyOnWriteArrayList<>();

    /** Consumer that opens a file, {@code null} until the shell is ready. */
    private static volatile Consumer<File> consumer = null;

    private PendingOpens() {
    }

    /**
     * Installs the open-file handler of the desktop, if the platform supports it.
     *
     * <p>Called from the launcher before JavaFX starts, so no event is lost.</p>
     */
    static void install() {
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.APP_OPEN_FILE)) {
                Desktop.getDesktop().setOpenFileHandler(e -> accept(e.getFiles()));
            }
        } catch (Exception e) {
            System.err.println("Open-file handler not available: " + e);
        }
    }

    /**
     * Hands the files over to the shell, or queues them when it is not ready yet.
     *
     * @param files files the operating system wants the Studio to open
     */
    private static void accept(List<File> files) {
        final Consumer<File> target = consumer;
        if (target == null) {
            QUEUE.addAll(files);
            return;
        }
        files.forEach(target);
    }

    /**
     * Registers the consumer that opens a file and drains the files queued so far.
     *
     * @param target consumer for one file
     */
    static void consumeWith(Consumer<File> target) {
        consumer = target;
        if (!QUEUE.isEmpty()) {
            final List<File> pending = List.copyOf(QUEUE);
            QUEUE.clear();
            pending.forEach(target);
        }
    }
}
