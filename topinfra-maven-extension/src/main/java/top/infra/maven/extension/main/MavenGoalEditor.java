package top.infra.maven.extension.main;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;
import static top.infra.maven.extension.main.MavenBuildExtensionOption.MVN_MULTI_STAGE_BUILD;
import static top.infra.maven.extension.main.MavenBuildExtensionOption.ORIGIN_REPO;
import static top.infra.maven.extension.main.MavenBuildExtensionOption.PUBLISH_TO_REPO;
import static top.infra.maven.shared.extension.Constants.BOOL_STRING_FALSE;
import static top.infra.maven.shared.extension.Constants.BOOL_STRING_TRUE;
import static top.infra.maven.shared.extension.Constants.GIT_REF_NAME_DEVELOP;
import static top.infra.maven.shared.extension.Constants.PHASES_AFTER_PREPARE_PACKAGE;
import static top.infra.maven.shared.extension.Constants.PHASE_CLEAN;
import static top.infra.maven.shared.extension.Constants.PHASE_INSTALL;
import static top.infra.maven.shared.extension.Constants.PHASE_PACKAGE;
import static top.infra.maven.shared.extension.Constants.PHASE_VERIFY;
import static top.infra.maven.shared.extension.Constants.PROP_MAVEN_CLEAN_SKIP;
import static top.infra.maven.shared.extension.Constants.PROP_MAVEN_INSTALL_SKIP;
import static top.infra.maven.shared.extension.Constants.PROP_MAVEN_JAVADOC_SKIP;
import static top.infra.maven.shared.extension.Constants.PROP_MAVEN_PACKAGES_SKIP;
import static top.infra.maven.shared.extension.Constants.PROP_MAVEN_SOURCE_SKIP;
import static top.infra.maven.shared.extension.Constants.PROP_MVN_MULTI_STAGE_BUILD_GOAL_DEPLOY;
import static top.infra.maven.shared.extension.Constants.PROP_NEXUS2_STAGING;
import static top.infra.maven.shared.utils.MavenUtils.findInProperties;
import static top.infra.maven.shared.utils.SupportFunction.isEmpty;
import static top.infra.maven.shared.utils.SupportFunction.newTuple;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;

import top.infra.maven.CiOptionContext;
import top.infra.maven.logging.Logger;
import top.infra.maven.shared.MavenPhase;
import top.infra.maven.shared.extension.Constants;
import top.infra.maven.shared.extension.MavenOption;
import top.infra.maven.shared.extension.VcsProperties;
import top.infra.maven.shared.utils.MavenBuildPomUtils;

public class MavenGoalEditor {

    static final String PROP_MVN_MULTI_STAGE_BUILD_GOAL = "mvn.multi.stage.build.goal";
    static final String PROP_MVN_MULTI_STAGE_BUILD_GOAL_INSTALL = "mvn.multi.stage.build.goal.install";
    static final String PROP_MVN_MULTI_STAGE_BUILD_GOAL_PACKAGE = "mvn.multi.stage.build.goal.package";
    private static final Map<String, MavenPhase> phaseMap = Collections.unmodifiableMap(
        Arrays.stream(MavenPhase.values())
            .collect(toMap(MavenPhase::getPhase, Function.identity()))
    );
    private final Logger logger;
    private final Path altDeployRepoPath;
    private final Boolean generateReports;
    private final String gitRefName;
    private final Boolean mvnMultiStageBuild;
    private final Boolean originRepo;
    private final Boolean publishToRepo;

    public MavenGoalEditor(
        final Logger logger,
        final Path altDeployRepoPath,
        final Boolean generateReports,
        final String gitRefName,
        final Boolean mvnMultiStageBuild,
        final Boolean originRepo,
        final Boolean publishToRepo
    ) {
        this.logger = logger;

        this.altDeployRepoPath = altDeployRepoPath;
        this.generateReports = generateReports;
        this.gitRefName = gitRefName;
        this.mvnMultiStageBuild = mvnMultiStageBuild;
        this.originRepo = originRepo;
        this.publishToRepo = publishToRepo;
    }

    public static MavenGoalEditor newMavenGoalEditor(
        final Logger logger,
        final CiOptionContext ciOptContext
    ) {
        return new MavenGoalEditor(
            logger,
            MavenBuildPomUtils.altDeploymentRepositoryPath(ciOptContext),
            MavenOption.GENERATEREPORTS.getValue(ciOptContext).map(Boolean::parseBoolean).orElse(null),
            VcsProperties.GIT_REF_NAME.getValue(ciOptContext).orElse(null),
            MVN_MULTI_STAGE_BUILD.getValue(ciOptContext).map(Boolean::parseBoolean).orElse(FALSE),
            ORIGIN_REPO.getValue(ciOptContext).map(Boolean::parseBoolean).orElse(null),
            PUBLISH_TO_REPO.getValue(ciOptContext).map(Boolean::parseBoolean).orElse(null) // make sure version is valid too
        );
    }

    private static boolean isSiteGoal(final String goal) {
        return !isEmpty(goal) && goal.contains(Constants.PHASE_SITE);
    }

    private static boolean isDeployGoal(final String goal) {
        return !isEmpty(goal) && goal.endsWith(Constants.PHASE_DEPLOY) && !isSiteGoal(goal);
    }

    private static boolean isInstallGoal(final String goal) {
        return !isEmpty(goal) && !isDeployGoal(goal) && !isSiteGoal(goal) && goal.endsWith(Constants.PHASE_INSTALL);
    }

    private static Collection<String> pluginExecution(final Collection<String> in) {
        // things like
        // 'org.apache.maven.plugins:maven-antrun-plugin:run@wagon-repository-clean',
        // 'org.codehaus.mojo:wagon-maven-plugin:merge-maven-repos@merge-maven-repos-deploy'
        return in.stream().filter(it -> it.contains("@")).collect(toCollection(LinkedHashSet::new));
    }

    private static Collection<String> pluginGoal(final Collection<String> in) { // things like 'clean:clean', 'compiler:compile', 'jar:jar'
        return in.stream().filter(it -> it.contains(":") && !it.contains("@")).collect(toCollection(LinkedHashSet::new));
    }

    private static Collection<MavenPhase> phases(final Collection<String> in) { // things like 'clean', 'compile', 'package'.
        return in.stream()
            .filter(it -> !it.contains(":"))
            .map(phaseMap::get)
            .filter(Objects::nonNull)
            .collect(toCollection(LinkedHashSet::new));
    }

    private static boolean notIncludes(final Collection<MavenPhase> phases, final MavenPhase phase) {
        return !includes(phases, phase);
    }

    private static boolean includes(final Collection<MavenPhase> phases, final MavenPhase phase) {
        // TODO group phases by lifecycle then match phases in their own lifecycle
        return phases.stream().anyMatch(it -> it.ordinal() >= phase.ordinal());
    }

    public Entry<List<String>, Properties> goalsAndUserProperties(
        final CiOptionContext ciOptContext,
        final List<String> requestedGoals
    ) {
        final Collection<String> resultGoals = this.editGoals(requestedGoals);
        final Properties additionalUserProperties = this.additionalUserProperties(ciOptContext, requestedGoals, resultGoals);
        return newTuple(new ArrayList<>(resultGoals), additionalUserProperties);
    }

    public Collection<String> editGoals(
        final List<String> requestedGoals
    ) {
        final Collection<String> resultGoals = new LinkedHashSet<>();
        final Collection<MavenPhase> requestedPhases = phases(requestedGoals);

        for (final String goal : requestedGoals) {
            if (isDeployGoal(goal)) {
                // deploy, site-deploy
                if (this.publishToRepo == null || this.publishToRepo) {
                    resultGoals.add(goal);
                } else {
                    logger.info(String.format("    editGoals skip [%s] (%s: %s)",
                        goal, MavenBuildExtensionOption.PUBLISH_TO_REPO.getEnvVariableName(), this.publishToRepo));
                }
            } else if (isSiteGoal(goal)) {
                if (this.generateReports == null || this.generateReports) {
                    resultGoals.add(goal);
                } else {
                    logger.info(String.format("    editGoals skip [%s] (%s: %s)",
                        goal, MavenOption.GENERATEREPORTS.getEnvVariableName(), this.generateReports));
                }
            } else if (PHASE_PACKAGE.equals(goal) || PHASE_VERIFY.equals(goal) || isInstallGoal(goal)) {
                // goals need to alter
                if (this.mvnMultiStageBuild) {
                    // In multiple stage build, user should not request goal 'deploy' manually at first stage of build
                    resultGoals.add(Constants.PHASE_DEPLOY); // deploy artifacts into -DaltDeploymentRepository=wagonRepository
                    logger.info(String.format("    editGoals replace [%s] to %s (%s: %s)",
                        goal, Constants.PHASE_DEPLOY,
                        MavenBuildExtensionOption.MVN_MULTI_STAGE_BUILD.getEnvVariableName(),
                        this.mvnMultiStageBuild.toString()));
                } else {
                    resultGoals.add(goal);
                }
            } else if (goal.endsWith("sonar")) {
                final boolean isRefNameDevelop = GIT_REF_NAME_DEVELOP.equals(this.gitRefName);

                if (this.originRepo != null && this.originRepo && isRefNameDevelop) {
                    resultGoals.add(goal);
                } else {
                    logger.info(String.format("    editGoals skip [%s] (%s: %s, %s: %s)",
                        goal,
                        MavenBuildExtensionOption.ORIGIN_REPO.getEnvVariableName(), this.originRepo,
                        VcsProperties.GIT_REF_NAME.getEnvVariableName(), this.gitRefName));
                }
            } else {
                resultGoals.add(goal);
            }
        }

        if (this.mvnMultiStageBuild) {
            if (this.altDeployRepoPath.toFile().exists()
                && requestedPhases.contains(MavenPhase.CLEAN)
                && requestedPhases.contains(MavenPhase.DEPLOY)
            ) {
                logger.warn(String.format(
                    "    editGoals remove [%s], Do not request clean and deploy simultaneously in a multi stage build.", PHASE_CLEAN));
            }
        }
        return resultGoals;
    }

    public Properties additionalUserProperties(
        final CiOptionContext ciOptContext,
        final List<String> requestedGoals,
        final Collection<String> result
    ) {
        final Collection<MavenPhase> requestedPhases = phases(requestedGoals);
        final Collection<MavenPhase> resultPhases = phases(result);

        final Optional<Boolean> dpsDeploy = findInProperties(PROP_MVN_MULTI_STAGE_BUILD_GOAL_DEPLOY, ciOptContext)
            .map(Boolean::parseBoolean);

        final Properties properties = new Properties();
        if (!dpsDeploy.isPresent()) {
            properties.setProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL_DEPLOY, BOOL_STRING_FALSE);
        }
        properties.setProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL_INSTALL, BOOL_STRING_FALSE);
        properties.setProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL_PACKAGE, BOOL_STRING_FALSE);
        if (!findInProperties(PROP_MAVEN_CLEAN_SKIP, ciOptContext).isPresent()) {
            if (requestedPhases.contains(MavenPhase.CLEAN)) { // phase clean present
                properties.setProperty(PROP_MAVEN_CLEAN_SKIP, BOOL_STRING_FALSE);
            } else {
                // phase clean absent and goal deploy present and maven.clean.skip absent.
                final Optional<String> deployGoalRequested = requestedGoals.stream().filter(MavenGoalEditor::isDeployGoal).findAny();
                if (deployGoalRequested.isPresent() || includes(requestedPhases, MavenPhase.DEPLOY)) {
                    properties.setProperty(PROP_MAVEN_CLEAN_SKIP, BOOL_STRING_TRUE);
                }
            }
        }

        if (this.mvnMultiStageBuild) {
            if (notIncludes(requestedPhases, MavenPhase.INSTALL) && includes(resultPhases, MavenPhase.INSTALL)) {
                properties.setProperty(MavenOption.MAVEN_INSTALL_SKIP.getPropertyName(), BOOL_STRING_TRUE);
            }

            if (includes(resultPhases, MavenPhase.DEPLOY)) {
                final Optional<Boolean> nexus2Staging = findInProperties(PROP_NEXUS2_STAGING, ciOptContext).map(Boolean::parseBoolean);

                if (notIncludes(requestedPhases, MavenPhase.DEPLOY)) { // deploy added
                    properties.setProperty(PROP_NEXUS2_STAGING, BOOL_STRING_FALSE);
                }
                if (!nexus2Staging.isPresent()) {
                    properties.setProperty(PROP_NEXUS2_STAGING, BOOL_STRING_FALSE);
                } else if (!nexus2Staging.get()
                    && includes(requestedPhases, MavenPhase.DEPLOY)
                ) {
                    if (Collections.disjoint(requestedGoals, PHASES_AFTER_PREPARE_PACKAGE)) {
                        properties.setProperty(PROP_MAVEN_PACKAGES_SKIP, BOOL_STRING_TRUE);
                    }
                    if (!requestedGoals.contains(PHASE_INSTALL)) {
                        properties.setProperty(PROP_MAVEN_INSTALL_SKIP, BOOL_STRING_TRUE);
                    }
                }
            }

            final Optional<Boolean> packagesSkip = findInProperties(PROP_MAVEN_PACKAGES_SKIP, ciOptContext).map(Boolean::parseBoolean);
            if (includes(resultPhases, MavenPhase.PACKAGE) && !packagesSkip.orElse(FALSE)) {
                properties.setProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL_PACKAGE, BOOL_STRING_TRUE);
                properties.setProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL, PHASE_PACKAGE);
            }

            final Optional<String> installGoalRequested = requestedGoals.stream().filter(MavenGoalEditor::isInstallGoal).findAny();
            if ((installGoalRequested.isPresent() || includes(requestedPhases, MavenPhase.INSTALL))
                && notIncludes(requestedPhases, MavenPhase.DEPLOY)
                && includes(resultPhases, MavenPhase.DEPLOY)) {
                properties.setProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL_INSTALL, BOOL_STRING_TRUE);
                if (!packagesSkip.orElse(FALSE)) {
                    properties.setProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL_PACKAGE, BOOL_STRING_TRUE);
                }
                properties.setProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL, Constants.PHASE_INSTALL);
            }

            if (this.publishToRepo) {
                final boolean altDeployRepoPresent = this.altDeployRepoPath.toFile().exists();
                final Optional<String> deployGoalRequested = requestedGoals.stream().filter(MavenGoalEditor::isDeployGoal).findAny();
                if (altDeployRepoPresent
                    && !resultPhases.contains(MavenPhase.CLEAN)
                    && (deployGoalRequested.isPresent() || includes(requestedPhases, MavenPhase.DEPLOY))
                    && dpsDeploy.orElse(TRUE) // dpsDeploy true or absent
                ) {
                    properties.setProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL_DEPLOY, BOOL_STRING_TRUE);
                    properties.setProperty(PROP_MVN_MULTI_STAGE_BUILD_GOAL, Constants.PHASE_DEPLOY);
                }
            }
        } else {
            if (includes(resultPhases, MavenPhase.DEPLOY)) {
                final Optional<Boolean> nexus2Staging = findInProperties(PROP_NEXUS2_STAGING, ciOptContext).map(Boolean::parseBoolean);
                if (nexus2Staging.orElse(FALSE)) {
                    properties.setProperty(PROP_MAVEN_JAVADOC_SKIP, BOOL_STRING_FALSE);
                    properties.setProperty(PROP_MAVEN_SOURCE_SKIP, BOOL_STRING_FALSE);
                }
            }
        }

        return properties;
    }
}
