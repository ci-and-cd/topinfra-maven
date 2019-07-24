package top.infra.maven.extension.docker;

import static java.lang.Boolean.FALSE;
import static top.infra.maven.extension.docker.Docker.dockerHost;
import static top.infra.maven.extension.docker.DockerOption.DOCKERFILE_USEMAVENSETTINGSFORAUTH;
import static top.infra.maven.extension.docker.DockerOption.DOCKER_REGISTRY;
import static top.infra.maven.extension.docker.DockerOption.DOCKER_REGISTRY_PASS;
import static top.infra.maven.extension.docker.DockerOption.DOCKER_REGISTRY_URL;
import static top.infra.maven.extension.docker.DockerOption.DOCKER_REGISTRY_USER;
import static top.infra.maven.shared.extension.GlobalOption.FAST;
import static top.infra.maven.shared.utils.MavenUtils.cmdArgOffline;
import static top.infra.maven.shared.utils.MavenUtils.cmdArgUpdateSnapshots;
import static top.infra.maven.shared.utils.SupportFunction.isEmpty;
import static top.infra.maven.shared.utils.SupportFunction.logEnd;
import static top.infra.maven.shared.utils.SupportFunction.logStart;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.cli.CliRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.project.ProjectBuildingRequest;

import top.infra.maven.CiOptionContext;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.logging.Logger;
import top.infra.maven.shared.extension.Constants;
import top.infra.maven.shared.extension.Orders;
import top.infra.maven.shared.logging.LoggerPlexusImpl;
import top.infra.maven.shared.utils.SystemUtils;

@Named
@Singleton
public class DockerEventAware implements MavenEventAware {

    private Logger logger;

    @Inject
    public DockerEventAware(
        final org.codehaus.plexus.logging.Logger logger
    ) {
        this.logger = new LoggerPlexusImpl(logger);
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
        final List<String> goals = mavenExecution.getGoals();
        final boolean dockerEnabled = DockerOption.DOCKER.getValue(ciOptContext).map(Boolean::parseBoolean).orElse(FALSE)
            && goals
            .stream()
            .filter(goal -> !goal.contains(Constants.PHASE_SITE))
            .anyMatch(goal ->
                goal.endsWith("build")
                    || goal.endsWith(Constants.PHASE_DEPLOY)
                    || goal.endsWith("push")
                    || goal.equals(Constants.PHASE_INSTALL)
                    || goal.equals(Constants.PHASE_PACKAGE)
            );

        if (dockerEnabled) {
            final Docker docker = new Docker(
                dockerHost(ciOptContext.getSystemProperties()).orElse(null),
                SystemUtils.systemUserHome(),
                DOCKER_REGISTRY.getValue(ciOptContext).orElse(null),
                DOCKER_REGISTRY_PASS.getValue(ciOptContext).orElse(null),
                DOCKER_REGISTRY_URL.getValue(ciOptContext).orElse(null),
                DOCKER_REGISTRY_USER.getValue(ciOptContext).orElse(null)
            );

            docker.initConfigFile();

            final boolean offline = cmdArgOffline(cliRequest);
            final boolean useMavenSettingsForAuth = DOCKERFILE_USEMAVENSETTINGSFORAUTH.getValue(ciOptContext)
                .map(Boolean::parseBoolean).orElse(FALSE);
            if (!useMavenSettingsForAuth && !offline) {
                this.login(docker);
            }

            final boolean fast = FAST.getValue(ciOptContext).map(Boolean::parseBoolean).orElse(FALSE);
            if (!fast) {
                this.cleanOldImages(docker);
            }

            if (!fast && !offline) {
                final boolean update = cmdArgUpdateSnapshots(cliRequest);
                this.pullBaseImages(docker, update);
            }
        }
    }

    @Override
    public int getOrder() {
        return Orders.EVENT_AWARE_ORDER_DOCKER;
    }

    private void cleanOldImages(final Docker docker) {
        final List<String> imageIds = docker.imageIdsToClean();
        if (logger.isInfoEnabled()) {
            logger.info(String.format("    Found imageIdsToClean %s", imageIds));
        }
        docker.deleteImages(imageIds).forEach((id, retCode) -> {
            if (retCode == 0) {
                logger.info(String.format("    Image [%s] deleted.", id));
            } else {
                logger.warn(String.format("    Failed to delete image [%s].", id));
            }
        });
    }

    private void login(final Docker docker) {
        final String target = docker.getLoginTarget();
        if (!isEmpty(target) && target.startsWith("https://")) {
            logger.info(String.format("    docker logging into secure registry %s", target));
        } else {
            logger.info(String.format("    docker logging into insecure registry %s", target));
        }

        final Optional<Integer> result = docker.login(target);
        if (result.isPresent()) {
            logger.info(String.format("    docker login [%s] result [%s]", target, result.orElse(null)));
        } else {
            logger.info(String.format("    docker login [%s] skipped", target));
        }
    }

    private void pullBaseImages(final Docker docker, final boolean forceUpdate) {
        logger.info(logStart(this, "pullBaseImages"));

        final List<String> dockerfiles = Docker.dockerfiles();
        if (logger.isInfoEnabled()) {
            logger.info(String.format("    Found dockerfiles %s", dockerfiles));
        }
        final List<String> baseImages = Docker.baseImages(dockerfiles);
        if (logger.isInfoEnabled()) {
            logger.info(String.format("    Found baseImages %s", baseImages));
        }

        final boolean haveAllLocalCopies = docker.imageRepositoryColonTags().containsAll(baseImages);
        if (forceUpdate || !haveAllLocalCopies) {
            baseImages.forEach(image -> {
                logger.info(String.format("    Pull baseImage [%s]", image));
                docker.pullImage(image);
            });
        } else {
            logger.info(String.format(
                "    Skip pullBaseImages %s, you have local copies of these images. forceUpdate [false]",
                baseImages));
        }

        logger.info(logEnd(this, "pullBaseImages", Void.TYPE));
    }
}
