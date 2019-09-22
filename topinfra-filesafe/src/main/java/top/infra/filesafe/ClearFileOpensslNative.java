package top.infra.filesafe;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

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

            final Entry<Integer, Entry<String, String>> opensslVersion = this.exec(Arrays.asList("openssl", "version", "-a"));
            logger.info(String.format("code [%s], [%s][%s]",
                opensslVersion.getKey(), opensslVersion.getValue().getKey(), opensslVersion.getValue().getValue()));

            // openssl aes-256-cbc -a -k ${CI_OPT_GPG_PASSPHRASE} -md md5 -salt -in codesigning.asc -out codesigning.asc.enc -p
            final Entry<Integer, Entry<String, String>> result = this.exec(Arrays.asList(
                "openssl", "aes-256-cbc",
                "-a", "-k", passphrase,
                "-md", "md5", "-salt",
                "-in", this.getPath().toString(),
                "-out", targetPath.toString()
            ));
            logger.info(String.format("    Encrypt [%s] by openssl. code [%s], output: [%s][%s]",
                this.getPath(), result.getKey(), result.getValue().getKey(), result.getValue().getValue()));
        }
    }

    @Override
    protected Map<String, String> getEnvironment() {
        return this.environment;
    }
}
