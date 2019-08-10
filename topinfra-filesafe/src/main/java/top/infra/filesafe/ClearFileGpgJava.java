package top.infra.filesafe;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.DSYNC;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags.AES_256;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Map;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.examples.ByteArrayHandler;

import top.infra.logging.Logger;

public class ClearFileGpgJava extends AbstractResource implements ClearFile {

    private final Map<String, String> environment;

    public ClearFileGpgJava(final Logger logger, final Path path) {
        super(logger, path);

        this.environment = Collections.emptyMap();
    }

    @Override
    protected Map<String, String> getEnvironment() {
        return this.environment;
    }

    @Override
    public void encrypt(final String passphrase, final Path targetPath) {
        logger.info(String.format("    Encrypting [%s] by bcpg", this.getPath()));
        try {
            final byte[] encryptedBytes = encrypt(this.getPath(), passphrase);
            Files.write(targetPath, encryptedBytes, CREATE, TRUNCATE_EXISTING, WRITE, DSYNC);
        } catch (final GeneralSecurityException | IOException | PGPException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    private static byte[] encrypt(
        final Path inPath,
        final String passphrase
    ) throws IOException, PGPException, GeneralSecurityException {
        final byte[] inBytes = Files.readAllBytes(inPath.toAbsolutePath());
        return ByteArrayHandler.encrypt(inBytes, passphrase.toCharArray(), null, AES_256, true);
    }
}
