package top.infra.maven.extension.gitflow;

import static java.lang.Boolean.FALSE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static top.infra.maven.Constants.BOOL_STRING_TRUE;
import static top.infra.maven.Constants.GIT_REF_NAME_DEVELOP;
import static top.infra.maven.extension.MavenBuildExtensionOption.GIT_REF_NAME;
import static top.infra.maven.extension.MavenBuildExtensionOption.ORIGIN_REPO;
import static top.infra.maven.extension.MavenBuildExtensionOption.PUBLISH_TO_REPO;
import static top.infra.maven.extension.MavenOption.GENERATEREPORTS;
import static top.infra.maven.extension.gitflow.GitflowSemanticVersionChecker.checkProjectVersion;
import static top.infra.maven.utils.SupportFunction.isSemanticSnapshotVersion;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import top.infra.maven.core.CiOptionContext;
import top.infra.maven.core.GitProperties;
import top.infra.maven.core.GitPropertiesFactoryBean;
import top.infra.maven.extension.OptionCollections;
import top.infra.maven.logging.Logger;
import top.infra.maven.logging.LoggerSlf4jImpl;

public class ProjectVersionTest {

    private static final org.slf4j.Logger slf4jLogger = LoggerFactory.getLogger(ProjectVersionTest.class);

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
    public void testSemanticSnapshotVersion() {
        assertTrue(isSemanticSnapshotVersion("2.0.1-SNAPSHOT"));
        assertTrue(isSemanticSnapshotVersion("1.0.0-SNAPSHOT"));
        assertTrue(isSemanticSnapshotVersion("1.0-SNAPSHOT"));
        assertTrue(isSemanticSnapshotVersion("1-SNAPSHOT"));

        assertFalse(isSemanticSnapshotVersion("2.0.1-feature-SNAPSHOT"));
        assertFalse(isSemanticSnapshotVersion("2.0.1"));
    }

    @Test
    public void testVersionsOnDevelopBranch() {
        final Properties systemProperties = new Properties();
        systemProperties.setProperty(GIT_REF_NAME.getSystemPropertyName(), GIT_REF_NAME_DEVELOP);
        systemProperties.setProperty(ORIGIN_REPO.getSystemPropertyName(), BOOL_STRING_TRUE);

        final Properties userProperties = new Properties();
        userProperties.setProperty(GENERATEREPORTS.getPropertyName(), BOOL_STRING_TRUE);

        final CiOptionContext ciOptContext = new CiOptionContext(
            gitProperties(),
            systemProperties,
            userProperties
        );

        final Properties loadedProperties = new Properties();
        ciOptContext.updateSystemProperties(loadedProperties);

        final Properties newProperties = ciOptContext.setCiOptPropertiesInto(OptionCollections.optionCollections(), userProperties);

        final Optional<String> gitRefName = GIT_REF_NAME.getValue(ciOptContext);
        slf4jLogger.info("{} [{}]", GIT_REF_NAME.getPropertyName(), gitRefName.orElse(null));

        slf4jLogger.info("{} [{}]", PUBLISH_TO_REPO.getPropertyName(), PUBLISH_TO_REPO.getValue(ciOptContext).orElse(null));
        assertTrue(PUBLISH_TO_REPO.getValue(ciOptContext).map(Boolean::parseBoolean).orElse(FALSE));

        final String projectVersion = "2.0.1-SNAPSHOT";
        final Map.Entry<Boolean, RuntimeException> checkProjectVersionResult = checkProjectVersion(ciOptContext, projectVersion);
        slf4jLogger.info("checkProjectVersion result: [{}]", checkProjectVersionResult);
        assertTrue(checkProjectVersionResult.getKey());

        assertFalse(checkProjectVersion(ciOptContext, "2.0.1-feature1-SNAPSHOT").getKey());
        assertFalse(checkProjectVersion(ciOptContext, "2.0.1").getKey());
    }

    @Test
    public void testVersionsOnFeatureBranch() {
        final Properties systemProperties = new Properties();
        systemProperties.setProperty(GIT_REF_NAME.getSystemPropertyName(), "feature/feature1");
        systemProperties.setProperty(ORIGIN_REPO.getSystemPropertyName(), BOOL_STRING_TRUE);

        final Properties userProperties = new Properties();
        userProperties.setProperty(GENERATEREPORTS.getPropertyName(), BOOL_STRING_TRUE);

        final CiOptionContext ciOptContext = new CiOptionContext(
            gitProperties(),
            systemProperties,
            userProperties
        );

        final Properties loadedProperties = new Properties();
        ciOptContext.updateSystemProperties(loadedProperties);

        final Properties newProperties = ciOptContext.setCiOptPropertiesInto(OptionCollections.optionCollections(), userProperties);

        final Optional<String> gitRefName = GIT_REF_NAME.getValue(ciOptContext);
        slf4jLogger.info("{} [{}]", GIT_REF_NAME.getPropertyName(), gitRefName.orElse(null));

        slf4jLogger.info("{} [{}]", PUBLISH_TO_REPO.getPropertyName(), PUBLISH_TO_REPO.getValue(ciOptContext).orElse(null));
        assertTrue(PUBLISH_TO_REPO.getValue(ciOptContext).map(Boolean::parseBoolean).orElse(FALSE));

        final String projectVersion = "2.0.1-feature1-SNAPSHOT";
        final Map.Entry<Boolean, RuntimeException> checkProjectVersionResult = checkProjectVersion(ciOptContext, projectVersion);
        slf4jLogger.info("checkProjectVersion result: [{}]", checkProjectVersionResult);
        assertTrue(checkProjectVersionResult.getKey());

        assertFalse(checkProjectVersion(ciOptContext, "2.0.1-SNAPSHOT").getKey());
        assertFalse(checkProjectVersion(ciOptContext, "2.0.1").getKey());
    }
}
