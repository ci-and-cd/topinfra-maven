package top.infra.maven.core;

import static top.infra.maven.utils.SupportFunction.isEmpty;
import static top.infra.maven.utils.SupportFunction.notEmpty;

import java.util.Optional;
import java.util.Properties;

public class GitProperties {

    public static final String GIT_COMMIT_ID = "git.commit.id";
    public static final String GIT_REF_NAME = "git.ref.name";
    public static final String GIT_REMOTE_ORIGIN_URL = "git.remote.origin.url";

    private final Properties properties;

    protected GitProperties(final Properties properties) {
        this.properties = properties;
    }

    public static GitProperties newBlankGitProperties() {
        return newGitProperties(new Properties());
    }

    public static GitProperties newGitProperties(final Properties properties) {
        return new GitProperties(properties);
    }

    public Optional<String> commitId() {
        // `git rev-parse HEAD`
        final String value = this.properties.getProperty(GIT_COMMIT_ID);
        return isEmpty(value) ? Optional.empty() : Optional.of(value);
    }

    public Optional<String> refName() {
        final String value = this.properties.getProperty(GIT_REF_NAME);
        return notEmpty(value) ? Optional.of(value) : Optional.empty();
    }

    public Optional<String> remoteOriginUrl() {
        final String value = this.properties.getProperty(GIT_REMOTE_ORIGIN_URL);
        return notEmpty(value) ? Optional.of(value) : Optional.empty();
    }
}
