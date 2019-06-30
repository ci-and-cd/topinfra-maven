package top.infra.maven.extension;

import static top.infra.maven.Constants.BOOL_STRING_FALSE;
import static top.infra.maven.Constants.BOOL_STRING_TRUE;
import static top.infra.maven.extension.Docker.dockerHost;
import static top.infra.maven.extension.Docker.dockerfiles;
import static top.infra.maven.utils.SystemUtils.existsInPath;

import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

import top.infra.maven.Constants;
import top.infra.maven.core.CiOption;
import top.infra.maven.core.CiOptionNames;
import top.infra.maven.core.GitProperties;
import top.infra.maven.utils.UrlUtils;

public enum DockerOption implements CiOption {
    /**
     * Docker enabled.
     */
    DOCKER("docker") {
        @Override
        public Optional<String> calculateValue(
            final GitProperties gitProperties,
            final Properties systemProperties,
            final Properties userProperties
        ) {
            final boolean result;

            if (existsInPath("docker")) {
                // TODO Support named pipe (for windows).
                // Unix sock file
                // [[ -f /var/run/docker.sock ]] || [[ -L /var/run/docker.sock ]]
                final String dockerSockFile = "/var/run/docker.sock"; // TODO windows ?
                final boolean dockerSockFilePresent = Paths.get(dockerSockFile).toFile().exists();
                // TCP
                final boolean envDockerHostPresent = dockerHost(systemProperties).isPresent();
                // [[ -n "$(find . -name '*Docker*')" ]] || [[ -n "$(find . -name '*docker-compose*.yml')" ]]
                final int dockerComposeFilesCount = org.unix4j.Unix4j.find(".", "*docker-compose*.yml").toStringList().size();
                final boolean dockerFilesFound = !dockerfiles().isEmpty() || dockerComposeFilesCount > 0;

                result = dockerFilesFound && (dockerSockFilePresent || envDockerHostPresent);
            } else {
                result = false;
            }

            return Optional.of(result ? BOOL_STRING_TRUE : BOOL_STRING_FALSE);
        }
    },
    DOCKER_IMAGE_PREFIX("docker.image.prefix"),
    /**
     * com.spotify:docker-maven-plugin
     */
    DOCKER_IMAGENAME("docker.imageName") {
        @Override
        public Optional<String> calculateValue(
            final GitProperties gitProperties,
            final Properties systemProperties,
            final Properties userProperties
        ) {
            return Optional.of(DOCKER_REGISTRY.getValue(gitProperties, systemProperties, userProperties)
                .map(registry -> "${docker.registry}/${docker.image.prefix}${project.artifactId}")
                .orElse("${docker.image.prefix}${project.artifactId}"));
        }
    },
    /**
     * Domain / hostname of docker registry. e.g. docker-registry.some.org
     */
    DOCKER_REGISTRY("docker.registry") {
        @Override
        public Optional<String> calculateValue(
            final GitProperties gitProperties,
            final Properties systemProperties,
            final Properties userProperties
        ) {
            final Optional<String> dockerRegistryUrl = DOCKER_REGISTRY_URL.calculateValue(gitProperties, systemProperties, userProperties);
            return dockerRegistryUrl
                .flatMap(UrlUtils::domainOrHostFromUrl)
                .flatMap(value -> Optional.ofNullable(value.endsWith("docker.io") ? null : value));
        }

        @Override
        public Optional<String> getValue(
            final GitProperties gitProperties,
            final Properties systemProperties,
            final Properties userProperties
        ) {
            return super.getValue(gitProperties, systemProperties, userProperties)
                .map(value -> value.endsWith("docker.io") ? null : value);
        }

        @Override
        public Optional<String> setProperties(
            final GitProperties gitProperties,
            final Properties systemProperties,
            final Properties userProperties,
            final Properties properties
        ) {
            final Optional<String> result = super.setProperties(gitProperties, systemProperties, userProperties, properties);

            result.ifPresent(value ->
                InfraOption.INFRASTRUCTURE.getValue(gitProperties, systemProperties, userProperties).ifPresent(infra ->
                    properties.setProperty(infra + "." + this.getPropertyName(), value))
            );

            return result;
        }
    },
    DOCKER_REGISTRY_PASS("docker.registry.pass") {
        @Override
        public Optional<String> calculateValue(
            final GitProperties gitProperties,
            final Properties systemProperties,
            final Properties userProperties
        ) {
            return InfraOption.getInfrastructureSpecificValue(this, gitProperties, systemProperties, userProperties);
        }

        @Override
        public Optional<String> setProperties(
            final GitProperties gitProperties,
            final Properties systemProperties,
            final Properties userProperties,
            final Properties properties
        ) {
            final Optional<String> result = super.setProperties(gitProperties, systemProperties, userProperties, properties);

            result.ifPresent(value ->
                InfraOption.INFRASTRUCTURE.getValue(gitProperties, systemProperties, userProperties).ifPresent(infra ->
                    properties.setProperty(infra + "." + this.getPropertyName(), value))
            );

            return result;
        }
    },
    DOCKER_REGISTRY_URL("docker.registry.url") {
        @Override
        public Optional<String> calculateValue(
            final GitProperties gitProperties,
            final Properties systemProperties,
            final Properties userProperties
        ) {
            return InfraOption.getInfrastructureSpecificValue(this, gitProperties, systemProperties, userProperties);
        }

        @Override
        public Optional<String> setProperties(
            final GitProperties gitProperties,
            final Properties systemProperties,
            final Properties userProperties,
            final Properties properties
        ) {
            final Optional<String> result = super.setProperties(gitProperties, systemProperties, userProperties, properties);

            result.ifPresent(value ->
                InfraOption.INFRASTRUCTURE.getValue(gitProperties, systemProperties, userProperties).ifPresent(infra ->
                    properties.setProperty(infra + "." + this.getPropertyName(), value))
            );

            return result;
        }
    },
    DOCKER_REGISTRY_USER("docker.registry.user") {
        @Override
        public Optional<String> calculateValue(
            final GitProperties gitProperties,
            final Properties systemProperties,
            final Properties userProperties
        ) {
            return InfraOption.getInfrastructureSpecificValue(this, gitProperties, systemProperties, userProperties);
        }

        @Override
        public Optional<String> setProperties(
            final GitProperties gitProperties,
            final Properties systemProperties,
            final Properties userProperties,
            final Properties properties
        ) {
            final Optional<String> result = super.setProperties(gitProperties, systemProperties, userProperties, properties);

            result.ifPresent(value ->
                InfraOption.INFRASTRUCTURE.getValue(gitProperties, systemProperties, userProperties).ifPresent(infra ->
                    properties.setProperty(infra + "." + this.getPropertyName(), value))
            );

            return result;
        }
    },
    /**
     * com.spotify:dockerfile-maven-plugin
     */
    DOCKERFILE_REPOSITORY("dockerfile.repository") {
        @Override
        public Optional<String> calculateValue(
            final GitProperties gitProperties,
            final Properties systemProperties,
            final Properties userProperties
        ) {
            return Optional.of(DOCKER_REGISTRY.getValue(gitProperties, systemProperties, userProperties)
                .map(registry -> "${docker.registry}/${docker.image.prefix}${project.artifactId}")
                .orElse("${docker.image.prefix}${project.artifactId}"));
        }
    },
    DOCKERFILE_USEMAVENSETTINGSFORAUTH("dockerfile.useMavenSettingsForAuth", Constants.BOOL_STRING_FALSE),
    ;

    private final String defaultValue;
    private final String envVariableName;
    private final String propertyName;
    private final String systemPropertyName;

    DockerOption(final String propertyName) {
        this(propertyName, null);
    }

    DockerOption(final String propertyName, final String defaultValue) {
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
}
