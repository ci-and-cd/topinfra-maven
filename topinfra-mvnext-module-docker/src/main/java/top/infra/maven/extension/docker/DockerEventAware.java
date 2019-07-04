package top.infra.maven.extension.docker;

import static java.lang.Boolean.FALSE;
import static top.infra.maven.extension.docker.Docker.dockerHost;
import static top.infra.maven.extension.docker.DockerOption.DOCKERFILE_USEMAVENSETTINGSFORAUTH;
import static top.infra.maven.extension.docker.DockerOption.DOCKER_REGISTRY;
import static top.infra.maven.extension.docker.DockerOption.DOCKER_REGISTRY_PASS;
import static top.infra.maven.extension.docker.DockerOption.DOCKER_REGISTRY_URL;
import static top.infra.maven.extension.docker.DockerOption.DOCKER_REGISTRY_USER;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.cli.CliRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.project.ProjectBuildingRequest;

import top.infra.maven.Constants;
import top.infra.maven.core.CiOptionContext;
import top.infra.maven.extension.FastOption;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.extension.Orders;
import top.infra.maven.logging.Logger;
import top.infra.maven.logging.LoggerPlexusImpl;
import top.infra.maven.utils.SupportFunction;
import top.infra.maven.utils.SystemUtils;

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

            if (!DOCKERFILE_USEMAVENSETTINGSFORAUTH.getValue(ciOptContext).map(Boolean::parseBoolean).orElse(FALSE)) {
                this.login(docker);
            }

            if (!FastOption.FAST.getValue(ciOptContext).map(Boolean::parseBoolean).orElse(FALSE)) {
                this.cleanOldImages(docker);

                this.pullBaseImages(docker);
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
            logger.info(String.format("Found imageIdsToClean %s", imageIds));
        }
        docker.deleteImages(imageIds).forEach((id, retCode) -> {
            if (retCode == 0) {
                logger.info(String.format("Image [%s] deleted.", id));
            } else {
                logger.warn(String.format("Failed to delete image [%s].", id));
            }
        });
    }

    private void login(final Docker docker) {
        final String target = docker.getLoginTarget();
        if (SupportFunction.isNotEmpty(target) && target.startsWith("https://")) {
            logger.info(String.format("docker logging into secure registry %s", target));
        } else {
            logger.info(String.format("docker logging into insecure registry %s", target));
        }

        final Optional<Integer> result = docker.login(target);
        if (result.isPresent()) {
            logger.info(String.format("docker login [%s] result [%s]", target, result.orElse(null)));
        } else {
            logger.info(String.format("docker login [%s] skipped", target));
        }
    }

    private void pullBaseImages(final Docker docker) {
        logger.info(">>>>>>>>>> ---------- pull_base_image ---------- >>>>>>>>>>");
        final List<String> dockerfiles = Docker.dockerfiles();
        if (logger.isInfoEnabled()) {
            logger.info(String.format("Found dockerfiles %s", dockerfiles));
        }
        final List<String> baseImages = docker.pullBaseImages(dockerfiles);
        if (logger.isInfoEnabled()) {
            logger.info(String.format("Found baseImages %s", baseImages));
        }
        logger.info("<<<<<<<<<< ---------- pull_base_image ---------- <<<<<<<<<<");
    }
}
