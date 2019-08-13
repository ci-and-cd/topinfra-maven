package top.infra.maven.extension.infra;

import static top.infra.maven.extension.infra.InfraOption.GIT_AUTH_TOKEN;
import static top.infra.maven.extension.infra.InfraOption.MAVEN_BUILD_OPTS_REPO;
import static top.infra.maven.extension.infra.InfraOption.MAVEN_BUILD_OPTS_REPO_REF;
import static top.infra.maven.shared.extension.GlobalOption.INFRASTRUCTURE;
import static top.infra.maven.shared.extension.VcsProperties.GIT_REMOTE_ORIGIN_URL;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.cli.CliRequest;

import top.infra.logging.Logger;
import top.infra.maven.CiOptionContext;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.shared.extension.Orders;
import top.infra.maven.shared.logging.LoggerPlexusImpl;

@Named
@Singleton
public class GitRepositoryFactory implements MavenEventAware {

    private final Logger logger;

    private GitRepository object;

    @Inject
    public GitRepositoryFactory(
        final org.codehaus.plexus.logging.Logger logger
    ) {
        this.logger = new LoggerPlexusImpl(logger);
    }

    @Override
    public boolean afterInit() {
        return true;
    }

    @Override
    public void afterInit(final CliRequest cliRequest, final CiOptionContext ciOptContext) {
        this.object = newGitRepository(ciOptContext, logger).orElse(null);
    }

    public Optional<GitRepository> getObject() {
        return Optional.ofNullable(this.object);
    }

    @Override
    public int getOrder() {
        return Orders.ORDER_GIT_REPO_FACTORY;
    }

    public static Optional<GitRepository> newGitRepository(
        final CiOptionContext ciOptContext,
        final Logger logger
    ) {
        return GitRepository.newGitRepository(
            logger,
            ciOptContext.getSystemProperties(),
            INFRASTRUCTURE.getValue(ciOptContext).orElse(null),
            MAVEN_BUILD_OPTS_REPO.getValue(ciOptContext).orElse(null),
            MAVEN_BUILD_OPTS_REPO_REF.getValue(ciOptContext).orElse(null),
            GIT_REMOTE_ORIGIN_URL.getValue(ciOptContext).orElse(null),
            GIT_AUTH_TOKEN.getValue(ciOptContext).orElse(null)
        );
    }
}
