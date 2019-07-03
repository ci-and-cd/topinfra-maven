package top.infra.maven.extension.gpg;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.MULTILINE;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import top.infra.maven.logging.Logger;
import top.infra.maven.utils.FileUtils;
import top.infra.maven.utils.SupportFunction;
import top.infra.maven.utils.SystemUtils;

public class Gpg {

    private static final String CODESIGNING_PUB = "codesigning.pub";
    private static final String CODESIGNING_ASC = "codesigning.asc";
    private static final String CODESIGNING_ASC_ENC = "codesigning.asc.enc";
    private static final String CODESIGNING_ASC_GPG = "codesigning.asc.gpg";
    private static final String DOT_GNUPG = ".gnupg";

    private final Logger logger;

    private final Path homeDir;
    private final Path workingDir;
    private final String keyId;
    private final String keyName;
    private final String passphrase;

    private final String[] cmd;
    private final Map<String, String> environment;

    public Gpg(
        final Logger logger,
        final Path homeDir,
        final Path workingDir,
        final String executable,
        final String keyId,
        final String keyName,
        final String passphrase
    ) {
        this.logger = logger;
        this.homeDir = homeDir;
        this.workingDir = workingDir;
        this.keyId = keyId;
        this.keyName = keyName;
        this.passphrase = passphrase;

        this.cmd = "gpg2".equals(executable) ? new String[]{"gpg2", "--use-agent"} : new String[]{"gpg"};
        if (logger.isInfoEnabled()) {
            logger.info(String.format("Using %s", Arrays.toString(this.cmd)));
        }

        final Map<String, String> env = new LinkedHashMap<>();
        env.put("LC_CTYPE", "UTF-8");
        final Entry<Integer, String> tty = SystemUtils.exec("tty");
        if (tty.getKey() == 0) {
            if (logger.isInfoEnabled()) {
                logger.info(String.format("GPG_TTY=%s", tty.getValue()));
            }
            env.put("GPG_TTY", tty.getValue());
        }
        this.environment = Collections.unmodifiableMap(env);
    }

    public static boolean gpgVersionGreater(
        final String gpgVersionInfo,
        final String thanVersion
    ) {
        // e.g. "gpg (GnuPG) 2.2.14\nlibgcrypt 1.8.4\nCopyright ( ... pressed, ZIP, ZLIB, BZIP2"
        final Matcher matcher = Pattern
            .compile("[^0-9]*([0-9]+\\.[0-9]+\\.[0-9]+).*", DOTALL | MULTILINE)
            .matcher(gpgVersionInfo);

        final boolean result;
        if (matcher.matches()) {
            final String versionValue = matcher.group(1);
            result = new DefaultArtifactVersion(versionValue).compareTo(new DefaultArtifactVersion(thanVersion)) > 0;
        } else {
            result = false;
        }
        return result;
    }

    public void decryptAndImportKeys() {
        final boolean encryptedKeysPresent = this.isFilePresent(CODESIGNING_ASC_ENC) || this.isFilePresent(CODESIGNING_ASC_GPG);

        if (encryptedKeysPresent) {
            // config gpg (version > 2.1)
            this.configFile();
        }

        if (encryptedKeysPresent) {
            this.gpgFindPrivateKey(this.keyName).ifPresent(privateKeyFound -> {
                if (!SupportFunction.isEmpty(this.passphrase) && !privateKeyFound) {
                    // decrypt gpg key
                    this.decryptKey();
                }

                if (!privateKeyFound) {
                    this.importPublicKeys();

                    this.importPrivateKeys();
                }
            });
        }
    }

    public void configFile() {
        // use --batch=true to avoid 'gpg tty not a tty' error
        final Entry<Integer, String> resultGpgVersion = this.exec(this.cmdGpgBatch("--version"));
        logger.info(resultGpgVersion.getValue());

        final Path dotGnupg = this.homeDir.resolve(DOT_GNUPG);
        final Path dotGnupgGpgConf = dotGnupg.resolve("gpg.conf");
        if (gpgVersionGreater(resultGpgVersion.getValue(), "2.1")) {
            logger.info("gpg version greater than 2.1");
            try {
                Files.createDirectories(dotGnupg);
            } catch (final FileAlreadyExistsException ex) {
                // ignored
            } catch (final IOException ex) {
                if (logger.isWarnEnabled()) {
                    logger.warn(String.format("%s%n%s", ex.getMessage(), SupportFunction.stackTrace(ex)));
                }
            }
            try {
                Files.setPosixFilePermissions(dotGnupg, PosixFilePermissions.fromString("rwx------"));
            } catch (final IOException ex) {
                if (logger.isWarnEnabled()) {
                    logger.warn(String.format("%s%n%s", ex.getMessage(), SupportFunction.stackTrace(ex)));
                }
            }
            logger.info("add 'use-agent' to '~/.gnupg/gpg.conf'");
            FileUtils.writeFile(dotGnupgGpgConf, "use-agent\n".getBytes(UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.SYNC, StandardOpenOption.TRUNCATE_EXISTING);
            if (logger.isInfoEnabled()) {
                logger.info(FileUtils.readFile(dotGnupgGpgConf, UTF_8).orElse(""));
            }

            if (gpgVersionGreater(resultGpgVersion.getValue(), "2.2")) {
                // on gpg-2.1.11 'pinentry-mode loopback' is invalid option
                logger.info("add 'pinentry-mode loopback' to '~/.gnupg/gpg.conf'");
                FileUtils.writeFile(dotGnupgGpgConf, "pinentry-mode loopback\n".getBytes(UTF_8),
                    StandardOpenOption.APPEND, StandardOpenOption.SYNC);
                if (logger.isInfoEnabled()) {
                    logger.info(FileUtils.readFile(dotGnupgGpgConf, UTF_8).orElse(""));
                }
            }
            // gpg_cmd="${gpg_cmd} --pinentry-mode loopback"
            // export GPG_OPTS='--pinentry-mode loopback'
            // echo GPG_OPTS: ${GPG_OPTS}

            logger.info("add 'allow-loopback-pinentry' to '~/.gnupg/gpg-agent.conf'");
            final Path dotGnupgGpgAgentConf = dotGnupg.resolve("gpg-agent.conf");
            FileUtils.writeFile(dotGnupgGpgAgentConf, "allow-loopback-pinentry\n".getBytes(UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.SYNC, StandardOpenOption.TRUNCATE_EXISTING);
            if (logger.isInfoEnabled()) {
                logger.info(FileUtils.readFile(dotGnupgGpgAgentConf, UTF_8).orElse(""));
            }

            logger.info("restart the agent");
            this.exec("RELOADAGENT", singletonList("gpg-connect-agent"));
        }
    }

    public void decryptKey() {
        this.decryptByOpenSSL(CODESIGNING_ASC_ENC, CODESIGNING_ASC, this.passphrase);


        this.decryptByGpg(CODESIGNING_ASC_GPG, CODESIGNING_ASC, this.passphrase);
    }

    public void importPublicKeys() {
        if (this.isFilePresent(CODESIGNING_PUB)) {
            this.gpgImport(CODESIGNING_PUB, false);
            this.gpgListKeys(false);
        }
    }

    public void importPrivateKeys() {
        if (this.isFilePresent(CODESIGNING_ASC)) {
            logger.info("import private keys");
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

                if (!SupportFunction.isEmpty(this.keyId)) {
                    logger.info("export secret key for gradle build");
                    // ${gpg_cmd_batch} --keyring secring.gpg --export-secret-key ${CI_OPT_GPG_KEYID} > secring.gpg;
                    final List<String> exportKey = this.cmdGpgBatch("--keyring", "secring.gpg", "--export-secret-key", this.keyId);
                    final Entry<Integer, String> resultExportKey = this.exec("", exportKey);
                    if (resultExportKey.getKey() == 0) {
                        final Path secringGpg = this.workingDir.resolve("secring.gpg");
                        FileUtils.writeFile(secringGpg, resultExportKey.getValue().getBytes(UTF_8), StandardOpenOption.SYNC);
                    }
                }
            }
        }
    }

    private List<String> cmdGpgBatchYes(final String... command) {
        return SupportFunction.asList(SupportFunction.concat(this.cmd, "--batch=true", "--yes"), command);
    }

    private List<String> cmdGpgBatch(final String... command) {
        return SupportFunction.asList(SupportFunction.concat(this.cmd, "--batch=true"), command);
    }

    private void decryptByGpg(final String in, final String out, final String passphrase) {
        if (!SupportFunction.isEmpty(this.passphrase) && this.isFilePresent(in)) {
            logger.info("decrypt private key");
            // LC_CTYPE="UTF-8" echo ${CI_OPT_GPG_PASSPHRASE}
            //   | ${gpg_cmd_batch_yes} --passphrase-fd 0 --cipher-algo AES256 -o codesigning.asc codesigning.asc.gpg
            final List<String> gpgDecrypt = this.cmdGpgBatchYes(
                "--passphrase-fd", "0",
                "--cipher-algo", "AES256",
                "-o", out,
                in);
            final Entry<Integer, String> resultGpgDecrypt = this.exec(passphrase, gpgDecrypt);
            logger.info(resultGpgDecrypt.getValue());
        }
    }

    private void decryptByOpenSSL(final String in, final String out, final String passphrase) {
        if (!SupportFunction.isEmpty(this.passphrase) && this.isFilePresent(in)) {
            logger.info("decrypt private key by openssl");

            final Entry<Integer, String> opensslVersion = this.exec(Arrays.asList("openssl", "version", "-a"));
            logger.info(opensslVersion.getValue());

            // bad decrypt
            // 140611360391616:error:06065064:digital envelope routines:EVP_DecryptFinal_ex:bad decrypt:../crypto/evp/evp_enc.c:536:
            // see: https://stackoverflow.com/questions/34304570/how-to-resolve-the-evp-decryptfinal-ex-bad-decrypt-during-file-decryption
            // openssl aes-256-cbc -k ${CI_OPT_GPG_PASSPHRASE} -in codesigning.asc.enc -out codesigning.asc -d -md md5
            final Entry<Integer, String> resultOpensslDecrypt = this.exec(Arrays.asList(
                "openssl", "aes-256-cbc",
                "-k", passphrase,
                "-in", in,
                "-out", out,
                "-d", "-md", "md5"));
            logger.info(resultOpensslDecrypt.getValue());
        }
    }

    private Map.Entry<Integer, String> exec(final List<String> command) {
        return this.exec(null, command);
    }

    private Map.Entry<Integer, String> exec(final String stdIn, final List<String> command) {
        return SystemUtils.exec(this.environment, stdIn, command);
    }

    public Optional<Boolean> gpgFindPrivateKey(final String keyName) {
        final Boolean found;

        if (!SupportFunction.isEmpty(keyName)) {
            // $(${gpg_cmd} --list-secret-keys | { grep ${CI_OPT_GPG_KEYNAME} || true; }
            final Entry<Integer, String> resultListSecKeys = this.exec(null, this.cmdGpgBatch("--list-secret-keys"));
            final List<String> secretKeyFound = SupportFunction.lines(resultListSecKeys.getValue())
                .stream()
                .filter(line -> line.contains(keyName))
                .collect(Collectors.toList());

            found = !secretKeyFound.isEmpty();
            if (logger.isInfoEnabled()) {
                logger.info(String.format(
                    "gpg%s found '%s' in '%s'. result: [%s]",
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

    private Entry<Integer, String> gpgImport(final String keyFile, final boolean fastimport) {
        final String importOption = fastimport ? "--fast-import" : "--import";
        logger.info(String.format("gpg %s %s", importOption, keyFile));
        // e.g.
        // ${gpg_cmd_batch_yes} --import codesigning.pub
        // ${gpg_cmd_batch_yes} --import codesigning.asc
        // ${gpg_cmd_batch_yes} --fast-import codesigning.asc
        final Entry<Integer, String> result = this.exec(null, this.cmdGpgBatchYes(importOption, keyFile));
        logger.info(result.getValue());
        return result;
    }

    private Entry<Integer, String> gpgListKeys(final boolean privateKey) {
        final String listOption = privateKey ? "--list-secret-keys" : "--list-keys";
        logger.info(String.format("gpg %s", listOption));
        // ${gpg_cmd_batch} --list-keys
        // ${gpg_cmd_batch} --list-secret-keys
        final Entry<Integer, String> result = this.exec(null, this.cmdGpgBatch(listOption));
        logger.info(result.getValue());
        return result;
    }

    private void gpgSetDefaultKey(final String keyName) {
        if (!SupportFunction.isEmpty(keyName)) {
            logger.info(String.format("gpg set default key to %s", keyName));
            // echo -e "trust\n5\ny\n" | gpg --cmd-fd 0 --batch=true --edit-key ${CI_OPT_GPG_KEYNAME}
            final List<String> setDefaultKey = this.cmdGpgBatch("--cmd-fd", "0", "--edit-key", keyName);
            final Entry<Integer, String> resultSetDefaultKey = this.exec("", setDefaultKey);
            logger.info(resultSetDefaultKey.getValue());
        }
    }

    private boolean isFilePresent(final String file) {
        return SupportFunction.exists(this.workingDir.resolve(file));
    }
}
