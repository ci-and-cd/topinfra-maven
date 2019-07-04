package top.infra.maven.extension.infra;

import static top.infra.maven.Constants.SRC_CI_OPTS_PROPERTIES;
import static top.infra.maven.extension.InfraOption.CACHE_SETTINGS_PATH;
import static top.infra.maven.extension.InfraOption.GIT_AUTH_TOKEN;
import static top.infra.maven.extension.VcsProperties.GIT_REMOTE_ORIGIN_URL;
import static top.infra.maven.utils.SupportFunction.isEmpty;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.cli.CliRequest;

import top.infra.maven.core.CiOptionContext;
import top.infra.maven.exception.RuntimeIOException;
import top.infra.maven.extension.InfraOption;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.extension.Orders;
import top.infra.maven.logging.Logger;
import top.infra.maven.logging.LoggerPlexusImpl;
import top.infra.maven.utils.FileUtils;
import top.infra.maven.utils.MavenUtils;
import top.infra.maven.utils.PropertiesUtils;

@Named
@Singleton
public class OptionFileLoader implements MavenEventAware {

    private final Logger logger;

    @Inject
    public OptionFileLoader(
        final org.codehaus.plexus.logging.Logger logger
    ) {
        this.logger = new LoggerPlexusImpl(logger);
    }

    @Override
    public boolean afterInit() {
        return true;
    }

    @Override
    public void afterInit(
        final CliRequest cliRequest,
        final CiOptionContext ciOptionContext
    ) {
        this.load(cliRequest, ciOptionContext);
    }

    @Override
    public int getOrder() {
        return Orders.ORDER_OPTION_FILE_LOADER;
    }

    public void load(
        final CliRequest cliRequest,
        final CiOptionContext ciOptionContext
    ) {
        CACHE_SETTINGS_PATH.getValue(ciOptionContext).ifPresent(FileUtils::createDirectories);
        checkGitAuthToken(logger, ciOptionContext);

        // ci options from file
        final boolean offline = MavenUtils.cmdArgOffline(cliRequest);
        final boolean update = MavenUtils.cmdArgUpdateSnapshots(cliRequest);
        final String remoteOriginUrl = GIT_REMOTE_ORIGIN_URL
            .findInProperties(ciOptionContext.getSystemProperties(), ciOptionContext.getUserProperties())
            .orElse(null);
        final Optional<Properties> loadedProperties = ciOptContextFromFile(ciOptionContext, logger, remoteOriginUrl, offline, update);

        loadedProperties.ifPresent(props -> {
            logger.info(">>>>>>>>>> ---------- load options from file ---------- >>>>>>>>>>");
            logger.info(PropertiesUtils.toString(props, null));
            logger.info("<<<<<<<<<< ---------- load options from file ---------- <<<<<<<<<<");
        });

        ciOptionContext.updateSystemProperties(loadedProperties.orElse(null));
    }

    private static void checkGitAuthToken(final Logger logger, final CiOptionContext ciOptContext) {
        if (isEmpty(GIT_AUTH_TOKEN.getValue(ciOptContext).orElse(null))) {
            // if (!originRepo) { // For PR build on travis-ci or appveyor }
            if (logger.isWarnEnabled()) {
                logger.warn(String.format("%s not set.", GIT_AUTH_TOKEN.getEnvVariableName()));
            }
        }
    }

    static Optional<Properties> ciOptContextFromFile(
        final CiOptionContext ciOptContext,
        final Logger logger,
        final String remoteOriginUrl,
        final boolean offline,
        final boolean update
    ) {
        return InfraOption.CI_OPTS_FILE.getValue(ciOptContext).map(optsFile -> {
            final Properties properties = new Properties();

            CACHE_SETTINGS_PATH.getValue(ciOptContext).ifPresent(FileUtils::createDirectories);

            GitRepository.newGitRepository(ciOptContext, logger, remoteOriginUrl).ifPresent(repo -> {
                repo.download(SRC_CI_OPTS_PROPERTIES, Paths.get(optsFile), true, offline, update);

                try {
                    properties.load(new FileInputStream(optsFile));
                } catch (final IOException ex) {
                    final String errorMsg = String.format("Can not load ci options file %s", ex.getMessage());
                    throw new RuntimeIOException(errorMsg, ex);
                }
            });

            return properties;
        });
    }
}
