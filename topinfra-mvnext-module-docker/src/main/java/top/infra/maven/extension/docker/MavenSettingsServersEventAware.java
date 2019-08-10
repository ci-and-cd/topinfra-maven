package top.infra.maven.extension.docker;

import static top.infra.util.StringUtils.isEmpty;

import cn.home1.tools.maven.MavenSettingsSecurity;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.cli.CliRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.unix4j.Unix4j;

import top.infra.logging.Logger;
import top.infra.maven.CiOptionContext;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.shared.extension.Constants;
import top.infra.maven.shared.extension.Orders;
import top.infra.maven.shared.logging.LoggerPlexusImpl;
import top.infra.maven.shared.utils.MavenUtils;
import top.infra.util.StringUtils;

/**
 * Auto fill empty or blank properties (e.g. CI_OPT_GPG_PASSPHRASE) in maven settings.xml.
 * Fix 'Failed to decrypt passphrase for server foo: org.sonatype.plexus.components.cipher.PlexusCipherException...'.
 */
@Named
@Singleton
public class MavenSettingsServersEventAware extends AbstractMavenLifecycleParticipant implements MavenEventAware {

    static final Pattern PATTERN_ENV_VAR = Pattern.compile("\\$\\{env\\..+?\\}");

    private final Logger logger;

    private final SettingsDecrypter settingsDecrypter;

    private String encryptedBlankString;

    @Inject
    public MavenSettingsServersEventAware(
        final org.codehaus.plexus.logging.Logger logger,
        final SettingsDecrypter settingsDecrypter
    ) {
        this.logger = new LoggerPlexusImpl(logger);

        this.settingsDecrypter = settingsDecrypter;

        this.encryptedBlankString = null;
    }

    /**
     * Method of {@link AbstractMavenLifecycleParticipant}.
     *
     * @param session maven session
     */
    @Override
    public void afterSessionStart(final MavenSession session) {
        if (session != null) {
            final Settings settings = session.getSettings();
            if (settings != null) {
                final List<String> envVars = settings.getServers()
                    .stream()
                    .flatMap(server -> this.absentEnvVars(server).stream())
                    .distinct()
                    .collect(Collectors.toList());

                final Properties systemProperties = session.getSystemProperties();

                if (!envVars.isEmpty()) {
                    final Optional<String> blankString = this.getEncryptedBlankString();
                    if (blankString.isPresent()) {
                        envVars.forEach(envVar -> {
                            logger.info(String.format(
                                "    Write blank value for env variable [%s] (in settings.xml), to avoid passphrase decrypt error.",
                                envVar));
                            systemProperties.setProperty(envVar, blankString.get());
                        });
                    } else {
                        logger.info(String.format(
                            "    Skip writting blank value for env variables [%s] (in settings.xml), settings-security.xml not found.",
                            envVars));
                    }
                }

                this.checkServers(settings.getServers(), systemProperties);
            }
        }
    }

    /**
     * Method of {@link MavenEventAware}.
     *
     * @return order
     */
    @Override
    public int getOrder() {
        return Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_SERVERS;
    }

    @Override
    public boolean onSettingsBuildingRequest() {
        return true;
    }

    /**
     * Method of {@link MavenEventAware}.
     *
     * @param cliRequest   cliRequest
     * @param request      SettingsBuildingRequest
     * @param ciOptContext ciOptContext
     */
    @Override
    public void onSettingsBuildingRequest(
        final CliRequest cliRequest,
        final SettingsBuildingRequest request,
        final CiOptionContext ciOptContext
    ) {
        MavenUtils.findInProperties(
            Constants.PROP_SETTINGS,
            ciOptContext.getSystemProperties(),
            ciOptContext.getUserProperties()
        )
            .ifPresent(settingsXml -> {
                final Properties systemProperties = ciOptContext.getSystemProperties();
                final List<String> envVars = envVarsInSettingsXml(Paths.get(settingsXml));
                envVars.forEach(envVar -> {
                    if (!systemProperties.containsKey(envVar)) {
                        logger.warn(String.format(
                            "    Please set a value for env variable [%s] (in settings.xml), to avoid passphrase decrypt error.", envVar));
                    }
                });
            });
    }

    @Override
    public boolean onMavenExecutionRequest() {
        return true;
    }

    /**
     * Method of {@link MavenEventAware}.
     *
     * @param request      request
     * @param ciOptContext ciOptContext
     */
    @Override
    public void onMavenExecutionRequest(
        final CliRequest cliRequest,
        final MavenExecutionRequest request,
        final CiOptionContext ciOptContext
    ) {
        final Optional<String> settingsSecurityXml = MavenUtils.findInProperties(Constants.PROP_SETTINGS_SECURITY, ciOptContext);
        final Optional<MavenSettingsSecurity> settingsSecurity = settingsSecurityXml
            .map(xml -> new MavenSettingsSecurity(xml, false));
        this.encryptedBlankString = settingsSecurity.map(ss -> ss.encodeText(" ")).orElse(null);
        this.checkServers(request.getServers(), ciOptContext.getSystemProperties());
    }

    private List<String> absentEnvVars(final Server server) {
        final List<String> found = new LinkedList<>();
        if (this.isSystemPropertyNameOfEnvVar(server.getPassphrase())) {
            found.add(server.getPassphrase());
        }
        if (this.isSystemPropertyNameOfEnvVar(server.getPassword())) {
            found.add(server.getPassword());
        }
        if (this.isSystemPropertyNameOfEnvVar(server.getUsername())) {
            found.add(server.getUsername());
        }
        return found.stream().map(line -> line.substring(2, line.length() - 1)).distinct().collect(Collectors.toList());
    }

    private void checkServers(final List<Server> servers, final Properties systemProperties) {
        // see: https://github.com/shyiko/servers-maven-extension/blob/master/src/main/java/com/github/shyiko/sme/ServersExtension.java
        for (final Server server : servers) {

            if (server.getPassphrase() != null) {
                this.serverPassphrase(server, systemProperties);
            }
            if (server.getPassword() != null) {
                this.serverPassword(server, systemProperties);
            }
            if (server.getUsername() != null) {
                this.serverUsername(server, systemProperties);
            }

            // final SettingsDecryptionRequest decryptionRequest = new DefaultSettingsDecryptionRequest(server);
            // final SettingsDecryptionResult decryptionResult = this.settingsDecrypter.decrypt(decryptionRequest);
            // final Server decryptedServer = decryptionResult.getServer();
        }
    }

    private Optional<String> getEncryptedBlankString() {
        return Optional.ofNullable(this.encryptedBlankString);
    }

    private boolean isSystemPropertyNameOfEnvVar(final String str) {
        return !isEmpty(str) && PATTERN_ENV_VAR.matcher(str).matches();
    }

    private String replaceEmptyValue(final String value, final Properties systemProperties) {
        final String result;
        if (value == null) {
            result = null; // not a field
        } else if ("".equals(value)) {
            result = this.encryptedBlankString;
        } else {
            if (isSystemPropertyNameOfEnvVar(value)) {
                final List<String> envVars = envVars(value);
                if (envVars.isEmpty()) {
                    result = this.encryptedBlankString;
                } else {
                    // TODO move this into topinfra-maven-dist
                    result = systemProperties.getProperty(envVars.get(0), this.encryptedBlankString);
                }
            } else {
                result = value;
            }
        }
        return result;
    }

    private void serverPassphrase(final Server server, final Properties systemProperties) {
        final String passphrase = this.replaceEmptyValue(server.getPassphrase(), systemProperties);
        if (passphrase != null && !passphrase.equals(server.getPassphrase())) {
            if (passphrase.equals(this.encryptedBlankString)) {
                logger.warn(String.format("    server [%s] has an empty passphrase [%s]", server.getId(), server.getPassphrase()));
            } else {
                logger.info(String.format("    server [%s] passphrase [%s] found in properties.", server.getId(), server.getPassphrase()));
            }
            server.setPassphrase(passphrase);
        }
    }

    private void serverPassword(final Server server, final Properties systemProperties) {
        final String password = this.replaceEmptyValue(server.getPassword(), systemProperties);
        if (password != null && !password.equals(server.getPassword())) {
            if (password.equals(this.encryptedBlankString)) {
                logger.warn(String.format("    server [%s] has an empty password [%s]", server.getId(), server.getPassword()));
            } else {
                logger.info(String.format("    server [%s] password [%s] found in properties.", server.getId(), server.getPassword()));
            }
            server.setPassword(password);
        }
    }

    private void serverUsername(final Server server, final Properties systemProperties) {
        final String username = this.replaceEmptyValue(server.getUsername(), systemProperties);
        if (username != null && !username.equals(server.getUsername())) {
            if (username.equals(this.encryptedBlankString)) {
                logger.warn(String.format("    server [%s] has an empty username [%s]", server.getId(), server.getUsername()));
            } else {
                logger.info(String.format("    server [%s] username [%s] found in properties.", server.getId(), server.getUsername()));
            }
            server.setUsername(username);
        }
    }

    private static List<String> envVars(final String text) {
        final Matcher matcher = PATTERN_ENV_VAR.matcher(text);
        final List<String> matches = new LinkedList<>();
        while (matcher.find()) {
            final String matched = matcher.group(0);
            matches.add(matched.substring(2, matched.length() - 1));
        }
        return matches;
    }

    /**
     * Find properties that absent in systemProperties but used in settings.xml.
     *
     * @param settingsXml settings.xml
     * @return variables absent in systemProperties but used in settings.xml
     */
    private static List<String> envVarsInSettingsXml(final Path settingsXml) {
        return settingsXml != null
            ? StringUtils.lines(Unix4j.cat(settingsXml.toFile()).toStringResult())
            .stream()
            .flatMap(line -> envVars(line).stream())
            .distinct()
            .collect(Collectors.toList())
            : Collections.emptyList();
    }
}
