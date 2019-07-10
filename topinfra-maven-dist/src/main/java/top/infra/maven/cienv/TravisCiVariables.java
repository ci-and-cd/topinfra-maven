package top.infra.maven.cienv;

import static top.infra.maven.cienv.GitlabCiVariables.NA;

import java.util.Optional;
import java.util.Properties;

public class TravisCiVariables {

    private final Properties systemProperties;

    public TravisCiVariables(final Properties systemProperties) {
        // if variable TRAVIS present
        this.systemProperties = systemProperties;
    }

    public boolean isPullRequestEvent() {
        return "pull_request".equals(this.eventType().orElse(""));
    }

    public Optional<String> eventType() {
        return this.getEnvironmentVariable("env.TRAVIS_EVENT_TYPE");
    }

    private Optional<String> getEnvironmentVariable(final String name) {
        return Optional.ofNullable(this.systemProperties.getProperty(name, null));
    }

    @Override
    public String toString() {
        return String.format(
            "travis-ci variables: TRAVIS_BRANCH: [%s], TRAVIS_EVENT_TYPE: [%s], TRAVIS_REPO_SLUG: [%s], TRAVIS_PULL_REQUEST: [%s]",
            this.branch().orElse(NA),
            this.eventType().orElse(NA),
            this.repoSlug().orElse(NA),
            this.pullRequest().orElse(NA));
    }

    public Optional<String> branch() {
        return this.getEnvironmentVariable("env.TRAVIS_BRANCH");
    }

    public Optional<String> pullRequest() {
        return this.getEnvironmentVariable("env.TRAVIS_PULL_REQUEST");
    }

    public Optional<String> repoSlug() {
        return this.getEnvironmentVariable("env.TRAVIS_REPO_SLUG");
    }
}
