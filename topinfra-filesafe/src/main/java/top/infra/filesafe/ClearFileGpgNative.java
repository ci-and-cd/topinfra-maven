package top.infra.filesafe;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import top.infra.logging.Logger;

public class ClearFileGpgNative extends AbstractResource implements ClearFile {

    private final String gpgExecutable;
    private final Map<String, String> environment;

    public ClearFileGpgNative(final Logger logger, final Path path, final String gpgExecutable) {
        super(logger, path);
        this.gpgExecutable = gpgExecutable;

        final Map<String, String> env = new LinkedHashMap<>();
        env.put("LC_CTYPE", "UTF-8");
        this.environment = Collections.unmodifiableMap(env);
    }

    @Override
    public void encrypt(final String passphrase, final Path targetPath) {
        logger.info(String.format("    Encrypting [%s] by native gpg", this.getPath()));
        // echo ${CI_OPT_GPG_PASSPHRASE} |
        // gpg --yes --passphrase-fd 0 --cipher-algo AES256 --symmetric --no-symkey-cache --output src/test/resources/testfile.txt.enc
        // src/test/resources/testfile.txt

        final List<String> command;
        final Optional<String> gpgVersion = GpgUtils.gpgVersion(this.gpgExecutable);
        if (GpgUtils.gpgVersionGreater(gpgVersion.orElse(null), "2.2.6")) {
            command = GpgUtils.cmdGpgBatchYes(
                this.gpgExecutable,
                "--passphrase-fd", "0",
                "--cipher-algo", "AES256", "--symmetric", "--no-symkey-cache",
                "--output", targetPath.toString(),
                this.getPath().toString()
            );
        } else {
            command = GpgUtils.cmdGpgBatchYes(
                this.gpgExecutable,
                "--passphrase-fd", "0",
                "--cipher-algo", "AES256", "--symmetric",
                "--output", targetPath.toString(),
                this.getPath().toString()
            );
        }

        final Entry<Integer, Entry<String, String>> result = this.exec(passphrase, command);
        logger.info(String.format("    Encrypt [%s] by native gpg. code [%s], output: [%s][%s]",
            this.getPath(), result.getKey(), result.getValue().getKey(), result.getValue().getValue()));
    }

    @Override
    protected Map<String, String> getEnvironment() {
        return this.environment;
    }
}
