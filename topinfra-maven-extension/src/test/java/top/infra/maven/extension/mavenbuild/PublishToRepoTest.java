package top.infra.maven.extension.mavenbuild;

import static java.lang.Boolean.FALSE;
import static org.junit.Assert.assertTrue;
import static top.infra.maven.Constants.BOOL_STRING_TRUE;
import static top.infra.maven.Constants.GIT_REF_NAME_DEVELOP;
import static top.infra.maven.extension.MavenBuildExtensionOption.ORIGIN_REPO;
import static top.infra.maven.extension.MavenBuildExtensionOption.PUBLISH_TO_REPO;
import static top.infra.maven.extension.MavenOption.GENERATEREPORTS;
import static top.infra.maven.extension.VcsProperties.GIT_REF_NAME;

import java.util.Optional;
import java.util.Properties;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import top.infra.maven.core.CiOptionContext;
import top.infra.maven.extension.OptionCollections;

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

        final CiOptionContext ciOptContext = new CiOptionContext(
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
    }

    @Test
    public void testFeatureBranch() {
        final Properties systemProperties = new Properties();
        systemProperties.setProperty(GIT_REF_NAME.getSystemPropertyName(), "feature/feature1");
        systemProperties.setProperty(ORIGIN_REPO.getSystemPropertyName(), BOOL_STRING_TRUE);

        final Properties userProperties = new Properties();
        userProperties.setProperty(GENERATEREPORTS.getPropertyName(), BOOL_STRING_TRUE);

        // gitProperties if needed

        final CiOptionContext ciOptContext = new CiOptionContext(
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
    }
}
