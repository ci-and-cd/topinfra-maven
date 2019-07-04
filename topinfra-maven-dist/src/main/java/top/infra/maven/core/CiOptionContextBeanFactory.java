package top.infra.maven.core;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

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

    private CiOptionContext ciOptContext;

    @Inject
    public CiOptionContextBeanFactory(
        final org.codehaus.plexus.logging.Logger logger
    ) {
        this.logger = new LoggerPlexusImpl(logger);
    }

    public CiOptionContext getCiOpts() {
        return this.ciOptContext;
    }

    @Override
    public boolean onInit() {
        return true;
    }

    @Override
    public void onInit(final Context context) {
        this.ciOptContext = new CiOptionContext(
            MavenUtils.systemProperties(context),
            MavenUtils.userProperties(context)
        );
    }

    @Override
    public int getOrder() {
        return Orders.ORDER_OPTIONS_FACTORY;
    }
}
