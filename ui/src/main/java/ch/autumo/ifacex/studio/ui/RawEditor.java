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

import java.awt.Cursor;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

import ch.autumo.ui.fx.view.DarkScrollBarUI;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

/**
 * Raw configuration editor for the "Raw" tab of the main window.
 *
 * <p>The Studio shows the loaded interface configuration as text next to the form editor, and it
 * wants the very same widget the reference Studio used for its source views: an
 * {@link RSyntaxTextArea} (the framework's code editor component) with YAML highlighting and the
 * dark scrollbars of {@link DarkScrollBarUI}. The full pane of the framework
 * ({@code DefaultCodeEditorView}) is a separate window with a header and footer - not what a tab
 * needs - so this small holder builds only the editor widget and its JavaFX container.</p>
 *
 * <p>Look and feel come from the framework: the RSyntaxTextArea theme
 * ({@code /ch/autumo/ui/cfg/autumo-edit.xml}) defines the background, caret, gutter and token
 * colours, exactly as in the JSON formatter, and {@link DarkScrollBarUI} paints the scrollbars
 * dark instead of Swing's light Metal look.</p>
 *
 * <p><b>Threading - why this class is built the way it is.</b> The editor is a Swing component
 * inside a JavaFX window, and the two worlds have a common trap: Swing takes the AWT tree lock,
 * and while it holds that lock the AWT event dispatch thread can be stuck in a native call (on
 * macOS while a Swing component is removed). A JavaFX thread that then touches Swing waits for
 * that lock - and for ever, because the AWT thread never comes back. The application then
 * freezes.</p>
 *
 * <p>Two rules avoid it, and they are the reason for the somewhat unusual code here:</p>
 * <ul>
 *   <li><b>The JavaFX thread never touches Swing.</b> The editor widget is built on the AWT
 *       event dispatch thread and the text is kept on the JavaFX side: the JavaFX thread only
 *       ever reads and writes that copy. It never waits for the AWT thread - not even with
 *       {@code invokeAndWait}.</li>
 *   <li><b>The editor is disposed before it is removed.</b> {@link #dispose()} switches the
 *       input methods off, so macOS does not call into the native input method while the
 *       component is removed - that call can block the AWT thread for ever.</li>
 * </ul>
 */
public final class RawEditor {

    /** Resource path of the framework's RSyntaxTextArea theme. */
    private static final String THEME_RESOURCE = "/ch/autumo/ui/cfg/autumo-edit.xml";

    /** Editor background ({@code Constants.BG_COLOR}). */
    private static final java.awt.Color BG = new java.awt.Color(47, 47, 47);
    /** Editor foreground/caret colour ({@code Constants.FG_COLOR}). */
    private static final java.awt.Color FG = new java.awt.Color(240, 240, 240);
    /** Editor frame colour ({@code Constants.BG_COLOR_FIELD}). */
    private static final java.awt.Color BORDER = new java.awt.Color(85, 85, 85);

    /**
     * The editor text on the JavaFX side. The Swing document listener keeps it up to date with
     * what the user typed; {@link #setText(String)} sets it directly. It is the only text the
     * JavaFX thread ever reads, so it never has to touch the Swing component.
     */
    private volatile String text = "";

    /**
     * Set while the editor widget is filled with the text of the JavaFX side copy: the document
     * listener must not write that text back, it would overwrite a newer one that arrived in
     * between.
     */
    private final java.util.concurrent.atomic.AtomicBoolean applying =
            new java.util.concurrent.atomic.AtomicBoolean();

    /** Notified when the user changed the text in the editor. */
    private volatile Runnable onEdit;

    /** The JavaFX container holding the editor; this is the node the tab shows. */
    private final BorderPane root = new BorderPane();

    /** The Swing editor widget; {@code null} until the AWT thread has built it. */
    private volatile RSyntaxTextArea textArea;

    /** The Swing scroll pane around the editor; {@code null} until the AWT thread has built it. */
    private volatile RTextScrollPane scrollPane;

    /**
     * Creates the editor. The Swing widget is built on the AWT event dispatch thread, so the
     * JavaFX thread is never blocked by it; the container is filled as soon as the widget is
     * there.
     */
    public RawEditor() {
        this.root.getStyleClass().add("raw-editor");
        SwingUtilities.invokeLater(this::buildEditor);
    }

    /** Builds the Swing editor widget on the AWT event dispatch thread. */
    private void buildEditor() {
        final RSyntaxTextArea area = createTextArea();
        // The document listener mirrors what the user typed into the JavaFX side copy.
        area.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                mirror();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                mirror();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                mirror();
            }

            private void mirror() {
                if (RawEditor.this.applying.get()) {
                    return;
                }
                RawEditor.this.text = area.getText();
                RawEditor.this.fireEdit();
            }
        });
        final RTextScrollPane pane = createScrollPane(area);
        applyTheme(area);
        this.textArea = area;
        this.scrollPane = pane;
        // The SwingNode is a JavaFX node: it is filled on the JavaFX thread.
        Platform.runLater(() -> {
            final SwingNode node = new SwingNode();
            node.setContent(pane);
            this.root.setCenter(node);
        });
        // The text that was set before the widget existed.
        writeToWidget(area);
    }

    /**
     * Fills the editor widget with the text of the JavaFX side copy.
     *
     * <p>Always the <em>latest</em> text is written, so a text that arrives while an older one
     * is still on its way to the widget wins: the widget and the copy always converge.</p>
     *
     * @param area the editor widget
     */
    private void writeToWidget(RSyntaxTextArea area) {
        final String latest = this.text;
        if (latest.equals(area.getText())) {
            return;
        }
        this.applying.set(true);
        try {
            area.setText(latest);
            // setText() puts the caret behind the last character, and the view scrolls there:
            // the editor would always show the end of the document. It starts at the top.
            area.setCaretPosition(0);
        } finally {
            this.applying.set(false);
        }
    }

    /**
     * Sets the listener that is notified when the user changes the text in the editor.
     *
     * <p>The listener runs on the JavaFX thread.</p>
     *
     * @param onEdit the listener, or {@code null} to remove it
     */
    public void setOnEdit(Runnable onEdit) {
        this.onEdit = onEdit;
    }

    /**
     * Notifies the listener that the user changed the text in the editor.
     *
     * <p>Called by the document listener of the editor widget - on the AWT event dispatch thread -
     * and by the UI tests. The listener always runs on the JavaFX thread: the window title and
     * the status bar belong to it.</p>
     */
    void fireEdit() {
        final Runnable listener = this.onEdit;
        if (listener == null) {
            return;
        }
        if (Platform.isFxApplicationThread()) {
            listener.run();
        } else {
            Platform.runLater(listener);
        }
    }

    /**
     * Creates the editor widget: an RSyntaxTextArea with the settings of the framework's code
     * editor (tab size, margin, line wrap, colours) and the YAML syntax style.
     *
     * @return the editor widget
     */
    private RSyntaxTextArea createTextArea() {
        final RSyntaxTextArea area = new RSyntaxTextArea();
        area.setTabSize(4);
        area.setEditable(true);
        area.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        area.setForeground(FG);
        area.setCaretColor(FG);
        area.setBackground(BG);
        area.setMargin(new java.awt.Insets(8, 8, 8, 8));
        area.setLineWrap(true);
        area.setAntiAliasingEnabled(true);
        area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_YAML);
        return area;
    }

    /**
     * Creates the scroll pane around the editor widget: the line-number gutter with the fold
     * indicator and the dark scrollbars ({@link DarkScrollBarUI} - Swing's own look &amp; feel
     * would paint light ones, which clash with the dark editor theme).
     *
     * @param area the editor widget
     * @return the scroll pane holding the editor
     */
    private RTextScrollPane createScrollPane(RSyntaxTextArea area) {
        final RTextScrollPane pane = new RTextScrollPane(area);
        pane.setFoldIndicatorEnabled(true);
        pane.setBorder(BorderFactory.createLineBorder(BORDER));
        pane.getVerticalScrollBar().setUI(new DarkScrollBarUI());
        pane.getHorizontalScrollBar().setUI(new DarkScrollBarUI());
        pane.getVerticalScrollBar().setUnitIncrement(16);
        pane.getHorizontalScrollBar().setUnitIncrement(16);
        return pane;
    }

    /** Apply the framework's RSyntaxTextArea theme; the gutter is styled only now that it exists. */
    private void applyTheme(RSyntaxTextArea area) {
        try (InputStream in = RawEditor.class.getResourceAsStream(THEME_RESOURCE)) {
            if (in != null) {
                Theme.load(in).apply(area);
            } else {
                System.err.println("Editor theme not found: " + THEME_RESOURCE);
            }
        } catch (IOException e) {
            System.err.println("Could not load the editor theme: " + e.getMessage());
        }
        area.setFont(new java.awt.Font(fontFamily(), java.awt.Font.BOLD, 14));
        area.revalidate();
    }

    /** The JavaFX container holding the editor; this is the node the tab shows. */
    public Pane node() {
        return this.root;
    }

    /**
     * The current editor text.
     *
     * <p>Read from the JavaFX side copy, never from the Swing component: the JavaFX thread must
     * not wait for the AWT event dispatch thread, a busy AWT thread would freeze the
     * window.</p>
     *
     * @return the text
     */
    public String getText() {
        return this.text;
    }

    /**
     * Sets the editor text.
     *
     * <p>The JavaFX side copy is updated at once, the Swing component follows on the AWT event
     * dispatch thread. The call never blocks the caller.</p>
     *
     * @param text the text
     */
    public void setText(String text) {
        final String value = text == null ? "" : text;
        this.text = value;
        final RSyntaxTextArea area = this.textArea;
        if (area == null) {
            // The widget is not built yet: it picks the text up when it is.
            return;
        }
        SwingUtilities.invokeLater(() -> writeToWidget(area));
    }

    /**
     * Releases the editor before it is removed from the window.
     *
     * <p>Switching the input methods off keeps macOS from calling into the native input method
     * while the component is removed: {@code Component.removeNotify()} asks the input context of
     * the component, and that native call can block the AWT thread forever - which would freeze
     * every Swing window of the application and keep the JVM from exiting. The whole hierarchy
     * is covered, not only the text area: the gutter and its fold indicator are removed as
     * well.</p>
     *
     * <p>The work goes to the AWT event dispatch thread and is <b>not</b> waited for: the
     * JavaFX thread must not block on the AWT thread, neither here nor anywhere else.</p>
     */
    public void dispose() {
        final RTextScrollPane pane = this.scrollPane;
        if (pane == null) {
            return;
        }
        javax.swing.SwingUtilities.invokeLater(
                () -> ch.autumo.ui.fx.view.DefaultCodeEditorView.disableInputMethods(pane));
    }

    /** Editor font family: the reference used Consolas on Windows and Monaco everywhere else. */
    private static String fontFamily() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows")
                ? "Consolas" : "Monaco";
    }
}
