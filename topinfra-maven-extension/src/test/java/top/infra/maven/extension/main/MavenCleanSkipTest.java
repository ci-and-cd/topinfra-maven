package top.infra.maven.extension.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static top.infra.maven.shared.extension.Constants.BOOL_STRING_FALSE;
import static top.infra.maven.shared.extension.Constants.BOOL_STRING_TRUE;
import static top.infra.maven.shared.extension.Constants.PROP_MAVEN_CLEAN_SKIP;

import java.util.Arrays;
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

public class MavenCleanSkipTest {

    private static final Logger logger = LoggerFactory.getLogger(MavenCleanSkipTest.class);

    @Test
    public void testMavenCleanSkipIfGoalCleanAbsent() {
        final Properties systemProperties = new Properties();
        final Properties userProperties = new Properties();
        final CiOptionContext ciOptionContext = new DefaultCiOptionContext(systemProperties, userProperties);
        final MavenGoalEditor editor = MavenGoalEditor.newMavenGoalEditor(new LoggerSlf4jImpl(logger), ciOptionContext);

        final List<String> requestedGoals = Arrays.asList(Constants.PHASE_DEPLOY, Constants.PHASE_SITE_DEPLOY);
        final Collection<String> resultGoals = editor.editGoals(requestedGoals);
        final Properties additionalUserProperties = editor.additionalUserProperties(ciOptionContext, requestedGoals, resultGoals);
        logger.info("requestedGoals: {}", requestedGoals);
        logger.info("resultGoals: {}", resultGoals);
        logger.info("additionalUserProperties: {}", additionalUserProperties);
        assertEquals(BOOL_STRING_TRUE, additionalUserProperties.getProperty(PROP_MAVEN_CLEAN_SKIP));
    }

    @Test
    public void testMavenCleanSkipIfGoalCleanPresent() {
        final Properties systemProperties = new Properties();
        final Properties userProperties = new Properties();
        final CiOptionContext ciOptionContext = new DefaultCiOptionContext(systemProperties, userProperties);
        final MavenGoalEditor editor = MavenGoalEditor.newMavenGoalEditor(new LoggerSlf4jImpl(logger), ciOptionContext);

        final List<String> requestedGoals = Arrays.asList(Constants.PHASE_CLEAN, Constants.PHASE_DEPLOY);
        final Collection<String> resultGoals = editor.editGoals(requestedGoals);
        final Properties additionalUserProperties = editor.additionalUserProperties(ciOptionContext, requestedGoals, resultGoals);
        logger.info("requestedGoals: {}", requestedGoals);
        logger.info("resultGoals: {}", resultGoals);
        logger.info("additionalUserProperties: {}", additionalUserProperties);
        assertEquals(BOOL_STRING_FALSE, additionalUserProperties.getProperty(PROP_MAVEN_CLEAN_SKIP));
    }

    @Test
    public void testMavenCleanSkipUserSpecified() {
        final Properties systemProperties = new Properties();
        final Properties userProperties = new Properties();
        userProperties.setProperty(PROP_MAVEN_CLEAN_SKIP, BOOL_STRING_TRUE);
        final CiOptionContext ciOptionContext = new DefaultCiOptionContext(systemProperties, userProperties);
        final MavenGoalEditor editor = MavenGoalEditor.newMavenGoalEditor(new LoggerSlf4jImpl(logger), ciOptionContext);

        final List<String> requestedGoals = Arrays.asList(Constants.PHASE_CLEAN, Constants.PHASE_DEPLOY);
        final Collection<String> resultGoals = editor.editGoals(requestedGoals);
        final Properties additionalUserProperties = editor.additionalUserProperties(ciOptionContext, requestedGoals, resultGoals);
        logger.info("requestedGoals: {}", requestedGoals);
        logger.info("resultGoals: {}", resultGoals);
        logger.info("additionalUserProperties: {}", additionalUserProperties);
        assertNull(additionalUserProperties.getProperty(PROP_MAVEN_CLEAN_SKIP));
    }
}
