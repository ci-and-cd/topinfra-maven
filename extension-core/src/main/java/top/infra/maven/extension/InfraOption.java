package top.infra.maven.extension;

import static top.infra.maven.Constants.GIT_REF_NAME_MASTER;
import static top.infra.maven.Constants.SRC_CI_OPTS_PROPERTIES;
import static top.infra.maven.Constants.SRC_MAVEN_SETTINGS_XML;
import static top.infra.maven.utils.SystemUtils.systemJavaIoTmp;
import static top.infra.maven.utils.SystemUtils.systemUserHome;
import static top.infra.maven.utils.UrlUtils.domainOrHostFromUrl;
import static top.infra.maven.utils.UrlUtils.urlWithoutPath;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

import top.infra.maven.core.CiOption;
import top.infra.maven.core.CiOptionContext;
import top.infra.maven.core.CiOptionNames;
import top.infra.maven.utils.MavenUtils;

public enum InfraOption implements CiOption {
    //
    CACHE_SETTINGS_PATH("cache.settings.path") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            // We need a stable global cache path
            final String infrastructure = INFRASTRUCTURE.getValue(context)
                .orElseGet(() -> MavenUtils.rootProjectPath(context.getSystemProperties()).getFileName().toString());
            return Optional.of(Paths.get(systemUserHome(), ".ci-and-cd", infrastructure).toString());
        }
    },
    CI_OPTS_FILE("ci.opts.file") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            final Optional<String> result;
            if (new File(SRC_CI_OPTS_PROPERTIES).exists()) {
                result = Optional.of(SRC_CI_OPTS_PROPERTIES);
            } else {
                final String cacheDirectory = CACHE_SETTINGS_PATH.getValue(context)
                    .orElse(systemJavaIoTmp());
                final String cachedCiOptsProperties = Paths.get(cacheDirectory, SRC_CI_OPTS_PROPERTIES).toString();
                result = Optional.of(cachedCiOptsProperties);
            }
            return result;
        }
    },
    GIT_AUTH_TOKEN("git.auth.token") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            return getInfrastructureSpecificValue(this, context);
        }

        @Override
        public Optional<String> setProperties(final CiOptionContext context, final Properties properties) {
            final Optional<String> result = super.setProperties(context, properties);

            result.ifPresent(value ->
                INFRASTRUCTURE.getValue(context).ifPresent(infra ->
                    properties.setProperty(infra + "." + this.getPropertyName(), value))
            );

            return result;
        }
    },
    /**
     * Auto determine CI_OPT_GIT_PREFIX by infrastructure for further download.<br/>
     * prefix of git service url (infrastructure specific), i.e. https://github.com
     */
    GIT_PREFIX("git.prefix") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            final Optional<String> found = getInfrastructureSpecificValue(this, context);

            final Optional<String> result;
            if (found.isPresent()) {
                result = found;
            } else {
                final Optional<String> ciProjectUrl = Optional.ofNullable(context.getSystemProperties().getProperty("env.CI_PROJECT_URL"));
                if (ciProjectUrl.isPresent()) {
                    result = ciProjectUrl.map(url -> urlWithoutPath(url).orElse(null));
                } else {
                    result = context.getGitProperties().remoteOriginUrl()
                        .map(url -> url.startsWith("http")
                            ? urlWithoutPath(url).orElse(null)
                            : domainOrHostFromUrl(url).map(value -> "http://" + value).orElse(null));
                }
            }
            return result;
        }

        @Override
        public Optional<String> setProperties(final CiOptionContext context, final Properties properties) {
            final Optional<String> result = super.setProperties(context, properties);

            result.ifPresent(value ->
                INFRASTRUCTURE.getValue(context).ifPresent(infra ->
                    properties.setProperty(infra + "." + this.getPropertyName(), value))
            );

            return result;
        }
    },
    /**
     * Auto detect infrastructure using for this build.<br/>
     * example of gitlab-ci's CI_PROJECT_URL: "https://example.com/gitlab-org/gitlab-ce"<br/>
     * ossrh, private or customized infrastructure name.
     */
    INFRASTRUCTURE("infrastructure"),

    MAVEN_BUILD_OPTS_REPO("maven.build.opts.repo") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            return INFRASTRUCTURE.getValue(context)
                .map(infra -> {
                    final String repoOwner = "ci-and-cd";
                    final String repoName = String.format("maven-build-opts-%s", infra);
                    return GIT_PREFIX.getValue(context)
                        .map(gitPrefix -> String.format("%s/%s/%s", gitPrefix, repoOwner, repoName));
                })
                .orElse(Optional.empty());
        }
    },
    MAVEN_BUILD_OPTS_REPO_REF("maven.build.opts.repo.ref", GIT_REF_NAME_MASTER),

    MAVEN_SETTINGS_FILE("maven.settings.file") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            final Path rootProjectPath = MavenUtils.rootProjectPath(context.getSystemProperties());
            final String settingsFile = rootProjectPath.resolve(SRC_MAVEN_SETTINGS_XML).toAbsolutePath().toString();

            final Optional<String> result;

            if (Paths.get(settingsFile).toFile().exists()) {
                result = Optional.of(settingsFile);
            } else {
                final String cacheDir = CACHE_SETTINGS_PATH.getValue(context)
                    .orElse(systemJavaIoTmp());

                final String filename = "settings"
                    + INFRASTRUCTURE.getValue(context).map(infra -> "-" + infra).orElse("")
                    + ".xml";

                final String targetFile = Paths.get(cacheDir, filename).toString();

                result = Optional.of(targetFile);
            }

            return result;
        }
    },
    @Deprecated
    MAVEN_SETTINGS_SECURITY_FILE("maven.settings.security.file") {
        @Override
        public Optional<String> setProperties(final CiOptionContext context, final Properties properties) {
            final Optional<String> result = super.setProperties(context, properties);

            result.ifPresent(file -> {
                if (Paths.get(file).toFile().exists()) {
                    properties.setProperty("settings.security", file);
                }
            });

            return result;
        }
    },

    NEXUS2("nexus2") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            return getInfrastructureSpecificValue(this, context);
        }

        @Override
        public Optional<String> setProperties(final CiOptionContext context, final Properties properties) {
            final Optional<String> result = super.setProperties(context, properties);

            result.ifPresent(value ->
                INFRASTRUCTURE.getValue(context).ifPresent(infra ->
                    properties.setProperty(infra + "." + this.getPropertyName(), value))
            );

            return result;
        }
    },

    NEXUS3("nexus3") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            return getInfrastructureSpecificValue(this, context);
        }

        @Override
        public Optional<String> setProperties(final CiOptionContext context, final Properties properties) {
            final Optional<String> result = super.setProperties(context, properties);

            result.ifPresent(value ->
                INFRASTRUCTURE.getValue(context).ifPresent(infra ->
                    properties.setProperty(infra + "." + this.getPropertyName(), value))
            );

            return result;
        }
    },
    // OSSRH_MVNSITE_PASSWORD("ossrh.mvnsite.password"),
    // OSSRH_MVNSITE_USERNAME("ossrh.mvnsite.username"),
    SONAR_HOST_URL("sonar.host.url") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            return getInfrastructureSpecificValue(this, context);
        }

        @Override
        public Optional<String> setProperties(final CiOptionContext context, final Properties properties) {
            final Optional<String> result = super.setProperties(context, properties);

            result.ifPresent(value ->
                INFRASTRUCTURE.getValue(context).ifPresent(infra ->
                    properties.setProperty(infra + "." + this.getPropertyName(), value))
            );

            return result;
        }
    },
    SONAR_LOGIN("sonar.login") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            return getInfrastructureSpecificValue(this, context);
        }

        @Override
        public Optional<String> setProperties(final CiOptionContext context, final Properties properties) {
            final Optional<String> result = super.setProperties(context, properties);

            result.ifPresent(value ->
                INFRASTRUCTURE.getValue(context).ifPresent(infra ->
                    properties.setProperty(infra + "." + this.getPropertyName(), value))
            );

            return result;
        }
    },
    SONAR_ORGANIZATION("sonar.organization") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            return getInfrastructureSpecificValue(this, context);
        }

        @Override
        public Optional<String> setProperties(final CiOptionContext context, final Properties properties) {
            final Optional<String> result = super.setProperties(context, properties);

            result.ifPresent(value ->
                INFRASTRUCTURE.getValue(context).ifPresent(infra ->
                    properties.setProperty(infra + "." + this.getPropertyName(), value))
            );

            return result;
        }
    },
    SONAR_PASSWORD("sonar.password") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            return getInfrastructureSpecificValue(this, context);
        }

        @Override
        public Optional<String> setProperties(final CiOptionContext context, final Properties properties) {
            final Optional<String> result = super.setProperties(context, properties);

            result.ifPresent(value ->
                INFRASTRUCTURE.getValue(context).ifPresent(infra ->
                    properties.setProperty(infra + "." + this.getPropertyName(), value))
            );

            return result;
        }
    };

    private final String defaultValue;
    private final String envVariableName;
    private final String propertyName;
    private final String systemPropertyName;

    InfraOption(final String propertyName) {
        this(propertyName, null);
    }

    InfraOption(final String propertyName, final String defaultValue) {
        if (!CiOptionNames.name(propertyName).equals(this.name())) {
            throw new IllegalArgumentException(String.format("invalid property name [%s] for enum name [%s]", this.name(), propertyName));
        }

        this.defaultValue = defaultValue;
        this.envVariableName = CiOptionNames.envVariableName(propertyName);
        this.propertyName = propertyName;
        this.systemPropertyName = CiOptionNames.systemPropertyName(propertyName);
    }

    public Optional<String> getDefaultValue() {
        return Optional.ofNullable(this.defaultValue);
    }

    public String getEnvVariableName() {
        return this.envVariableName;
    }

    public String getPropertyName() {
        return this.propertyName;
    }

    public String getSystemPropertyName() {
        return this.systemPropertyName;
    }

    public static Optional<String> getInfrastructureSpecificValue(
        final CiOption ciOption,
        final CiOptionContext context
    ) {
        return INFRASTRUCTURE.getValue(context)
            .map(infra -> {
                final String propName = infra + "." + ciOption.getPropertyName();
                final String systemPropName = CiOptionNames.systemPropertyName(propName);
                return Optional.ofNullable(context.getUserProperties().getProperty(propName))
                    .orElseGet(() -> context.getSystemProperties().getProperty(systemPropName));
            });
    }
}
