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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the console tokens are contiguous and cover the line exactly. A token whose
 * offset or length is off would paint the text shifted - and no syntax scheme could repair that.
 */
class ConsoleTokenMakerTest {

    /** The tokens of one line, in order, as {@code offset:length:type:text}. */
    private static String tokens(String line) {
        final ConsoleTokenMaker maker = new ConsoleTokenMaker();
        final Segment segment = new Segment(line.toCharArray(), 0, line.length());
        Token token = maker.getTokenList(segment, TokenTypes.NULL, 0);
        assertNotNull(token);
        final StringBuilder sb = new StringBuilder();
        int expectedOffset = 0;
        while (token != null && token.getType() != TokenTypes.NULL) {
            assertEquals(expectedOffset, token.getOffset(),
                    "token '" + token.getLexeme() + "' must start where the previous one ended");
            expectedOffset += token.length();
            if (sb.length() > 0) {
                sb.append('|');
            }
            sb.append(token.getOffset()).append(':').append(token.length()).append(':')
                    .append(token.getType()).append(':').append(token.getLexeme());
            token = token.getNextToken();
        }
        assertEquals(line.length(), expectedOffset,
                "the tokens must cover the whole line, without a gap and without an overlap");
        return sb.toString();
    }

    @Test
    void readerIsOneTokenWithTheReaderType() {
        final String line = "20:10:20.449 : READER '";
        final String tokens = tokens(line);

        // "20:10:20.449 : " is 15 characters, so "READER" starts at index 15 and is 6 long.
        assertTrue(tokens.contains("15:6:" + ConsoleTokenMaker.TOKEN_READER + ":READER"), tokens);
    }

    @Test
    void theTextBeforeAMatchIsASingleToken() {
        final String line = "20:10:20.449 : READER '";

        assertTrue(tokens(line).startsWith(
                "0:15:" + TokenTypes.IDENTIFIER + ":20:10:20.449 : "), tokens(line));
    }

    @Test
    void tokensCoverTheLineForSeveralPatterns() {
        for (final String line : new String[] {
                "INFO  20:10:20.449 : WRITER 'csv_out' succeeded",
                "ERROR 20:10:20.449 : READER 'rest_in' Exception",
                "****  ifaceX processing END.  ****",
                "    at ch.autumo.ifacex.Caused by something",
                "", "x", "READER", "READERREADER" }) {
            tokens(line); // asserts contiguity and coverage
        }
    }
}
