package top.infra.filesafe;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import top.infra.logging.Logger;
import top.infra.util.StringUtils;

public class ClearFileOpensslNative extends AbstractResource implements ClearFile {

    private final Map<String, String> environment;

    public ClearFileOpensslNative(final Logger logger, final Path path) {
        super(logger, path);

        this.environment = Collections.emptyMap();
    }

    @Override
    public void encrypt(final String passphrase, final Path targetPath) {
        if (!StringUtils.isEmpty(passphrase) && this.getPath().toFile().exists()) {
            logger.info(String.format("    Encrypting [%s] by openssl", this.getPath()));

            final Map.Entry<Integer, String> opensslVersion = this.exec(Arrays.asList("openssl", "version", "-a"));
            logger.info(opensslVersion.getValue());

            // openssl aes-256-cbc -a -k ${CI_OPT_GPG_PASSPHRASE} -md md5 -salt -in codesigning.asc.enc -out codesigning.asc -p
            final Map.Entry<Integer, String> resultOpensslEncrypt = this.exec(Arrays.asList(
                "openssl", "aes-256-cbc",
                "-a", "-k", passphrase,
                "-md", "md5", "-salt",
                "-in", this.getPath().toString(),
                "-out", targetPath.toString()
            ));
            logger.info(resultOpensslEncrypt.getValue());
        }
    }

    @Override
    protected Map<String, String> getEnvironment() {
        return this.environment;
    }
}
