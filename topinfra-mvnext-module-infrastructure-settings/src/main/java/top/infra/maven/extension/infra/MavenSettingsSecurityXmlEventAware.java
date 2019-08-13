package top.infra.maven.extension.infra;

import static top.infra.maven.shared.extension.Constants.PROP_SETTINGS_SECURITY;
import static top.infra.maven.shared.extension.Constants.SETTINGS_SECURITY_XML;

import java.io.File;
import java.nio.file.Path;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.cli.CliRequest;

import top.infra.logging.Logger;
import top.infra.maven.CiOptionContext;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.shared.extension.Constants;
import top.infra.maven.shared.extension.Orders;
import top.infra.maven.shared.logging.LoggerPlexusImpl;
import top.infra.maven.shared.utils.MavenUtils;

@Named
@Singleton
public class MavenSettingsSecurityXmlEventAware implements MavenEventAware {

    static final String SRC_MAIN_MAVEN = "src/main/maven";

    private final Logger logger;
    private final CacheSettingsResourcesFactory resourcesFactory;

    private Path settingsSecurityXml;

    @Inject
    public MavenSettingsSecurityXmlEventAware(
        final org.codehaus.plexus.logging.Logger logger,
        final CacheSettingsResourcesFactory resourcesFactory
    ) {
        this.logger = new LoggerPlexusImpl(logger);
        this.resourcesFactory = resourcesFactory;

        this.settingsSecurityXml = null;
    }

    @Override
    public int getOrder() {
        return Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_FILES;
    }


    @Override
    public boolean afterInit() {
        return true;
    }

    @Override
    public void afterInit(
        final CliRequest cliRequest,
        final CiOptionContext ciOptContext
    ) {
        final Resources resources = this.resourcesFactory.getObject();
        this.settingsSecurityXml = resources.findOrDownload(
            cliRequest.getCommandLine(),
            true,
            Constants.PROP_SETTINGS_SECURITY,
            SRC_MAIN_MAVEN + "/" + SETTINGS_SECURITY_XML,
            SETTINGS_SECURITY_XML,
            userHomeDotM2(SETTINGS_SECURITY_XML),
            SRC_MAIN_MAVEN.replace("/", File.separator) + File.separator + SETTINGS_SECURITY_XML
        ).orElse(null);
        if (this.settingsSecurityXml != null) {
            if (logger.isInfoEnabled()) {
                logger.info(String.format(
                    "    Setting file [%s], using [%s]. (override system property [%s])",
                    SETTINGS_SECURITY_XML,
                    this.settingsSecurityXml,
                    cliRequest.getSystemProperties().getProperty(PROP_SETTINGS_SECURITY)));
            }
            cliRequest.getSystemProperties()
                .setProperty(PROP_SETTINGS_SECURITY, this.settingsSecurityXml.toAbsolutePath().toString());
            ciOptContext.getSystemProperties()
                .setProperty(PROP_SETTINGS_SECURITY, this.settingsSecurityXml.toAbsolutePath().toString());
        }
    }

    static String userHomeDotM2(final String filename) {
        final Path path = MavenUtils.userHomeDotM2().resolve(filename);
        return path.toFile().exists() ? path.toString() : null;
    }
}
