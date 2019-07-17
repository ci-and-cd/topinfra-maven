package top.infra.maven.extension.internal;

import static java.util.stream.Collectors.toMap;
import static top.infra.maven.extension.shared.CiOptions.PATTERN_VARS_ENV_DOT_CI;
import static top.infra.maven.utils.PropertiesUtils.logProperties;
import static top.infra.maven.utils.SupportFunction.logEnd;
import static top.infra.maven.utils.SupportFunction.logStart;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.IntStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.cli.CliRequest;

import top.infra.maven.CiOption;
import top.infra.maven.CiOptionContext;
import top.infra.maven.extension.CiOptionFactoryBean;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.extension.shared.Orders;
import top.infra.maven.logging.Logger;
import top.infra.maven.logging.LoggerPlexusImpl;
import top.infra.maven.utils.PropertiesUtils;

@Named
@Singleton
public class CiOptionInitEventAware implements MavenEventAware {

    private final Logger logger;

    private Map<Class<?>, List<CiOption>> optionCollections;

    @Inject
    public CiOptionInitEventAware(
        final org.codehaus.plexus.logging.Logger logger,
        final List<CiOptionFactoryBean> optionFactoryBeans
    ) {
        this.logger = new LoggerPlexusImpl(logger);
        this.optionCollections = optionFactoryBeans
            .stream()
            .sorted()
            .collect(toMap(
                CiOptionFactoryBean::getType,
                CiOptionFactoryBean::getOptions,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }

    @Override
    public boolean afterInit() {
        return true;
    }

    @Override
    public void afterInit(
        final CliRequest cliRequest,
        final CiOptionContext ciOptContext
    ) {
        this.initCiOptions(ciOptContext);
    }

    @Override
    public int getOrder() {
        return Orders.ORDER_CI_OPTION_INIT;
    }

    private void initCiOptions(final CiOptionContext ciOptContext) {
        logger.info(logStart(this, "initCiOptions"));

        final Properties userProperties = ciOptContext.getUserProperties();
        // write all ciOpt properties into userProperties
        final Properties ciOptProperties = ciOptContext.setCiOptPropertiesInto(this.optionCollections.values(), userProperties);

        if (logger.isInfoEnabled()) {
            logger.info(String.format("    There are [%s] groups of options.", this.optionCollections.size()));
            final List<Class<?>> types = new ArrayList<>(this.optionCollections.keySet());
            final List<List<CiOption>> groups = new ArrayList<>(this.optionCollections.values());
            IntStream
                .range(0, this.optionCollections.size())
                .forEach(idx -> {
                    final List<CiOption> group = groups.get(idx);
                    logger.info(String.format(
                        "    option group index: [%s], name: [%s], size: [%s]",
                        String.format("%02d ", idx),
                        types.get(idx).getSimpleName(),
                        String.format("%03d ", group.size())
                    ));

                    group.stream().sorted().forEach(ciOption -> { // TODO better toString methods
                        final String displayName = ciOption.getEnvVariableName();
                        final String displayValue = ciOptProperties.getProperty(ciOption.getPropertyName(), "null");
                        logger.info(PropertiesUtils.maskSecrets(String.format("        %s=%s", displayName, displayValue)));
                    });
                });

            final Properties systemProperties = ciOptContext.getSystemProperties();
            logProperties(logger, "    ciOptionContext.systemProperties", systemProperties, PATTERN_VARS_ENV_DOT_CI);
            logProperties(logger, "    ciOptionContext.userProperties", userProperties, null);
        }

        logger.info(logEnd(this, "initCiOptions", Void.TYPE));
    }
}
