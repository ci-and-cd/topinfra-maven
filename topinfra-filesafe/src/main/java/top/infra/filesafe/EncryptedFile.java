package top.infra.filesafe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import top.infra.exception.RuntimeIOException;

public interface EncryptedFile extends Resource {

    default Path decrypt(final String passphrase) {
        try {
            final Path targetPath = Files.createTempFile("decrypted-", ".tmp");
            this.decrypt(passphrase, targetPath);
            return targetPath;
        } catch (final IOException ex) {
            throw new RuntimeIOException(ex.getMessage(), ex);
        }
    }

    void decrypt(String passphrase, Path targetPath);
}
