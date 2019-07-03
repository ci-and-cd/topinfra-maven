package top.infra.maven.cienv;

import java.util.Optional;
import java.util.Properties;

public class GitlabCiVariables {

    public static final String CI_PROJECT_URL = "env.CI_PROJECT_URL";
    static final String NA = "N/A";
    private final Properties systemProperties;

    public GitlabCiVariables(final Properties systemProperties) {
        this.systemProperties = systemProperties;
    }

    @Override
    public String toString() {
        return String.format(
            "gitlab-ci variables: CI_REF_NAME or CI_COMMIT_REF_NAME: [%s], CI_PROJECT_PATH: [%s], CI_PROJECT_URL: [%s]",
            this.refName().orElse(NA),
            this.repoSlug().orElse(NA),
            this.projectUrl().orElse(NA));
    }

    public Optional<String> repoSlug() {
        return this.projectPath();
    }

    private Optional<String> projectPath() {
        return this.getEnvironmentVariable("env.CI_PROJECT_PATH");
    }

    public Optional<String> projectUrl() {
        return this.ciProjectUrl();
    }

    public Optional<String> ciProjectUrl() {
        return this.getEnvironmentVariable(CI_PROJECT_URL);
    }

    /**
     * Gitlab-ci.
     * <br/>
     * ${CI_REF_NAME} show branch or tag since GitLab-CI 5.2<br/>
     * CI_REF_NAME for gitlab 8.x, see: https://gitlab.com/help/ci/variables/README.md<br/>
     * CI_COMMIT_REF_NAME for gitlab 9.x, see: https://gitlab.com/help/ci/variables/README.md<br/>
     *
     * @return ref name
     */
    public Optional<String> refName() {
        final Optional<String> result;

        final Optional<String> ciRefName = this.getEnvironmentVariable("env.CI_REF_NAME");
        if (ciRefName.isPresent()) {
            result = ciRefName;
        } else {
            result = this.commitRefName();
        }

        return result;
    }

    private Optional<String> commitRefName() {
        return this.getEnvironmentVariable("env.CI_COMMIT_REF_NAME");
    }

    private Optional<String> getEnvironmentVariable(final String name) {
        return Optional.ofNullable(this.systemProperties.getProperty(name, null));
    }
}
