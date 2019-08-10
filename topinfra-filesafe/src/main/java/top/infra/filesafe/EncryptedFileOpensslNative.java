package top.infra.filesafe;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import top.infra.logging.Logger;
import top.infra.util.StringUtils;

public class EncryptedFileOpensslNative extends AbstractResource implements EncryptedFile {

    private final Map<String, String> environment;

    public EncryptedFileOpensslNative(final Logger logger, final Path path) {
        super(logger, path);

        this.environment = Collections.emptyMap();
    }

    @Override
    protected Map<String, String> getEnvironment() {
        return this.environment;
    }

    @Override
    public void decrypt(final String passphrase, final Path targetPath) {
        if (!StringUtils.isEmpty(passphrase) && this.getPath().toFile().exists()) {
            logger.info(String.format("    Decrypting [%s] by openssl", this.getPath()));

            final Entry<Integer, String> opensslVersion = this.exec(Arrays.asList("openssl", "version", "-a"));
            logger.info(opensslVersion.getValue());

            // bad decrypt
            // 140611360391616:error:06065064:digital envelope routines:EVP_DecryptFinal_ex:bad decrypt:../crypto/evp/evp_enc.c:536:
            // see: https://stackoverflow.com/questions/34304570/how-to-resolve-the-evp-decryptfinal-ex-bad-decrypt-during-file-decryption
            // openssl aes-256-cbc -A -a -d -k ${CI_OPT_GPG_PASSPHRASE} -md md5 -in codesigning.asc.enc -out codesigning.asc -p
            final Entry<Integer, String> resultOpensslDecrypt = this.exec(Arrays.asList(
                "openssl", "aes-256-cbc",
                "-A", "-a",
                "-d", "-k", passphrase,
                "-md", "md5",
                "-in", this.getPath().toString(),
                "-out", targetPath.toString()
            ));
            logger.info(resultOpensslDecrypt.getValue());
        }
    }
}
