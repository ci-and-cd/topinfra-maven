package top.infra.filesafe;

import static java.nio.file.Files.createTempFile;
import static top.infra.util.StringUtils.isEmpty;

import java.io.IOException;
import java.nio.file.Paths;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import top.infra.logging.Logger;
import top.infra.test.logging.LoggerSlf4jImpl;

public class EncryptedFileOpensslJavaTest {

    private Logger logger = new LoggerSlf4jImpl(LoggerFactory.getLogger(EncryptedFileOpensslJavaTest.class));

    @Test
    public void testDecrypt() throws IOException {
        final String CODESIGNING_ASC_ENC = System.getenv("CODESIGNING_ASC_ENC"); // codesigning.asc.enc
        final String passphrase = System.getenv("PASSPHRASE");

        if (!isEmpty(CODESIGNING_ASC_ENC) && !isEmpty(passphrase)) {
            logger.info(String.format("CODESIGNING_ASC_ENC: %s", CODESIGNING_ASC_ENC));

            final EncryptedFile codesigningAscEnc = FileSafe.decryptByJavaOpenssl(this.logger, Paths.get(CODESIGNING_ASC_ENC));
            if (codesigningAscEnc.exists()) {
                codesigningAscEnc.decrypt(passphrase, createTempFile("tmp", "codesigning.asc.enc"));
            } else {
                logger.info(String.format("    Skip decrypting [%s], file absent.", CODESIGNING_ASC_ENC));
            }
        }
    }
}
