package top.infra.maven.extension.main;

import static java.util.stream.Collectors.toList;
import static top.infra.maven.shared.utils.SupportFunction.componentDisabled;
import static top.infra.maven.shared.utils.SupportFunction.componentName;
import static top.infra.maven.shared.utils.SupportFunction.logEnd;
import static top.infra.maven.shared.utils.SupportFunction.logStart;

import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.IntStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.cli.CliRequest;
import org.apache.maven.cli.configuration.ConfigurationProcessor;
import org.apache.maven.cli.configuration.SettingsXmlConfigurationProcessor;
import org.apache.maven.settings.building.SettingsBuildingRequest;

import top.infra.logging.Logger;
import top.infra.maven.extension.OrderedConfigurationProcessor;
import top.infra.maven.shared.logging.LoggerPlexusImpl;

/**
 * See {@link org.apache.maven.cli.MavenCli}.
 * <p/>
 * Run before {@link SettingsBuildingRequest}.
 */
// @Component(role = ConfigurationProcessor.class, hint = "settings-security")
@Named
@Singleton
public class MainConfigurationProcessor implements ConfigurationProcessor {

    private final Logger logger;

    private final List<OrderedConfigurationProcessor> processors;

    // @Requirement(role = ConfigurationProcessor.class, hint = SettingsXmlConfigurationProcessor.HINT)
    private final ConfigurationProcessor settingsXmlConfigurationProcessor;

    @Inject
    public MainConfigurationProcessor(
        final org.codehaus.plexus.logging.Logger logger,
        final List<OrderedConfigurationProcessor> processors,
        final SettingsXmlConfigurationProcessor settingsXmlConfigurationProcessor
    ) {
        logger.info(logStart(this, "constructor"));

        this.logger = new LoggerPlexusImpl(logger);
        this.processors = Collections.unmodifiableList(processors.stream().sorted().collect(toList()));
        this.settingsXmlConfigurationProcessor = settingsXmlConfigurationProcessor;

        logger.info(logEnd(this, "constructor", Void.TYPE));
    }

    @Override
    public void process(final CliRequest cliRequest) throws Exception {
        logger.info(logStart(this, "process"));

        final Properties systemProperties = cliRequest.getSystemProperties();
        final Properties userProperties = cliRequest.getUserProperties();
        final List<OrderedConfigurationProcessor> availableProcessors = this.processors
            .stream()
            .filter(it -> {
                final boolean disabled = componentDisabled(it.getClass(), systemProperties, userProperties);
                if (disabled) {
                    logger.info(String.format("    processor [%s] disabled", componentName(it.getClass())));
                }
                return !disabled;
            })
            .collect(toList());
        IntStream
            .range(0, availableProcessors.size())
            .forEach(idx -> {
                final OrderedConfigurationProcessor it = availableProcessors.get(idx);
                logger.info(String.format(
                    "    processor index: [%s], order: [%s], name: [%s]",
                    String.format("%02d ", idx),
                    String.format("%011d ", it.getOrder()),
                    componentName(it.getClass())
                ));
            });

        for (final OrderedConfigurationProcessor processor : availableProcessors) {
            processor.process(cliRequest);
        }

        this.settingsXmlConfigurationProcessor.process(cliRequest);

        logger.info(logEnd(this, "process", Void.TYPE));
    }
}
