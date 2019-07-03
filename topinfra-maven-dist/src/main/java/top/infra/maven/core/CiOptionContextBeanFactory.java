package top.infra.maven.core;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.cli.CliRequest;
import org.apache.maven.eventspy.EventSpy.Context;

import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.extension.Orders;
import top.infra.maven.logging.Logger;
import top.infra.maven.logging.LoggerPlexusImpl;
import top.infra.maven.utils.MavenUtils;

@Named
@Singleton
public class CiOptionContextBeanFactory implements MavenEventAware {

    private final Logger logger;

    private final GitProperties gitProperties;

    private CiOptionContext ciOptContext;

    @Inject
    public CiOptionContextBeanFactory(
        final org.codehaus.plexus.logging.Logger logger,
        final GitPropertiesBeanFactory gitPropertiesBeanFactory
    ) {
        this.logger = new LoggerPlexusImpl(logger);
        this.gitProperties = gitPropertiesBeanFactory.getObject();
    }

    @Override
    public void onInit(final Context context) {
        this.ciOptContext = new CiOptionContext(
            this.gitProperties,
            MavenUtils.systemProperties(context),
            MavenUtils.userProperties(context)
        );
    }

    @Deprecated
    public void process(final CliRequest request) throws Exception {
        this.ciOptContext = new CiOptionContext(
            this.gitProperties,
            request.getSystemProperties(),
            request.getUserProperties()
        );
    }

    @Override
    public int getOrder() {
        return Orders.ORDER_OPTIONS_FACTORY;
    }

    public CiOptionContext getCiOpts() {
        return this.ciOptContext;
    }
}
