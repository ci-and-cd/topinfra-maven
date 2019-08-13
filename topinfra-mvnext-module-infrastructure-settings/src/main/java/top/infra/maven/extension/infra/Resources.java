package top.infra.maven.extension.infra;

import static java.lang.Boolean.FALSE;
import static top.infra.maven.shared.extension.CiOptions.systemPropertyName;
import static top.infra.maven.shared.utils.SupportFunction.logEnd;
import static top.infra.maven.shared.utils.SupportFunction.logStart;
import static top.infra.util.StringUtils.isEmpty;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.maven.cli.CLIManager;
import org.jetbrains.annotations.Nullable;

import top.infra.logging.Logger;
import top.infra.maven.shared.utils.FileUtils;
import top.infra.maven.shared.utils.MavenUtils;

public class Resources {

    private final Logger logger;
    private final Path executionRootPath;
    private final String cacheDir;
    private final GitRepository gitRepository;
    private final Properties systemProperties;
    private final Properties userProperties;

    public Resources(
        final Logger logger,
        final Path executionRootPath,
        @Nullable final String cacheDir,
        @Nullable final GitRepository gitRepository,
        final Properties systemProperties,
        final Properties userProperties
    ) {
        this.logger = logger;
        this.executionRootPath = executionRootPath;
        this.cacheDir = cacheDir;
        this.gitRepository = gitRepository;
        this.systemProperties = systemProperties;
        this.userProperties = userProperties;
    }

    public Optional<Path> findOrDownload(
        final CommandLine commandLine,
        final boolean optional,
        final String propertyName,
        final String sourceFile,
        final String filename
    ) {
        return this.findOrDownload(commandLine, optional, propertyName, sourceFile, filename, null, null);
    }

    public Optional<Path> findOrDownload(
        final CommandLine commandLine,
        final boolean optional,
        final String propertyName,
        final String sourceFile,
        final String filename,
        @Nullable final String defaultFilePath,
        @Nullable final String alternativeFilePath
    ) {
        logger.info(logStart(this, "findOrDownload", filename));

        final boolean offline = MavenUtils.cmdArgOffline(commandLine);
        final boolean update = MavenUtils.cmdArgUpdateSnapshots(commandLine);

        final Optional<Path> foundAtLocal = findFile(
            this.executionRootPath,
            commandLine,
            this.systemProperties,
            this.userProperties,
            propertyName,
            filename,
            alternativeFilePath
        );
        final Optional<Path> defaultValue = isEmpty(defaultFilePath) ? Optional.empty() : Optional.of(Paths.get(defaultFilePath));
        final Optional<Path> result;
        final Optional<String> cacheDirOptional = Optional.ofNullable(this.cacheDir);
        if (cacheDirOptional.isPresent()) {
            final Path targetFile = Paths.get(cacheDirOptional.get(), filename);
            if (foundAtLocal.map(value -> !FileUtils.isSameFile(value, targetFile)).orElse(FALSE)) {
                result = foundAtLocal;
            } else if (this.gitRepository != null) {
                FileUtils.createDirectories(cacheDirOptional.get());
                this.gitRepository.download(sourceFile, targetFile, !optional, offline, update);
                if (targetFile.toFile().exists()) {
                    result = Optional.of(targetFile);
                } else {
                    result = defaultValue;
                }
            } else {
                result = defaultValue;
            }
        } else {
            if (foundAtLocal.isPresent()) {
                result = foundAtLocal;
            } else {
                result = defaultValue;
            }
        }

        if (result.isPresent()) {
            logger.info(String.format("    Resource file [%s], result [%s].", filename, result.get()));
            this.systemProperties.setProperty(propertyName, result.get().toString());
            this.userProperties.setProperty(propertyName, result.get().toString());
        } else {
            logger.info(String.format("    Resource file [%s], not found.", filename));
        }

        logger.info(logEnd(this, "findOrDownload", result.orElse(null), filename));
        return result;
    }

    public static Optional<Path> findFile(
        final Path executionRootPath,
        final CommandLine commandLine,
        final Properties systemProperties,
        final Properties userProperties,
        final String propertyName,
        final String filename,
        final String alternativeFilePath
    ) {
        return Stream.of(
            FindFiles.findInUserProperties(userProperties, propertyName),
            FindFiles.findInSystemProperties(systemProperties, propertyName),
            FindFiles.findInWorkingDir(executionRootPath, filename),
            isEmpty(alternativeFilePath)
                ? Optional.<Path>empty()
                : FindFiles.findInWorkingDir(executionRootPath, alternativeFilePath),
            FindFiles.findInCustomSettingsDir(commandLine, filename)
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
            return Optional.of(workingDirectory.resolve(filename))
                .filter(path -> path.toFile().exists());
        }

        private static Optional<Path> findInCustomSettingsDir(
            final CommandLine commandLine,
            final String filename
        ) {
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
