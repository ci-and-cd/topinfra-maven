package top.infra.maven.shared.utils;

import java.nio.file.Path;
import java.nio.file.Paths;

import top.infra.maven.CiOptionContext;
import top.infra.maven.shared.extension.VcsProperties;

public class MavenBuildPomUtils {

    public static Path altDeploymentRepositoryPath(final CiOptionContext ciOptionContext) {
        final String commitId = VcsProperties.GIT_COMMIT_ID.getValue(ciOptionContext)
            .map(value -> value.substring(0, 8))
            .orElse("unknown-commit");

        final String executionRoot = MavenUtils
            .findInProperties(MavenUtils.PROP_MAVEN_MULTIMODULEPROJECTDIRECTORY, ciOptionContext)
            .orElse(SystemUtils.systemUserDir());

        // return Paths.get(executionRoot, ".mvn", "wagonRepository", "deferred");
        return Paths.get(executionRoot, ".mvn", "wagonRepository", "altDeployment", commitId);
    }
}
