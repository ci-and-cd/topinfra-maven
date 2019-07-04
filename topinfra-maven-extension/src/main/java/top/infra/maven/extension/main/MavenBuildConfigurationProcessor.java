package top.infra.maven.extension.main;

import static java.util.stream.Collectors.toList;

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
import top.infra.maven.logging.LoggerPlexusImpl;

/**
 * See {@link org.apache.maven.cli.MavenCli}.
 * <p/>
 * Run before {@link SettingsBuildingRequest}.
 */
// @Component(role = ConfigurationProcessor.class, hint = "settings-security")
@Named
@Singleton
public class MavenBuildConfigurationProcessor implements ConfigurationProcessor {

    private Logger logger;

    private List<OrderedConfigurationProcessor> processors;

    // @Requirement(role = ConfigurationProcessor.class, hint = SettingsXmlConfigurationProcessor.HINT)
    private ConfigurationProcessor settingsXmlConfigurationProcessor;

    private CliRequest cliRequest;

    @Inject
    public MavenBuildConfigurationProcessor(
        final org.codehaus.plexus.logging.Logger logger,
        final List<OrderedConfigurationProcessor> processors,
        final SettingsXmlConfigurationProcessor settingsXmlConfigurationProcessor
    ) {
        this.logger = new LoggerPlexusImpl(logger);
        this.processors = processors.stream().sorted().collect(toList());
        this.settingsXmlConfigurationProcessor = settingsXmlConfigurationProcessor;

        logger.info(String.format("MavenBuildConfigurationProcessor [%s]", this));
        IntStream
            .range(0, this.processors.size())
            .forEach(idx -> {
                final OrderedConfigurationProcessor it = this.processors.get(idx);
                logger.info(String.format(
                    "processor index: [%s], order: [%s], name: [%s]",
                    String.format("%02d ", idx),
                    String.format("%011d ", it.getOrder()),
                    it.getClass().getSimpleName()
                ));
            });
    }

    public CliRequest getCliRequest() {
        return this.cliRequest;
    }

    @Override
    public void process(final CliRequest cliRequest) throws Exception {
        logger.info("MavenBuildConfigurationProcessor process");

        this.cliRequest = cliRequest;

        for (final OrderedConfigurationProcessor processor : this.processors) {
            processor.process(cliRequest);
        }

        this.settingsXmlConfigurationProcessor.process(cliRequest);
    }
}
