package top.infra.maven.extension.infra;

import static java.lang.Boolean.FALSE;
import static top.infra.maven.extension.shared.CiOptionNames.systemPropertyName;
import static top.infra.maven.extension.shared.Constants.SETTINGS_SECURITY_XML;
import static top.infra.maven.extension.shared.InfraOption.CACHE_SETTINGS_PATH;
import static top.infra.maven.extension.shared.InfraOption.SETTINGS;
import static top.infra.maven.extension.shared.InfraOption.SETTINGS_SECURITY;
import static top.infra.maven.extension.shared.InfraOption.TOOLCHAINS;
import static top.infra.maven.extension.shared.VcsProperties.GIT_REMOTE_ORIGIN_URL;
import static top.infra.maven.utils.SystemUtils.os;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.cli.CommandLine;
import org.apache.maven.building.FileSource;
import org.apache.maven.cli.CLIManager;
import org.apache.maven.cli.CliRequest;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.toolchain.building.ToolchainsBuildingRequest;

import top.infra.maven.CiOption;
import top.infra.maven.CiOptionContext;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.extension.shared.Orders;
import top.infra.maven.logging.Logger;
import top.infra.maven.logging.LoggerPlexusImpl;
import top.infra.maven.utils.FileUtils;
import top.infra.maven.utils.MavenUtils;

@Named
@Singleton
public class MavenSettingsFilesEventAware implements MavenEventAware {

    private static final String SRC_MAIN_MAVEN = "src/main/maven";
    private static final String SETTINGS_XML = "settings.xml";
    private static final String TOOLCHAINS_XML = toolchainsXml();

    private final Logger logger;

    private GitRepository gitRepository;

    private Path settingsXml;
    private Path toolchainsXml;

    @Inject
    public MavenSettingsFilesEventAware(
        final org.codehaus.plexus.logging.Logger logger
    ) {
        this.logger = new LoggerPlexusImpl(logger);

        this.gitRepository = null;
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
        final String remoteOriginUrl = GIT_REMOTE_ORIGIN_URL.getValue(ciOptContext).orElse(null);
        this.gitRepository = GitRepository.newGitRepository(ciOptContext, logger, remoteOriginUrl).orElse(null);

        logger.info(">>>>>>>>>> ---------- Setting file [settings.xml]. ---------- >>>>>>>>>>");
        this.settingsXml = this.findOrDownload(
            cliRequest,
            ciOptContext,
            SETTINGS,
            SRC_MAIN_MAVEN + "/" + SETTINGS_XML,
            SETTINGS_XML,
            false
        ).orElse(null);
        logger.info("<<<<<<<<<< ---------- Setting file [settings.xml]. ---------- <<<<<<<<<<");

        logger.info(">>>>>>>>>> ---------- Setting file [settings-security.xml]. ---------- >>>>>>>>>>");
        this.findOrDownload(
            cliRequest,
            ciOptContext,
            SETTINGS_SECURITY,
            SRC_MAIN_MAVEN + "/" + SETTINGS_SECURITY_XML,
            SETTINGS_SECURITY_XML,
            true
        );
        logger.info("<<<<<<<<<< ---------- Setting file [settings-security.xml]. ---------- <<<<<<<<<<");

        if (this.settingsXml != null) {
            if (logger.isInfoEnabled()) {
                logger.info(String.format("Setting file [%s], using [%s]. (override userSettingsFile [%s])",
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
        logger.info(">>>>>>>>>> ---------- Setting file [toolchains.xml]. ---------- >>>>>>>>>>");
        this.toolchainsXml = this.findOrDownload(
            cliRequest,
            ciOptContext,
            TOOLCHAINS,
            SRC_MAIN_MAVEN + "/" + TOOLCHAINS_XML,
            TOOLCHAINS_XML,
            false
        ).orElse(null);
        logger.info("<<<<<<<<<< ---------- Setting file [toolchains.xml]. ---------- <<<<<<<<<<");

        if (this.toolchainsXml != null) {
            if (logger.isInfoEnabled()) {
                logger.info(String.format("Setting file [%s], using [%s]. (override userToolchainsSource [%s])",
                    TOOLCHAINS_XML, this.toolchainsXml, request.getUserToolchainsSource()));
            }

            request.setUserToolchainsSource(new FileSource(this.toolchainsXml.toFile()));
        }
    }

    private Optional<Path> findOrDownload(
        final CliRequest cliRequest,
        final CiOptionContext ciOptContext,
        final CiOption option,
        final String sourceFile,
        final String filename,
        final boolean optional
    ) {
        final String propertyName = option.getPropertyName();

        final Optional<String> cacheDir = CACHE_SETTINGS_PATH.getValue(ciOptContext);

        final boolean offline = MavenUtils.cmdArgOffline(cliRequest);
        final boolean update = MavenUtils.cmdArgUpdateSnapshots(cliRequest);

        final Optional<Path> result;
        final Optional<Path> inM2 = userHomeDotM2(filename);
        final Optional<Path> foundAtLocal = findFile(cliRequest, ciOptContext, filename, propertyName);
        if (cacheDir.isPresent()) {
            final Path targetFile = Paths.get(cacheDir.get(), filename);
            if (foundAtLocal.map(value -> !FileUtils.isSameFile(value, targetFile)).orElse(FALSE)) {
                result = foundAtLocal;
            } else if (this.gitRepository != null) {
                FileUtils.createDirectories(cacheDir.get());
                this.gitRepository.download(sourceFile, targetFile, !optional, offline, update);
                if (targetFile.toFile().exists()) {
                    result = Optional.of(targetFile);
                } else {
                    result = inM2;
                }
            } else {
                result = inM2;
            }
        } else {
            if (foundAtLocal.isPresent()) {
                result = foundAtLocal;
            } else {
                result = inM2;
            }
        }

        if (result.isPresent()) {
            logger.info(String.format("Setting file [%s], using [%s].", filename, result.get()));
            ciOptContext.getSystemProperties().setProperty(propertyName, result.get().toString());
            ciOptContext.getUserProperties().setProperty(propertyName, result.get().toString());
        } else {
            logger.info(String.format("Setting file [%s], not found.", filename));
        }

        return result;
    }

    private static String toolchainsXml() {
        final String os = os();
        return "generic".equals(os) ? "toolchains.xml" : "toolchains-" + os + ".xml";
    }

    private static Optional<Path> userHomeDotM2(final String filename) {
        final Path path = MavenUtils.userHomeDotM2().resolve(filename);
        return path.toFile().exists() ? Optional.of(path) : Optional.empty();
    }

    public static Optional<Path> findFile(
        final CliRequest cliRequest,
        final CiOptionContext ciOptContext,
        final String filename,
        final String propertyName
    ) {
        return Stream.of(
            SettingsFiles.findInUserProperties(ciOptContext.getUserProperties(), propertyName),
            SettingsFiles.findInSystemProperties(ciOptContext.getSystemProperties(), propertyName),
            SettingsFiles.findInWorkingDir(MavenUtils.executionRootPath(cliRequest, ciOptContext), filename),
            SettingsFiles.findInCustomSettingsDir(cliRequest, filename)
        )
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
    }

    private static class SettingsFiles {

        private static Optional<Path> findInUserProperties(
            final Properties userProperties,
            final String propertyName
        ) {
            return Optional.ofNullable(userProperties.getProperty(propertyName))
                .map(Paths::get)
                .filter(path -> path.toFile().exists());
        }

        private static Optional<Path> findInSystemProperties(
            final Properties systemProperties,
            final String propertyName
        ) {
            return Stream.of(
                systemProperties.getProperty(propertyName),
                systemProperties.getProperty(systemPropertyName(propertyName))
            )
                .filter(Objects::nonNull)
                .map(Paths::get)
                .filter(path -> path.toFile().exists())
                .findFirst();
        }

        private static Optional<Path> findInWorkingDir(
            final Path workingDirectory,
            final String filename
        ) {
            return Stream.of(
                workingDirectory.resolve(filename),
                workingDirectory.resolve(SRC_MAIN_MAVEN + "/" + filename)
            )
                .filter(path -> path.toFile().exists())
                .findFirst();
        }

        private static Optional<Path> findInCustomSettingsDir(
            final CliRequest cliRequest,
            final String filename
        ) {
            final CommandLine commandLine = cliRequest.getCommandLine();
            final Optional<Path> result;
            if (commandLine.hasOption(CLIManager.ALTERNATE_USER_SETTINGS)) {
                final Path settingsDir = Paths.get(commandLine.getOptionValue(CLIManager.ALTERNATE_USER_SETTINGS)).getParent();
                final Path fileInSettingsDir = settingsDir.resolve(filename);
                result = Optional.ofNullable(fileInSettingsDir.toFile().exists() ? fileInSettingsDir : null);
            } else {
                result = Optional.empty();
            }
            return result;
        }
    }
}
