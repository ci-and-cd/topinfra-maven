package top.infra.maven.extension.gitflow;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static top.infra.maven.extension.gitflow.GitFlowSemanticVersionChecker.checkProjectVersion;
import static top.infra.maven.extension.gitflow.GitFlowSemanticVersionChecker.isSemFeature;
import static top.infra.maven.extension.gitflow.GitFlowSemanticVersionChecker.isSemRelease;
import static top.infra.maven.extension.gitflow.GitFlowSemanticVersionChecker.isSemSnapshot;
import static top.infra.maven.shared.extension.Constants.BOOL_STRING_TRUE;
import static top.infra.maven.shared.extension.Constants.GIT_REF_NAME_DEVELOP;
import static top.infra.maven.shared.extension.MavenOption.GENERATEREPORTS;
import static top.infra.maven.shared.extension.VcsProperties.GIT_REF_NAME;

import java.util.Map;
import java.util.Properties;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import top.infra.maven.CiOptionContext;
import top.infra.maven.shared.DefaultCiOptionContext;
import top.infra.maven.test.extension.OptionCollections;
import top.infra.maven.shared.utils.PropertiesUtils;

public class ProjectVersionTest {

    private static final org.slf4j.Logger slf4jLogger = LoggerFactory.getLogger(ProjectVersionTest.class);

    @Test
    public void testSemanticSnapshotVersion() {
        assertTrue(isSemSnapshot("2.0.1-SNAPSHOT"));
        assertTrue(isSemSnapshot("1.0.0-SNAPSHOT"));
        assertTrue(isSemSnapshot("1.0-SNAPSHOT"));
        assertTrue(isSemSnapshot("1-SNAPSHOT"));

        assertTrue(isSemFeature("2.0.1-feature_1-SNAPSHOT"));
        assertTrue(isSemFeature("1.0.0-feature_1-SNAPSHOT"));
        assertTrue(isSemFeature("1.0-feature_1-SNAPSHOT"));
        assertTrue(isSemFeature("1-feature_1-SNAPSHOT"));

        assertFalse(isSemSnapshot("2.0.1-feature-SNAPSHOT"));
        assertTrue(isSemFeature("2.0.1-feature-SNAPSHOT"));
        assertFalse(isSemSnapshot("2.0.1"));
        assertTrue(isSemRelease("2.0.1"));
    }

    @Test
    public void testVersionsOnDevelopBranch() {
        final Properties systemProperties = new Properties();
        systemProperties.setProperty(GIT_REF_NAME.getSystemPropertyName(), GIT_REF_NAME_DEVELOP);

        final Properties userProperties = new Properties();
        userProperties.setProperty(GENERATEREPORTS.getPropertyName(), BOOL_STRING_TRUE);

        // gitProperties if needed

        final CiOptionContext ciOptContext = new DefaultCiOptionContext(
            systemProperties,
            userProperties
        );

        final Properties loadedProperties = new Properties();
        PropertiesUtils.setSystemPropertiesIfAbsent(ciOptContext.getSystemProperties(), loadedProperties);

        final Properties newProperties = ciOptContext.setCiOptPropertiesInto(OptionCollections.optionCollections(), userProperties);

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

        final Properties userProperties = new Properties();
        userProperties.setProperty(GENERATEREPORTS.getPropertyName(), BOOL_STRING_TRUE);

        // gitProperties if needed

        final CiOptionContext ciOptContext = new DefaultCiOptionContext(
            systemProperties,
            userProperties
        );

        final Properties loadedProperties = new Properties();
        PropertiesUtils.setSystemPropertiesIfAbsent(ciOptContext.getSystemProperties(), loadedProperties);

        final Properties newProperties = ciOptContext.setCiOptPropertiesInto(OptionCollections.optionCollections(), userProperties);

        final String projectVersion = "2.0.1-feature1-SNAPSHOT";
        final Map.Entry<Boolean, RuntimeException> checkProjectVersionResult = checkProjectVersion(ciOptContext, projectVersion);
        slf4jLogger.info("checkProjectVersion result: [{}]", checkProjectVersionResult);
        assertTrue(checkProjectVersionResult.getKey());

        assertFalse(checkProjectVersion(ciOptContext, "2.0.1-SNAPSHOT").getKey());
        assertFalse(checkProjectVersion(ciOptContext, "2.0.1").getKey());
    }
}
