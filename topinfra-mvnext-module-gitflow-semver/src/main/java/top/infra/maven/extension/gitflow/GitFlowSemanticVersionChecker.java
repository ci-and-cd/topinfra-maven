package top.infra.maven.extension.gitflow;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static top.infra.maven.shared.extension.Constants.GIT_REF_NAME_DEVELOP;
import static top.infra.maven.shared.extension.VcsProperties.GIT_REF_NAME;
import static top.infra.maven.shared.utils.SupportFunction.logEnd;
import static top.infra.maven.shared.utils.SupportFunction.logStart;
import static top.infra.maven.shared.utils.SupportFunction.newTuple;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.cli.CliRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.project.ProjectBuildingRequest;

import top.infra.logging.Logger;
import top.infra.maven.CiOptionContext;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.extension.MavenProjectInfo;
import top.infra.maven.extension.MavenProjectInfoFactory;
import top.infra.maven.shared.extension.Orders;
import top.infra.maven.shared.extension.VcsProperties;
import top.infra.maven.shared.logging.LoggerPlexusImpl;

@Named
@Singleton
public class GitFlowSemanticVersionChecker implements MavenEventAware {

    private final Logger logger;

    private final MavenProjectInfoFactory projectInfoFactory;

    @Inject
    public GitFlowSemanticVersionChecker(
        final org.codehaus.plexus.logging.Logger logger,
        final MavenProjectInfoFactory projectInfoFactory
    ) {
        this.logger = new LoggerPlexusImpl(logger);
        this.projectInfoFactory = projectInfoFactory;
    }

    @Override
    public boolean onProjectBuildingRequest() {
        return true;
    }

    @Override
    public void onProjectBuildingRequest(
        final CliRequest cliRequest,
        final MavenExecutionRequest mavenExecution,
        final ProjectBuildingRequest projectBuilding,
        final CiOptionContext ciOptContext
    ) {
        final MavenProjectInfo mavenProjectInfo = this.projectInfoFactory.getRootProjectInfo();

        final Collection<String> requestedGoals = mavenExecution.getGoals();
        if (requestedGoals.stream().anyMatch(goal -> goal.contains("gitflow") || goal.contains("versions"))) {
            logger.info(String.format("    Bypass version check when executing git-flow versions goals (%s).", requestedGoals));
        } else {
            this.check(ciOptContext, mavenProjectInfo);
        }
    }

    @Override
    public int getOrder() {
        return Orders.ORDER_GIT_FLOW_SEMANTIC_VERSION;
    }

    private void check(
        final CiOptionContext ciOptContext,
        final MavenProjectInfo mavenProjectInfo
    ) {
        logger.info(logStart(this, "check", mavenProjectInfo));

        final String gitRefName = GIT_REF_NAME.getValue(ciOptContext).orElse("");
        final Entry<Boolean, RuntimeException> checkResult = checkProjectVersion(ciOptContext, mavenProjectInfo.getVersion());
        final boolean result = checkResult.getKey();
        final String valid = result ? "Valid" : "Invalid";
        logger.info(String.format("    %s version [%s] for ref [%s].", valid, mavenProjectInfo.getVersion(), gitRefName));

        if (!result) {
            logger.warn("    You should use versions with '-SNAPSHOT' suffix on develop branch or feature branches");
            logger.warn("    You should use versions like 1.0.0-SNAPSHOT develop branch");
            logger.warn("    You should use versions like 1.0.0-feature-SNAPSHOT or 1.0.0-branch-SNAPSHOT on feature branches");
            logger.warn("    You should use versions like 1.0.0 without '-SNAPSHOT' suffix on releases");
            final RuntimeException ex = checkResult.getValue();
            if (ex != null) {
                logger.error(ex.getMessage());
                logger.info(logEnd(this, "check", valid));
                throw ex;
            }
        }
        logger.info(logEnd(this, "check", valid));
    }

    static Entry<Boolean, RuntimeException> checkProjectVersion(
        final CiOptionContext ciOptContext,
        final String projectVersion
    ) {
        final Entry<Boolean, RuntimeException> result;

        final String gitRef = GIT_REF_NAME.getValue(ciOptContext).orElse(null);

        final Entry<Boolean, RuntimeException> ok = newTuple(TRUE, null);
        final Entry<Boolean, RuntimeException> warn = newTuple(FALSE, null);
        final String errMsg = String.format("Invalid version [%s] for ref [%s]", projectVersion, gitRef);
        final Entry<Boolean, RuntimeException> err = newTuple(FALSE, new IllegalArgumentException(errMsg));

        if (gitRef != null && !gitRef.isEmpty()) {
            if (VcsProperties.isSnapshotRef(gitRef)) {
                if (GIT_REF_NAME_DEVELOP.equals(gitRef)) { // develop branch
                    // no feature name in version
                    result = isSemSnapshot(projectVersion) ? ok : warn;
                } else { // feature branches
                    result = isSemFeature(projectVersion) ? ok : warn;
                }
            } else if (VcsProperties.isReleaseRef(gitRef)) {
                result = isSemRelease(projectVersion) ? ok : err;
            } else {
                result = ok;
            }
        } else {
            result = ok;
        }

        return result;
    }

    private static final Pattern PATTERN_SEMANTIC_VERSION_FEATURE = Pattern.compile("^([0-9]+\\.){0,2}[0-9]+-[0-9a-zA-Z_-]+-SNAPSHOT$");

    private static final Pattern PATTERN_SEMANTIC_VERSION_RELEASE = Pattern.compile("^([0-9]+\\.){0,2}[0-9]+$");

    private static final Pattern PATTERN_SEMANTIC_VERSION_SNAPSHOT = Pattern.compile("^([0-9]+\\.){0,2}[0-9]+-SNAPSHOT$");

    static boolean isSemFeature(final String version) {
        return version != null && PATTERN_SEMANTIC_VERSION_FEATURE.matcher(version).matches();
    }

    static boolean isSemRelease(final String version) {
        return version != null && PATTERN_SEMANTIC_VERSION_RELEASE.matcher(version).matches();
    }

    static boolean isSemSnapshot(final String version) {
        return version != null && PATTERN_SEMANTIC_VERSION_SNAPSHOT.matcher(version).matches();
    }
}
