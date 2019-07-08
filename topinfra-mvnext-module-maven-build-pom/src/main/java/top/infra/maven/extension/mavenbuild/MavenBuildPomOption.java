package top.infra.maven.extension.mavenbuild;

import static java.lang.Boolean.FALSE;
import static top.infra.maven.extension.shared.Constants.BOOL_STRING_FALSE;
import static top.infra.maven.extension.shared.Constants.BOOL_STRING_TRUE;
import static top.infra.maven.extension.shared.FastOption.FAST;
import static top.infra.maven.extension.shared.InfraOption.GIT_AUTH_TOKEN;

import java.util.Optional;
import java.util.Properties;

import top.infra.maven.CiOption;
import top.infra.maven.CiOptionContext;
import top.infra.maven.extension.shared.MavenOption;
import top.infra.maven.extension.shared.VcsProperties;

// TODO Move all options that depend on project properties to ProjectOption class.
public enum MavenBuildPomOption implements CiOption {

    DEPENDENCYCHECK("dependency-check") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            return Optional.of(FAST.getValue(context)
                .map(Boolean::parseBoolean)
                .filter(fast -> !fast)
                .map(fast -> BOOL_STRING_TRUE)
                .orElse(BOOL_STRING_FALSE));
        }
    },

    GIT_COMMIT_ID_SKIP("git.commit.id.skip"),
    GITHUB_GLOBAL_OAUTH2TOKEN("github.global.oauth2Token") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            return GIT_AUTH_TOKEN.getValue(context);
        }
    },
    GITHUB_GLOBAL_REPOSITORYNAME("github.global.repositoryName") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            return GITHUB_GLOBAL_REPOSITORYOWNER.calculateValue(context);
        }

        @Override
        public Optional<String> setProperties(final CiOptionContext context, final Properties properties) {
            final Optional<String> result = super.setProperties(context, properties);
            result.ifPresent(value ->
                context.getSystemProperties().setProperty(GITHUB_GLOBAL_REPOSITORYNAME.getSystemPropertyName(), value));
            return result;
        }
    },
    GITHUB_GLOBAL_REPOSITORYOWNER("github.global.repositoryOwner") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            final boolean generateReports = MavenOption.GENERATEREPORTS.getValue(context)
                .map(Boolean::parseBoolean).orElse(FALSE);

            return generateReports
                ? VcsProperties.gitRepoSlug(context)
                .map(slug -> slug.split("/")[0])
                : Optional.empty();
        }

        @Override
        public Optional<String> setProperties(final CiOptionContext context, final Properties properties) {
            final Optional<String> result = super.setProperties(context, properties);
            result.ifPresent(value ->
                context.getSystemProperties().setProperty(GITHUB_GLOBAL_REPOSITORYOWNER.getSystemPropertyName(), value));
            return result;
        }
    },

    GITHUB_SITE_PATH("github.site.path") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            final String refName = VcsProperties.GIT_REF_NAME.getValue(context).orElse("");
            final boolean releaseRef = VcsProperties.isReleaseRef(refName);
            final boolean snapshotRef = VcsProperties.isSnapshotRef(refName);
            return Optional.of(releaseRef ? "releases" : (snapshotRef ? "snapshots" : ""));
        }
    },
    GITHUB_SITE_PUBLISH("github.site.publish", BOOL_STRING_FALSE) {
        @Override
        public Optional<String> getValue(final CiOptionContext context) {
            final boolean generateReports = MavenOption.GENERATEREPORTS.getValue(context)
                .map(Boolean::parseBoolean).orElse(FALSE);

            return generateReports
                ? this.findInProperties(context.getSystemProperties(), context.getUserProperties())
                : Optional.of(BOOL_STRING_FALSE);
        }
    },

    /**
     * Run jacoco if true, skip jacoco and enable cobertura if false, skip bothe jacoco and cobertura if absent.
     */
    JACOCO("jacoco") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            final boolean skip = FAST.getValue(context)
                .map(Boolean::parseBoolean).orElse(FALSE);

            return Optional.ofNullable(skip ? null : BOOL_STRING_TRUE);
        }
    },

    NEXUS2_STAGING("nexus2.staging") {
        @Override
        public Optional<String> getValue(final CiOptionContext context) {
            final String refName = VcsProperties.GIT_REF_NAME.getValue(context).orElse("");
            final boolean releaseRef = VcsProperties.isReleaseRef(refName);
            return releaseRef ? super.getValue(context) : Optional.of(BOOL_STRING_FALSE);
        }
    },

    @Deprecated
    SITE_PATH("site.path"),

    SONAR("sonar"),

    @Deprecated
    CHECKSTYLE_CONFIG_LOCATION("checkstyle.config.location"),

    @Deprecated
    FRONTEND_NODEDOWNLOADROOT("frontend.nodeDownloadRoot"),
    @Deprecated
    FRONTEND_NPMDOWNLOADROOT("frontend.npmDownloadRoot"),

    @Deprecated
    PMD_RULESET_LOCATION("pmd.ruleset.location"),
    ;

    private final String defaultValue;
    private final String propertyName;

    MavenBuildPomOption(final String propertyName) {
        this(propertyName, null);
    }

    MavenBuildPomOption(final String propertyName, final String defaultValue) {
        this.defaultValue = defaultValue;
        this.propertyName = propertyName;
    }

    @Override
    public Optional<String> getDefaultValue() {
        return Optional.ofNullable(this.defaultValue);
    }

    @Override
    public String getPropertyName() {
        return this.propertyName;
    }
}
