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

    //private final Path decryptedFilePath = Paths.get("src/test/resources/testfile.txt.out");
    //private final Path clearFilePath = Paths.get("src/test/resources/testfile.txt");

    @Before
    public void setUp() throws IOException {
        this.logger = new LoggerSlf4jImpl(LoggerFactory.getLogger(OpensslTest.class));
    }

    @Test
    public void testNative() throws IOException {
        assertEquals("openssl should present", Integer.valueOf(0), CliUtils.exec("which openssl").getKey());

        final String clearFilePath = "src/test/resources/testfile.txt";
        final Path encryptedFilePath = Paths.get(clearFilePath + ".enc");
        final EncryptedFile encryptedFile = FileSafe.decryptByNativeOpenssl(this.logger, encryptedFilePath);
        this.testEncryptAndDecrypt(clearFilePath, encryptedFile);
    }

    @Test
    public void testNativeEncryptJavaDecrypt() throws IOException {
        assertEquals("openssl should present", Integer.valueOf(0), CliUtils.exec("which openssl").getKey());

        final String testfileTxt = "src/test/resources/testfile.txt";
        this.testEncryptAndDecrypt(testfileTxt, FileSafe.decryptByJavaOpenssl(this.logger, Paths.get(testfileTxt + ".enc")));

        final String codesigningAsc = "../codesigning.asc";
        if (Paths.get(codesigningAsc).toFile().exists()) {
            final Path codesigningAscEnc = Files.createTempFile("tmp", "codesigning.asc");
            this.testEncryptAndDecrypt(codesigningAsc, FileSafe.decryptByJavaOpenssl(this.logger, codesigningAscEnc));
        }
    }

    @Test
    public void testJavaEncryptNativeDecrypt() throws IOException {
        assertEquals("openssl should present", Integer.valueOf(0), CliUtils.exec("which openssl").getKey());

        final String clearFilePath = "src/test/resources/testfile.txt";
        final Path encryptedFilePath = Paths.get(clearFilePath + ".enc");
        final EncryptedFile encryptedFile = FileSafe.decryptByNativeOpenssl(this.logger, encryptedFilePath);
        this.testEncryptAndDecrypt(clearFilePath, encryptedFile);
    }

    private void testEncryptAndDecrypt(final String clearFilePath, final EncryptedFile encryptedFile) throws IOException {
        final Path decryptedFilePath = Paths.get(clearFilePath + ".out");
        Files.deleteIfExists(decryptedFilePath);
        Files.deleteIfExists(encryptedFile.getPath());

        assertFalse(String.format("encryptedFile [%s] should not exists.", encryptedFile.getPath()),
            encryptedFile.getPath().toFile().exists());
        assertFalse(String.format("decryptedFile [%s] should not exists.", decryptedFilePath),
            decryptedFilePath.toFile().exists());

        final String passphrase = "openssl_passphrase";

        final ClearFile clearFile = FileSafe.encryptByNativeOpenssl(this.logger, Paths.get(clearFilePath));
        clearFile.encrypt(passphrase, encryptedFile.getPath());
        assertTrue(String.format("encryptedFile [%s] should exists.", encryptedFile.getPath()),
            encryptedFile.getPath().toFile().exists());
        assertFalse(String.format("decryptedFile [%s] should not exists.", decryptedFilePath),
            decryptedFilePath.toFile().exists());

        encryptedFile.decrypt(passphrase, decryptedFilePath);
        assertTrue(String.format("decryptedFile [%s] should exists.", decryptedFilePath),
            decryptedFilePath.toFile().exists());

        assertThat(decryptedFilePath).hasSameContentAs(clearFile.getPath());
    }
}
