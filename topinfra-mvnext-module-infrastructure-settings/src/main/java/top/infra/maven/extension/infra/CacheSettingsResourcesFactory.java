package top.infra.maven.extension.infra;

import static top.infra.maven.extension.infra.InfraOption.CACHE_SETTINGS_PATH;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.cli.CliRequest;

import top.infra.logging.Logger;
import top.infra.maven.CiOptionContext;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.shared.extension.Orders;
import top.infra.maven.shared.logging.LoggerPlexusImpl;
import top.infra.maven.shared.utils.MavenUtils;

@Named
@Singleton
public class CacheSettingsResourcesFactory implements MavenEventAware {

    private final Logger logger;
    private final GitRepositoryFactory gitRepositoryFactory;
    private Resources object;

    @Inject
    public CacheSettingsResourcesFactory(
        final org.codehaus.plexus.logging.Logger logger,
        final GitRepositoryFactory gitRepositoryFactory
    ) {
        this.logger = new LoggerPlexusImpl(logger);
        this.gitRepositoryFactory = gitRepositoryFactory;
    }

    @Override
    public boolean afterInit() {
        return true;
    }

    @Override
    public void afterInit(final CliRequest cliRequest, final CiOptionContext ciOptContext) {
        this.object = new Resources(
            logger,
            MavenUtils.executionRootPath(cliRequest),
            CACHE_SETTINGS_PATH.getValue(ciOptContext).orElse(null),
            this.gitRepositoryFactory.getObject().orElse(null),
            ciOptContext.getSystemProperties(),
            ciOptContext.getUserProperties()
        );
    }

    public Resources getObject() {
        return this.object;
    }

    @Override
    public int getOrder() {
        return Orders.ORDER_CACHE_SETTINGS_RESOURCES_FACTORY;
    }
}
