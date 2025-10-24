package org.frostbyte.archive;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * KeyProvider handles secure retrieval of the encryption key used by the ConfigEncryptor.
 * It avoids hardcoding secrets and supports multiple retrieval sources.
 */
public class KeyProvider {

    private static final String ENV_KEY_NAME = "CONFIG_MASTER_KEY";

    /**
     * Retrieves the encryption key from the environment variable.
     *
     * @return the encryption key as a String
     * @throws IllegalStateException if the key is not found
     */
    public static String getEncryptionKey() {
        String key = System.getenv(ENV_KEY_NAME);
        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "Environment variable '" + ENV_KEY_NAME + "' is not set.\n" +
                            "Set it before running the application.\n\n" +
                            "Example:\n" +
                            "  Windows: set CONFIG_MASTER_KEY=YourStrongPassword\n" +
                            "  Linux/Mac: export CONFIG_MASTER_KEY=YourStrongPassword"
            );
        }
        return key.trim();
    }

    /**
     * (Optional) Reads the encryption key from a secure file location.
     * This allows CI/CD or containerized environments to mount secrets safely.
     *
     * @param filePath path to the key file
     * @return the key as a String
     * @throws IOException if the file cannot be read
     */
    public static String getKeyFromFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        return Files.readString(path).trim();
    }

    /**
     * Convenience method: attempts to fetch the key from environment first,
     * then from a fallback file path if provided.
     *
     * @param fallbackFile optional path to a key file
     * @return the encryption key
     * @throws IOException if fallback file is specified but unreadable
     */
    public static String resolveKey(String fallbackFile) throws IOException {
        try {
            return getEncryptionKey();
        } catch (IllegalStateException ex) {
            if (fallbackFile != null && !fallbackFile.isBlank()) {
                return getKeyFromFile(fallbackFile);
            }
            throw ex;
        }
    }
}
