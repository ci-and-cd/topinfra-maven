package top.infra.maven.extension;

import static org.junit.Assert.assertEquals;
import static top.infra.maven.Constants.BOOL_STRING_TRUE;
import static top.infra.maven.extension.InfraOption.SONAR_HOST_URL;
import static top.infra.maven.extension.InfraOption.SONAR_ORGANIZATION;
import static top.infra.maven.extension.MavenBuildPomOption.GITHUB_GLOBAL_REPOSITORYOWNER;
import static top.infra.maven.extension.MavenBuildPomOption.SONAR;
import static top.infra.maven.extension.MavenOption.GENERATEREPORTS;

import java.util.Properties;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import top.infra.maven.core.CiOptionContext;

public class CiOptionTests {

    private static final org.slf4j.Logger slf4jLogger = LoggerFactory.getLogger(CiOptionTests.class);

    @Test
    public void testGithubSiteRepoOwner() {
        final Properties systemProperties = new Properties();

        final Properties userProperties = new Properties();
        userProperties.setProperty(GENERATEREPORTS.getPropertyName(), BOOL_STRING_TRUE);

        // gitProperties if needed

        final CiOptionContext ciOptContext = new CiOptionContext(
            systemProperties,
            userProperties
        );

        final Properties loadedProperties = new Properties();
        ciOptContext.updateSystemProperties(loadedProperties);

        // final Optional<String> owner = GITHUB_GLOBAL_REPOSITORYOWNER);
        slf4jLogger.info("{} {}",
            GITHUB_GLOBAL_REPOSITORYOWNER.getPropertyName(),
            GITHUB_GLOBAL_REPOSITORYOWNER.getValue(ciOptContext).orElse(null));
        assertEquals("ci-and-cd", GITHUB_GLOBAL_REPOSITORYOWNER.getValue(ciOptContext).orElse(null));
    }

    @Test
    public void testSonar() {
        final String expectedSonarHostUrl = "https://sonarqube.com";
        final String expectedSonarOrganization = "home1-oss-github";

        final Properties systemProperties = new Properties();
        systemProperties.setProperty(GENERATEREPORTS.getSystemPropertyName(), BOOL_STRING_TRUE);
        systemProperties.setProperty(SONAR.getSystemPropertyName(), BOOL_STRING_TRUE);
        systemProperties.setProperty(SONAR_ORGANIZATION.getSystemPropertyName(), expectedSonarOrganization);

        final Properties userProperties = new Properties();

        // gitProperties if needed

        final CiOptionContext ciOptContext = new CiOptionContext(
            systemProperties,
            userProperties
        );

        final Properties loadedProperties = new Properties();
        loadedProperties.setProperty(SONAR_HOST_URL.getEnvVariableName(), expectedSonarHostUrl);
        ciOptContext.updateSystemProperties(loadedProperties);

        final Properties newProperties = ciOptContext.setCiOptPropertiesInto(OptionCollections.optionCollections(), userProperties);

        slf4jLogger.info("{} {}", SONAR_HOST_URL.getEnvVariableName(), SONAR_HOST_URL.getValue(ciOptContext).orElse(null));
        slf4jLogger.info("{} {}", SONAR_HOST_URL.getPropertyName(), userProperties.getProperty(SONAR_HOST_URL.getPropertyName()));
        assertEquals(expectedSonarHostUrl, SONAR_HOST_URL.getValue(ciOptContext).orElse(null));
        assertEquals(expectedSonarHostUrl, userProperties.getProperty(SONAR_HOST_URL.getPropertyName()));

        slf4jLogger.info("{} {}", SONAR_ORGANIZATION.getEnvVariableName(), SONAR_ORGANIZATION.getValue(ciOptContext).orElse(null));
        slf4jLogger.info("{} {}", SONAR_ORGANIZATION.getPropertyName(), userProperties.getProperty(SONAR_ORGANIZATION.getPropertyName()));
        assertEquals(expectedSonarOrganization, SONAR_ORGANIZATION.getValue(ciOptContext).orElse(null));
        assertEquals(expectedSonarOrganization, userProperties.getProperty(SONAR_ORGANIZATION.getPropertyName()));
    }
}
