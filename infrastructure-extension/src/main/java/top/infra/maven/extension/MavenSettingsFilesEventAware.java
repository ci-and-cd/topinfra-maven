package top.infra.maven.extension;

import static java.lang.Boolean.FALSE;
import static top.infra.maven.Constants.SRC_MAVEN_SETTINGS_SECURITY_XML;
import static top.infra.maven.Constants.SRC_MAVEN_SETTINGS_XML;
import static top.infra.maven.extension.InfraOption.CACHE_SETTINGS_PATH;
import static top.infra.maven.utils.SupportFunction.isNotEmpty;
import static top.infra.maven.utils.SystemUtils.os;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.eventspy.EventSpy.Context;
import org.apache.maven.settings.building.SettingsBuildingRequest;

import top.infra.maven.core.CiOptions;
import top.infra.maven.logging.Logger;
import top.infra.maven.logging.LoggerPlexusImpl;
import top.infra.maven.utils.FileUtils;
import top.infra.maven.utils.MavenUtils;

@Named
@Singleton
public class MavenSettingsFilesEventAware implements MavenEventAware {

    private final Logger logger;

    private GitRepository gitRepository;

    private String settingsXmlPathname;

    @Inject
    public MavenSettingsFilesEventAware(
        final org.codehaus.plexus.logging.Logger logger
    ) {
        this.logger = new LoggerPlexusImpl(logger);

        this.gitRepository = null;
        this.settingsXmlPathname = null;
    }

    @Override
    public int getOrder() {
        return Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_LOCALREPOSITORY;
    }

    @Override
    public void afterInit(final Context context, final CiOptions ciOpts) {
        this.settingsXmlPathname = ciOpts.getOption(InfraOption.MAVEN_SETTINGS_FILE).orElse(null);

        this.gitRepository = GitRepository.newGitRepository(ciOpts, logger).orElse(null);

        final boolean offline = MavenUtils.cmdArgOffline(context).orElse(FALSE);
        final boolean update = MavenUtils.cmdArgUpdate(context).orElse(FALSE);

        ciOpts.getOption(CACHE_SETTINGS_PATH).ifPresent(FileUtils::createDirectories);
        logger.info(">>>>>>>>>> ---------- download settings.xml and settings-security.xml ---------- >>>>>>>>>>");
        this.downloadSettingsXml(offline, update);
        this.downloadSettingsSecurityXml(offline, update);
        logger.info("<<<<<<<<<< ---------- download settings.xml and settings-security.xml ---------- <<<<<<<<<<");

        logger.info(">>>>>>>>>> ---------- download toolchains.xml ---------- >>>>>>>>>>");
        this.downloadToolchainsXml(offline, update);
        logger.info("<<<<<<<<<< ---------- download toolchains.xml ---------- <<<<<<<<<<");
    }

    @Override
    public void onSettingsBuildingRequest(
        final SettingsBuildingRequest request,
        final CiOptions ciOpts
    ) {
        if (this.settingsXmlPathname != null) {
            if (logger.isInfoEnabled()) {
                logger.info(String.format("Use userSettingsFile [%s] instead of [%s]",
                    this.settingsXmlPathname, request.getUserSettingsFile()));
            }

            request.setUserSettingsFile(new File(this.settingsXmlPathname));
        }
    }

    private void downloadSettingsXml(final boolean offline, final boolean update) {
        if (this.gitRepository != null) {
            // settings.xml
            final String targetFile = this.settingsXmlPathname;
            if (isNotEmpty(targetFile)) {
                this.gitRepository.download(SRC_MAVEN_SETTINGS_XML, targetFile, true, offline, update);
            }
        }
    }

    private void downloadSettingsSecurityXml(final boolean offline, final boolean update) {
        if (this.gitRepository != null) {
            // settings-security.xml (optional)
            final String targetFile = MavenUtils.settingsSecurityXml();
            this.gitRepository.download(SRC_MAVEN_SETTINGS_SECURITY_XML, targetFile, false, offline, update);
        }
    }

    private void downloadToolchainsXml(final boolean offline, final boolean update) {
        if (this.gitRepository != null) {
            // toolchains.xml
            final String os = os();
            final String sourceFile = "generic".equals(os)
                ? "src/main/maven/toolchains.xml"
                : "src/main/maven/toolchains-" + os + ".xml";
            final String targetFile = MavenUtils.toolchainsXml();
            this.gitRepository.download(sourceFile, targetFile, true, offline, update);
        }
    }
}
