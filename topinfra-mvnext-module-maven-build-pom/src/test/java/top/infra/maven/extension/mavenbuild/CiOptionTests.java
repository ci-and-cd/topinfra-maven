package top.infra.maven.extension.mavenbuild;

import static org.junit.Assert.assertEquals;
import static top.infra.maven.extension.mavenbuild.MavenBuildPomOption.GITHUB_GLOBAL_REPOSITORYOWNER;
import static top.infra.maven.extension.mavenbuild.MavenBuildPomOption.GITHUB_SITE_PUBLISH;
import static top.infra.maven.shared.extension.Constants.BOOL_STRING_FALSE;
import static top.infra.maven.shared.extension.Constants.BOOL_STRING_TRUE;
import static top.infra.maven.shared.extension.MavenOption.GENERATEREPORTS;
import static top.infra.maven.test.utils.TestUtils.blankCiOptCtx;

import java.util.Properties;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import top.infra.maven.CiOptionContext;
import top.infra.maven.shared.DefaultCiOptionContext;
import top.infra.maven.shared.utils.PropertiesUtils;

public class CiOptionTests {

    private static final org.slf4j.Logger slf4jLogger = LoggerFactory.getLogger(CiOptionTests.class);

    @Test
    public void testGithubSitePublish() {
        final CiOptionContext ctxTrue = blankCiOptCtx();
        ctxTrue.getSystemProperties().setProperty(GITHUB_SITE_PUBLISH.getSystemPropertyName(), BOOL_STRING_TRUE);
        assertEquals(BOOL_STRING_TRUE, GITHUB_SITE_PUBLISH.getValue(ctxTrue).orElse(null));

        ctxTrue.getUserProperties().setProperty(GENERATEREPORTS.getPropertyName(), BOOL_STRING_TRUE);
        assertEquals(BOOL_STRING_TRUE, GITHUB_SITE_PUBLISH.getValue(ctxTrue).orElse(null));

        final CiOptionContext ctxFalse = blankCiOptCtx();
        ctxFalse.getSystemProperties().setProperty(GITHUB_SITE_PUBLISH.getSystemPropertyName(), BOOL_STRING_FALSE);
        assertEquals(BOOL_STRING_FALSE, GITHUB_SITE_PUBLISH.getValue(ctxFalse).orElse(null));

        final CiOptionContext ctxNotGenerateReports = blankCiOptCtx();
        ctxNotGenerateReports.getUserProperties().setProperty(GENERATEREPORTS.getPropertyName(), BOOL_STRING_FALSE);
        assertEquals(BOOL_STRING_FALSE, GITHUB_SITE_PUBLISH.getValue(ctxFalse).orElse(null));

        ctxNotGenerateReports.getSystemProperties().setProperty(GITHUB_SITE_PUBLISH.getSystemPropertyName(), BOOL_STRING_TRUE);
        assertEquals(BOOL_STRING_FALSE, GITHUB_SITE_PUBLISH.getValue(ctxFalse).orElse(null));

        assertEquals(BOOL_STRING_FALSE, GITHUB_SITE_PUBLISH.getValue(blankCiOptCtx()).orElse(null));
    }

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
