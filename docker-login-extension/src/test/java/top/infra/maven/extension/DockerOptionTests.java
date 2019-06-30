package top.infra.maven.extension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static top.infra.maven.Constants.BOOL_STRING_TRUE;
import static top.infra.maven.extension.DockerOption.DOCKER_REGISTRY;
import static top.infra.maven.extension.DockerOption.DOCKER_REGISTRY_URL;
import static top.infra.maven.extension.MavenOption.GENERATEREPORTS;

import java.util.Properties;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import top.infra.maven.core.CiOptions;
import top.infra.maven.core.GitProperties;
import top.infra.maven.core.GitPropertiesFactoryBean;
import top.infra.maven.logging.Logger;
import top.infra.maven.logging.LoggerSlf4jImpl;


public class DockerOptionTests {

    private static final org.slf4j.Logger slf4jLogger = LoggerFactory.getLogger(DockerOptionTests.class);

    private static Logger logger() {
        return new LoggerSlf4jImpl(slf4jLogger);
    }

    private static GitProperties gitProperties() {
        final Logger logger = logger();
        return new GitPropertiesFactoryBean(logger)
            .getObjct()
            .orElseGet(GitProperties::newBlankGitProperties);
    }

    @Test
    public void testDockerRegistry() {
        final Properties systemProperties = new Properties();

        final Properties userProperties = new Properties();
        userProperties.setProperty(GENERATEREPORTS.getPropertyName(), BOOL_STRING_TRUE);

        final CiOptions ciOpts = new CiOptions(
            gitProperties(),
            systemProperties,
            userProperties
        );

        final Properties loadedProperties = new Properties();
        loadedProperties.setProperty(DOCKER_REGISTRY_URL.getEnvVariableName(), "https://docker.io/v2/");
        ciOpts.updateSystemProperties(loadedProperties);

        // ciOpts.githubSiteRepoOwner().ifPresent(githubSiteRepoOwner ->
        //     ciOpts.setSystemProperty(GITHUB_GLOBAL_REPOSITORYOWNER, githubSiteRepoOwner));

        final Properties newProperties = ciOpts.setCiOptPropertiesInto(OptionCollections.optionCollections(), userProperties);

        slf4jLogger.info("{} {}", DOCKER_REGISTRY_URL.getPropertyName(), ciOpts.getOption(DOCKER_REGISTRY_URL).orElse(null));
        slf4jLogger.info("{} {}", DOCKER_REGISTRY.getPropertyName(), ciOpts.getOption(DOCKER_REGISTRY).orElse(null));
        assertEquals("https://docker.io/v2/", ciOpts.getOption(DOCKER_REGISTRY_URL).orElse(null));
        // assertEquals("docker.io", ciOpts.getOption(DOCKER_REGISTRY).orElse(null));
        assertNull(ciOpts.getOption(DOCKER_REGISTRY).orElse(null));
        assertNull(newProperties.getProperty(DOCKER_REGISTRY.getPropertyName()));
        assertFalse(newProperties.containsKey(DOCKER_REGISTRY.getPropertyName()));
    }
}
