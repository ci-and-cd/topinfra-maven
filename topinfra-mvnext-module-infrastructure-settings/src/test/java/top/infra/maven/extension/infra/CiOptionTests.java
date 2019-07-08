package top.infra.maven.extension.infra;

import static java.lang.Boolean.TRUE;
import static org.junit.Assert.assertEquals;
import static top.infra.maven.extension.shared.Constants.BOOL_STRING_TRUE;
import static top.infra.maven.extension.infra.InfraOption.SONAR_HOST_URL;
import static top.infra.maven.extension.infra.InfraOption.SONAR_ORGANIZATION;
import static top.infra.maven.extension.shared.MavenOption.GENERATEREPORTS;
import static top.infra.maven.extension.shared.VcsProperties.GIT_REMOTE_ORIGIN_URL;

import java.util.Properties;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import top.infra.maven.CiOptionContext;
import top.infra.maven.extension.OptionCollections;
import top.infra.maven.logging.Logger;
import top.infra.maven.logging.LoggerSlf4jImpl;

public class CiOptionTests {

    private static final org.slf4j.Logger slf4jLogger = LoggerFactory.getLogger(CiOptionTests.class);

    @Test
    public void testGenerateReports() {
        final Properties systemProperties = new Properties();

        final Properties userProperties = new Properties();
        userProperties.setProperty(GENERATEREPORTS.getPropertyName(), BOOL_STRING_TRUE);

        // gitProperties if needed

        final CiOptionContext ciOptContext = new CiOptionContext(
            systemProperties,
            userProperties
        );

        slf4jLogger.info("generateReports {}", GENERATEREPORTS.getValue(ciOptContext).orElse(null));
        assertEquals(TRUE.toString(), GENERATEREPORTS.getValue(ciOptContext).orElse(null));

        final String remoteOriginUrl = GIT_REMOTE_ORIGIN_URL.getValue(ciOptContext).orElse(null);
        CiOptionConfigLoader.ciOptContextFromFile(ciOptContext, logger(), remoteOriginUrl, false, true)
            .ifPresent(ciOptContext::updateSystemProperties);

        slf4jLogger.info("generateReports {}", GENERATEREPORTS.getValue(ciOptContext).orElse(null));
        assertEquals(TRUE.toString(), GENERATEREPORTS.getValue(ciOptContext).orElse(null));

        ciOptContext.setCiOptPropertiesInto(OptionCollections.optionCollections(), userProperties);

        slf4jLogger.info("generateReports {}", GENERATEREPORTS.getValue(ciOptContext).orElse(null));
        assertEquals(TRUE.toString(), GENERATEREPORTS.getValue(ciOptContext).orElse(null));
    }

    @Test
    public void testSonarOptions() {
        final String expectedSonarHostUrl = "https://sonarqube.com";
        final String expectedSonarOrganization = "home1-oss-github";

        final Properties systemProperties = new Properties();
        systemProperties.setProperty(GENERATEREPORTS.getSystemPropertyName(), BOOL_STRING_TRUE);
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

    private static Logger logger() {
        return new LoggerSlf4jImpl(slf4jLogger);
    }
}
