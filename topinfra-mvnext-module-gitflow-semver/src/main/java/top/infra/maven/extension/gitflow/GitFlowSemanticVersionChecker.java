package top.infra.maven.extension.gitflow;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static top.infra.maven.Constants.GIT_REF_NAME_DEVELOP;
import static top.infra.maven.Constants.GIT_REF_PREFIX_FEATURE;
import static top.infra.maven.Constants.GIT_REF_PREFIX_HOTFIX;
import static top.infra.maven.Constants.GIT_REF_PREFIX_RELEASE;
import static top.infra.maven.Constants.GIT_REF_PREFIX_SUPPORT;
import static top.infra.maven.extension.VcsProperties.GIT_REF_NAME;
import static top.infra.maven.utils.SupportFunction.newTuple;

import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.cli.CliRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.project.ProjectBuildingRequest;

import top.infra.maven.core.CiOptionContext;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.extension.MavenProjectInfo;
import top.infra.maven.extension.MavenProjectInfoEventAware;
import top.infra.maven.extension.Orders;
import top.infra.maven.logging.Logger;
import top.infra.maven.logging.LoggerPlexusImpl;

@Named
@Singleton
public class GitFlowSemanticVersionChecker implements MavenEventAware {

    private final Logger logger;

    private final MavenProjectInfoEventAware projectInfoBean;

    @Inject
    public GitFlowSemanticVersionChecker(
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

        final String gitRefName = GIT_REF_NAME.getValue(ciOptContext).orElse("");
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
        final Entry<Boolean, RuntimeException> result;

        final String gitRef = GIT_REF_NAME.getValue(ciOptContext).orElse(null);

        final Entry<Boolean, RuntimeException> ok = newTuple(TRUE, null);
        final Entry<Boolean, RuntimeException> warn = newTuple(FALSE, null);
        final String errMsg = String.format("Invalid version [%s] for ref [%s]", projectVersion, gitRef);
        final Entry<Boolean, RuntimeException> err = newTuple(FALSE, new IllegalArgumentException(errMsg));

        if (gitRef != null && !gitRef.isEmpty()) {
            final boolean snapshotRef = GIT_REF_NAME_DEVELOP.equals(gitRef)
                || gitRef.startsWith(GIT_REF_PREFIX_FEATURE);
            final boolean releaseRef = gitRef.startsWith(GIT_REF_PREFIX_HOTFIX)
                || gitRef.startsWith(GIT_REF_PREFIX_RELEASE)
                || gitRef.startsWith(GIT_REF_PREFIX_SUPPORT);

            if (snapshotRef) {
                if (GIT_REF_NAME_DEVELOP.equals(gitRef)) { // develop branch
                    // no feature name in version
                    result = isSemSnapshot(projectVersion) ? ok : warn;
                } else { // feature branches
                    result = isSemFeature(projectVersion) ? ok : warn;
                }
            } else if (releaseRef) {
                result = isSemRelease(projectVersion) ? ok : err;
            } else {
                result = ok;
            }
        } else {
            result = ok;
        }

        return result;
    }

    private static final Pattern PATTERN_SEMANTIC_VERSION_FEATURE = Pattern.compile("^([0-9]+\\.){0,2}[0-9]+-\\w+-SNAPSHOT$");

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