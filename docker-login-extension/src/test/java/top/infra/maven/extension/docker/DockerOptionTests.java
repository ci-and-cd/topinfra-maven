package top.infra.maven.extension.docker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static top.infra.maven.Constants.BOOL_STRING_TRUE;
import static top.infra.maven.extension.MavenOption.GENERATEREPORTS;
import static top.infra.maven.extension.docker.DockerOption.DOCKER_REGISTRY;
import static top.infra.maven.extension.docker.DockerOption.DOCKER_REGISTRY_URL;

import java.util.Properties;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import top.infra.maven.core.CiOptionContext;
import top.infra.maven.core.GitProperties;
import top.infra.maven.core.GitPropertiesBeanFactory;
import top.infra.maven.extension.OptionCollections;
import top.infra.maven.logging.Logger;
import top.infra.maven.logging.LoggerSlf4jImpl;


public class DockerOptionTests {

    private static final org.slf4j.Logger slf4jLogger = LoggerFactory.getLogger(DockerOptionTests.class);

    @Test
    public void testDockerRegistry() {
        final Properties systemProperties = new Properties();

        final Properties userProperties = new Properties();
        userProperties.setProperty(GENERATEREPORTS.getPropertyName(), BOOL_STRING_TRUE);

        final CiOptionContext ciOptContext = new CiOptionContext(
            gitProperties(),
            systemProperties,
            userProperties
        );

        final Properties loadedProperties = new Properties();
        loadedProperties.setProperty(DOCKER_REGISTRY_URL.getEnvVariableName(), "https://docker.io/v2/");
        ciOptContext.updateSystemProperties(loadedProperties);

        // ciOptContext.githubSiteRepoOwner().ifPresent(githubSiteRepoOwner ->
        //     ciOptContext.setSystemProperty(GITHUB_GLOBAL_REPOSITORYOWNER, githubSiteRepoOwner));

        final Properties newProperties = ciOptContext.setCiOptPropertiesInto(OptionCollections.optionCollections(), userProperties);

        slf4jLogger.info("{} {}", DOCKER_REGISTRY_URL.getPropertyName(), DOCKER_REGISTRY_URL.getValue(ciOptContext).orElse(null));
        slf4jLogger.info("{} {}", DOCKER_REGISTRY.getPropertyName(), DOCKER_REGISTRY.getValue(ciOptContext).orElse(null));
        assertEquals("https://docker.io/v2/", DOCKER_REGISTRY_URL.getValue(ciOptContext).orElse(null));
        // assertEquals("docker.io", DOCKER_REGISTRY).orElse(null));
        assertNull(DOCKER_REGISTRY.getValue(ciOptContext).orElse(null));
        assertNull(newProperties.getProperty(DOCKER_REGISTRY.getPropertyName()));
        assertFalse(newProperties.containsKey(DOCKER_REGISTRY.getPropertyName()));
    }

    private static GitProperties gitProperties() {
        return new GitPropertiesBeanFactory(logger()).getObject();
    }

    private static Logger logger() {
        return new LoggerSlf4jImpl(slf4jLogger);
    }
}