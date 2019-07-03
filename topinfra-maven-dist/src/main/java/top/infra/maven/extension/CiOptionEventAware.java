package top.infra.maven.extension;

import static java.util.stream.Collectors.toList;
import static top.infra.maven.core.CiOptionNames.PATTERN_VARS_ENV_DOT_CI;

import java.util.List;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.eventspy.EventSpy.Context;

import top.infra.maven.core.CiOption;
import top.infra.maven.core.CiOptionContext;
import top.infra.maven.core.CiOptionContextBeanFactory;
import top.infra.maven.core.CiOptionFactoryBean;
import top.infra.maven.logging.Logger;
import top.infra.maven.logging.LoggerPlexusImpl;
import top.infra.maven.utils.MavenUtils;
import top.infra.maven.utils.PropertiesUtils;

@Named
@Singleton
public class CiOptionEventAware implements MavenEventAware {

    private final Logger logger;

    private CiOptionContextBeanFactory ciOptContextFactoryBean;

    private List<List<CiOption>> optionCollections;

    @Inject
    public CiOptionEventAware(
        final org.codehaus.plexus.logging.Logger logger,
        final CiOptionContextBeanFactory ciOptContextFactoryBean,
        final List<CiOptionFactoryBean> optionFactoryBeans
    ) {
        this.logger = new LoggerPlexusImpl(logger);
        this.ciOptContextFactoryBean = ciOptContextFactoryBean;
        this.optionCollections = optionFactoryBeans.stream().map(CiOptionFactoryBean::getOptions).collect(toList());
    }

    @Override
    public int getOrder() {
        return Orders.EVENT_AWARE_ORDER_CI_OPTION;
    }

    @Override
    public void onInit(final Context context) {
        final CiOptionContext ciOptContext = this.ciOptContextFactoryBean.getCiOpts();

        final Properties userProperties = MavenUtils.userProperties(context);
        // write all ciOpt properties into userProperties
        final Properties ciOptProperties = ciOptContext.setCiOptPropertiesInto(this.optionCollections, userProperties);

        if (logger.isInfoEnabled()) {
            logger.info(">>>>>>>>>> ---------- set options (update userProperties) ---------- >>>>>>>>>>");
            this.optionCollections
                .stream()
                .flatMap(collection -> collection.stream().sorted())
                .forEach(ciOption -> { // TODO better toString methods
                    final String displayName = ciOption.getEnvVariableName();
                    final String displayValue = ciOptProperties.getProperty(ciOption.getPropertyName(), "");
                    logger.info(PropertiesUtils.maskSecrets(String.format("setOption %s=%s", displayName, displayValue)));
                });
            logger.info("<<<<<<<<<< ---------- set options (update userProperties) ---------- <<<<<<<<<<");

            final Properties systemProperties = MavenUtils.systemProperties(context);
            logger.info(PropertiesUtils.toString(systemProperties, PATTERN_VARS_ENV_DOT_CI));
            logger.info(PropertiesUtils.toString(userProperties, null));
        }
    }
}
