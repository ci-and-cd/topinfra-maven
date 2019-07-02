package top.infra.maven.extension.infra;

import static java.lang.Boolean.FALSE;
import static top.infra.maven.Constants.SETTINGS_SECURITY_XML;
import static top.infra.maven.Constants.SETTINGS_XML;
import static top.infra.maven.Constants.SRC_MAIN_MAVEN;
import static top.infra.maven.core.CiOptionNames.systemPropertyName;
import static top.infra.maven.extension.InfraOption.CACHE_SETTINGS_PATH;
import static top.infra.maven.extension.InfraOption.SETTINGS;
import static top.infra.maven.extension.InfraOption.SETTINGS_SECURITY;
import static top.infra.maven.extension.InfraOption.TOOLCHAINS;
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
import org.apache.maven.cli.CLIManager;
import org.apache.maven.cli.CliRequest;
import org.apache.maven.eventspy.EventSpy.Context;
import org.apache.maven.settings.building.SettingsBuildingRequest;

import top.infra.maven.core.CiOption;
import top.infra.maven.core.CiOptionContext;
import top.infra.maven.core.GitPropertiesBeanFactory;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.extension.Orders;
import top.infra.maven.logging.Logger;
import top.infra.maven.logging.LoggerPlexusImpl;
import top.infra.maven.utils.FileUtils;
import top.infra.maven.utils.MavenUtils;
import top.infra.maven.utils.SystemUtils;

@Named
@Singleton
public class MavenSettingsFilesEventAware implements MavenEventAware {

    private final Logger logger;

    private final String remoteOriginUrl;

    private GitRepository gitRepository;

    private Path settingsXml;

    @Inject
    public MavenSettingsFilesEventAware(
        final org.codehaus.plexus.logging.Logger logger,
        final GitPropertiesBeanFactory gitPropertiesBeanFactory
    ) {
        this.logger = new LoggerPlexusImpl(logger);
        this.remoteOriginUrl = gitPropertiesBeanFactory.getObject().remoteOriginUrl().orElse(null);

        this.gitRepository = null;
        this.settingsXml = null;
    }

    @Override
    public int getOrder() {
        return Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_FILES;
    }

    @Override
    public void afterInit(final Context context, final CiOptionContext ciOptContext) {
        this.gitRepository = GitRepository.newGitRepository(ciOptContext, logger, this.remoteOriginUrl).orElse(null);

        logger.info(">>>>>>>>>> ---------- Setting file [settings.xml]. ---------- >>>>>>>>>>");
        this.settingsXml = this.findOrDownload(
            context,
            ciOptContext,
            SETTINGS,
            SRC_MAIN_MAVEN + "/" + SETTINGS_XML,
            SETTINGS_XML,
            false
        ).orElse(null);
        logger.info("<<<<<<<<<< ---------- Setting file [settings.xml]. ---------- <<<<<<<<<<");

        logger.info(">>>>>>>>>> ---------- Setting file [settings-security.xml]. ---------- >>>>>>>>>>");
        this.findOrDownload(
            context,
            ciOptContext,
            SETTINGS_SECURITY,
            SRC_MAIN_MAVEN + "/" + SETTINGS_SECURITY_XML,
            SETTINGS_SECURITY_XML,
            true
        );
        logger.info("<<<<<<<<<< ---------- Setting file [settings-security.xml]. ---------- <<<<<<<<<<");

        logger.info(">>>>>>>>>> ---------- Setting file [toolchains.xml]. ---------- >>>>>>>>>>");
        final String os = os();
        this.findOrDownload(
            context,
            ciOptContext,
            TOOLCHAINS,
            "generic".equals(os) ? "src/main/maven/toolchains.xml" : "src/main/maven/toolchains-" + os + ".xml",
            "toolchains.xml",
            false
        );
        logger.info("<<<<<<<<<< ---------- Setting file [toolchains.xml]. ---------- <<<<<<<<<<");
    }

    @Override
    public void onSettingsBuildingRequest(
        final SettingsBuildingRequest request,
        final CiOptionContext ciOptContext
    ) {
        if (this.settingsXml != null) {
            if (logger.isInfoEnabled()) {
                logger.info(String.format("Setting file [%s], using [%s]. (override userSettingsFile [%s])",
                    SETTINGS_XML, this.settingsXml, request.getUserSettingsFile()));
            }

            request.setUserSettingsFile(this.settingsXml.toFile());
        }
    }

    private Optional<Path> findOrDownload(
        final Context context,
        final CiOptionContext ciOptContext,
        final CiOption option,
        final String sourceFile,
        final String filename,
        final boolean optional
    ) {
        final String propertyName = option.getPropertyName();

        final Optional<String> cacheDir = CACHE_SETTINGS_PATH.getValue(ciOptContext);

        final boolean offline = MavenUtils.cmdArgOffline(context).orElse(FALSE);
        final boolean update = MavenUtils.cmdArgUpdate(context).orElse(FALSE);

        final Optional<Path> result;
        final Optional<Path> inM2 = userHomeDotM2(filename);
        final Optional<Path> foundAtLocal = findFile(context, filename, propertyName);
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

    private static Optional<Path> userHomeDotM2(final String filename) {
        final Path path = Paths.get(SystemUtils.systemUserHome(), ".m2", filename);
        return path.toFile().exists() ? Optional.of(path) : Optional.empty();
    }

    public static Optional<Path> findFile(
        final Context context,
        final String filename,
        final String propertyName

    ) {
        return Stream.of(
            SettingsFiles.findInUserProperties(MavenUtils.userProperties(context), propertyName),
            SettingsFiles.findInSystemProperties(MavenUtils.systemProperties(context), propertyName),
            SettingsFiles.findInWorkingDir(MavenUtils.executionRootPath(MavenUtils.systemProperties(context)), filename)
        )
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
    }

    private static class SettingsFiles {

        static Optional<Path> findFile(
            final CliRequest cliRequest,
            final String filename,
            final String propertyName

        ) {
            return Stream.of(
                SettingsFiles.findInUserProperties(cliRequest.getUserProperties(), propertyName),
                SettingsFiles.findInSystemProperties(cliRequest.getSystemProperties(), propertyName),
                SettingsFiles.findInWorkingDir(Paths.get(cliRequest.getWorkingDirectory()), filename),
                SettingsFiles.findInCustomSettingsDir(cliRequest, filename)
            )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
        }

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
            // TODO support read user settings from session context (include .mvn/maven.config)
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
