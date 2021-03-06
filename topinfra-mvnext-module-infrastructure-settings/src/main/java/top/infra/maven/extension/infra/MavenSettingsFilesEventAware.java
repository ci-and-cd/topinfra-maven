package top.infra.maven.extension.infra;

import static top.infra.maven.extension.infra.MavenSettingsSecurityXmlEventAware.SRC_MAIN_MAVEN;
import static top.infra.maven.extension.infra.MavenSettingsSecurityXmlEventAware.userHomeDotM2;
import static top.infra.maven.shared.utils.SystemUtils.os;

import java.io.File;
import java.nio.file.Path;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.building.FileSource;
import org.apache.maven.cli.CliRequest;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.toolchain.building.ToolchainsBuildingRequest;

import top.infra.logging.Logger;
import top.infra.maven.CiOptionContext;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.shared.extension.Constants;
import top.infra.maven.shared.extension.Orders;
import top.infra.maven.shared.logging.LoggerPlexusImpl;

@Named
@Singleton
public class MavenSettingsFilesEventAware implements MavenEventAware {

    private static final String SETTINGS_XML = "settings.xml";
    private static final String TOOLCHAINS_XML = toolchainsXml();

    private final Logger logger;
    private final CacheSettingsResourcesFactory resourcesFactory;

    private Path settingsXml;
    private Path toolchainsXml;

    @Inject
    public MavenSettingsFilesEventAware(
        final org.codehaus.plexus.logging.Logger logger,
        final CacheSettingsResourcesFactory resourcesFactory
    ) {
        this.logger = new LoggerPlexusImpl(logger);
        this.resourcesFactory = resourcesFactory;

        this.settingsXml = null;
        this.toolchainsXml = null;
    }

    @Override
    public int getOrder() {
        return Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_FILES;
    }

    @Override
    public boolean onSettingsBuildingRequest() {
        return true;
    }

    @Override
    public void onSettingsBuildingRequest(
        final CliRequest cliRequest,
        final SettingsBuildingRequest request,
        final CiOptionContext ciOptContext
    ) {
        final Resources resources = this.resourcesFactory.getObject();
        this.settingsXml = resources.findOrDownload(
            cliRequest.getCommandLine(),
            false,
            Constants.PROP_SETTINGS,
            SRC_MAIN_MAVEN + "/" + SETTINGS_XML,
            SETTINGS_XML,
            userHomeDotM2(SETTINGS_XML),
            SRC_MAIN_MAVEN.replace("/", File.separator) + File.separator + SETTINGS_XML
        ).orElse(null);
        if (this.settingsXml != null) {
            if (logger.isInfoEnabled()) {
                logger.info(String.format("    Setting file [%s], using [%s]. (override userSettingsFile [%s])",
                    SETTINGS_XML, this.settingsXml, request.getUserSettingsFile()));
            }
            request.setUserSettingsFile(this.settingsXml.toFile());
        }
    }

    @Override
    public boolean onToolchainsBuildingRequest() {
        return true;
    }

    @Override
    public void onToolchainsBuildingRequest(
        final CliRequest cliRequest,
        final ToolchainsBuildingRequest request,
        final CiOptionContext ciOptContext
    ) {
        final Resources resources = this.resourcesFactory.getObject();
        this.toolchainsXml = resources.findOrDownload(
            cliRequest.getCommandLine(),
            false,
            Constants.PROP_TOOLCHAINS,
            SRC_MAIN_MAVEN + "/" + TOOLCHAINS_XML,
            TOOLCHAINS_XML,
            userHomeDotM2(TOOLCHAINS_XML),
            SRC_MAIN_MAVEN.replace("/", File.separator) + File.separator + TOOLCHAINS_XML
        ).orElse(null);

        if (this.toolchainsXml != null) {
            if (logger.isInfoEnabled()) {
                logger.info(String.format("    Setting file [%s], using [%s]. (override userToolchainsSource [%s])",
                    TOOLCHAINS_XML, this.toolchainsXml, request.getUserToolchainsSource()));
            }

            request.setUserToolchainsSource(new FileSource(this.toolchainsXml.toFile()));
        }
    }

    private static String toolchainsXml() {
        final String os = os();
        return "generic".equals(os) ? "toolchains.xml" : "toolchains-" + os + ".xml";
    }
}
