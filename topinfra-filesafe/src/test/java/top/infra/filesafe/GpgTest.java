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
    private final Path encryptedFilePath = Paths.get("src/test/resources/testfile.txt.gpg");
    private final Path clearFilePath = Paths.get("src/test/resources/testfile.txt");
    private EncryptedFile nativeEncryptedFile;
    private ClearFile nativeClearFile;

    @Before
    public void setUp() throws IOException {
        this.logger = new LoggerSlf4jImpl(LoggerFactory.getLogger(GpgTest.class));
        final Optional<String> gpgExecutable = GpgUtils.gpgExecutable();
        if (gpgExecutable.isPresent()) {
            this.nativeEncryptedFile = FileSafe.decryptByNativeGpg(this.logger, this.encryptedFilePath, gpgExecutable.get());
            this.nativeClearFile = FileSafe.encryptByNativeGpg(this.logger, this.clearFilePath, gpgExecutable.get());

            Files.deleteIfExists(this.decryptedFilePath);
            Files.deleteIfExists(this.nativeEncryptedFile.getPath());
        }
    }

    @Test
    public void testNative() {
        assertNotNull("gpgExecutable should present", this.nativeEncryptedFile);
        assertNotNull("gpgExecutable should present", this.nativeClearFile);

        final ClearFile clearFile = this.nativeClearFile;
        final EncryptedFile encryptedFile = this.nativeEncryptedFile;

        assertFalse(String.format("encryptedFile [%s] should not exists.", encryptedFile.getPath()),
            encryptedFile.getPath().toFile().exists());
        assertFalse(String.format("decryptedFile [%s] should not exists.", this.decryptedFilePath),
            this.decryptedFilePath.toFile().exists());

        final String passphrase = "gpg_passphrase";

        clearFile.encrypt(passphrase, encryptedFile.getPath());
        assertTrue(String.format("encryptedFile [%s] should exists.", encryptedFile.getPath()),
            encryptedFile.getPath().toFile().exists());
        assertFalse(String.format("decryptedFile [%s] should not exists.", this.decryptedFilePath),
            this.decryptedFilePath.toFile().exists());

        encryptedFile.decrypt(passphrase, this.decryptedFilePath);
        assertTrue(String.format("decryptedFile [%s] should exists.", this.decryptedFilePath),
            this.decryptedFilePath.toFile().exists());

        assertThat(this.decryptedFilePath).hasSameContentAs(clearFile.getPath());
    }

    @Test
    public void testJavaEncryptNativeDecrypt() {
        assertNotNull("gpgExecutable should present", this.nativeEncryptedFile);

        final ClearFile clearFile = FileSafe.encryptByBcpg(this.logger, this.clearFilePath);
        final EncryptedFile encryptedFile = this.nativeEncryptedFile;

        assertFalse(String.format("encryptedFile [%s] should not exists.", encryptedFile.getPath()),
            encryptedFile.getPath().toFile().exists());
        assertFalse(String.format("decryptedFile [%s] should not exists.", this.decryptedFilePath),
            this.decryptedFilePath.toFile().exists());

        final String passphrase = "gpg_passphrase";

        clearFile.encrypt(passphrase, encryptedFile.getPath());
        assertTrue(String.format("encryptedFile [%s] should exists.", encryptedFile.getPath()),
            encryptedFile.getPath().toFile().exists());
        assertFalse(String.format("decryptedFile [%s] should not exists.", this.decryptedFilePath),
            this.decryptedFilePath.toFile().exists());

        encryptedFile.decrypt(passphrase, this.decryptedFilePath);
        assertTrue(String.format("decryptedFile [%s] should exists.", this.decryptedFilePath),
            this.decryptedFilePath.toFile().exists());

        assertThat(this.decryptedFilePath).hasSameContentAs(clearFile.getPath());
    }

    @Test
    public void testNativeEncryptJavaDecrypt() {
        assertNotNull("gpgExecutable should present", this.nativeClearFile);

        final ClearFile clearFile = this.nativeClearFile;
        final EncryptedFile encryptedFile = FileSafe.decryptByBcpg(this.logger, this.encryptedFilePath);

        assertFalse(String.format("encryptedFile [%s] should not exists.", encryptedFile.getPath()),
            encryptedFile.getPath().toFile().exists());
        assertFalse(String.format("decryptedFile [%s] should not exists.", this.decryptedFilePath),
            this.decryptedFilePath.toFile().exists());

        final String passphrase = "gpg_passphrase";

        clearFile.encrypt(passphrase, encryptedFile.getPath());
        assertTrue(String.format("encryptedFile [%s] should exists.", encryptedFile.getPath()),
            encryptedFile.getPath().toFile().exists());
        assertFalse(String.format("decryptedFile [%s] should not exists.", this.decryptedFilePath),
            this.decryptedFilePath.toFile().exists());

        encryptedFile.decrypt(passphrase, this.decryptedFilePath);
        assertTrue(String.format("decryptedFile [%s] should exists.", this.decryptedFilePath),
            this.decryptedFilePath.toFile().exists());

        assertThat(this.decryptedFilePath).hasSameContentAs(clearFile.getPath());
    }
}
