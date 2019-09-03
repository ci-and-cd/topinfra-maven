package top.infra.maven.extension.infra;

import cn.home1.tools.maven.MavenSettingsSecurity;
import org.apache.maven.cli.CliRequest;
import org.jetbrains.annotations.Nullable;
import top.infra.exception.RuntimeIOException;
import top.infra.logging.Logger;
import top.infra.maven.CiOptionContext;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.shared.extension.Orders;
import top.infra.maven.shared.logging.LoggerPlexusImpl;
import top.infra.maven.shared.utils.FileUtils;
import top.infra.maven.shared.utils.MavenUtils;
import top.infra.maven.shared.utils.PropertiesUtils;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

import static top.infra.maven.extension.infra.InfraOption.CACHE_SETTINGS_PATH;
import static top.infra.maven.extension.infra.InfraOption.GIT_AUTH_TOKEN;
import static top.infra.maven.shared.extension.Constants.SRC_CI_OPTS_PROPERTIES;
import static top.infra.maven.shared.extension.GlobalOption.MASTER_PASSWORD;
import static top.infra.maven.shared.utils.PropertiesUtils.logProperties;
import static top.infra.util.StringUtils.isEmpty;

@Named
@Singleton
public class CiOptionConfigLoader implements MavenEventAware {

    private final Logger logger;
    private final GitRepositoryFactory gitRepositoryFactory;

    @Inject
    public CiOptionConfigLoader(
        final org.codehaus.plexus.logging.Logger logger,
        final GitRepositoryFactory gitRepositoryFactory
    ) {
        this.logger = new LoggerPlexusImpl(logger);
        this.gitRepositoryFactory = gitRepositoryFactory;
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
        return Orders.ORDER_CI_OPTION_CONFIG_LOADER;
    }

    public void load(
        final CliRequest cliRequest,
        final CiOptionContext ciOptionContext
    ) {
        CACHE_SETTINGS_PATH.getValue(ciOptionContext).ifPresent(FileUtils::createDirectories);
        checkGitAuthToken(logger, ciOptionContext);

        // ci options from file
        final GitRepository gitRepository = this.gitRepositoryFactory.getObject().orElse(null);
        final boolean offline = MavenUtils.cmdArgOffline(cliRequest.getCommandLine());
        final boolean update = MavenUtils.cmdArgUpdateSnapshots(cliRequest.getCommandLine());
        final Optional<Properties> loadedProperties = ciOptContextFromFile(gitRepository, ciOptionContext, offline, update);

        loadedProperties.ifPresent(props -> {
            logProperties(logger, "    ci_opts.properties", props, null);

            MASTER_PASSWORD
                .findInProperties(MASTER_PASSWORD.getPropertyName(), ciOptionContext)
                .map(MavenSettingsSecurity::surroundByBrackets)
                .map(password -> new MavenSettingsSecurity(false, password))
                .ifPresent(settingsSecurity -> {
                    logger.info("    Master password found, decrypt loadedProperties.");
                    MavenUtils.decryptProperties(props, settingsSecurity)
                        .forEach(entry -> {
                            logger.info(String.format("    Decrypted loadedProperty [%s].", entry.getKey()));
                            props.put(entry.getKey(), entry.getValue());
                        });
                });
        });

        PropertiesUtils.setSystemPropertiesIfAbsent(ciOptionContext.getSystemProperties(), loadedProperties.orElse(null));
        PropertiesUtils.setSystemPropertiesIfAbsent(System.getProperties(), loadedProperties.orElse(null));
    }

    private static void checkGitAuthToken(final Logger logger, final CiOptionContext ciOptContext) {
        if (isEmpty(GIT_AUTH_TOKEN.getValue(ciOptContext).orElse(null))) {
            // if (!originRepo) { // For PR build on travis-ci or appveyor }
            if (logger.isWarnEnabled()) {
                logger.warn(String.format("    %s not set.", GIT_AUTH_TOKEN.getEnvVariableName()));
            }
        }
    }

    static Optional<Properties> ciOptContextFromFile(
        @Nullable final GitRepository gitRepository,
        final CiOptionContext ciOptContext,
        final boolean offline,
        final boolean update
    ) {
        return InfraOption.CI_OPTS_FILE.getValue(ciOptContext).map(optsFile -> {
            final Properties properties = new Properties();

            CACHE_SETTINGS_PATH.getValue(ciOptContext).ifPresent(FileUtils::createDirectories);

            Optional.ofNullable(gitRepository).ifPresent(repo -> {
                repo.download(SRC_CI_OPTS_PROPERTIES, Paths.get(optsFile), true, offline, update);

                try {
                    properties.load(new FileInputStream(optsFile));
                } catch (final IOException ex) {
                    final String errorMsg = String.format("    Can not load ci options file %s", ex.getMessage());
                    throw new RuntimeIOException(errorMsg, ex);
                }
            });

            return properties;
        });
    }
}
