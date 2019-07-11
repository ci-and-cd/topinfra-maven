package top.infra.maven.extension.mavenbuild;

import static java.lang.Boolean.FALSE;
import static top.infra.maven.extension.shared.Constants.BOOL_STRING_FALSE;
import static top.infra.maven.extension.shared.Constants.BOOL_STRING_TRUE;
import static top.infra.maven.extension.shared.Constants.PROP_MVN_DEPLOY_PUBLISH_SEGREGATION_GOAL_DEPLOY;
import static top.infra.maven.extension.shared.GlobalOption.FAST;
import static top.infra.maven.extension.shared.GlobalOption.getInfrastructureSpecificValue;
import static top.infra.maven.extension.shared.GlobalOption.setInfrastructureSpecificValue;

import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

import top.infra.maven.CiOption;
import top.infra.maven.CiOptionContext;
import top.infra.maven.extension.shared.Constants;
import top.infra.maven.extension.shared.MavenOption;
import top.infra.maven.extension.shared.VcsProperties;
import top.infra.maven.utils.MavenUtils;
import top.infra.maven.utils.SystemUtils;

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
    GITHUB_GLOBAL_OAUTH2TOKEN("github.global.oauth2Token", "${git.auth.token}"),
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
    /**
     * Need a explicit 'false' default value to activate profiles.
     */
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
            final boolean skip = FAST.getValue(context).map(Boolean::parseBoolean).orElse(FALSE);

            return Optional.ofNullable(skip ? null : BOOL_STRING_TRUE);
        }
    },

    NEXUS2_STAGING(Constants.PROP_NEXUS2_STAGING),

    MVN_DEPLOY_PUBLISH_SEGREGATION_GOAL_DEPLOY(PROP_MVN_DEPLOY_PUBLISH_SEGREGATION_GOAL_DEPLOY) {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            final Optional<Boolean> nexus2Staging = NEXUS2_STAGING.getValue(context).map(Boolean::parseBoolean);
            return nexus2Staging.map(staging -> staging ? BOOL_STRING_FALSE : null);
        }
    },

    NOTGENERATEREPORTS("notGenerateReports") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            final Optional<String> generateReports = MavenOption.GENERATEREPORTS.getValue(context);
            return Optional.of(String.valueOf(generateReports.map(generate -> !Boolean.parseBoolean(generate)).orElse(FALSE)));
        }
    },

    RELEASEREF("releaseRef") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            final String refName = VcsProperties.GIT_REF_NAME.getValue(context).orElse("");
            return Optional.of(String.valueOf(VcsProperties.isReleaseRef(refName)));
        }
    },

    SITE_PATH("site.path"),

    SNAPSHOTREF("snapshotRef") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            final String refName = VcsProperties.GIT_REF_NAME.getValue(context).orElse("");
            return Optional.of(String.valueOf(VcsProperties.isSnapshotRef(refName)));
        }
    },

    SONAR("sonar"),

    CHECKSTYLE_CONFIG_LOCATION("checkstyle.config.location") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            return getInfrastructureSpecificValue(this, context);
        }

        @Override
        public Optional<String> setProperties(final CiOptionContext context, final Properties properties) {
            return setInfrastructureSpecificValue(this, super::setProperties, context, properties);
        }
    },
    FRONTEND_NODEDOWNLOADROOT("frontend.nodeDownloadRoot") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            return getInfrastructureSpecificValue(this, context);
        }

        @Override
        public Optional<String> setProperties(final CiOptionContext context, final Properties properties) {
            return setInfrastructureSpecificValue(this, super::setProperties, context, properties);
        }
    },
    FRONTEND_NPMDOWNLOADROOT("frontend.npmDownloadRoot") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            return getInfrastructureSpecificValue(this, context);
        }

        @Override
        public Optional<String> setProperties(final CiOptionContext context, final Properties properties) {
            return setInfrastructureSpecificValue(this, super::setProperties, context, properties);
        }
    },
    PMD_RULESET_LOCATION("pmd.ruleset.location") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            return getInfrastructureSpecificValue(this, context);
        }

        @Override
        public Optional<String> setProperties(final CiOptionContext context, final Properties properties) {
            return setInfrastructureSpecificValue(this, super::setProperties, context, properties);
        }
    },

    /**
     * Need to calculate this in extension for profile activation.
     */
    WAGON_MERGEMAVENREPOS_SOURCE("wagon.merge-maven-repos.source") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            final Optional<String> result;
            final String commitId = VcsProperties.GIT_COMMIT_ID.getValue(context)
                .map(value -> value.substring(0, 8))
                .orElse("unknown-commit");

            final String executionRoot = MavenUtils.findInProperties(MavenUtils.PROP_MAVEN_MULTIMODULEPROJECTDIRECTORY, context)
                .orElse(SystemUtils.systemUserDir());

            final String prefix = Paths.get(executionRoot, ".mvn", "wagonRepository").toString();

            result = Optional.of(Paths.get(prefix, commitId).toString());

            return result;
        }

        @Override
        public Optional<String> setProperties(final CiOptionContext context, final Properties properties) {
            final Optional<String> result = super.setProperties(context, properties);

            result.ifPresent(source -> properties.setProperty("altDeploymentRepository", "repo::default::file://" + source));

            return result;
        }
    },
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
