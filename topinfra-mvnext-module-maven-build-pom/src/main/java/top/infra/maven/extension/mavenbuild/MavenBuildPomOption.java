package top.infra.maven.extension.mavenbuild;

import static java.lang.Boolean.FALSE;
import static top.infra.maven.extension.shared.Constants.BOOL_STRING_FALSE;
import static top.infra.maven.extension.shared.Constants.BOOL_STRING_TRUE;
import static top.infra.maven.extension.shared.Constants.GIT_REF_NAME_DEVELOP;
import static top.infra.maven.extension.shared.Constants.GIT_REF_PREFIX_FEATURE;
import static top.infra.maven.extension.shared.Constants.GIT_REF_PREFIX_HOTFIX;
import static top.infra.maven.extension.shared.Constants.GIT_REF_PREFIX_RELEASE;
import static top.infra.maven.extension.shared.Constants.GIT_REF_PREFIX_SUPPORT;
import static top.infra.maven.extension.shared.Constants.PUBLISH_CHANNEL_RELEASE;
import static top.infra.maven.extension.shared.Constants.PUBLISH_CHANNEL_SNAPSHOT;
import static top.infra.maven.extension.shared.FastOption.FAST;
import static top.infra.maven.extension.shared.InfraOption.GIT_AUTH_TOKEN;
import static top.infra.maven.extension.shared.InfraOption.INFRASTRUCTURE;
import static top.infra.maven.extension.shared.InfraOption.NEXUS2;
import static top.infra.maven.utils.SystemUtils.systemUserDir;

import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

import top.infra.maven.CiOption;
import top.infra.maven.CiOptionContext;
import top.infra.maven.extension.shared.MavenOption;
import top.infra.maven.extension.shared.VcsProperties;

public enum MavenBuildPomOption implements CiOption {
    CHECKSTYLE_CONFIG_LOCATION("checkstyle.config.location",
        "https://raw.githubusercontent.com/ci-and-cd/maven-build/master/src/main/checkstyle/google_checks_8.10.xml"),
    // @Deprecated
    // CI_SCRIPT("ci.script"),
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
    // https://npm.taobao.org/mirrors/node/
    FRONTEND_NODEDOWNLOADROOT("frontend.nodeDownloadRoot", "https://nodejs.org/dist/"),
    // http://registry.npm.taobao.org/npm/-/
    FRONTEND_NPMDOWNLOADROOT("frontend.npmDownloadRoot", "https://registry.npmjs.org/npm/-/"),
    GIT_COMMIT_ID_SKIP("git.commit.id.skip", BOOL_STRING_FALSE),
    GITHUB_GLOBAL_OAUTH2TOKEN("github.global.oauth2Token") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            return GIT_AUTH_TOKEN.getValue(context);
        }
    },
    GITHUB_GLOBAL_REPOSITORYNAME("github.global.repositoryName") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            return SITE_PATH_PREFIX.getValue(context);
        }
    },
    GITHUB_GLOBAL_REPOSITORYOWNER("github.global.repositoryOwner", "unknown-owner") {
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
            result.ifPresent(owner ->
                context.getSystemProperties().setProperty(GITHUB_GLOBAL_REPOSITORYOWNER.getSystemPropertyName(), owner));
            return result;
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
    MAVEN_CENTRAL_PASS("maven.central.pass"),
    MAVEN_CENTRAL_USER("maven.central.user"),
    NEXUS2_STAGING("nexus2.staging") {
        @Override
        public Optional<String> getValue(final CiOptionContext context) {
            final Optional<String> publishChannel = PUBLISH_CHANNEL.getValue(context);
            final boolean publishSnapshot = publishChannel.map(PUBLISH_CHANNEL_SNAPSHOT::equals).orElse(FALSE);
            return publishSnapshot
                ? Optional.of(BOOL_STRING_FALSE)
                : super.getValue(context);
        }
    },
    PMD_RULESET_LOCATION("pmd.ruleset.location",
        "https://raw.githubusercontent.com/ci-and-cd/maven-build/master/src/main/pmd/pmd-ruleset-6.8.0.xml"),

    /**
     * Auto determine current build publish channel by current build ref name.<br/>
     * snapshot or release
     */
    PUBLISH_CHANNEL("publish.channel", "snapshot") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            final String result;

            final String refName = VcsProperties.GIT_REF_NAME.getValue(
                context).orElse("");
            if (GIT_REF_NAME_DEVELOP.equals(refName)) {
                result = PUBLISH_CHANNEL_SNAPSHOT;
            } else if (refName.startsWith(GIT_REF_PREFIX_FEATURE)) {
                result = PUBLISH_CHANNEL_SNAPSHOT;
            } else if (refName.startsWith(GIT_REF_PREFIX_HOTFIX)) {
                result = PUBLISH_CHANNEL_RELEASE;
            } else if (refName.startsWith(GIT_REF_PREFIX_RELEASE)) {
                result = PUBLISH_CHANNEL_RELEASE;
            } else if (refName.startsWith(GIT_REF_PREFIX_SUPPORT)) {
                result = PUBLISH_CHANNEL_RELEASE;
            } else {
                result = PUBLISH_CHANNEL_SNAPSHOT;
            }

            return Optional.of(result);
        }
    },
    SITE_PATH("site.path", "${publish.channel}/${site.path.prefix}"),
    SITE_PATH_PREFIX("site.path.prefix") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            final boolean generateReports = MavenOption.GENERATEREPORTS.getValue(context)
                .map(Boolean::parseBoolean).orElse(FALSE);

            final Optional<String> result;
            if (generateReports) {
                final Optional<String> gitRepoSlug = VcsProperties.gitRepoSlug(context);
                result = gitRepoSlug.map(slug -> slug.split("/")[0]);
            } else {
                result = Optional.empty();
            }
            return result;
        }
    },
    SONAR("sonar"),
    WAGON_MERGEMAVENREPOS_ARTIFACTDIR("wagon.merge-maven-repos.artifactDir") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            // TODO System.setProperty("wagon.merge-maven-repos.artifactDir", "${project.groupId}".replace('.', '/') + "/${project.artifactId}")
            // TODO Extract all options that depend on project properties to ProjectOption class.
            return Optional.of("${project.groupId}/${project.artifactId}");
        }
    },
    WAGON_MERGEMAVENREPOS_SOURCE("wagon.merge-maven-repos.source") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            final String commitId = VcsProperties.GIT_COMMIT_ID.getValue(context)
                .map(value -> value.substring(0, 8))
                .orElse("unknown-commit");
            // final String prefix = Paths.get(systemUserHome(), ".ci-and-cd", "local-deploy").toString();
            final String prefix = Paths.get(systemUserDir(), ".mvn", "wagonRepository").toString();
            return Optional.of(Paths.get(prefix, commitId).toString());
        }

        @Override
        public Optional<String> setProperties(final CiOptionContext context, final Properties properties) {
            final Optional<String> result = super.setProperties(context, properties);

            result.ifPresent(source -> properties.setProperty("altDeploymentRepository", "repo::default::file://" + source));

            return result;
        }
    },
    WAGON_MERGEMAVENREPOS_TARGET("wagon.merge-maven-repos.target") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            final Optional<String> infrastructure = INFRASTRUCTURE.getValue(context);
            final Optional<String> nexus2 = NEXUS2.getValue(context);

            final boolean nexus2Staging = NEXUS2_STAGING.getValue(context)
                .map(Boolean::parseBoolean).orElse(FALSE);
            final Optional<String> publishChannel = PUBLISH_CHANNEL.getValue(context);
            final boolean publishRelease = publishChannel.map(PUBLISH_CHANNEL_RELEASE::equals).orElse(FALSE);

            final String prefix = infrastructure.map(infra -> String.format("%s", infra)).orElse("");
            final String value;
            if (nexus2.isPresent()) {
                if (publishRelease) {
                    value = nexus2Staging
                        ? String.format("${%snexus2}service/local/staging/deploy/maven2/", prefix)
                        : String.format("${%snexus2}content/repositories/releases/", prefix);
                } else {
                    value = String.format("${%snexus2}content/repositories/snapshots/", prefix);
                }
            } else {
                value = String.format("${%snexus3}repository/maven-${publish.channel}s", prefix);
            }
            return Optional.ofNullable(value);
        }
    },
    WAGON_MERGEMAVENREPOS_TARGETID("wagon.merge-maven-repos.targetId") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            final Optional<String> infrastructure = INFRASTRUCTURE.getValue(context);
            return Optional.of(infrastructure
                .map(infra -> String.format("%s-${publish.channel}s", infra))
                .orElse("${publish.channel}s"));
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
