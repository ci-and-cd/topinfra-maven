package top.infra.maven.extension.shared;

import static org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION;
import static top.infra.maven.extension.shared.Constants.GIT_REF_NAME_MASTER;
import static top.infra.maven.extension.shared.Constants.SRC_CI_OPTS_PROPERTIES;
import static top.infra.maven.utils.SystemUtils.systemJavaIoTmp;
import static top.infra.maven.utils.SystemUtils.systemUserHome;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

import top.infra.maven.CiOption;
import top.infra.maven.CiOptionContext;
import top.infra.maven.utils.MavenUtils;

public enum InfraOption implements CiOption {
    //
    CACHE_SETTINGS_PATH("cache.settings.path") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            // We need a stable global cache path
            return Optional.of(INFRASTRUCTURE.getValue(context)
                .map(infra -> Paths.get(systemUserHome(), ".ci-and-cd", infra).toString())
                .orElseGet(() -> MavenUtils.userHomeDotM2().toString()));
        }
    },
    CI_OPTS_FILE("ci.opts.file") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            final Optional<String> result;

            final Path optsFilePath = Paths.get(SRC_CI_OPTS_PROPERTIES);
            if (optsFilePath.toFile().exists()) {
                result = Optional.of(optsFilePath.toString());
            } else {
                final String cacheDirectory = CACHE_SETTINGS_PATH.getValue(context)
                    .orElse(systemJavaIoTmp());
                final String cachedCiOptsProperties = Paths.get(cacheDirectory, optsFilePath.toFile().getName()).toString();
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
     * Auto detect infrastructure using for this build.<br/>
     * example of gitlab-ci's CI_PROJECT_URL: "https://example.com/gitlab-org/gitlab-ce"<br/>
     * ossrh, private or customized infrastructure name.
     */
    INFRASTRUCTURE("infrastructure"),

    /**
     * TODO unused?
     */
    MAVEN_BUILD_OPTS_REPO("maven.build.opts.repo"),
    MAVEN_BUILD_OPTS_REPO_REF("maven.build.opts.repo.ref", GIT_REF_NAME_MASTER),

    SETTINGS("settings"),
    SETTINGS_SECURITY(SYSTEM_PROPERTY_SEC_LOCATION),
    TOOLCHAINS("toolchains"),

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
    private final String propertyName;

    InfraOption(final String propertyName) {
        this(propertyName, null);
    }

    InfraOption(final String propertyName, final String defaultValue) {
        this.defaultValue = defaultValue;
        this.propertyName = propertyName;
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

    @Override
    public Optional<String> getDefaultValue() {
        return Optional.ofNullable(this.defaultValue);
    }

    @Override
    public String getPropertyName() {
        return this.propertyName;
    }
}
