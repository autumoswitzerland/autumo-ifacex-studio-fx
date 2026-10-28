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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.util.List;
import java.util.function.Predicate;

import javax.swing.JComponent;
import javax.swing.TransferHandler;
import javax.swing.text.JTextComponent;

/**
 * The file drop of the Swing editors (console, raw editor) inside a
 * {@link javafx.embed.swing.SwingNode}.
 *
 * <p>JavaFX never sees a drop over such a component: the drag events are forwarded to the drop
 * target of the embedded Swing component by the {@code javafx.swing} interop.</p>
 *
 * <p>A {@link TransferHandler} is deliberately <em>not</em> used for that. The interop posts the
 * events to the drop target of the component as they come; a Swing drop target reports a
 * {@code dragOver} to the handler behind the {@code TransferHandler} even when no
 * {@code dragEnter} preceded it, and that handler then fails with
 *
 * <pre>NullPointerException: Cannot invoke "...getTransferHandler()" because "this.component" is null</pre>
 *
 * (its component is only set in {@code dragEnter}). A {@link DropTarget} of our own keeps no such
 * state, so the order of the events does not matter.</p>
 *
 * <p>Dropped files open a configuration; a dropped text keeps working on editable components, it
 * is handed to the transfer handler the component had before.</p>
 */
final class FileDropTarget extends DropTargetAdapter {

    /** Receives the dropped files, returns {@code true} when one was opened. */
    private final Predicate<List<File>> handler;

    /** The transfer handler of the component for text drops, may be {@code null}. */
    private final TransferHandler textHandler;

    /** The Swing component that accepts the drop. */
    private final JComponent component;

    private FileDropTarget(final JComponent component, final Predicate<List<File>> handler,
            final TransferHandler textHandler) {
        this.component = component;
        this.handler = handler;
        this.textHandler = textHandler;
    }

    /**
     * Lets a Swing component accept dropped files.
     *
     * @param component the component that accepts the drop
     * @param handler receives the dropped files, returns {@code true} when one was opened
     */
    static void install(final JComponent component, final Predicate<List<File>> handler) {
        // The transfer handler goes: it brings the Swing drop target that cannot survive the
        // event order of the interop (see the class comment).
        final TransferHandler textHandler = component.getTransferHandler();
        if (textHandler != null) {
            component.setTransferHandler(null);
        }
        new DropTarget(component, DnDConstants.ACTION_COPY_OR_MOVE,
                new FileDropTarget(component, handler, textHandler), true);
    }

    @Override
    public void dragEnter(final DropTargetDragEvent event) {
        accept(event);
    }

    @Override
    public void dragOver(final DropTargetDragEvent event) {
        accept(event);
    }

    /**
     * Accepts a file drop, a text drop on an editable component, or rejects the drag.
     *
     * @param event the drag event
     */
    private void accept(final DropTargetDragEvent event) {
        if (event.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            event.acceptDrag(DnDConstants.ACTION_COPY);
        } else if (isTextDrop(event)) {
            event.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
        } else {
            event.rejectDrag();
        }
    }

    @Override
    public void drop(final DropTargetDropEvent event) {
        if (event.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            event.acceptDrop(DnDConstants.ACTION_COPY);
            event.dropComplete(importFiles(event.getTransferable()));
        } else if (isTextDrop(event)) {
            event.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
            event.dropComplete(importText(event.getTransferable()));
        } else {
            event.rejectDrop();
        }
    }

    /**
     * Hands the dropped files to the receiver.
     *
     * @param transferable the dropped data
     * @return {@code true} when a file was opened
     */
    private boolean importFiles(final Transferable transferable) {
        if (handler == null) {
            return false;
        }
        try {
            @SuppressWarnings("unchecked")
            final List<File> files = (List<File>) transferable
                    .getTransferData(DataFlavor.javaFileListFlavor);
            return handler.test(files);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Hands a dropped text to the transfer handler the component had before.
     *
     * @param transferable the dropped data
     * @return {@code true} when the text was taken over
     */
    private boolean importText(final Transferable transferable) {
        if (textHandler == null) {
            return false;
        }
        try {
            return textHandler.importData(this.component, transferable);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Whether a drag carries a text that an editable editor can take over.
     *
     * @param event the drag event
     * @return {@code true} for a text drop
     */
    private boolean isTextDrop(final DropTargetDragEvent event) {
        return textHandler != null
                && component instanceof JTextComponent text
                && text.isEditable()
                && event.isDataFlavorSupported(DataFlavor.stringFlavor);
    }

    /**
     * Whether a drop carries a text that an editable editor can take over.
     *
     * @param event the drop event
     * @return {@code true} for a text drop
     */
    private boolean isTextDrop(final DropTargetDropEvent event) {
        return textHandler != null
                && component instanceof JTextComponent text
                && text.isEditable()
                && event.isDataFlavorSupported(DataFlavor.stringFlavor);
    }
}
