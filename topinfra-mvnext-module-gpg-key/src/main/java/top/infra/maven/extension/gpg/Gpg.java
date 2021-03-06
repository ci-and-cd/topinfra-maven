package top.infra.maven.extension.gpg;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import top.infra.filesafe.EncryptedFile;
import top.infra.filesafe.FileSafe;
import top.infra.filesafe.GpgUtils;
import top.infra.logging.Logger;
import top.infra.maven.shared.utils.FileUtils;
import top.infra.maven.shared.utils.SupportFunction;
import top.infra.util.StringUtils;
import top.infra.util.cli.CliUtils;

public class Gpg {

    private static final String CODESIGNING_PUB = "codesigning.pub";
    private static final String CODESIGNING_ASC = "codesigning.asc";
    private static final String CODESIGNING_ASC_ENC = "codesigning.asc.enc";
    private static final String CODESIGNING_ASC_GPG = "codesigning.asc.gpg";
    private static final String DOT_GNUPG = ".gnupg";

    private final Logger logger;

    private final String executable;
    private final Path homeDir;
    private final Path executionRoot;
    private final String keyId;
    private final String keyName;
    private final String passphrase;

    private final Map<String, String> environment;

    public Gpg(
        final Logger logger,
        final Path homeDir,
        final Path executionRoot,
        final String executable,
        final String keyId,
        final String keyName,
        final String passphrase
    ) {
        this.logger = logger;
        this.executable = executable;
        this.homeDir = homeDir;
        this.executionRoot = executionRoot;
        this.keyId = keyId;
        this.keyName = keyName;
        this.passphrase = passphrase;

        if (logger.isInfoEnabled()) {
            logger.info(String.format("    Using %s", Arrays.toString(GpgUtils.gpgCommand(executable))));
        }

        this.environment = GpgUtils.gpgEnvironment();
        if (this.environment.containsKey("GPG_TTY")) {
            logger.info(String.format("    GPG_TTY=%s", this.environment.get("GPG_TTY")));
        }
    }

    public void decryptAndImportKeys() {
        final boolean encryptedKeysPresent = this.isFilePresent(CODESIGNING_ASC_ENC) || this.isFilePresent(CODESIGNING_ASC_GPG);

        if (encryptedKeysPresent) {
            // config gpg (version > 2.1)
            this.configFile();
        }

        if (encryptedKeysPresent) {
            this.gpgFindPrivateKey(this.keyName).ifPresent(privateKeyFound -> {
                if (!StringUtils.isEmpty(this.passphrase) && !privateKeyFound) {
                    // decrypt gpg key
                    this.decryptKey();
                }

                if (!privateKeyFound) {
                    this.importPublicKeys();

                    this.importPrivateKeys();
                }
            });
        } else {
            logger.info(String.format(
                "    Skip decrypting and importing keys. Encrypted keys %s absent.",
                Arrays.asList(CODESIGNING_ASC_ENC, CODESIGNING_ASC_GPG)));
        }
    }

    public void configFile() {
        final Optional<String> gpgVersion = GpgUtils.gpgVersion(this.executable);
        logger.info(String.format("[%s] version [%s]", this.executable, gpgVersion.orElse(null)));

        final Path dotGnupg = this.homeDir.resolve(DOT_GNUPG);
        final Path dotGnupgGpgConf = dotGnupg.resolve("gpg.conf");
        if (GpgUtils.gpgVersionGreater(gpgVersion.orElse(null), "2.1")) {
            logger.info("    gpg version greater than 2.1");
            try {
                Files.createDirectories(dotGnupg);
            } catch (final FileAlreadyExistsException ex) {
                // ignored
            } catch (final IOException ex) {
                if (logger.isWarnEnabled()) {
                    logger.warn(String.format("    %s%n%s", ex.getMessage(), SupportFunction.stackTrace(ex)));
                }
            }
            try {
                Files.setPosixFilePermissions(dotGnupg, PosixFilePermissions.fromString("rwx------"));
            } catch (final IOException ex) {
                if (logger.isWarnEnabled()) {
                    logger.warn(String.format("    %s%n%s", ex.getMessage(), SupportFunction.stackTrace(ex)));
                }
            }
            logger.info("    add 'use-agent' to '~/.gnupg/gpg.conf'");
            FileUtils.writeFile(dotGnupgGpgConf, "use-agent\n".getBytes(UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.SYNC, StandardOpenOption.TRUNCATE_EXISTING);
            if (logger.isInfoEnabled()) {
                logger.info(FileUtils.readFile(dotGnupgGpgConf, UTF_8).orElse(""));
            }

            if (GpgUtils.gpgVersionGreater(gpgVersion.orElse(null), "2.2")) {
                // on gpg-2.1.11 'pinentry-mode loopback' is invalid option
                logger.info("    add 'pinentry-mode loopback' to '~/.gnupg/gpg.conf'");
                FileUtils.writeFile(dotGnupgGpgConf, "pinentry-mode loopback\n".getBytes(UTF_8),
                    StandardOpenOption.APPEND, StandardOpenOption.SYNC);
                if (logger.isInfoEnabled()) {
                    logger.info(FileUtils.readFile(dotGnupgGpgConf, UTF_8).orElse(""));
                }
            }
            // gpg_cmd="${gpg_cmd} --pinentry-mode loopback"
            // export GPG_OPTS='--pinentry-mode loopback'
            // echo GPG_OPTS: ${GPG_OPTS}

            logger.info("    add 'allow-loopback-pinentry' to '~/.gnupg/gpg-agent.conf'");
            final Path dotGnupgGpgAgentConf = dotGnupg.resolve("gpg-agent.conf");
            FileUtils.writeFile(dotGnupgGpgAgentConf, "allow-loopback-pinentry\n".getBytes(UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.SYNC, StandardOpenOption.TRUNCATE_EXISTING);
            if (logger.isInfoEnabled()) {
                logger.info(FileUtils.readFile(dotGnupgGpgAgentConf, UTF_8).orElse(""));
            }

            logger.info("    restart the agent");
            this.exec("RELOADAGENT", singletonList("gpg-connect-agent"));
        }
    }

    public void decryptKey() {
        if (!StringUtils.isEmpty(this.passphrase)) {
            final EncryptedFile codesigningAscEnc = FileSafe.decryptByJavaOpenssl(this.logger, Paths.get(CODESIGNING_ASC_ENC));
            if (codesigningAscEnc.exists()) {
                codesigningAscEnc.decrypt(this.passphrase, Paths.get(CODESIGNING_ASC));
            } else {
                logger.info(String.format("    Skip decrypting [%s], file absent.", CODESIGNING_ASC_ENC));
            }

            final EncryptedFile codesigningAscGpg = FileSafe.decryptByBcpg(this.logger, Paths.get(CODESIGNING_ASC_GPG));
            if (codesigningAscGpg.exists()) {
                codesigningAscGpg.decrypt(this.passphrase, Paths.get(CODESIGNING_ASC));
            }
        } else {
            logger.info("    Skip decrypting [%s], passphraseEmpty");
        }
    }

    public void importPublicKeys() {
        if (this.isFilePresent(CODESIGNING_PUB)) {
            this.gpgImport(CODESIGNING_PUB, false);
            this.gpgListKeys(false);
        }
    }

    public void importPrivateKeys() {
        if (this.isFilePresent(CODESIGNING_ASC)) {
            logger.info("    import private keys");
            // some versions only can import public key from a keypair file, some can import key pair
            if (this.isFilePresent(CODESIGNING_PUB)) {
                this.gpgImport(CODESIGNING_ASC, false);
            } else {
                this.gpgFindPrivateKey(this.keyName).ifPresent(privateKeyFound -> {
                    if (!privateKeyFound) {
                        this.gpgImport(CODESIGNING_ASC, true);
                    }
                });

                this.gpgListKeys(true);

                //   issue: You need a passphrase to unlock the secret key
                //   no-tty causes "gpg: Sorry, no terminal at all requested - can't get input"
                // echo 'no-tty' >> ~/.gnupg/gpg.conf
                // echo 'default-cache-ttl 600' > ~/.gnupg/gpg-agent.conf
                //
                //   test key
                //   this test not working on appveyor
                //   gpg: skipped "KEYID": secret key not available
                //   gpg: signing failed: secret key not available
                // if [[ -f LICENSE ]]; then
                //     echo test private key imported
                //     echo ${CI_OPT_GPG_PASSPHRASE} | gpg --passphrase-fd 0 --yes --batch=true -u ${CI_OPT_GPG_KEYNAME} --armor --detach-sig LICENSE
                // fi

                this.gpgSetDefaultKey(this.keyName);

                if (!StringUtils.isEmpty(this.keyId)) {
                    logger.info("    export secret key for gradle build");
                    // ${gpg_cmd_batch} --keyring secring.gpg --export-secret-key ${CI_OPT_GPG_KEYID} > secring.gpg;
                    final List<String> exportKey = this.cmdGpgBatch("--keyring", "secring.gpg", "--export-secret-key", this.keyId);
                    final Entry<Integer, Entry<String, String>> resultExportKey = this.exec("", exportKey);
                    if (resultExportKey.getKey() == 0) {
                        final Path secringGpg = this.executionRoot.resolve("secring.gpg");
                        final String stdOut = resultExportKey.getValue().getKey();
                        FileUtils.writeFile(secringGpg, stdOut.getBytes(UTF_8), StandardOpenOption.SYNC);
                    }
                }
            }
        }
    }

    private List<String> cmdGpgBatchYes(final String... command) {
        return GpgUtils.cmdGpgBatchYes(this.executable, command);
    }

    private List<String> cmdGpgBatch(final String... command) {
        return GpgUtils.cmdGpgBatch(this.executable, command);
    }

    private Entry<Integer, Entry<String, String>> exec(final List<String> command) {
        return this.exec(null, command);
    }

    private Entry<Integer, Entry<String, String>> exec(final String stdIn, final List<String> command) {
        return CliUtils.exec(this.environment, stdIn, command);
    }

    public Optional<Boolean> gpgFindPrivateKey(final String keyName) {
        final Boolean found;

        if (!StringUtils.isEmpty(keyName)) {
            // $(${gpg_cmd} --list-secret-keys | { grep ${CI_OPT_GPG_KEYNAME} || true; }
            final Entry<Integer, Entry<String, String>> resultListSecKeys = this.exec(null, this.cmdGpgBatch("--list-secret-keys"));
            final String stdOut = resultListSecKeys.getValue().getKey();
            final List<String> secretKeyFound = StringUtils.lines(stdOut)
                .stream()
                .filter(line -> line.contains(keyName))
                .collect(Collectors.toList());

            found = !secretKeyFound.isEmpty();
            if (logger.isInfoEnabled()) {
                logger.info(String.format(
                    "    gpg%s found '%s' in '%s'. result: [%s]",
                    found ? "" : " not",
                    keyName,
                    resultListSecKeys.getValue(),
                    secretKeyFound));
            }
        } else {
            found = null;
        }

        return Optional.ofNullable(found);
    }

    private Entry<Integer, Entry<String, String>> gpgImport(final String keyFile, final boolean fastimport) {
        final String importOption = fastimport ? "--fast-import" : "--import";
        logger.info(String.format("    gpg %s %s", importOption, keyFile));
        // e.g.
        // ${gpg_cmd_batch_yes} --import codesigning.pub
        // ${gpg_cmd_batch_yes} --import codesigning.asc
        // ${gpg_cmd_batch_yes} --fast-import codesigning.asc
        final Entry<Integer, Entry<String, String>> result = this.exec(null, this.cmdGpgBatchYes(importOption, keyFile));
        logger.info(String.format("code [%s], [%s][%s]", result.getKey(), result.getValue().getKey(), result.getValue().getValue()));
        return result;
    }

    private Entry<Integer, Entry<String, String>> gpgListKeys(final boolean privateKey) {
        final String listOption = privateKey ? "--list-secret-keys" : "--list-keys";
        logger.info(String.format("    gpg %s", listOption));
        // ${gpg_cmd_batch} --list-keys
        // ${gpg_cmd_batch} --list-secret-keys
        final Entry<Integer, Entry<String, String>> result = this.exec(null, this.cmdGpgBatch(listOption));
        logger.info(String.format("code [%s], [%s][%s]", result.getKey(), result.getValue().getKey(), result.getValue().getValue()));
        return result;
    }

    private void gpgSetDefaultKey(final String keyName) {
        if (!StringUtils.isEmpty(keyName)) {
            logger.info(String.format("    gpg set default key to %s", keyName));
            // echo -e "trust\n5\ny\n" | gpg --cmd-fd 0 --batch=true --edit-key ${CI_OPT_GPG_KEYNAME}
            final List<String> command = this.cmdGpgBatch("--cmd-fd", "0", "--edit-key", keyName);
            final Entry<Integer, Entry<String, String>> result = this.exec("", command);
            logger.info(String.format("code [%s], [%s][%s]", result.getKey(), result.getValue().getKey(), result.getValue().getValue()));
        }
    }

    private boolean isFilePresent(final String file) {
        return SupportFunction.exists(this.executionRoot.resolve(file));
    }
}
