package top.infra.maven.extension.main;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static top.infra.maven.extension.main.MavenGoalEditor.PROP_MVN_DEPLOY_PUBLISH_SEGREGATION_GOAL;
import static top.infra.maven.extension.main.MavenGoalEditor.PROP_MVN_DEPLOY_PUBLISH_SEGREGATION_GOAL_INSTALL;
import static top.infra.maven.extension.main.MavenGoalEditor.PROP_MVN_DEPLOY_PUBLISH_SEGREGATION_GOAL_PACKAGE;
import static top.infra.maven.shared.extension.Constants.BOOL_STRING_FALSE;
import static top.infra.maven.shared.extension.Constants.BOOL_STRING_TRUE;
import static top.infra.maven.shared.extension.Constants.PROP_MAVEN_PACKAGES_SKIP;
import static top.infra.maven.shared.extension.Constants.PROP_MVN_DEPLOY_PUBLISH_SEGREGATION_GOAL_DEPLOY;
import static top.infra.maven.shared.extension.Constants.PROP_NEXUS2_STAGING;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import top.infra.maven.CiOptionContext;
import top.infra.maven.shared.DefaultCiOptionContext;
import top.infra.maven.shared.extension.Constants;
import top.infra.maven.test.logging.LoggerSlf4jImpl;

public class MultiStageBuildTest {

    private static final Logger logger = LoggerFactory.getLogger(MultiStageBuildTest.class);

    @Test
    public void testGoalPackageReplacedByDeployAtFirstStage() {
        final CiOptionContext ciOptionContext = ciOptionContext();
        final MavenGoalEditor editor = MavenGoalEditor.newMavenGoalEditor(new LoggerSlf4jImpl(logger), ciOptionContext);

        final List<String> requestedGoals = singletonList(Constants.PHASE_PACKAGE);
        final Collection<String> resultGoals = editor.editGoals(requestedGoals);
        final Properties additionalUserProperties = editor.additionalUserProperties(ciOptionContext, requestedGoals, resultGoals);
        logger.info("requestedGoals: {}", requestedGoals);
        logger.info("resultGoals: {}", resultGoals);
        logger.info("additionalUserProperties: {}", additionalUserProperties);
        assertFalse(resultGoals.contains(Constants.PHASE_PACKAGE));
        assertTrue(resultGoals.contains(Constants.PHASE_DEPLOY));
        assertEquals(BOOL_STRING_FALSE, additionalUserProperties.getProperty(PROP_MVN_DEPLOY_PUBLISH_SEGREGATION_GOAL_DEPLOY));
        assertEquals(BOOL_STRING_FALSE, additionalUserProperties.getProperty(PROP_MVN_DEPLOY_PUBLISH_SEGREGATION_GOAL_INSTALL));
        assertEquals(BOOL_STRING_TRUE, additionalUserProperties.getProperty(PROP_MVN_DEPLOY_PUBLISH_SEGREGATION_GOAL_PACKAGE));
        assertEquals(Constants.PHASE_PACKAGE, additionalUserProperties.getProperty(PROP_MVN_DEPLOY_PUBLISH_SEGREGATION_GOAL));
        assertEquals(BOOL_STRING_FALSE, additionalUserProperties.getProperty(PROP_NEXUS2_STAGING));
    }

    @Test
    public void testGoalInstallReplacedByDeployAtFirstStage() {
        final CiOptionContext ciOptionContext = ciOptionContext();
        final MavenGoalEditor editor = MavenGoalEditor.newMavenGoalEditor(new LoggerSlf4jImpl(logger), ciOptionContext);

        final List<String> requestedGoals = singletonList(Constants.PHASE_INSTALL);
        final Collection<String> resultGoals = editor.editGoals(requestedGoals);
        final Properties additionalUserProperties = editor.additionalUserProperties(ciOptionContext, requestedGoals, resultGoals);
        logger.info("requestedGoals: {}", requestedGoals);
        logger.info("resultGoals: {}", resultGoals);
        logger.info("additionalUserProperties: {}", additionalUserProperties);
        assertFalse(resultGoals.contains(Constants.PHASE_INSTALL));
        assertTrue(resultGoals.contains(Constants.PHASE_DEPLOY));
        assertEquals(BOOL_STRING_FALSE, additionalUserProperties.getProperty(PROP_MVN_DEPLOY_PUBLISH_SEGREGATION_GOAL_DEPLOY));
        assertEquals(BOOL_STRING_TRUE, additionalUserProperties.getProperty(PROP_MVN_DEPLOY_PUBLISH_SEGREGATION_GOAL_INSTALL));
        assertEquals(BOOL_STRING_TRUE, additionalUserProperties.getProperty(PROP_MVN_DEPLOY_PUBLISH_SEGREGATION_GOAL_PACKAGE));
        assertEquals(Constants.PHASE_INSTALL, additionalUserProperties.getProperty(PROP_MVN_DEPLOY_PUBLISH_SEGREGATION_GOAL));
        assertEquals(BOOL_STRING_FALSE, additionalUserProperties.getProperty(PROP_NEXUS2_STAGING));
    }

    @Test
    public void testGoalInstallReplacedByDeployAtFirstStageAndMavenPackagesSkip() {
        final CiOptionContext ciOptionContext = ciOptionContext();
        ciOptionContext.getUserProperties().setProperty(PROP_MAVEN_PACKAGES_SKIP, BOOL_STRING_TRUE);
        final MavenGoalEditor editor = MavenGoalEditor.newMavenGoalEditor(new LoggerSlf4jImpl(logger), ciOptionContext);

        final List<String> requestedGoals = singletonList(Constants.PHASE_INSTALL);
        final Collection<String> resultGoals = editor.editGoals(requestedGoals);
        final Properties additionalUserProperties = editor.additionalUserProperties(ciOptionContext, requestedGoals, resultGoals);
        logger.info("requestedGoals: {}", requestedGoals);
        logger.info("resultGoals: {}", resultGoals);
        logger.info("additionalUserProperties: {}", additionalUserProperties);
        assertFalse(resultGoals.contains(Constants.PHASE_INSTALL));
        assertTrue(resultGoals.contains(Constants.PHASE_DEPLOY));
        assertEquals(BOOL_STRING_FALSE, additionalUserProperties.getProperty(PROP_MVN_DEPLOY_PUBLISH_SEGREGATION_GOAL_DEPLOY));
        assertEquals(BOOL_STRING_TRUE, additionalUserProperties.getProperty(PROP_MVN_DEPLOY_PUBLISH_SEGREGATION_GOAL_INSTALL));
        assertEquals(BOOL_STRING_FALSE, additionalUserProperties.getProperty(PROP_MVN_DEPLOY_PUBLISH_SEGREGATION_GOAL_PACKAGE));
        assertEquals(Constants.PHASE_INSTALL, additionalUserProperties.getProperty(PROP_MVN_DEPLOY_PUBLISH_SEGREGATION_GOAL));
        assertEquals(BOOL_STRING_FALSE, additionalUserProperties.getProperty(PROP_NEXUS2_STAGING));
    }

    private CiOptionContext ciOptionContext() {
        final Properties systemProperties = new Properties();
        final Properties userProperties = new Properties();
        userProperties.setProperty(Constants.PROP_MVN_DEPLOY_PUBLISH_SEGREGATION, BOOL_STRING_TRUE);
        userProperties.setProperty(Constants.PROP_NEXUS2_STAGING, BOOL_STRING_FALSE);
        userProperties.setProperty(Constants.PROP_PUBLISH_TO_REPO, BOOL_STRING_TRUE);
        return new DefaultCiOptionContext(systemProperties, userProperties);
    }
}
