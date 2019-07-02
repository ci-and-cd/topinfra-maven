package top.infra.maven.extension;

import static top.infra.maven.Constants.SETTINGS_SECURITY_XML;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.cli.CliRequest;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

import top.infra.maven.logging.Logger;
import top.infra.maven.logging.LoggerPlexusImpl;

// @Component(role = OrderedConfigurationProcessor.class, hint = "settings-security")
@Named
@Singleton
public class SettingsSecurityConfigurationProcessor implements OrderedConfigurationProcessor {

    private Logger logger;

    // @Requirement
    private SecDispatcher secDispatcher;

    @Inject
    public SettingsSecurityConfigurationProcessor(
        final org.codehaus.plexus.logging.Logger logger,
        final SecDispatcher secDispatcher
    ) {
        this.logger = new LoggerPlexusImpl(logger);
        this.secDispatcher = secDispatcher;
    }

    @Override
    public int getOrder() {
        return Orders.ORDER_SETTINGS_SECURITY;
    }

    @Override
    public void process(final CliRequest cliRequest) throws Exception {
        final Optional<Path> settingsSecurity = InfraOption.SETTINGS_SECURITY
            .findInProperties(cliRequest.getSystemProperties(), cliRequest.getUserProperties())
            .map(Paths::get);

        if (logger.isInfoEnabled()) {
            logger.info(String.format("Setting file [%s], using [%s].",
                SETTINGS_SECURITY_XML,
                settingsSecurity.map(Path::toString).orElse("not found")));
        }

        settingsSecurity.ifPresent(ss -> {
            final DefaultSecDispatcher defaultSecDispatcher = (DefaultSecDispatcher) this.secDispatcher;
            defaultSecDispatcher.setConfigurationFile(ss.toAbsolutePath().toString());
        });
    }
}
