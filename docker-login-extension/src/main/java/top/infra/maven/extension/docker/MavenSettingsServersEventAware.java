package top.infra.maven.extension.docker;

import static top.infra.maven.extension.InfraOption.MAVEN_SETTINGS_FILE;
import static top.infra.maven.utils.SupportFunction.isEmpty;
import static top.infra.maven.utils.SupportFunction.isNotEmpty;

import cn.home1.tools.maven.MavenSettingsSecurity;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.eventspy.EventSpy.Context;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.unix4j.Unix4j;

import top.infra.maven.core.CiOptionContext;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.extension.Orders;
import top.infra.maven.logging.Logger;
import top.infra.maven.logging.LoggerPlexusImpl;
import top.infra.maven.utils.MavenUtils;
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

    private MavenSettingsSecurity settingsSecurity;

    @Inject
    public MavenSettingsServersEventAware(
        final org.codehaus.plexus.logging.Logger logger,
        final SettingsDecrypter settingsDecrypter
    ) {
        this.logger = new LoggerPlexusImpl(logger);

        this.settingsDecrypter = settingsDecrypter;

        this.encryptedBlankString = null;
        this.settingsSecurity = null;
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
                envVars.forEach(envVar -> {
                    logger.info(
                        String.format("Set a value for env variable [%s] (in settings.xml), to avoid passphrase decrypt error.", envVar));
                    session.getSystemProperties().setProperty(envVar, this.getEncryptedBlankString());
                });

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

    /**
     * Method of {@link MavenEventAware}.
     *
     * @param context      context
     * @param ciOptContext ciOptContext
     */
    @Override
    public void afterInit(final Context context, final CiOptionContext ciOptContext) {
        final String settingsXmlPathname = MAVEN_SETTINGS_FILE.getValue(ciOptContext).orElse(null);

        final Properties systemProperties = (Properties) context.getData().get("systemProperties");
        // final Properties absentVarsInSettingsXml = absentVarsInSettingsXml(logger, settingsXmlPathname, systemProperties);
        // PropertiesUtils.merge(absentVarsInSettingsXml, systemProperties);

        final List<String> envVars = absentVarsInSettingsXml(logger, settingsXmlPathname, systemProperties);
        envVars.forEach(envVar -> {
            if (!systemProperties.containsKey(envVar)) {
                logger.warn(String.format(
                    "Please set a value for env variable [%s] (in settings.xml), to avoid passphrase decrypt error.", envVar));
            }
        });
    }

    /**
     * Method of {@link MavenEventAware}.
     *
     * @param request      request
     * @param ciOptContext ciOptContext
     */
    @Override
    public void onMavenExecutionRequest(final MavenExecutionRequest request, final CiOptionContext ciOptContext) {
        this.settingsSecurity = new MavenSettingsSecurity(MavenUtils.settingsSecurityXml(), false);
        this.encryptedBlankString = this.settingsSecurity.encodeText(" ");
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

    private String getEncryptedBlankString() {
        return this.encryptedBlankString;
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
     * @param logger              logger
     * @param settingsXmlPathname settings.xml
     * @param systemProperties    systemProperties of current maven session
     * @return variables absent in systemProperties but used in settings.xml
     */
    private static List<String> absentVarsInSettingsXml(
        final Logger logger,
        final String settingsXmlPathname,
        final Properties systemProperties
    ) {
        return isNotEmpty(settingsXmlPathname)
            ? SupportFunction.lines(Unix4j.cat(settingsXmlPathname).toStringResult())
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
