package top.infra.maven.extension.main;

import static java.util.stream.Collectors.toList;
import static top.infra.maven.shared.utils.SupportFunction.logEnd;
import static top.infra.maven.shared.utils.SupportFunction.logStart;

import java.util.List;
import java.util.stream.IntStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.cli.CliRequest;
import org.apache.maven.cli.configuration.ConfigurationProcessor;
import org.apache.maven.cli.configuration.SettingsXmlConfigurationProcessor;
import org.apache.maven.settings.building.SettingsBuildingRequest;

import top.infra.maven.extension.OrderedConfigurationProcessor;
import top.infra.maven.logging.Logger;
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
        this.processors = processors.stream().sorted().collect(toList());
        this.settingsXmlConfigurationProcessor = settingsXmlConfigurationProcessor;

        IntStream
            .range(0, this.processors.size())
            .forEach(idx -> {
                final OrderedConfigurationProcessor it = this.processors.get(idx);
                logger.info(String.format(
                    "    processor index: [%s], order: [%s], name: [%s]",
                    String.format("%02d ", idx),
                    String.format("%011d ", it.getOrder()),
                    it.getClass().getSimpleName()
                ));
            });
        logger.info(logEnd(this, "constructor", Void.TYPE));
    }

    @Override
    public void process(final CliRequest cliRequest) throws Exception {
        logger.info(logStart(this, "process"));

        for (final OrderedConfigurationProcessor processor : this.processors) {
            processor.process(cliRequest);
        }

        this.settingsXmlConfigurationProcessor.process(cliRequest);

        logger.info(logEnd(this, "process", Void.TYPE));
    }
}
