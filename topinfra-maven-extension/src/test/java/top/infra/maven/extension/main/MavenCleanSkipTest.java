package top.infra.maven.extension.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static top.infra.maven.extension.main.MavenGoalEditorTest.goalsAndUserProps;
import static top.infra.maven.shared.extension.Constants.BOOL_STRING_FALSE;
import static top.infra.maven.shared.extension.Constants.BOOL_STRING_TRUE;
import static top.infra.maven.shared.extension.Constants.PHASE_CLEAN;
import static top.infra.maven.shared.extension.Constants.PHASE_DEPLOY;
import static top.infra.maven.shared.extension.Constants.PHASE_SITE_DEPLOY;
import static top.infra.maven.shared.extension.Constants.PROP_MAVEN_CLEAN_SKIP;
import static top.infra.maven.test.utils.TestUtils.blankCiOptCtx;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import top.infra.maven.CiOptionContext;

public class MavenCleanSkipTest {

    private static final Logger logger = LoggerFactory.getLogger(MavenCleanSkipTest.class);

    @Test
    public void testMavenCleanSkipIfGoalCleanAbsent() {
        final CiOptionContext ciOptionContext = blankCiOptCtx();

        final List<String> requestedGoals = Arrays.asList(PHASE_DEPLOY, PHASE_SITE_DEPLOY);

        final Entry<List<String>, Properties> goalsAndProps = goalsAndUserProps(ciOptionContext, requestedGoals);
        final Collection<String> resultGoals = goalsAndProps.getKey();
        final Properties additionalUserProperties = goalsAndProps.getValue();

        assertTrue(resultGoals.contains(PHASE_DEPLOY));
        assertTrue(resultGoals.contains(PHASE_SITE_DEPLOY));
        assertEquals(2, resultGoals.size());
        assertEquals(BOOL_STRING_TRUE, additionalUserProperties.getProperty(PROP_MAVEN_CLEAN_SKIP));
    }

    @Test
    public void testMavenCleanSkipIfGoalCleanPresent() {
        final CiOptionContext ciOptionContext = blankCiOptCtx();

        final List<String> requestedGoals = Arrays.asList(PHASE_CLEAN, PHASE_DEPLOY);

        final Entry<List<String>, Properties> goalsAndProps = goalsAndUserProps(ciOptionContext, requestedGoals);
        final Collection<String> resultGoals = goalsAndProps.getKey();
        final Properties additionalUserProperties = goalsAndProps.getValue();

        assertTrue(resultGoals.contains(PHASE_CLEAN));
        assertTrue(resultGoals.contains(PHASE_DEPLOY));
        assertEquals(2, resultGoals.size());
        assertEquals(BOOL_STRING_FALSE, additionalUserProperties.getProperty(PROP_MAVEN_CLEAN_SKIP));
    }

    @Test
    public void testMavenCleanSkipUserSpecified() {
        final CiOptionContext ciOptionContext = blankCiOptCtx();
        ciOptionContext.getUserProperties().setProperty(PROP_MAVEN_CLEAN_SKIP, BOOL_STRING_TRUE);

        final List<String> requestedGoals = Arrays.asList(PHASE_CLEAN, PHASE_DEPLOY);

        final Entry<List<String>, Properties> goalsAndProps = goalsAndUserProps(ciOptionContext, requestedGoals);
        final Collection<String> resultGoals = goalsAndProps.getKey();
        final Properties additionalUserProperties = goalsAndProps.getValue();

        assertNull(additionalUserProperties.getProperty(PROP_MAVEN_CLEAN_SKIP));
    }
}
