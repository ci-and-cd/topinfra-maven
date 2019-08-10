package top.infra.filesafe;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.DSYNC;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.examples.ByteArrayHandler;

import top.infra.logging.Logger;

public class EncryptedFileGpgJava extends AbstractResource implements EncryptedFile {

    private final Map<String, String> environment;

    public EncryptedFileGpgJava(final Logger logger, final Path path) {
        super(logger, path);

        this.environment = Collections.emptyMap();
    }

    @Override
    protected Map<String, String> getEnvironment() {
        return this.environment;
    }

    @Override
    public void decrypt(final String passphrase, final Path targetPath) {
        logger.info(String.format("    Decrypting [%s] by bcpg", this.getPath()));
        try {
            final byte[] clearBytes = decrypt(this.getPath(), passphrase);
            Files.write(targetPath, clearBytes, CREATE, TRUNCATE_EXISTING, WRITE, DSYNC);
        } catch (final GeneralSecurityException | IOException | PGPException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    private static byte[] decrypt(
        final Path inPath,
        final String passphrase
    ) throws IOException, PGPException, GeneralSecurityException {
        final byte[] inBytes = FileUtils.readFileToByteArray(inPath.toFile());
        return ByteArrayHandler.decrypt(inBytes, passphrase.toCharArray());
    }
}
