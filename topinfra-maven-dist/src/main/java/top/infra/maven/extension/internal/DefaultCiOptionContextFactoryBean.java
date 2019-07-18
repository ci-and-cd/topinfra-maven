package top.infra.maven.extension.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.eventspy.EventSpy.Context;

import top.infra.maven.CiOptionContext;
import top.infra.maven.shared.DefaultCiOptionContext;
import top.infra.maven.extension.CiOptionContextFactoryBean;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.shared.extension.Orders;
import top.infra.maven.logging.Logger;
import top.infra.maven.shared.logging.LoggerPlexusImpl;
import top.infra.maven.shared.utils.MavenUtils;

@Named
@Singleton
public class DefaultCiOptionContextFactoryBean implements CiOptionContextFactoryBean, MavenEventAware {

    private final Logger logger;

    private CiOptionContext ciOptContext;

    @Inject
    public DefaultCiOptionContextFactoryBean(
        final org.codehaus.plexus.logging.Logger logger
    ) {
        this.logger = new LoggerPlexusImpl(logger);
    }

    @Override
    public CiOptionContext getObject() {
        return this.ciOptContext;
    }

    @Override
    public boolean onInit() {
        return true;
    }

    @Override
    public void onInit(final Context context) {
        this.ciOptContext = new DefaultCiOptionContext(
            MavenUtils.systemProperties(context),
            MavenUtils.userProperties(context)
        );
    }

    @Override
    public int getOrder() {
        return Orders.ORDER_CI_OPTION_CONTEXT_FACTORY;
    }
}
