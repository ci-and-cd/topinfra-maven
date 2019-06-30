package top.infra.maven.extension;

import static java.lang.Boolean.FALSE;
import static top.infra.maven.Constants.SRC_CI_OPTS_PROPERTIES;
import static top.infra.maven.extension.InfraOption.CACHE_SETTINGS_PATH;
import static top.infra.maven.extension.InfraOption.GIT_AUTH_TOKEN;
import static top.infra.maven.utils.SupportFunction.isEmpty;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.cli.CliRequest;

import top.infra.maven.core.CiOptions;
import top.infra.maven.core.CiOptionsFactoryBean;
import top.infra.maven.exception.RuntimeIOException;
import top.infra.maven.logging.Logger;
import top.infra.maven.logging.LoggerPlexusImpl;
import top.infra.maven.utils.FileUtils;
import top.infra.maven.utils.MavenUtils;
import top.infra.maven.utils.PropertiesUtils;

@Named
@Singleton
public class OptionFileLoader implements OrderedConfigurationProcessor {

    private final Logger logger;

    private final CiOptions ciOpts;

    @Inject
    public OptionFileLoader(
        final org.codehaus.plexus.logging.Logger logger,
        final CiOptionsFactoryBean ciOptionsFactoryBean
    ) {
        this.logger = new LoggerPlexusImpl(logger);
        this.ciOpts = ciOptionsFactoryBean.getCiOpts();
    }

    @Override
    public void process(final CliRequest request) throws Exception {
        this.ciOpts.getOption(CACHE_SETTINGS_PATH).ifPresent(FileUtils::createDirectories);
        checkGitAuthToken(logger, this.ciOpts);

        // ci options from file
        final Optional<Properties> loadedProperties = ciOptsFromFile(this.ciOpts, logger);

        loadedProperties.ifPresent(props -> {
            logger.info(">>>>>>>>>> ---------- load options from file ---------- >>>>>>>>>>");
            logger.info(PropertiesUtils.toString(props, null));
            logger.info("<<<<<<<<<< ---------- load options from file ---------- <<<<<<<<<<");
        });

        this.ciOpts.updateSystemProperties(loadedProperties.orElse(null));
    }

    @Override
    public int getOrder() {
        return Orders.CONFIGURATION_PROCESSOR_ORDER_OPTION_FILE_LOADER;
    }

    private static void checkGitAuthToken(final Logger logger, final CiOptions ciOpts) {
        if (isEmpty(ciOpts.getOption(GIT_AUTH_TOKEN).orElse(null))) {
            // if (!originRepo) { // For PR build on travis-ci or appveyor }
            if (logger.isWarnEnabled()) {
                logger.warn(String.format("%s not set.", GIT_AUTH_TOKEN.getEnvVariableName()));
            }
        }
    }

    static Optional<Properties> ciOptsFromFile(
        final CiOptions ciOpts,
        final Logger logger
    ) {
        return ciOpts.getOption(InfraOption.CI_OPTS_FILE).map(ciOptsFile -> {
            final Properties properties = new Properties();

            ciOpts.getOption(CACHE_SETTINGS_PATH).ifPresent(FileUtils::createDirectories);

            final boolean offline = MavenUtils.cmdArgOffline(ciOpts.getSystemProperties()).orElse(FALSE);
            final boolean update = MavenUtils.cmdArgUpdate(ciOpts.getSystemProperties()).orElse(FALSE);
            GitRepository.newGitRepository(ciOpts, logger).ifPresent(repo -> {
                repo.download(SRC_CI_OPTS_PROPERTIES, ciOptsFile, true, offline, update);

                try {
                    properties.load(new FileInputStream(ciOptsFile));
                } catch (final IOException ex) {
                    final String errorMsg = String.format("Can not load ci options file %s", ex.getMessage());
                    throw new RuntimeIOException(errorMsg, ex);
                }
            });

            return properties;
        });
    }
}
