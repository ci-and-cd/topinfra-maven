package top.infra.filesafe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import top.infra.logging.Logger;
import top.infra.test.logging.LoggerSlf4jImpl;

public class GpgTest {

    private Logger logger;

    private final Path decryptedFilePath = Paths.get("src/test/resources/testfile.txt.out");
    private final Path plainFilePath = Paths.get("src/test/resources/testfile.txt");
    private EncryptedFile encryptedFile;
    private ClearFile clearFile;

    @Before
    public void setUp() throws IOException {
        this.logger = new LoggerSlf4jImpl(LoggerFactory.getLogger(GpgTest.class));
        final Optional<String> gpgExecutable = GpgUtils.gpgExecutable();
        if (gpgExecutable.isPresent()) {
            final Path encryptedFilePath = Paths.get("src/test/resources/testfile.txt.gpg");
            this.encryptedFile = FileSafe.decryptByGpg(this.logger, encryptedFilePath, gpgExecutable.get());
            this.clearFile = FileSafe.encryptByGpg(this.logger, this.plainFilePath, gpgExecutable.get());

            Files.deleteIfExists(this.decryptedFilePath);
            Files.deleteIfExists(this.encryptedFile.getPath());
        }
    }

    @Test
    public void testGpg() {
        assertNotNull("gpgExecutable should present", this.encryptedFile);
        assertNotNull("gpgExecutable should present", this.clearFile);

        assertFalse(String.format("encryptedFile [%s] should not exists.", this.encryptedFile.getPath()),
            this.encryptedFile.getPath().toFile().exists());
        assertFalse(String.format("decryptedFile [%s] should not exists.", this.decryptedFilePath),
            this.decryptedFilePath.toFile().exists());

        final String passphrase = "gpg_passphrase";

        this.clearFile.encrypt(passphrase, this.encryptedFile.getPath());
        assertTrue(String.format("encryptedFile [%s] should exists.", this.encryptedFile.getPath()),
            this.encryptedFile.getPath().toFile().exists());
        assertFalse(String.format("decryptedFile [%s] should not exists.", this.decryptedFilePath),
            this.decryptedFilePath.toFile().exists());

        this.encryptedFile.decrypt(passphrase, this.decryptedFilePath);
        assertTrue(String.format("decryptedFile [%s] should exists.", this.decryptedFilePath),
            this.decryptedFilePath.toFile().exists());

        assertThat(this.decryptedFilePath).hasSameContentAs(this.clearFile.getPath());
    }
}
