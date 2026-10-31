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

import java.awt.Color;
import java.awt.Cursor;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Style;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.fife.ui.rtextarea.RTextScrollPane;

import ch.autumo.ui.fx.view.DarkScrollBarUI;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

/**
 * Console for displaying ifaceX process output.
 * <p>
 * The console is non-editable and uses {@link SyntaxConstants#SYNTAX_STYLE_NONE} — ifaceX
 * produces plain log output, not YAML. Custom ifaceX log highlighting (INFO/DEBUG/WARN/ERROR/
 * FATAL, READER/WRITER, succeeded/Caused by/ifaceX processing END) can be added through a
 * custom TokenMaker in {@link #highlightConsole()}; for now that method is a documented
 * no-op placeholder.</p>
 */
public final class Console {

    /** Resource path of the framework's RSyntaxTextArea theme. */
    private static final String THEME_RESOURCE = "/ch/autumo/ui/cfg/edit.xml";

    /** Editor background ({@code Constants.BG_COLOR}). */
    private static final Color BG = new Color(47, 47, 47);
    /** Editor foreground/caret colour ({@code Constants.FG_COLOR}). */
    private static final Color FG = new Color(240, 240, 240);
    /** Editor frame colour ({@code Constants.BG_COLOR_FIELD}). */
    private static final Color BORDER = new Color(85, 85, 85);

    /** Legacy warn pattern from {@code Helper.getWarnPattern()} — just the literal {@code "Warning"}. */
    static final String WARN_PATTERN = "Warning";
    /** Legacy error pattern from {@code Helper.getErrorPattern()} — just the literal {@code "Error"}. */
    static final String ERROR_PATTERN = "Error";
    /** Legacy debug pattern from {@code MainFrame.highlightConsole()} — the literal {@code "DEBUG"}. */
    static final String DEBUG_PATTERN = "DEBUG";
    /** Legacy reader pattern from {@code MainFrame.highlightConsole()} — the literal {@code "READER"}. */
    static final String READER_PATTERN = "READER";
    /** Legacy writer pattern from {@code MainFrame.highlightConsole()} — the literal {@code "WRITER"}. */
    static final String WRITER_PATTERN = "WRITER";
    /** Legacy exception pattern from {@code MainFrame.highlightConsole()} — the literal {@code "Exception"}. */
    static final String EXCEPTION_PATTERN = "Exception";

    /**
     * The editor text on the JavaFX side. The Swing document listener keeps it up to date with
     * what the user typed; {@link #setText(String)} sets it directly. It is the only text the
     * JavaFX thread ever reads, so it never has to touch the Swing component.
     */
    private final StringBuilder textBuffer = new StringBuilder();

    /**
     * Set while the editor widget is filled with the text of the JavaFX side copy: the document
     * listener must not write that text back, it would overwrite a newer one that arrived in
     * between.
     */
    private final java.util.concurrent.atomic.AtomicBoolean applying =
            new java.util.concurrent.atomic.AtomicBoolean();

    /** Notified when files are dropped onto the console. */
    private volatile java.util.function.Predicate<java.util.List<java.io.File>> onFilesDropped;

    /** Notified when the user changed the text in the editor. */
    private volatile Runnable onEdit;

    /** The JavaFX container holding the editor; this is the node the tab shows. */
    private final BorderPane root = new BorderPane();

    /** The Swing editor widget; {@code null} until the AWT thread has built it. */
    private volatile RSyntaxTextArea textArea;

    /** The Swing scroll pane around the editor; {@code null} until the AWT thread has built it. */
    private volatile RTextScrollPane scrollPane;

    /**
     * Output that arrived but has not been appended to the Swing document yet. Appending line by
     * line would repaint the document thousands of times during a run; the buffer is flushed as
     * one block instead (see {@link #FLUSH_DELAY_MS}).
     */
    private final StringBuilder pending = new StringBuilder();

    /** Timer that flushes {@link #pending}; created lazily on the AWT event dispatch thread. */
    private javax.swing.Timer flushTimer;

    /** Delay in milliseconds after which buffered output is appended to the document. */
    private static final int FLUSH_DELAY_MS = 60;

    /**
     * Creates the editor. The Swing widget is built on the AWT event dispatch thread, so the
     * JavaFX thread is never blocked by it; the container is filled as soon as the widget is
     * there.
     */
    public Console() {
        this.root.getStyleClass().add("console");
        // The text cursor (I-beam) over the output: the console cannot be typed into, but it
        // can be selected. The Swing component's own cursor is not shown by JavaFX.
        this.root.setCursor(javafx.scene.Cursor.TEXT);
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
                if (Console.this.applying.get()) {
                    return;
                }
                synchronized (Console.this.textBuffer) {
                    Console.this.textBuffer.setLength(0);
                    Console.this.textBuffer.append(area.getText());
                }
                Console.this.fireEdit();
            }
        });
        final RTextScrollPane pane = createScrollPane(area);
        applyTheme(area);
        // The Swing popup follows the look & feel (bright): paint it like the editor.
        ch.autumo.ui.fx.view.EditorPopup.applyDarkTheme(area);
        // A file dropped onto the console opens it (the drop is a Swing event). Both the output
        // view and its scroll pane take the drop, so the gutter accepts one as well.
        FileDropTarget.install(area, files -> onFilesDropped != null && onFilesDropped.test(files));
        FileDropTarget.install(pane, files -> onFilesDropped != null && onFilesDropped.test(files));
        // Copy and select all of the output (paste/cut/undo stay inactive: the console is
        // read-only). Inside a SwingNode a Cmd/Ctrl combination does not reach Swing.
        ch.autumo.ui.fx.view.EditorShortcuts.install(this.root, area);
        // Highlight ifaceX log patterns with the legacy colours via a custom TokenMaker.
        applyConsoleSyntaxHighlighting(area);
        this.textArea = area;
        this.scrollPane = pane;
        // The SwingNode is a JavaFX node: it is filled on the JavaFX thread.
        Platform.runLater(() -> {
            final SwingNode node = new SwingNode();
            node.setContent(pane);
            this.root.setCenter(node);
        });
        // The text that was set before the widget existed.
        writeToWidget(area, getText());
    }

    /**
     * Replaces the whole content of the editor widget.
     *
     * <p>The text is the one that was captured when the write was queued, never the current
     * JavaFX side copy: output lines that arrive while the write is on its way to the AWT
     * thread are buffered in {@link #pending} and appended after it. Reading the live copy
     * here would write those lines twice - once as part of the full text, once from the
     * buffer.</p>
     *
     * <p>Running on the AWT event dispatch thread.</p>
     *
     * @param area the editor widget
     * @param text the complete new content
     */
    private void writeToWidget(RSyntaxTextArea area, String text) {
        synchronized (this.pending) {
            // Superseded appends must not be applied on top of the new content.
            this.pending.setLength(0);
        }
        this.applying.set(true);
        try {
            if (!text.equals(area.getText())) {
                area.setText(text);
            }
            // A console shows the newest output; keep the caret at the end.
            area.setCaretPosition(area.getDocument().getLength());
        } finally {
            this.applying.set(false);
        }
        repaint(area);
    }

    /**
     * Repaints the console now.
     *
     * <p>A {@link SwingNode} paints the embedded component into an image and does not repaint it
     * by itself when the document changed: cleared or replaced content stays visible until
     * something triggers a paint (e.g. scrolling).</p>
     */
    public void repaintConsole() {
        final RSyntaxTextArea area = this.textArea;
        if (area == null) {
            return;
        }
        SwingUtilities.invokeLater(() -> repaint(area));
    }

    /**
     * Repaints the editor widget and its scroll pane, twice: the pane has to be laid out before
     * it can paint the new content.
     *
     * @param area the editor widget
     */
    private void repaint(RSyntaxTextArea area) {
        final RTextScrollPane pane = this.scrollPane;
        if (pane != null) {
            pane.revalidate();
            pane.repaint();
        }
        area.repaint();
        SwingUtilities.invokeLater(() -> {
            if (pane != null) {
                pane.repaint();
            }
            area.repaint();
        });
    }

    /**
     * Sets the handler for files dropped onto the console.
     *
     * <p>The console is a Swing component: a drop is delivered to Swing, not to the JavaFX
     * scene, so it installs its own {@link java.awt.dnd.DropTarget} ({@link FileDropTarget}).</p>
     *
     * @param handler receives the dropped files, returns {@code true} when one was opened
     */
    public void setOnFilesDropped(java.util.function.Predicate<java.util.List<java.io.File>> handler) {
        this.onFilesDropped = handler;
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
     * editor (tab size, margin, line wrap, colours) and {@link SyntaxConstants#SYNTAX_STYLE_NONE}
     * — ifaceX produces plain log output, not YAML.
     *
     * @return the editor widget
     */
    private RSyntaxTextArea createTextArea() {
        final RSyntaxTextArea area = new RSyntaxTextArea();
        area.setTabSize(4);
        area.setEditable(false);
        area.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        area.setForeground(FG);
        area.setCaretColor(FG);
        area.setBackground(BG);
        area.setMargin(new java.awt.Insets(8, 8, 8, 8));
        // Line wrapping forces the WrappedSyntaxView to recalculate the line counts of the whole
        // document on every layout/selection - with tens of thousands of characters that is slow,
        // especially through the SwingNode. A log view does not need it: long lines are simply
        // cut and can be reached with the horizontal scrollbar.
        area.setLineWrap(false);
        // The current-line highlight is meaningless in a read-only log and makes the caret move
        // (and therefore select-all) call modelToView() on every change.
        area.setHighlightCurrentLine(false);
        area.setAntiAliasingEnabled(true);
        area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);

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
        // Inside a SwingNode the Swing content is painted off screen and copied into a JavaFX
        // texture on every repaint: the Swing double buffer is a redundant second copy and makes
        // scrolling slow (see DefaultCodeEditorView in autumo-ui, same setting).
        area.setDoubleBuffered(false);

        final RTextScrollPane pane = new RTextScrollPane(area);
        pane.setDoubleBuffered(false);
        pane.getViewport().setDoubleBuffered(false);
        pane.setFoldIndicatorEnabled(true);
        pane.setBorder(BorderFactory.createLineBorder(BORDER));
        pane.getVerticalScrollBar().setUI(new DarkScrollBarUI());
        pane.getHorizontalScrollBar().setUI(new DarkScrollBarUI());
        // The scroll pane and its viewport paint the corners around the bars - without this the
        // look & feel leaves light grey patches at the ends of the horizontal bar.
        pane.setOpaque(true);
        pane.setBackground(BG);
        pane.getViewport().setOpaque(true);
        pane.getViewport().setBackground(BG);
        // A wheel notch moves more pixels: fewer repaints for the same distance.
        pane.getVerticalScrollBar().setUnitIncrement(32);
        pane.getHorizontalScrollBar().setUnitIncrement(32);
        return pane;
    }

    /**
     * Applies the framework's RSyntaxTextArea theme; the gutter is styled only now that it exists.
     *
     * @param area the editor widget
     */
    private void applyTheme(RSyntaxTextArea area) {
        try (InputStream in = Console.class.getResourceAsStream(THEME_RESOURCE)) {
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

    /**
     * Installs the custom ifaceX log {@link ConsoleTokenMaker} and extends the editor's
     * {@link SyntaxScheme} with the legacy console colours, so that process output is
     * highlighted exactly like the legacy Swing Studio did
     * ({@code ch.autumo.ifacex.studio.view.MainFrame#highlightConsole()}).
     * <p>
     * Highlighting happens automatically whenever text is set on the editor; this method only
     * wires the token maker and colour scheme up once. It must be called after
     * {@link #applyTheme(RSyntaxTextArea)}, because applying the theme replaces the syntax
     * scheme with the framework's default one.
     *
     * @param area the editor widget
     */
    private void applyConsoleSyntaxHighlighting(RSyntaxTextArea area) {
        final SyntaxScheme scheme = area.getSyntaxScheme();
        final int base = TokenTypes.DEFAULT_NUM_TOKEN_TYPES;
        final int total = base + ConsoleTokenMaker.CUSTOM_TYPE_COUNT;
        final Style[] styles = new Style[total];
        System.arraycopy(scheme.getStyles(), 0, styles, 0, base);
        // Unmatched text: console foreground colour.
        styles[TokenTypes.IDENTIFIER] = new Style(FG, null);
        // Legacy console colours (MainFrame.highlightConsole() / DesignMainFrame).
        styles[base + 0] = new Style(new Color(230, 120, 30), null); // TOKEN_CAUSED: "Caused by"
        styles[base + 1] = new Style(new Color(255, 255, 255), null); // TOKEN_INFO: "INFO"
        styles[base + 2] = new Style(new Color(0, 255, 255), null);   // TOKEN_DEBUG: "DEBUG"
        styles[base + 3] = new Style(new Color(255, 255, 0), null);   // TOKEN_WARN: "WARN"/"Warning"
        styles[base + 4] = new Style(new Color(255, 0, 0), null);     // TOKEN_ERROR: "ERROR"/"Error"/"FATAL"
        styles[base + 5] = new Style(new Color(0, 210, 110), null);   // TOKEN_RESPONSE_HEADER: "Response Header:"
        styles[base + 6] = new Style(new Color(0, 210, 110), null);   // TOKEN_RESPONSE_BODY: "Response Body:"
        styles[base + 7] = new Style(new Color(0, 120, 20), null);    // TOKEN_READER: "READER"
        styles[base + 8] = new Style(new Color(180, 0, 0), null);     // TOKEN_WRITER: "WRITER"
        styles[base + 9] = new Style(new Color(0, 200, 100), null);   // TOKEN_SUCCEEDED: "succeeded"
        styles[base + 10] = new Style(new Color(0, 255, 255), null);  // TOKEN_END: "ifaceX processing END"
        scheme.setStyles(styles);
        area.setSyntaxScheme(scheme);
        ((RSyntaxDocument) area.getDocument()).setSyntaxStyle(new ConsoleTokenMaker());
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
        synchronized (this.textBuffer) {
            return this.textBuffer.toString();
        }
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
        synchronized (this.textBuffer) {
            this.textBuffer.setLength(0);
            this.textBuffer.append(value);
        }
        final RSyntaxTextArea area = this.textArea;
        if (area == null) {
            // The widget is not built yet: it picks the text up when it is.
            return;
        }
        SwingUtilities.invokeLater(() -> writeToWidget(area, value));
    }

    /**
     * Appends a line to the console text.
     *
     * <p>The line is added to the JavaFX side copy and the Swing component follows on the AWT
     * event dispatch thread.</p>
     *
     * @param line the line to append (without line terminator)
     */
    public void appendLine(String line) {
        final String l = line == null ? "" : line;
        final String chunk = l + "\n";
        synchronized (this.textBuffer) {
            this.textBuffer.append(chunk);
        }
        final RSyntaxTextArea area = this.textArea;
        if (area == null) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            synchronized (this.pending) {
                this.pending.append(chunk);
            }
            scheduleFlush();
        });
    }

    /**
     * Appends the buffered output to the document.
     *
     * <p>Appending is essential: the previous implementation replaced the whole document with
     * {@code setText()} for every line. That invalidated the editor's selection on every
     * incoming line (a selection in the console therefore looked corrupted until it was
     * cleared), reset the caret to the first line on every line, and re-tokenised the complete
     * document for every line - quadratic work with a quadratic repaint.</p>
     *
     * <p>Running on the AWT event dispatch thread.</p>
     */
    private void flushPending() {
        final RSyntaxTextArea area = this.textArea;
        final String chunk;
        synchronized (this.pending) {
            if (this.pending.length() == 0) {
                return;
            }
            chunk = this.pending.toString();
            this.pending.setLength(0);
        }
        if (area == null) {
            return;
        }
        this.applying.set(true);
        try {
            final javax.swing.text.Document doc = area.getDocument();
            doc.insertString(doc.getLength(), chunk, null);
            // A console shows the newest output; keep the caret at the end.
            area.setCaretPosition(doc.getLength());
        } catch (javax.swing.text.BadLocationException e) {
            // The document was reset in between (clear/setText); the next flush starts over.
        } finally {
            this.applying.set(false);
        }
    }

    /** Starts the flush timer if it is not running; running on the AWT event dispatch thread. */
    private void scheduleFlush() {
        if (this.flushTimer == null) {
            this.flushTimer = new javax.swing.Timer(FLUSH_DELAY_MS, _ -> flushPending());
            this.flushTimer.setRepeats(false);
        }
        if (!this.flushTimer.isRunning()) {
            this.flushTimer.start();
        }
    }

    /**
     * Clears the console text.
     */
    public void clear() {
        setText("");
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
