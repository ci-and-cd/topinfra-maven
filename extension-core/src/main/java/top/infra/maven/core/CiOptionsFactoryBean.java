package top.infra.maven.core;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.cli.CliRequest;

import top.infra.maven.extension.OrderedConfigurationProcessor;
import top.infra.maven.extension.Orders;
import top.infra.maven.logging.Logger;
import top.infra.maven.logging.LoggerPlexusImpl;

@Named
@Singleton
public class CiOptionsFactoryBean implements OrderedConfigurationProcessor {

    private final Logger logger;

    private final GitProperties gitProperties;

    private CiOptions ciOpts;

    @Inject
    public CiOptionsFactoryBean(
        final org.codehaus.plexus.logging.Logger logger,
        final GitPropertiesFactoryBean gitPropertiesFactoryBean
    ) {
        this.logger = new LoggerPlexusImpl(logger);
        this.gitProperties = gitPropertiesFactoryBean.getObjct().orElseGet(GitProperties::newBlankGitProperties);
    }

    @Override
    public void process(final CliRequest request) throws Exception {
        this.ciOpts = new CiOptions(
            this.gitProperties,
            request.getSystemProperties(),
            request.getUserProperties()
        );
    }

    @Override
    public int getOrder() {
        return Orders.CONFIGURATION_PROCESSOR_ORDER_OPTIONS_FACTORY;
    }

    public CiOptions getCiOpts() {
        return this.ciOpts;
    }
}
