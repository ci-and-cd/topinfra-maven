package top.infra.filesafe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import top.infra.logging.Logger;
import top.infra.test.logging.LoggerSlf4jImpl;
import top.infra.util.cli.CliUtils;

public class OpensslTest {

    private Logger logger;

    private final Path decryptedFilePath = Paths.get("src/test/resources/testfile.txt.out");
    private final Path plainFilePath = Paths.get("src/test/resources/testfile.txt");

    @Before
    public void setUp() throws IOException {
        this.logger = new LoggerSlf4jImpl(LoggerFactory.getLogger(OpensslTest.class));

        Files.deleteIfExists(this.decryptedFilePath);
    }

    @Test
    public void testOpenssl() throws IOException {
        assertEquals("openssl should present", Integer.valueOf(0), CliUtils.exec("which openssl").getKey());

        final Path encryptedFilePath = Paths.get("src/test/resources/testfile.txt.enc");
        final EncryptedFile encryptedFile = FileSafe.decryptByOpenssl(this.logger, encryptedFilePath);
        Files.deleteIfExists(encryptedFile.getPath());

        assertFalse(String.format("encryptedFile [%s] should not exists.", encryptedFile.getPath()),
            encryptedFile.getPath().toFile().exists());
        assertFalse(String.format("decryptedFile [%s] should not exists.", this.decryptedFilePath),
            this.decryptedFilePath.toFile().exists());

        final String passphrase = "openssl_passphrase";

        final ClearFile clearFile = FileSafe.encryptByOpenssl(this.logger, this.plainFilePath);
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
    public void testOpensslEncryptJavaDecrypt() throws IOException {
        assertEquals("openssl should present", Integer.valueOf(0), CliUtils.exec("which openssl").getKey());

        final Path encryptedFilePath = Paths.get("src/test/resources/testfile.txt.enc");
        final EncryptedFile encryptedFile = FileSafe.decryptByJava(this.logger, encryptedFilePath);
        Files.deleteIfExists(encryptedFile.getPath());

        assertFalse(String.format("encryptedFile [%s] should not exists.", encryptedFile.getPath()),
            encryptedFile.getPath().toFile().exists());
        assertFalse(String.format("decryptedFile [%s] should not exists.", this.decryptedFilePath),
            this.decryptedFilePath.toFile().exists());

        final String passphrase = "openssl_passphrase";

        final ClearFile clearFile = FileSafe.encryptByOpenssl(this.logger, this.plainFilePath);
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
    public void testJavaEncryptOpensslDecrypt() throws IOException {
        assertEquals("openssl should present", Integer.valueOf(0), CliUtils.exec("which openssl").getKey());

        final Path encryptedFilePath = Paths.get("src/test/resources/testfile.txt.enc");
        final EncryptedFile encryptedFile = FileSafe.decryptByOpenssl(this.logger, encryptedFilePath);
        Files.deleteIfExists(encryptedFile.getPath());

        assertFalse(String.format("encryptedFile [%s] should not exists.", encryptedFile.getPath()),
            encryptedFile.getPath().toFile().exists());
        assertFalse(String.format("decryptedFile [%s] should not exists.", this.decryptedFilePath),
            this.decryptedFilePath.toFile().exists());

        final String passphrase = "openssl_passphrase";

        final ClearFile clearFile = FileSafe.encryptByJava(this.logger, this.plainFilePath);
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
