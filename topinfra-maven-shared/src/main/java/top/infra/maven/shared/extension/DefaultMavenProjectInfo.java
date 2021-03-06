package top.infra.maven.shared.extension;

import static java.util.Collections.singletonList;
import static top.infra.maven.shared.utils.FileUtils.pathname;
import static top.infra.maven.shared.utils.SupportFunction.stackTrace;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import top.infra.logging.Logger;
import top.infra.maven.extension.MavenProjectInfo;

public class DefaultMavenProjectInfo implements MavenProjectInfo {

    private final String artifactId;
    private final String groupId;
    private final String packaging;
    private final String version;

    private final String javaVersion;

    public DefaultMavenProjectInfo(
        final String artifactId,
        final String groupId,
        final String packaging,
        final String version,
        final String javaVersion
    ) {
        this.artifactId = artifactId;
        this.groupId = groupId;
        this.packaging = packaging;
        this.version = version;

        this.javaVersion = javaVersion;
    }

    public static Optional<MavenProjectInfo> newProjectInfoByReadPom(
        final Logger logger,
        final File pomFile
    ) {
        final MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
        try {
            final Model model = xpp3Reader.read(new FileReader(pomFile));
            return Optional.of(new DefaultMavenProjectInfo(
                model.getArtifactId(),
                model.getGroupId(),
                model.getPackaging(),
                model.getVersion(),
                model.getProperties().getProperty("java.version")
            ));
        } catch (final IllegalArgumentException | IOException | XmlPullParserException ex) {
            if (logger.isWarnEnabled()) {
                logger.warn(String.format("    Failed to read project info from pomFile [%s] (by MavenXpp3Reader)",
                    pathname(pomFile)),
                    ex);
            }
            return Optional.empty();
        }
    }

    public static MavenProjectInfo newProjectInfoByBuildProject(
        final Logger logger,
        final ProjectBuilder projectBuilder,
        final File pomFile,
        final ProjectBuildingRequest projectBuildingRequest
    ) {
        // TODO FIXME set goals?
        final Optional<MavenProject> projectOptional = buildProject(logger, pomFile, projectBuilder, projectBuildingRequest);
        final String artifactId = projectOptional.map(MavenProject::getArtifactId).orElse(null);
        final String groupId = projectOptional.map(MavenProject::getGroupId).orElse(null);
        final String packaging = projectOptional.map(MavenProject::getPackaging).orElse(null);
        final String version = projectOptional.map(MavenProject::getVersion).orElse(null);
        final String javaVersion = projectOptional.map(project -> project.getProperties().getProperty("java.version")).orElse(null);
        return new DefaultMavenProjectInfo(
            artifactId,
            groupId,
            packaging,
            version,
            javaVersion
        );
    }

    public static Optional<MavenProject> buildProject(
        final Logger logger,
        final File pomFile,
        final ProjectBuilder projectBuilder,
        final ProjectBuildingRequest projectBuildingRequest
    ) {
        Optional<MavenProject> result;
        try {
            final ProjectBuildingRequest request = new DefaultProjectBuildingRequest(projectBuildingRequest);
            request.setActiveProfileIds(Collections.emptyList());
            request.setProcessPlugins(false);
            request.setProfiles(Collections.emptyList());
            request.setResolveDependencies(false);
            request.setValidationLevel(0);

            final List<ProjectBuildingResult> buildingResults = projectBuilder.build(
                singletonList(pomFile), false, request);

            result = Optional.of(buildingResults.get(0).getProject());
        } catch (final Exception ex) {
            if (logger.isWarnEnabled()) {
                logger.warn(String.format("    Error get project from pom %s. message: %s, stackTrace: %s",
                    pomFile.getPath(), ex.getMessage(), stackTrace(ex)));
            }
            result = Optional.empty();
        }
        return result;
    }

    /**
     * Same as {@link Model#getId()}.
     *
     * @return id
     */
    @Override
    public String getId() {
        final StringBuilder id = new StringBuilder(64);

        id.append((getGroupId() == null) ? "[inherited]" : getGroupId());
        id.append(":");
        id.append(getArtifactId());
        id.append(":");
        id.append(getPackaging());
        id.append(":");
        id.append((getVersion() == null) ? "[inherited]" : getVersion());

        return id.toString();
    }

    @Override
    public String getArtifactId() {
        return this.artifactId;
    }

    @Override
    public String getGroupId() {
        return this.groupId;
    }

    @Override
    public String getJavaVersion() {
        return this.javaVersion;
    }

    @Override
    public String getPackaging() {
        return this.packaging;
    }

    @Override
    public String getVersion() {
        return this.version;
    }

    @Override
    public boolean idEquals(final Model model) {
        return model != null && this.getId().equals(model.getId());
    }

    @Override
    public boolean idEqualsExceptInheritedGroupId(final Model model) {
        // e.g. [[inherited]:artifact-id:pom:0.0.1-SNAPSHOT], model: [id.group:artifact-id:jar:0.0.1-SNAPSHOT]

        final String id = this.getId();

        final boolean result;
        if (id.startsWith("[inherited]:") && model != null) {
            final String modelId = model.getId();
            result = id.substring(id.indexOf(':')).equals(modelId.substring(modelId.indexOf(':')));
        } else {
            result = false;
        }
        return result;
    }

    /**
     * Same as {@link Model#toString()}.
     *
     * @return id
     */
    @Override
    public String toString() {
        return this.getId();
    }
}
