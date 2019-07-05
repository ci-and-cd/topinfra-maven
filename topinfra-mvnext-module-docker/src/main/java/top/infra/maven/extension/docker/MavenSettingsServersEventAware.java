package top.infra.maven.extension.docker;

import static top.infra.maven.utils.SupportFunction.isEmpty;

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

import top.infra.maven.CiOptionContext;
import top.infra.maven.extension.shared.InfraOption;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.extension.shared.Orders;
import top.infra.maven.logging.Logger;
import top.infra.maven.logging.LoggerPlexusImpl;
import top.infra.maven.utils.SupportFunction;

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

                if (!envVars.isEmpty()) {
                    final Optional<String> blankString = this.getEncryptedBlankString();
                    if (blankString.isPresent()) {
                        envVars.forEach(envVar -> {
                            logger.info(String.format(
                                "Write blank value for env variable [%s] (in settings.xml), to avoid passphrase decrypt error.",
                                envVar));
                            session.getSystemProperties().setProperty(envVar, blankString.get());
                        });
                    } else {
                        logger.info(String.format(
                            "Skip writting blank value for env variables [%s] (in settings.xml), settings-security.xml not found.",
                            envVars));
                    }
                }

                this.checkServers(settings.getServers());
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
        InfraOption.SETTINGS
            .findInProperties(ciOptContext.getSystemProperties(), ciOptContext.getUserProperties())
            .ifPresent(settingsXml -> {
                final Properties systemProperties = ciOptContext.getSystemProperties();
                final List<String> envVars = absentVarsInSettingsXml(Paths.get(settingsXml), systemProperties);
                envVars.forEach(envVar -> {
                    if (!systemProperties.containsKey(envVar)) {
                        logger.warn(String.format(
                            "Please set a value for env variable [%s] (in settings.xml), to avoid passphrase decrypt error.", envVar));
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
        final Optional<String> settingsSecurityXml = InfraOption.SETTINGS_SECURITY
            .findInProperties(ciOptContext.getSystemProperties(), ciOptContext.getUserProperties());
        final Optional<MavenSettingsSecurity> settingsSecurity = settingsSecurityXml
            .map(xml -> new MavenSettingsSecurity(xml, false));
        this.encryptedBlankString = settingsSecurity.map(ss -> ss.encodeText(" ")).orElse(null);
        this.checkServers(request.getServers());
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

    private void checkServers(final List<Server> servers) {
        // see: https://github.com/shyiko/servers-maven-extension/blob/master/src/main/java/com/github/shyiko/sme/ServersExtension.java
        for (final Server server : servers) {

            if (server.getPassphrase() != null) {
                this.serverPassphrase(server);
            }
            if (server.getPassword() != null) {
                this.serverPassword(server);
            }
            if (server.getUsername() != null) {
                this.serverUsername(server);
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

    private String replaceEmptyValue(final String value) {
        final String result;
        if (value == null) {
            result = null; // not a field
        } else if ("".equals(value)) {
            result = this.encryptedBlankString;
        } else {
            if (isSystemPropertyNameOfEnvVar(value)) {
                result = this.encryptedBlankString;
            } else {
                result = value;
            }
        }
        return result;
    }

    private void serverPassphrase(final Server server) {
        final String passphrase = this.replaceEmptyValue(server.getPassphrase());
        if (passphrase != null && !passphrase.equals(server.getPassphrase())) {
            logger.warn(String.format("server [%s] has a empty passphrase [%s]", server.getId(), server.getPassphrase()));
            server.setPassphrase(passphrase);
        }
    }

    private void serverPassword(final Server server) {
        final String password = this.replaceEmptyValue(server.getPassword());
        if (password != null && !password.equals(server.getPassword())) {
            logger.warn(String.format("server [%s] has a empty password [%s]", server.getId(), server.getPassword()));
            server.setPassword(password);
        }
    }

    private void serverUsername(final Server server) {
        final String username = this.replaceEmptyValue(server.getUsername());
        if (username != null && !username.equals(server.getUsername())) {
            logger.warn(String.format("server [%s] has a empty username [%s]", server.getId(), server.getUsername()));
            server.setUsername(username);
        }
    }

    /**
     * Find properties that absent in systemProperties but used in settings.xml.
     *
     * @param settingsXml      settings.xml
     * @param systemProperties systemProperties of current maven session
     * @return variables absent in systemProperties but used in settings.xml
     */
    private static List<String> absentVarsInSettingsXml(
        final Path settingsXml,
        final Properties systemProperties
    ) {
        return settingsXml != null
            ? SupportFunction.lines(Unix4j.cat(settingsXml.toFile()).toStringResult())
            .stream()
            .flatMap(line -> {
                final Matcher matcher = PATTERN_ENV_VAR.matcher(line);
                final List<String> matches = new LinkedList<>();
                while (matcher.find()) {
                    matches.add(matcher.group(0));
                }
                return matches.stream();
            })
            .distinct()
            .map(line -> line.substring(2, line.length() - 1))
            .collect(Collectors.toList())
            : Collections.emptyList();
    }
}
