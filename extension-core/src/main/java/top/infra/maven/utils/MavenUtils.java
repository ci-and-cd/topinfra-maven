package top.infra.maven.utils;

import static top.infra.maven.utils.SystemUtils.systemUserDir;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

import org.apache.maven.eventspy.EventSpy.Context;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblemCollectorRequest;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.profile.ProfileActivationContext;

public abstract class MavenUtils {

    public static Optional<Boolean> cmdArgOffline(final Context context) {
        // TODO full length arg
        return cmdArgOffline(systemProperties(context));
    }

    public static Optional<Boolean> cmdArgOffline(final Properties systemProperties) {
        final String value = systemProperties != null ? systemProperties.getProperty(ENV_MAVEN_CMD_LINE_ARGS) : null;
        return Optional.ofNullable(value != null ? value.contains(" -o ") : null);
    }

    public static Optional<Boolean> cmdArgUpdate(final Context context) {
        return cmdArgUpdate(systemProperties(context));
    }

    public static Optional<Boolean> cmdArgUpdate(final Properties systemProperties) {
        final String value = systemProperties != null ? systemProperties.getProperty(ENV_MAVEN_CMD_LINE_ARGS) : null;
        return Optional.ofNullable(value != null ? value.contains(" -U ") : null);
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

    public static final String PROP_MAVEN_MULTIMODULEPROJECTDIRECTORY = "maven.multiModuleProjectDirectory";

    public static Path executionRootPath(final Properties systemProperties) {
        return mavenMultiModuleProjectDirectory(systemProperties).orElseGet(() -> Paths.get(systemUserDir()));
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

    private static final String ENV_MAVEN_CMD_LINE_ARGS = "env.MAVEN_CMD_LINE_ARGS";

    public static Properties systemProperties(final Context context) {
        return (Properties) context.getData().get("systemProperties");
    }

    public static Properties userProperties(final Context context) {
        return (Properties) context.getData().get("userProperties");
    }
}
