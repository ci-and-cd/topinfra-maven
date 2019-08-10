package top.infra.filesafe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import top.infra.exception.RuntimeIOException;

public interface ClearFile extends Resource {

    default Path encrypt(final String passphrase) {
        try {
            final Path targetPath = Files.createTempFile("clear-", ".tmp");
            this.encrypt(passphrase, targetPath);
            return targetPath;
        } catch (final IOException ex) {
            throw new RuntimeIOException(ex.getMessage(), ex);
        }
    }

    void encrypt(String passphrase, Path targetPath);
}
