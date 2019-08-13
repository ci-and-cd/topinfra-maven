package top.infra.maven.shared.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.maven.cli.CLIManager;
import org.apache.maven.cli.CliRequest;
import org.apache.maven.eventspy.EventSpy.Context;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblemCollectorRequest;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.profile.ProfileActivationContext;

import top.infra.maven.CiOptionContext;
import top.infra.maven.shared.extension.GlobalOption;

public abstract class MavenUtils {

    public static final String PROP_MAVEN_MULTIMODULEPROJECTDIRECTORY = "maven.multiModuleProjectDirectory";

    public static Boolean cmdArgOffline(final CommandLine commandLine) {
        return commandLine.hasOption(CLIManager.OFFLINE);
    }

    public static Boolean cmdArgUpdateSnapshots(final CommandLine commandLine) {
        return commandLine.hasOption(CLIManager.UPDATE_SNAPSHOTS);
    }

    public static Optional<String> findInProperties(
        final String propertyName,
        final CiOptionContext ciOptionContext
    ) {
        return MavenUtils.findInProperties(propertyName, ciOptionContext.getSystemProperties(), ciOptionContext.getUserProperties());
    }

    public static Optional<String> findInProperties(
        final String propertyName,
        final Properties systemProperties,
        final Properties userProperties
    ) {
        return GlobalOption.FAST.findInProperties(propertyName, systemProperties, userProperties);
    }

    /**
     * Report titled activator problem.
     */
    public static void reportProblem(
        final String title,
        final Exception error,
        final Profile profile,
        final ProfileActivationContext context,
        final ModelProblemCollector problems
    ) {
        final String message = String.format("%s: project='%s' profile='%s'", title, projectName(context), profileId(profile));
        registerProblem(message, error, profile.getLocation(""), problems);
    }

    /**
     * Inject new problem in reporter.
     */
    private static void registerProblem(
        final String message,
        final Exception error,
        final InputLocation location,
        final ModelProblemCollector problems
    ) {
        final ModelProblemCollectorRequest request = problemRequest()
            .setMessage(message)
            .setException(error)
            .setLocation(location);
        problems.add(request);
    }

    /**
     * Produce default problem descriptor.
     */
    private static ModelProblemCollectorRequest problemRequest() {
        return new ModelProblemCollectorRequest(ModelProblem.Severity.ERROR, ModelProblem.Version.BASE);
    }

    /**
     * Extract null-safe profile identity.
     */
    public static String profileId(final Profile profile) {
        return profile == null || profile.getId() == null ? "" : profile.getId();
    }

    /**
     * Extract optional project name from context.
     */
    public static String projectName(final ProfileActivationContext context) {
        final String missing = "<missing>";
        final File basedir = context.getProjectDirectory();
        if (basedir == null) {
            return missing;
        }

        final File pomFile = new File(basedir, "pom.xml");
        if (pomFile.exists()) {
            final Model model = readMavenModel(pomFile);
            final String artifactId = model.getArtifactId();
            if (artifactId != null) {
                return artifactId;
            } else {
                return missing;
            }
        } else {
            return basedir.getName();
        }
    }

    /**
     * Fail-safe pom.xml model reader.
     */
    private static Model readMavenModel(File pomFile) {
        try (final FileInputStream fileInput = new FileInputStream(pomFile)) {
            final InputStreamReader fileReader = new InputStreamReader(fileInput, StandardCharsets.UTF_8);

            final MavenXpp3Reader pomReader = new MavenXpp3Reader();
            return pomReader.read(fileReader);
        } catch (final Exception ex) {
            return new Model();
        }
    }

    /**
     * Extract profile property value.
     */
    private static String propertyValue(final Profile profile) {
        return profile.getActivation().getProperty().getValue();
    }

    public static Path executionRootPath(final CliRequest cliRequest) {
        return mavenMultiModuleProjectDirectory(cliRequest.getSystemProperties())
            .orElseGet(() -> {
                final Optional<Path> argFile = cmdArgFile(cliRequest).map(Paths::get).filter(path -> path.toFile().exists());
                final Path result;
                if (argFile.isPresent()) {
                    result = argFile.map(path -> path.toFile().isDirectory() ? path : path.getParent()).get();
                } else {
                    result = Paths.get(Optional.ofNullable(cliRequest.getWorkingDirectory()).orElseGet(SystemUtils::systemUserDir));
                }
                return result;
            });
    }

    public static Optional<String> cmdArgFile(final CliRequest cliRequest) {
        final Optional<String> result;
        final CommandLine commandLine = cliRequest.getCommandLine();
        if (commandLine.hasOption(CLIManager.ALTERNATE_POM_FILE)) {
            result = Optional.ofNullable(commandLine.getOptionValue(CLIManager.ALTERNATE_POM_FILE));
        } else {
            result = Optional.empty();
        }
        return result;
    }

    private static Optional<Path> mavenMultiModuleProjectDirectory(final Properties systemProperties) {
        final Optional<Path> result;
        if (systemProperties != null) {
            final String value = systemProperties.getProperty(PROP_MAVEN_MULTIMODULEPROJECTDIRECTORY);
            result = Optional.ofNullable(value != null ? Paths.get(value) : null);
        } else {
            result = Optional.empty();
        }
        return result;
    }

    public static Properties systemProperties(final Context context) {
        return (Properties) context.getData().get("systemProperties");
    }

    public static Path userHomeDotM2() {
        return Paths.get(SystemUtils.systemUserHome(), ".m2");
    }

    public static Properties userProperties(final Context context) {
        return (Properties) context.getData().get("userProperties");
    }
}
