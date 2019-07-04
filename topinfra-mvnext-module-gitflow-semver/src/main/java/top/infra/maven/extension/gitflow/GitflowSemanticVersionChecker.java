package top.infra.maven.extension.gitflow;

import static top.infra.maven.Constants.BRANCH_PREFIX_FEATURE;
import static top.infra.maven.Constants.GIT_REF_NAME_DEVELOP;
import static top.infra.maven.Constants.PUBLISH_CHANNEL_SNAPSHOT;
import static top.infra.maven.utils.SupportFunction.isEmpty;
import static top.infra.maven.utils.SupportFunction.isSemanticSnapshotVersion;
import static top.infra.maven.utils.SupportFunction.newTuple;

import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.cli.CliRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.project.ProjectBuildingRequest;

import top.infra.maven.core.CiOptionContext;
import top.infra.maven.extension.MavenBuildPomOption;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.extension.MavenProjectInfo;
import top.infra.maven.extension.MavenProjectInfoEventAware;
import top.infra.maven.extension.Orders;
import top.infra.maven.extension.VcsProperties;
import top.infra.maven.logging.Logger;
import top.infra.maven.logging.LoggerPlexusImpl;

@Named
@Singleton
public class GitflowSemanticVersionChecker implements MavenEventAware {

    private final Logger logger;

    private final MavenProjectInfoEventAware projectInfoBean;

    @Inject
    public GitflowSemanticVersionChecker(
        final org.codehaus.plexus.logging.Logger logger,
        final MavenProjectInfoEventAware projectInfoBean
    ) {
        this.logger = new LoggerPlexusImpl(logger);
        this.projectInfoBean = projectInfoBean;
    }

    @Override
    public int getOrder() {
        return Orders.ORDER_GITFLOW_SEMANTIC_VERSION;
    }

    @Override
    public void onProjectBuildingRequest(
        final CliRequest cliRequest,
        final MavenExecutionRequest mavenExecution,
        final ProjectBuildingRequest projectBuilding,
        final CiOptionContext ciOptContext
    ) {
        this.check(ciOptContext);
    }

    private void check(final CiOptionContext ciOptContext) {
        final MavenProjectInfo mavenProjectInfo = this.projectInfoBean.getProjectInfo();

        if (logger.isInfoEnabled()) {
            logger.info(">>>>>>>>>> ---------- resolve project version ---------- >>>>>>>>>>");
            logger.info(mavenProjectInfo.toString());
            logger.info("<<<<<<<<<< ---------- resolve project version ---------- <<<<<<<<<<");
        }

        final String gitRefName = VcsProperties.GIT_REF_NAME.getValue(ciOptContext).orElse("");
        final Entry<Boolean, RuntimeException> checkResult = checkProjectVersion(ciOptContext, mavenProjectInfo.getVersion());
        final boolean valid = checkResult.getKey();
        if (logger.isInfoEnabled()) {
            logger.info(">>>>>>>>>> ---------- check project version ---------- >>>>>>>>>>");
            logger.info(String.format("%s version [%s] for ref [%s].",
                valid ? "Valid" : "Invalid", mavenProjectInfo.getVersion(), gitRefName));
            logger.info("<<<<<<<<<< ---------- check project version ---------- <<<<<<<<<<");
        }

        if (!valid) {
            logger.warn("You should use versions with '-SNAPSHOT' suffix on develop branch or feature branches");
            logger.warn("You should use versions like 1.0.0-SNAPSHOT develop branch");
            logger.warn("You should use versions like 1.0.0-feature-SNAPSHOT or 1.0.0-branch-SNAPSHOT on feature branches");
            logger.warn("You should use versions like 1.0.0 without '-SNAPSHOT' suffix on releases");
            final RuntimeException ex = checkResult.getValue();
            if (ex != null) {
                logger.error(ex.getMessage());
                throw ex;
            }
        }
    }

    static Entry<Boolean, RuntimeException> checkProjectVersion(
        final CiOptionContext ciOptContext,
        final String projectVersion
    ) {
        final String gitRefName = VcsProperties.GIT_REF_NAME.getValue(ciOptContext).orElse("");
        final String msgInvalidVersion = String.format("Invalid version [%s] for ref [%s]", projectVersion, gitRefName);
        final boolean semanticSnapshotVersion = isSemanticSnapshotVersion(projectVersion); // no feature name in version
        final boolean snapshotChannel = PUBLISH_CHANNEL_SNAPSHOT.equals(
            MavenBuildPomOption.PUBLISH_CHANNEL.getValue(ciOptContext).orElse(null));
        final boolean snapshotVersion = projectVersion != null && projectVersion.endsWith("-SNAPSHOT");

        final boolean result;
        final RuntimeException ex;
        if (snapshotChannel) {
            final boolean onDevelopBranch = GIT_REF_NAME_DEVELOP.equals(gitRefName);
            final boolean onFeatureBranches = gitRefName.startsWith(BRANCH_PREFIX_FEATURE);
            if (onFeatureBranches) {
                result = snapshotVersion && !semanticSnapshotVersion;
                ex = null;
            } else if (isEmpty(gitRefName) || onDevelopBranch) {
                result = snapshotVersion && semanticSnapshotVersion;
                ex = null;
            } else {
                result = snapshotVersion;
                ex = result ? null : new IllegalArgumentException(msgInvalidVersion);
            }
        } else {
            result = !snapshotVersion;
            ex = result ? null : new IllegalArgumentException(msgInvalidVersion);
        }

        return newTuple(result, ex);
    }
}
