package top.infra.maven.extension.infra;

import static top.infra.maven.extension.infra.SettingFiles.SRC_MAIN_MAVEN;
import static top.infra.maven.utils.SystemUtils.os;

import java.nio.file.Path;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.building.FileSource;
import org.apache.maven.cli.CliRequest;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.toolchain.building.ToolchainsBuildingRequest;

import top.infra.maven.CiOptionContext;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.extension.shared.Constants;
import top.infra.maven.extension.shared.Orders;
import top.infra.maven.logging.Logger;
import top.infra.maven.logging.LoggerPlexusImpl;

@Named
@Singleton
public class MavenSettingsFilesEventAware implements MavenEventAware {

    private static final String SETTINGS_XML = "settings.xml";
    private static final String TOOLCHAINS_XML = toolchainsXml();

    private final Logger logger;

    private SettingFiles settingFiles;
    private Path settingsXml;
    private Path toolchainsXml;

    @Inject
    public MavenSettingsFilesEventAware(
        final org.codehaus.plexus.logging.Logger logger
    ) {
        this.logger = new LoggerPlexusImpl(logger);

        this.settingFiles = null;
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
        this.settingFiles = new SettingFiles(logger, ciOptContext);

        this.settingsXml = this.settingFiles.findOrDownload(
            cliRequest,
            ciOptContext,
            Constants.PROP_NAME_SETTINGS,
            SRC_MAIN_MAVEN + "/" + SETTINGS_XML,
            SETTINGS_XML,
            false
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
        this.toolchainsXml = this.settingFiles.findOrDownload(
            cliRequest,
            ciOptContext,
            Constants.PROP_NAME_TOOLCHAINS,
            SRC_MAIN_MAVEN + "/" + TOOLCHAINS_XML,
            TOOLCHAINS_XML,
            false
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
