package top.infra.maven.extension.ss;

import static top.infra.maven.shared.extension.Constants.SETTINGS_SECURITY_XML;
import static top.infra.maven.shared.extension.GlobalOption.MASTER_PASSWORD;

import cn.home1.tools.maven.MavenSettingsSecurity;
import cn.home1.tools.maven.MavenSettingsSecurityFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

import javax.inject.Inject;

import org.apache.maven.cli.CliRequest;
import org.apache.maven.cli.configuration.ConfigurationProcessor;
import org.apache.maven.eventspy.EventSpy;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

import top.infra.logging.Logger;
import top.infra.maven.extension.OrderedConfigurationProcessor;
import top.infra.maven.shared.extension.Constants;
import top.infra.maven.shared.extension.Orders;
import top.infra.maven.shared.logging.LoggerPlexusImpl;
import top.infra.maven.shared.utils.MavenUtils;

/**
 * {@link ConfigurationProcessor#process(CliRequest)} is called after {@link org.apache.maven.eventspy.EventSpy#init(EventSpy.Context)}.
 */
@Component(role = OrderedConfigurationProcessor.class, hint = "SettingsSecurityConfigurationProcessor")
// @Named
// @Singleton
public class SettingsSecurityConfigurationProcessor implements OrderedConfigurationProcessor {

    private Logger logger;

    @Requirement(hint = "maven")
    private SecDispatcher secDispatcher;

    @Inject
    public SettingsSecurityConfigurationProcessor(
        final org.codehaus.plexus.logging.Logger logger
    ) {
        this.logger = new LoggerPlexusImpl(logger);
    }

    @Override
    public int getOrder() {
        return Orders.ORDER_SETTINGS_SECURITY;
    }

    @Override
    public void process(final CliRequest cliRequest) throws Exception {
        final Optional<Path> settingsSecurityXml = MavenUtils
            .findInProperties(Constants.PROP_SETTINGS_SECURITY, cliRequest.getSystemProperties(), cliRequest.getUserProperties())
            .map(Paths::get)
            .map(Path::toAbsolutePath);

        if (logger.isInfoEnabled()) {
            logger.info(String.format("    Setting file [%s], using [%s].",
                SETTINGS_SECURITY_XML,
                settingsSecurityXml.map(Path::toString).orElse("not found")));
        }

        settingsSecurityXml.ifPresent(ss -> {
            final DefaultSecDispatcher defaultSecDispatcher = (DefaultSecDispatcher) this.secDispatcher;
            defaultSecDispatcher.setConfigurationFile(ss.toString());
        });

        testSecDispatcher(
            logger,
            cliRequest.getSystemProperties(),
            cliRequest.getUserProperties(),
            this.secDispatcher,
            settingsSecurityXml.map(Path::toString).orElse(null)
        );
    }

    public static void testSecDispatcher(
        final Logger logger,
        final Properties systemProperties,
        final Properties userProperties,
        final SecDispatcher secDispatcher,
        final String settingsSecurityXml
    ) {
        final MavenSettingsSecurity settingsSecurity = MASTER_PASSWORD
            .findInProperties(MASTER_PASSWORD.getPropertyName(), systemProperties, userProperties)
            .map(MavenSettingsSecurity::surroundByBrackets)
            .map(encodedMasterPassword -> new MavenSettingsSecurity(false, encodedMasterPassword))
            .orElseGet(() -> {
                return Optional.ofNullable(settingsSecurityXml)
                    .map(xml -> MavenSettingsSecurityFactory.newMavenSettingsSecurity(xml, false))
                    .orElse(null);
            });
        if (settingsSecurity != null) {
            logger.info("    Master password found, encode blank string.");
            final String plainText = " ";
            final String encrypted = settingsSecurity.encodeText(" ");
            try {
                final String decrypted = secDispatcher.decrypt(encrypted);
                if (!plainText.equals(decrypted)) {
                    throw new IllegalStateException(
                        String.format("    Failed on testing SecDispatcher, expected [%s], got [%s].", plainText, decrypted)
                    );
                }
            } catch (final SecDispatcherException ex) {
                throw new IllegalStateException("    Error on testing SecDispatcher.", ex);
            }
        }
    }
}
