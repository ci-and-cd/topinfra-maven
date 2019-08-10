package top.infra.maven.extension.infra;

import static top.infra.maven.extension.infra.SettingFiles.SRC_MAIN_MAVEN;
import static top.infra.maven.shared.extension.Constants.PROP_SETTINGS_SECURITY;
import static top.infra.maven.shared.extension.Constants.SETTINGS_SECURITY_XML;

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

@Named
@Singleton
public class MavenSettingsSecurityXmlEventAware implements MavenEventAware {

    private final Logger logger;

    private SettingFiles settingFiles;
    private Path settingsSecurityXml;

    @Inject
    public MavenSettingsSecurityXmlEventAware(
        final org.codehaus.plexus.logging.Logger logger
    ) {
        this.logger = new LoggerPlexusImpl(logger);

        this.settingFiles = null;
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
        this.settingFiles = new SettingFiles(logger, ciOptContext);

        this.settingsSecurityXml = this.settingFiles.findOrDownload(
            cliRequest,
            ciOptContext,
            Constants.PROP_SETTINGS_SECURITY,
            SRC_MAIN_MAVEN + "/" + SETTINGS_SECURITY_XML,
            SETTINGS_SECURITY_XML,
            true
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
}
