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

import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMaker;
import org.fife.ui.rsyntaxtextarea.TokenMap;
import org.fife.ui.rsyntaxtextarea.TokenTypes;

/**
 * Custom {@link TokenMaker} that highlights ifaceX log output in the
 * {@link Console} exactly like the legacy Swing Studio did
 * ({@code ch.autumo.ifacex.studio.view.MainFrame#highlightConsole()}).
 * <p>
 * The patterns (case-sensitive, as in the legacy code) are highlighted as
 * follows:
 * <table>
 *   <tr><th>Pattern</th><th>Colour</th></tr>
 *   <tr><td>{@code "Caused by"}</td><td>orange (230,120,30)</td></tr>
 *   <tr><td>{@code "INFO"}</td><td>white</td></tr>
 *   <tr><td>{@code "DEBUG"}</td><td>cyan</td></tr>
 *   <tr><td>{@code "WARN"} / {@code "Warning"}</td><td>yellow</td></tr>
 *   <tr><td>{@code "ERROR"} / {@code "Error"} / {@code "FATAL"}</td><td>red</td></tr>
 *   <tr><td>{@code "Response Header:"} / {@code "Response Body:"}</td><td>green (0,210,110)</td></tr>
 *   <tr><td>{@code "READER"}</td><td>dark green (0,120,20)</td></tr>
 *   <tr><td>{@code "WRITER"}</td><td>dark red (180,0,0)</td></tr>
 *   <tr><td>{@code "succeeded"}</td><td>green (0,200,100)</td></tr>
 *   <tr><td>{@code "ifaceX processing END"}</td><td>cyan</td></tr>
 * </table>
 * Everything else is painted in the console's default foreground colour.
 * <p>
 * The token maker uses custom token types allocated right after
 * {@link TokenTypes#DEFAULT_NUM_TOKEN_TYPES}. The {@link Console} registers
 * matching styles in the {@link org.fife.ui.rsyntaxtextarea.SyntaxScheme} of
 * the {@link org.fife.ui.rsyntaxtextarea.RSyntaxTextArea}.
 */
public final class ConsoleTokenMaker extends AbstractTokenMaker {

    /** First custom token type (the built-in types are {@code 0..DEFAULT_NUM_TOKEN_TYPES-1}). */
    private static final int CUSTOM_BASE = TokenTypes.DEFAULT_NUM_TOKEN_TYPES;

    /** {@code "Caused by"} - orange. */
    public static final int TOKEN_CAUSED = CUSTOM_BASE + 0;
    /** {@code "INFO"} - white. */
    public static final int TOKEN_INFO = CUSTOM_BASE + 1;
    /** {@code "DEBUG"} - cyan. */
    public static final int TOKEN_DEBUG = CUSTOM_BASE + 2;
    /** {@code "WARN"} / {@code "Warning"} - yellow. */
    public static final int TOKEN_WARN = CUSTOM_BASE + 3;
    /** {@code "ERROR"} / {@code "Error"} / {@code "FATAL"} - red. */
    public static final int TOKEN_ERROR = CUSTOM_BASE + 4;
    /** {@code "Response Header:"} - green. */
    public static final int TOKEN_RESPONSE_HEADER = CUSTOM_BASE + 5;
    /** {@code "Response Body:"} - green. */
    public static final int TOKEN_RESPONSE_BODY = CUSTOM_BASE + 6;
    /** {@code "READER"} - dark green. */
    public static final int TOKEN_READER = CUSTOM_BASE + 7;
    /** {@code "WRITER"} - dark red. */
    public static final int TOKEN_WRITER = CUSTOM_BASE + 8;
    /** {@code "succeeded"} - green. */
    public static final int TOKEN_SUCCEEDED = CUSTOM_BASE + 9;
    /** {@code "ifaceX processing END"} - cyan. */
    public static final int TOKEN_END = CUSTOM_BASE + 10;

    /**
     * Number of custom token types; the console's syntax scheme must cover
     * {@code TokenTypes.DEFAULT_NUM_TOKEN_TYPES + CUSTOM_TYPE_COUNT} entries.
     */
    public static final int CUSTOM_TYPE_COUNT = 11;

    /** The patterns to highlight, in legacy order. Case-sensitive. */
    private static final Pattern[] PATTERNS = {
        new Pattern("ifaceX processing END", TOKEN_END),
        new Pattern("Caused by", TOKEN_CAUSED),
        new Pattern("Response Header:", TOKEN_RESPONSE_HEADER),
        new Pattern("Response Body:", TOKEN_RESPONSE_BODY),
        new Pattern("FATAL", TOKEN_ERROR),
        new Pattern("ERROR", TOKEN_ERROR),
        new Pattern("Error", TOKEN_ERROR),
        new Pattern("DEBUG", TOKEN_DEBUG),
        new Pattern("Warning", TOKEN_WARN),
        new Pattern("WARN", TOKEN_WARN),
        new Pattern("READER", TOKEN_READER),
        new Pattern("WRITER", TOKEN_WRITER),
        new Pattern("succeeded", TOKEN_SUCCEEDED),
        new Pattern("INFO", TOKEN_INFO),
    };

    /** One pattern plus the token type it maps to. */
    private static final class Pattern {
        final char[] text;
        final int type;

        Pattern(String text, int type) {
            this.text = text.toCharArray();
            this.type = type;
        }
    }

    @Override
    public TokenMap getWordsToHighlight() {
        // Patterns are matched directly in getTokenList(...); no reserved-word map is needed.
        return new TokenMap();
    }

    @Override
    public Token getTokenList(Segment text, int initialTokenType, int startOffset) {
        resetTokenList();
        final char[] array = text.array;
        final int segStart = text.offset;
        final int len = text.count;
        int i = 0;       // index relative to segStart
        int runStart = 0;// start of the current unmatched run
        while (i < len) {
            int matchLen = 0;
            int matchType = TokenTypes.NULL;
            for (Pattern p : PATTERNS) {
                if (i + p.text.length <= len && matches(array, segStart + i, p.text)) {
                    if (p.text.length > matchLen) {
                        matchLen = p.text.length;
                        matchType = p.type;
                    }
                }
            }
            if (matchLen > 0) {
                // Paint the unmatched run before the match in the default colour.
                if (i > runStart) {
                    addToken(array, segStart + runStart, segStart + i,
                            TokenTypes.IDENTIFIER, startOffset + runStart);
                }
                addToken(array, segStart + i, segStart + i + matchLen,
                        matchType, startOffset + i);
                i += matchLen;
                runStart = i;
            } else {
                i++;
            }
        }
        // Trailing unmatched run.
        if (runStart < len) {
            addToken(array, segStart + runStart, segStart + len,
                    TokenTypes.IDENTIFIER, startOffset + runStart);
        }
        addNullToken();
        return firstToken;
    }

    /**
     * Compares {@code pattern} against {@code array} at {@code pos}, case-sensitively.
     */
    private static boolean matches(char[] array, int pos, char[] pattern) {
        for (int j = 0; j < pattern.length; j++) {
            if (array[pos + j] != pattern[j]) {
                return false;
            }
        }
        return true;
    }
}
