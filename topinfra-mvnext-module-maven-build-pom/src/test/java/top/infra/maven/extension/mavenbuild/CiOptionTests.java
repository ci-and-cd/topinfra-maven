package top.infra.maven.extension.mavenbuild;

import static org.junit.Assert.assertEquals;
import static top.infra.maven.extension.mavenbuild.MavenBuildPomOption.GITHUB_GLOBAL_REPOSITORYOWNER;
import static top.infra.maven.shared.extension.Constants.BOOL_STRING_TRUE;
import static top.infra.maven.shared.extension.MavenOption.GENERATEREPORTS;

import java.util.Properties;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import top.infra.maven.CiOptionContext;
import top.infra.maven.shared.DefaultCiOptionContext;
import top.infra.maven.shared.utils.PropertiesUtils;

public class CiOptionTests {

    private static final org.slf4j.Logger slf4jLogger = LoggerFactory.getLogger(CiOptionTests.class);

    @Test
    public void testGithubSiteRepoOwner() {
        final Properties systemProperties = new Properties();
        systemProperties.setProperty("env.CI_PROJECT_PATH", "ci-and-cd/topinfra-maven");

        final Properties userProperties = new Properties();
        userProperties.setProperty(GENERATEREPORTS.getPropertyName(), BOOL_STRING_TRUE);

        // gitProperties if needed

        final CiOptionContext ciOptContext = new DefaultCiOptionContext(
            systemProperties,
            userProperties
        );

        final Properties loadedProperties = new Properties();
        PropertiesUtils.setSystemPropertiesIfAbsent(ciOptContext.getSystemProperties(), loadedProperties);

        // final Optional<String> owner = GITHUB_GLOBAL_REPOSITORYOWNER);
        slf4jLogger.info("{} {}",
            GITHUB_GLOBAL_REPOSITORYOWNER.getPropertyName(),
            GITHUB_GLOBAL_REPOSITORYOWNER.getValue(ciOptContext).orElse(null));
        assertEquals("ci-and-cd", GITHUB_GLOBAL_REPOSITORYOWNER.getValue(ciOptContext).orElse(null));
    }
}
