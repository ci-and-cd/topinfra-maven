package top.infra.maven.cienv;

import static java.lang.Boolean.FALSE;
import static top.infra.maven.cienv.GitlabCiVariables.NA;

import java.util.Optional;
import java.util.Properties;

import top.infra.maven.utils.SupportFunction;

public class AppveyorVariables {

    public static final String APPVEYOR_PULL_REQUEST_HEAD_REPO_NAME = "env.APPVEYOR_PULL_REQUEST_HEAD_REPO_NAME";
    private static final String APPVEYOR_REPO_BRANCH = "env.APPVEYOR_REPO_BRANCH";
    private static final String APPVEYOR_REPO_NAME = "env.APPVEYOR_REPO_NAME";
    private static final String APPVEYOR_REPO_TAG = "env.APPVEYOR_REPO_TAG";
    private static final String APPVEYOR_REPO_TAG_NAME = "env.APPVEYOR_REPO_TAG_NAME";
    private final Properties systemProperties;

    public AppveyorVariables(final Properties systemProperties) {
        // if variable APPVEYOR present
        this.systemProperties = systemProperties;
    }

    public boolean isPullRequest() {
        return this.pullRequestHeadRepoName().map(SupportFunction::notEmpty).orElse(FALSE);
    }

    public Optional<String> pullRequestHeadRepoName() {
        return this.getEnvironmentVariable(APPVEYOR_PULL_REQUEST_HEAD_REPO_NAME);
    }

    private Optional<String> getEnvironmentVariable(final String name) {
        return Optional.ofNullable(this.systemProperties.getProperty(name, null));
    }

    public Optional<String> refName() {
        return this.repoTagName() ? this.repoTag() : this.repoBranch();
    }

    private Optional<String> repoBranch() {
        return this.getEnvironmentVariable(APPVEYOR_REPO_BRANCH);
    }

    private Optional<String> repoTag() {
        return this.getEnvironmentVariable(APPVEYOR_REPO_TAG);
    }

    public boolean repoTagName() {
        return this.getEnvironmentVariable(APPVEYOR_REPO_TAG_NAME).map(Boolean::parseBoolean).orElse(FALSE);
    }

    @Override
    public String toString() {
        return String.format(
            "appveyor variables: APPVEYOR_REPO_TAG_NAME: [%s], APPVEYOR_REPO_TAG: [%s], APPVEYOR_REPO_NAME: [%s], APPVEYOR_PULL_REQUEST_HEAD_REPO_NAME: [%s]",
            this.getEnvironmentVariable(APPVEYOR_REPO_TAG_NAME).orElse(NA),
            this.repoTag().orElse(NA),
            this.repoSlug().orElse(NA),
            this.pullRequestHeadRepoName().orElse(NA));
    }

    public Optional<String> repoSlug() {
        return this.repoName();
    }

    private Optional<String> repoName() {
        return this.getEnvironmentVariable(APPVEYOR_REPO_NAME);
    }
}
