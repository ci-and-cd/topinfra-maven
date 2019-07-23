package top.infra.maven.extension.main;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static top.infra.maven.extension.main.MavenGoalEditor.PROP_MVN_MULTI_STAGE_BUILD_GOAL;
import static top.infra.maven.extension.main.MavenGoalEditor.PROP_MVN_MULTI_STAGE_BUILD_GOAL_INSTALL;
import static top.infra.maven.extension.main.MavenGoalEditor.PROP_MVN_MULTI_STAGE_BUILD_GOAL_PACKAGE;
import static top.infra.maven.extension.main.MavenGoalEditorTest.goalsAndUserProps;
import static top.infra.maven.shared.extension.Constants.BOOL_STRING_FALSE;
import static top.infra.maven.shared.extension.Constants.BOOL_STRING_TRUE;
import static top.infra.maven.shared.extension.Constants.PHASE_CLEAN;
import static top.infra.maven.shared.extension.Constants.PHASE_DEPLOY;
import static top.infra.maven.shared.extension.Constants.PHASE_INSTALL;
import static top.infra.maven.shared.extension.Constants.PHASE_INTEGRATION_TEST;
import static top.infra.maven.shared.extension.Constants.PHASE_PACKAGE;
import static top.infra.maven.shared.extension.Constants.PHASE_POST_INTEGRATION_TEST;
import static top.infra.maven.shared.extension.Constants.PHASE_PRE_INTEGRATION_TEST;
import static top.infra.maven.shared.extension.Constants.PHASE_VERIFY;
import static top.infra.maven.shared.extension.Constants.PROP_MAVEN_CLEAN_SKIP;
import static top.infra.maven.shared.extension.Constants.PROP_MAVEN_INSTALL_SKIP;
import static top.infra.maven.shared.extension.Constants.PROP_MAVEN_PACKAGES_SKIP;
import static top.infra.maven.shared.extension.Constants.PROP_MVN_MULTI_STAGE_BUILD;
import static top.infra.maven.shared.extension.Constants.PROP_MVN_MULTI_STAGE_BUILD_GOAL_DEPLOY;
import static top.infra.maven.shared.extension.Constants.PROP_NEXUS2_STAGING;
import static top.infra.maven.shared.extension.Constants.PROP_PUBLISH_TO_REPO;
import static top.infra.maven.test.utils.TestUtils.blankCiOptCtx;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import top.infra.maven.CiOptionContext;
import top.infra.maven.shared.utils.MavenBuildPomUtils;

public class MultiStageBuildTest {

    private static final Logger logger = LoggerFactory.getLogger(MultiStageBuildTest.class);

    @Test
    public void testAutoDisableNexus2StagingIfNotSpecified() throws IOException {
        final CiOptionContext ciOptionContext = blankCiOptCtx();
        final Properties userProperties = ciOptionContext.getUserProperties();
        userProperties.setProperty(PROP_MVN_MULTI_STAGE_BUILD, BOOL_STRING_TRUE);
        userProperties.setProperty(PROP_PUBLISH_TO_REPO, BOOL_STRING_TRUE);

        final List<String> requestedGoals = singletonList(PHASE_DEPLOY);

        final Entry<List<String>, Properties> deployTrue;
        final Entry<List<String>, Properties> deployFalse;

        final Path altDeployRepo = MavenBuildPomUtils.altDeploymentRepositoryPath(ciOptionContext);
        logger.info("altDeployRepo: {}", altDeployRepo);
        if (altDeployRepo.toFile().exists()) {
            deployTrue = goalsAndUserProps(ciOptionContext, requestedGoals);

            Files.deleteIfExists(altDeployRepo);
            deployFalse = goalsAndUserProps(ciOptionContext, requestedGoals);
        } else {
            deployFalse = goalsAndUserProps(ciOptionContext, requestedGoals);

            Files.createDirectories(altDeployRepo);
            deployTrue = goalsAndUserProps(ciOptionContext, requestedGoals);
            Files.deleteIfExists(altDeployRepo);
        }

        assertEquals(BOOL_STRING_FALSE, deployTrue.getValue().getProperty(PROP_NEXUS2_STAGING));
        assertTrue(deployTrue.getKey().contains(PHASE_DEPLOY));
        assertEquals(1, deployTrue.getKey().size());
        assertEquals(BOOL_STRING_TRUE, deployTrue.getValue().getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL_DEPLOY));
        assertEquals(BOOL_STRING_FALSE, deployTrue.getValue().getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL_INSTALL));
        assertEquals(BOOL_STRING_TRUE, deployTrue.getValue().getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL_PACKAGE));
        assertEquals(PHASE_DEPLOY, deployTrue.getValue().getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL));

        assertEquals(BOOL_STRING_FALSE, deployFalse.getValue().getProperty(PROP_NEXUS2_STAGING));
        assertTrue(deployFalse.getKey().contains(PHASE_DEPLOY));
        assertEquals(1, deployFalse.getKey().size());
        assertEquals(BOOL_STRING_FALSE, deployFalse.getValue().getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL_DEPLOY));
        assertEquals(BOOL_STRING_FALSE, deployFalse.getValue().getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL_INSTALL));
        assertEquals(BOOL_STRING_TRUE, deployFalse.getValue().getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL_PACKAGE));
        assertEquals(PHASE_PACKAGE, deployFalse.getValue().getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL));
    }

    @Test
    public void testAutoSkipMavenInstall() {
        assertEquals(BOOL_STRING_TRUE,
            goalsAndUserProps(ciOptionContext(), asList(PHASE_CLEAN, PHASE_PACKAGE)).getValue().getProperty(PROP_MAVEN_INSTALL_SKIP));
        assertEquals(BOOL_STRING_TRUE,
            goalsAndUserProps(ciOptionContext(), asList(PHASE_CLEAN, PHASE_VERIFY)).getValue().getProperty(PROP_MAVEN_INSTALL_SKIP));

        assertEquals(BOOL_STRING_TRUE,
            goalsAndUserProps(ciOptionContext(), asList(PHASE_CLEAN, PHASE_DEPLOY)).getValue().getProperty(PROP_MAVEN_INSTALL_SKIP));

        final CiOptionContext ctxNexus2StagingAbsent = blankCiOptCtx();
        ctxNexus2StagingAbsent.getUserProperties().setProperty(PROP_MVN_MULTI_STAGE_BUILD, BOOL_STRING_TRUE);
        ctxNexus2StagingAbsent.getUserProperties().setProperty(PROP_PUBLISH_TO_REPO, BOOL_STRING_TRUE);
        assertNull(
            goalsAndUserProps(ctxNexus2StagingAbsent, asList(PHASE_CLEAN, PHASE_DEPLOY)).getValue().getProperty(PROP_MAVEN_INSTALL_SKIP));

        assertNull(
            goalsAndUserProps(ciOptionContext(), asList(PHASE_INSTALL, PHASE_DEPLOY)).getValue().getProperty(PROP_MAVEN_INSTALL_SKIP));
    }

    @Test
    public void testAutoSkipMavenPackages() {
        assertNull(goalsAndUserProps(
            ciOptionContext(),
            asList(PHASE_PACKAGE, PHASE_DEPLOY)).getValue().getProperty(PROP_MAVEN_PACKAGES_SKIP));
        assertNull(goalsAndUserProps(
            ciOptionContext(),
            asList(PHASE_PRE_INTEGRATION_TEST, PHASE_DEPLOY)).getValue().getProperty(PROP_MAVEN_PACKAGES_SKIP));
        assertNull(goalsAndUserProps(
            ciOptionContext(),
            asList(PHASE_INTEGRATION_TEST, PHASE_DEPLOY)).getValue().getProperty(PROP_MAVEN_PACKAGES_SKIP));
        assertNull(goalsAndUserProps(
            ciOptionContext(),
            asList(PHASE_POST_INTEGRATION_TEST, PHASE_DEPLOY)).getValue().getProperty(PROP_MAVEN_PACKAGES_SKIP));
        assertNull(goalsAndUserProps(
            ciOptionContext(),
            asList(PHASE_VERIFY, PHASE_DEPLOY)).getValue().getProperty(PROP_MAVEN_PACKAGES_SKIP));
        assertNull(goalsAndUserProps(
            ciOptionContext(),
            asList(PHASE_INSTALL, PHASE_DEPLOY)).getValue().getProperty(PROP_MAVEN_PACKAGES_SKIP));

        final Entry<List<String>, Properties> goalsAndProps = goalsAndUserProps(ciOptionContext(), singletonList(PHASE_DEPLOY));
        assertEquals(BOOL_STRING_TRUE, goalsAndProps.getValue().getProperty(PROP_MAVEN_PACKAGES_SKIP));
        assertEquals(BOOL_STRING_FALSE, goalsAndProps.getValue().getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL_PACKAGE));
        assertEquals(BOOL_STRING_FALSE, goalsAndProps.getValue().getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL_INSTALL));
        assertEquals(BOOL_STRING_TRUE, goalsAndProps.getValue().getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL_DEPLOY));
    }

    @Test
    public void testCleanDeploy() throws IOException {
        final CiOptionContext ciOptCtx = blankCiOptCtx();
        ciOptCtx.getUserProperties().setProperty(PROP_MVN_MULTI_STAGE_BUILD, BOOL_STRING_TRUE);
        ciOptCtx.getUserProperties().setProperty(PROP_PUBLISH_TO_REPO, BOOL_STRING_TRUE);
        final List<String> requestedGoals = asList(PHASE_CLEAN, PHASE_DEPLOY);

        final Path altDeployRepo = MavenBuildPomUtils.altDeploymentRepositoryPath(ciOptCtx);
        logger.info("altDeployRepo: {}", altDeployRepo);
        if (!altDeployRepo.toFile().exists()) {
            Files.createDirectories(altDeployRepo);
        }

        final Entry<List<String>, Properties> goalsAndProps = goalsAndUserProps(ciOptCtx, requestedGoals);
        final Collection<String> resultGoals = goalsAndProps.getKey();
        final Properties additionalUserProperties = goalsAndProps.getValue();

        assertTrue(resultGoals.contains(PHASE_CLEAN));
        assertTrue(resultGoals.contains(PHASE_DEPLOY));
        assertEquals(BOOL_STRING_FALSE, additionalUserProperties.getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL_DEPLOY));
        assertEquals(BOOL_STRING_FALSE, additionalUserProperties.getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL_INSTALL));
        assertEquals(BOOL_STRING_TRUE, additionalUserProperties.getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL_PACKAGE));
        assertEquals(PHASE_PACKAGE, additionalUserProperties.getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL));
        assertEquals(BOOL_STRING_FALSE, additionalUserProperties.getProperty(PROP_NEXUS2_STAGING));
    }

    @Test
    public void testCleanVerify() {
        final CiOptionContext ciOptCtx = blankCiOptCtx();
        ciOptCtx.getUserProperties().setProperty(PROP_MVN_MULTI_STAGE_BUILD, BOOL_STRING_TRUE);
        ciOptCtx.getUserProperties().setProperty(PROP_PUBLISH_TO_REPO, BOOL_STRING_TRUE);
        final List<String> requestedGoals = asList(PHASE_CLEAN, PHASE_VERIFY);

        final Entry<List<String>, Properties> goalsAndProps = goalsAndUserProps(ciOptCtx, requestedGoals);
        final Collection<String> resultGoals = goalsAndProps.getKey();
        final Properties additionalUserProperties = goalsAndProps.getValue();

        assertTrue(resultGoals.contains(PHASE_CLEAN));
        assertTrue(resultGoals.contains(PHASE_DEPLOY));
        // TODO -Dmaven.javadoc.skip=true -Dmaven.source.skip=true ?
        assertEquals(BOOL_STRING_FALSE, additionalUserProperties.getProperty(PROP_MAVEN_CLEAN_SKIP));
        assertEquals(BOOL_STRING_TRUE, additionalUserProperties.getProperty(PROP_MAVEN_INSTALL_SKIP));
        assertEquals(BOOL_STRING_FALSE, additionalUserProperties.getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL_DEPLOY));
        assertEquals(BOOL_STRING_FALSE, additionalUserProperties.getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL_INSTALL));
        assertEquals(BOOL_STRING_TRUE, additionalUserProperties.getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL_PACKAGE));
        assertEquals(PHASE_PACKAGE, additionalUserProperties.getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL));
        assertEquals(BOOL_STRING_FALSE, additionalUserProperties.getProperty(PROP_NEXUS2_STAGING));
    }

    @Test
    public void testDeploy() throws IOException {
        final CiOptionContext ciOptCtx = blankCiOptCtx();
        ciOptCtx.getUserProperties().setProperty(PROP_MVN_MULTI_STAGE_BUILD, BOOL_STRING_TRUE);
        ciOptCtx.getUserProperties().setProperty(PROP_NEXUS2_STAGING, BOOL_STRING_FALSE);
        ciOptCtx.getUserProperties().setProperty(PROP_PUBLISH_TO_REPO, BOOL_STRING_TRUE);
        final List<String> requestedGoals = singletonList(PHASE_DEPLOY);

        final Entry<List<String>, Properties> altRepoNotExists = goalsAndUserProps(ciOptCtx, requestedGoals);
        assertEquals(BOOL_STRING_FALSE, altRepoNotExists.getValue().getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL_DEPLOY));

        final Path altDeployRepo = MavenBuildPomUtils.altDeploymentRepositoryPath(ciOptCtx);
        logger.info("altDeployRepo: {}", altDeployRepo);
        try {
            Files.createDirectories(altDeployRepo);

            final Entry<List<String>, Properties> altRepoExists = goalsAndUserProps(ciOptCtx, requestedGoals);
            assertEquals(BOOL_STRING_FALSE, altRepoExists.getValue().getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL_INSTALL));
            assertEquals(BOOL_STRING_FALSE, altRepoExists.getValue().getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL_PACKAGE));
            assertEquals(PHASE_DEPLOY, altRepoExists.getValue().getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL));
            assertNull(altRepoExists.getValue().getProperty(PROP_NEXUS2_STAGING));
        } finally {
            Files.deleteIfExists(altDeployRepo);
        }
    }

    @Test
    public void testGoalPackageReplacedByDeployAtFirstStage() {
        final CiOptionContext ciOptionContext = ciOptionContext();
        final List<String> requestedGoals = singletonList(PHASE_PACKAGE);

        final Entry<List<String>, Properties> goalsAndProps = goalsAndUserProps(ciOptionContext, requestedGoals);
        final Collection<String> resultGoals = goalsAndProps.getKey();
        final Properties additionalUserProperties = goalsAndProps.getValue();

        assertFalse(resultGoals.contains(PHASE_PACKAGE));
        assertTrue(resultGoals.contains(PHASE_DEPLOY));
        assertEquals(BOOL_STRING_FALSE, additionalUserProperties.getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL_DEPLOY));
        assertEquals(BOOL_STRING_FALSE, additionalUserProperties.getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL_INSTALL));
        assertEquals(BOOL_STRING_TRUE, additionalUserProperties.getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL_PACKAGE));
        assertEquals(PHASE_PACKAGE, additionalUserProperties.getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL));
        assertEquals(BOOL_STRING_FALSE, additionalUserProperties.getProperty(PROP_NEXUS2_STAGING));
    }

    @Test
    public void testGoalInstallReplacedByDeployAtFirstStage() {
        final CiOptionContext ciOptionContext = ciOptionContext();
        final List<String> requestedGoals = singletonList(PHASE_INSTALL);

        final Entry<List<String>, Properties> goalsAndProps = goalsAndUserProps(ciOptionContext, requestedGoals);
        final Collection<String> resultGoals = goalsAndProps.getKey();
        final Properties additionalUserProperties = goalsAndProps.getValue();

        assertFalse(resultGoals.contains(PHASE_INSTALL));
        assertTrue(resultGoals.contains(PHASE_DEPLOY));
        assertEquals(BOOL_STRING_FALSE, additionalUserProperties.getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL_DEPLOY));
        assertEquals(BOOL_STRING_TRUE, additionalUserProperties.getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL_INSTALL));
        assertEquals(BOOL_STRING_TRUE, additionalUserProperties.getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL_PACKAGE));
        assertEquals(PHASE_INSTALL, additionalUserProperties.getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL));
        assertEquals(BOOL_STRING_FALSE, additionalUserProperties.getProperty(PROP_NEXUS2_STAGING));
    }

    @Test
    public void testGoalInstallReplacedByDeployAtFirstStageAndMavenPackagesSkip() {
        final CiOptionContext ciOptionContext = ciOptionContext();
        ciOptionContext.getUserProperties().setProperty(PROP_MAVEN_PACKAGES_SKIP, BOOL_STRING_TRUE);

        final List<String> requestedGoals = singletonList(PHASE_INSTALL);

        final Entry<List<String>, Properties> goalsAndProps = goalsAndUserProps(ciOptionContext, requestedGoals);
        final Collection<String> resultGoals = goalsAndProps.getKey();
        final Properties additionalUserProperties = goalsAndProps.getValue();

        assertFalse(resultGoals.contains(PHASE_INSTALL));
        assertTrue(resultGoals.contains(PHASE_DEPLOY));
        assertEquals(BOOL_STRING_FALSE, additionalUserProperties.getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL_DEPLOY));
        assertEquals(BOOL_STRING_TRUE, additionalUserProperties.getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL_INSTALL));
        assertEquals(BOOL_STRING_FALSE, additionalUserProperties.getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL_PACKAGE));
        assertEquals(PHASE_INSTALL, additionalUserProperties.getProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL));
        assertEquals(BOOL_STRING_FALSE, additionalUserProperties.getProperty(PROP_NEXUS2_STAGING));
    }

    static CiOptionContext ciOptionContext() {
        final CiOptionContext result = blankCiOptCtx();
        final Properties userProperties = result.getUserProperties();
        userProperties.setProperty(PROP_MVN_MULTI_STAGE_BUILD, BOOL_STRING_TRUE);
        userProperties.setProperty(PROP_NEXUS2_STAGING, BOOL_STRING_FALSE);
        userProperties.setProperty(PROP_PUBLISH_TO_REPO, BOOL_STRING_TRUE);
        return result;
    }
}
