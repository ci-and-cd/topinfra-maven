package top.infra.maven.extension.infra;

import static java.lang.Boolean.FALSE;
import static top.infra.maven.Constants.SRC_CI_OPTS_PROPERTIES;
import static top.infra.maven.extension.InfraOption.CACHE_SETTINGS_PATH;
import static top.infra.maven.extension.InfraOption.GIT_AUTH_TOKEN;
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
import org.apache.maven.eventspy.EventSpy.Context;

import top.infra.maven.core.CiOptionContext;
import top.infra.maven.core.CiOptionContextBeanFactory;
import top.infra.maven.core.GitPropertiesBeanFactory;
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

    private final String remoteOriginUrl;

    private final CiOptionContextBeanFactory ciOptionContextBeanFactory;

    @Inject
    public OptionFileLoader(
        final org.codehaus.plexus.logging.Logger logger,
        final CiOptionContextBeanFactory ciOptionContextBeanFactory,
        final GitPropertiesBeanFactory gitPropertiesBeanFactory
    ) {
        this.logger = new LoggerPlexusImpl(logger);
        this.remoteOriginUrl = gitPropertiesBeanFactory.getObject().remoteOriginUrl().orElse(null);
        this.ciOptionContextBeanFactory = ciOptionContextBeanFactory;
    }

    @Override
    public int getOrder() {
        return Orders.ORDER_OPTION_FILE_LOADER;
    }

    @Override
    public void onInit(final Context context) {
        this.load();
    }

    public void load() {
        final CiOptionContext context = this.ciOptionContextBeanFactory.getCiOpts();
        CACHE_SETTINGS_PATH.getValue(context).ifPresent(FileUtils::createDirectories);
        checkGitAuthToken(logger, context);

        // ci options from file
        final Optional<Properties> loadedProperties = ciOptContextFromFile(context, logger, this.remoteOriginUrl);

        loadedProperties.ifPresent(props -> {
            logger.info(">>>>>>>>>> ---------- load options from file ---------- >>>>>>>>>>");
            logger.info(PropertiesUtils.toString(props, null));
            logger.info("<<<<<<<<<< ---------- load options from file ---------- <<<<<<<<<<");
        });

        context.updateSystemProperties(loadedProperties.orElse(null));
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
        final String remoteOriginUrl
    ) {
        return InfraOption.CI_OPTS_FILE.getValue(ciOptContext).map(optsFile -> {
            final Properties properties = new Properties();

            CACHE_SETTINGS_PATH.getValue(ciOptContext).ifPresent(FileUtils::createDirectories);

            final boolean offline = MavenUtils.cmdArgOffline(ciOptContext.getSystemProperties()).orElse(FALSE);
            final boolean update = MavenUtils.cmdArgUpdate(ciOptContext.getSystemProperties()).orElse(FALSE);
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

    @Deprecated
    public void process(final CliRequest request) throws Exception {
        this.load();
    }
}
