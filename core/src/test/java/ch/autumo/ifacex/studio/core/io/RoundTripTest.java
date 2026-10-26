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
package ch.autumo.ifacex.studio.core.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ch.autumo.ifacex.studio.core.model.IpcConfig;

/**
 * Golden round-trip tests over a curated set of real {@code .ifacex} templates.
 *
 * <p>Guarantees that the model is lossless: legacy properties -> model -> properties and
 * legacy properties -> model -> {@code .ifcx} -> model both reproduce the same canonical
 * key/value map.</p>
 */
class RoundTripTest {

    static Stream<Path> templates() throws Exception {
        final Path dir = Path.of(RoundTripTest.class.getResource("/templates").toURI());
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> p.toString().endsWith(".ifacex")).sorted().toList().stream();
        }
    }

    @ParameterizedTest
    @MethodSource("templates")
    void propertiesRoundTripIsLossless(Path file) throws Exception {
        final IpcConfig original = PropertiesConfigReader.read(file);
        final Map<String, String> expected = PropertiesConfigWriter.toFlatMap(original);

        final String props = PropertiesConfigWriter.toText(original);
        final IpcConfig reparsed = PropertiesConfigReader.read(props.lines().toList());

        assertMetaEquals(original, reparsed, file);
        assertEquals(expected, PropertiesConfigWriter.toFlatMap(reparsed), keyDiff(expected, reparsed));
    }

    @ParameterizedTest
    @MethodSource("templates")
    void yamlRoundTripIsLossless(Path file) throws Exception {
        final IpcConfig original = PropertiesConfigReader.read(file);
        final Map<String, String> expected = PropertiesConfigWriter.toFlatMap(original);

        final String yaml = YamlConfigWriter.toText(original);
        final IpcConfig reparsed = YamlConfigReader.read(yaml);

        assertMetaEquals(original, reparsed, file);
        assertEquals(expected, PropertiesConfigWriter.toFlatMap(reparsed), keyDiff(expected, reparsed));
    }

    @ParameterizedTest
    @MethodSource("templates")
    void rawFileKeysAndValuesAreFullyPreserved(Path file) throws Exception {
        final List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        final Map<String, String> raw = normalize(PropertiesConfigReader.parseRaw(lines));
        final IpcConfig cfg = PropertiesConfigReader.read(file);
        final Map<String, String> model = normalize(PropertiesConfigWriter.toFlatMap(cfg));
        // Every key from the original file must be present in the model with the same value.
        // Additional catalogue parameters (with defaults) may also be present in the model -
        // they are part of the GUI model so every field is editable, even when the loaded file
        // does not contain them.
        raw.forEach((k, v) -> assertEquals(v, model.get(k),
                "key " + k + " must be preserved: " + keyDiff(raw, model)));
    }

    private static Map<String, String> normalize(Map<String, String> in) {
        final Map<String, String> out = new java.util.LinkedHashMap<>();
        in.forEach((k, v) -> {
            if (k.startsWith("meta")) {
                return;
            }
            if (k.endsWith("_fields")) {
                final StringBuilder sb = new StringBuilder();
                for (final String p : v.split(",")) {
                    final String t = p.trim();
                    if (!t.isEmpty()) {
                        if (sb.length() > 0) {
                            sb.append(',');
                        }
                        sb.append(t);
                    }
                }
                out.put(k, sb.toString());
            } else {
                out.put(k, v);
            }
        });
        return out;
    }

    @ParameterizedTest
    @MethodSource("templates")
    void headerIsPreservedVerbatim(Path file) throws Exception {
        final List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        final int close = closingRuler(lines);
        assertFalse(close < 0, "no closing ruler in " + file);
        final String originalHeader = String.join("\n", lines.subList(0, close + 1)) + "\n";

        final IpcConfig cfg = PropertiesConfigReader.read(file);
        assertEquals(originalHeader, CommentHeader.render(cfg.meta()), "header mismatch: " + file);
    }

    private static void assertMetaEquals(IpcConfig a, IpcConfig b, Path file) {
        assertNotNull(b.meta());
        assertEquals(a.meta().title(), b.meta().title(), "title: " + file);
        assertEquals(a.meta().version(), b.meta().version(), "version: " + file);
        assertEquals(a.meta().author(), b.meta().author(), "author: " + file);
        assertEquals(a.meta().notes(), b.meta().notes(), "notes: " + file);
    }

    private static int closingRuler(List<String> lines) {
        boolean first = false;
        for (int i = 0; i < lines.size(); i++) {
            final String l = lines.get(i);
            if (l.length() >= 32 && l.chars().allMatch(c -> c == '#')) {
                if (!first) {
                    first = true;
                } else {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String keyDiff(Map<String, String> expected, IpcConfig actual) {
        return keyDiff(expected, PropertiesConfigWriter.toFlatMap(actual));
    }

    private static String keyDiff(Map<String, String> expected, Map<String, String> a) {
        final StringBuilder sb = new StringBuilder("key/value differences:");
        expected.forEach((k, v) -> {
            if (!a.containsKey(k)) {
                sb.append("\n  missing: ").append(k).append('=').append(v);
            } else if (!v.equals(a.get(k))) {
                sb.append("\n  differs: ").append(k).append(" expected=<").append(v)
                        .append("> actual=<").append(a.get(k)).append('>');
            }
        });
        a.forEach((k, v) -> {
            if (!expected.containsKey(k)) {
                sb.append("\n  extra: ").append(k).append('=').append(v);
            }
        });
        return sb.toString();
    }
}
