package top.infra.maven.extension.infra;

import static java.lang.Boolean.FALSE;
import static top.infra.maven.extension.infra.InfraOption.CACHE_SETTINGS_PATH;
import static top.infra.maven.shared.extension.CiOptions.systemPropertyName;
import static top.infra.maven.shared.utils.SupportFunction.logEnd;
import static top.infra.maven.shared.utils.SupportFunction.logStart;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.maven.cli.CLIManager;
import org.apache.maven.cli.CliRequest;

import top.infra.maven.CiOptionContext;
import top.infra.maven.logging.Logger;
import top.infra.maven.shared.utils.FileUtils;
import top.infra.maven.shared.utils.MavenUtils;

public class SettingFiles {

    static final String SRC_MAIN_MAVEN = "src/main/maven";

    private final Logger logger;
    private final GitRepository gitRepository;

    public SettingFiles(
        final Logger logger,
        final CiOptionContext ciOptContext
    ) {
        this.logger = logger;
        this.gitRepository = GitRepository.newGitRepository(ciOptContext, logger).orElse(null);
    }

    public Optional<Path> findOrDownload(
        final CliRequest cliRequest,
        final CiOptionContext ciOptContext,
        final String propertyName,
        final String sourceFile,
        final String filename,
        final boolean optional
    ) {
        logger.info(logStart(this, "findOrDownload", filename));

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
            logger.info(String.format("    Setting file [%s], using [%s].", filename, result.get()));
            ciOptContext.getSystemProperties().setProperty(propertyName, result.get().toString());
            ciOptContext.getUserProperties().setProperty(propertyName, result.get().toString());
        } else {
            logger.info(String.format("    Setting file [%s], not found.", filename));
        }

        logger.info(logEnd(this, "findOrDownload", result.orElse(null), filename));
        return result;
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
            SettingFiles.FindFiles.findInUserProperties(ciOptContext.getUserProperties(), propertyName),
            SettingFiles.FindFiles.findInSystemProperties(ciOptContext.getSystemProperties(), propertyName),
            SettingFiles.FindFiles.findInWorkingDir(MavenUtils.executionRootPath(cliRequest, ciOptContext), filename),
            SettingFiles.FindFiles.findInCustomSettingsDir(cliRequest, filename)
        )
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
    }

    private static class FindFiles {

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
