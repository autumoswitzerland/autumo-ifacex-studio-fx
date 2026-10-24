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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ch.autumo.ifacex.studio.core.model.IpcConfig;
import ch.autumo.ifacex.studio.core.model.RwConfig;
import ch.autumo.ifacex.studio.core.model.Secret;

/**
 * Tests of the encoded secrets: the legacy notation of the engine, the structured object of the
 * new format.
 */
class SecretTest {

    /**
     * The legacy notation of an encoded secret becomes a secret of the model, the new format
     * writes it as the structured object and the properties writer gives the engine its notation
     * back.
     *
     * @param dir the temporary directory for the test file
     * @throws Exception if the file cannot be written or read
     */
    @Test
    void anEncodedSecretIsStructuredInTheNewFormatAndLegacyForTheEngine(@TempDir Path dir)
            throws Exception {
        final Path file = dir.resolve("secret.ifacex");
        Files.writeString(file, String.join("\n",
                "wb_out_url=jdbc:mariadb://localhost:3306/db",
                "wb_out_password={3:KU1LKGDLr3iPMlG3mYt4HqCAaZNImJ2K4wbVG9WqrOs=}",
                "wb_out_user=udahigoc_cspa",
                ""));

        final IpcConfig config = PropertiesConfigReader.read(file);
        final RwConfig writer = config.sections().stream()
                .filter(rw -> "wb_out".equals(rw.name())).findFirst().orElseThrow();

        // The model holds the secret in its structured form.
        final Object password = writer.global().get("password");
        final Secret secret = assertInstanceOf(Secret.class, password, "the password is a secret");
        assertEquals(3, secret.mode(), "the mode of the secret");
        assertEquals("KU1LKGDLr3iPMlG3mYt4HqCAaZNImJ2K4wbVG9WqrOs=", secret.value(), "the value");

        // A plain value stays a plain string - no "enc: 0".
        assertEquals("udahigoc_cspa", writer.global().get("user"), "a plain value stays a string");

        // The new format writes the object.
        final String yaml = YamlConfigWriter.toText(config);
        assertTrue(yaml.contains("password: {enc: 3, value: \"KU1LKGDLr3iPMlG3mYt4HqCAaZNImJ2K4wbVG9WqrOs=\"}"),
                "the secret is the structured object:\n" + yaml);

        // The engine gets its notation back.
        final String props = PropertiesConfigWriter.toText(config);
        assertTrue(props.contains("wb_out_password={3:KU1LKGDLr3iPMlG3mYt4HqCAaZNImJ2K4wbVG9WqrOs=}"),
                "the engine reads the legacy notation:\n" + props);

        // And the new format is read back into the model.
        final IpcConfig reparsed = YamlConfigReader.read(yaml);
        final RwConfig reread = reparsed.sections().stream()
                .filter(rw -> "wb_out".equals(rw.name())).findFirst().orElseThrow();
        assertEquals(secret, reread.global().get("password"), "the secret survives the round trip");
    }
}
