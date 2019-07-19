package top.infra.maven.extension.main;

import static java.lang.Boolean.FALSE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static top.infra.maven.extension.main.MavenBuildExtensionOption.ORIGIN_REPO;
import static top.infra.maven.extension.main.MavenBuildExtensionOption.PUBLISH_TO_REPO;
import static top.infra.maven.shared.extension.Constants.BOOL_STRING_TRUE;
import static top.infra.maven.shared.extension.Constants.GIT_REF_NAME_DEVELOP;
import static top.infra.maven.shared.extension.MavenOption.GENERATEREPORTS;
import static top.infra.maven.shared.extension.VcsProperties.GIT_REF_NAME;

import java.util.Optional;
import java.util.Properties;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import top.infra.maven.CiOptionContext;
import top.infra.maven.shared.DefaultCiOptionContext;
import top.infra.maven.shared.utils.PropertiesUtils;
import top.infra.maven.test.extension.OptionCollections;

public class PublishToRepoTest {

    private static final org.slf4j.Logger slf4jLogger = LoggerFactory.getLogger(PublishToRepoTest.class);

    @Test
    public void testDevelopBranch() {
        final Properties systemProperties = new Properties();
        systemProperties.setProperty(GIT_REF_NAME.getSystemPropertyName(), GIT_REF_NAME_DEVELOP);
        systemProperties.setProperty(ORIGIN_REPO.getSystemPropertyName(), BOOL_STRING_TRUE);

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

        final Optional<String> gitRefName = GIT_REF_NAME.getValue(ciOptContext);
        slf4jLogger.info("{} [{}]", GIT_REF_NAME.getPropertyName(), gitRefName.orElse(null));

        slf4jLogger.info("{} [{}]", PUBLISH_TO_REPO.getPropertyName(), PUBLISH_TO_REPO.getValue(ciOptContext).orElse(null));
        assertTrue(PUBLISH_TO_REPO.getValue(ciOptContext).map(Boolean::parseBoolean).orElse(FALSE));
    }

    @Test
    public void testFeatureBranch() {
        final Properties systemProperties = new Properties();
        systemProperties.setProperty(GIT_REF_NAME.getSystemPropertyName(), "feature/feature1");
        systemProperties.setProperty(ORIGIN_REPO.getSystemPropertyName(), BOOL_STRING_TRUE);

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

        final Optional<String> gitRefName = GIT_REF_NAME.getValue(ciOptContext);
        slf4jLogger.info("{} [{}]", GIT_REF_NAME.getPropertyName(), gitRefName.orElse(null));

        slf4jLogger.info("{} [{}]", PUBLISH_TO_REPO.getPropertyName(), PUBLISH_TO_REPO.getValue(ciOptContext).orElse(null));
        assertTrue(PUBLISH_TO_REPO.getValue(ciOptContext).map(Boolean::parseBoolean).orElse(FALSE));
    }

    @Test
    public void testRefNameAbsent() {
        final Properties systemProperties = new Properties();
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
        final Optional<String> gitRefName = GIT_REF_NAME.getValue(ciOptContext);

        slf4jLogger.info("{} [{}]", GIT_REF_NAME.getPropertyName(), gitRefName.orElse(null));
        assertFalse(gitRefName.isPresent());

        slf4jLogger.info("{} [{}]", PUBLISH_TO_REPO.getPropertyName(), PUBLISH_TO_REPO.getValue(ciOptContext).orElse(null));
        assertNull(PUBLISH_TO_REPO.getValue(ciOptContext).map(Boolean::parseBoolean).orElse(null));
    }
}
