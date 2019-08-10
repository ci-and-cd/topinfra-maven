package top.infra.maven.extension.main;

import static top.infra.maven.extension.main.MavenBuildExtensionOption.SETTINGS_LOCALREPOSITORY;
import static top.infra.maven.shared.extension.Constants.PROP_SETTINGS_LOCALREPOSITORY;
import static top.infra.util.StringUtils.isEmpty;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.cli.CliRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;

import top.infra.logging.Logger;
import top.infra.maven.CiOptionContext;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.shared.extension.Orders;
import top.infra.maven.shared.logging.LoggerPlexusImpl;

/**
 * Support specify maven local repository by user property settings.localRepository.
 */
@Deprecated
@Named
@Singleton
public class MavenSettingsLocalRepositoryEventAware implements MavenEventAware {

    private final Logger logger;

    @Inject
    public MavenSettingsLocalRepositoryEventAware(
        final org.codehaus.plexus.logging.Logger logger
    ) {
        this.logger = new LoggerPlexusImpl(logger);
    }

    @Override
    public int getOrder() {
        return Orders.ORDER_MAVEN_SETTINGS_LOCALREPOSITORY;
    }

    @Override
    public boolean onSettingsBuildingResult() {
        return true;
    }

    @Override
    public void onSettingsBuildingResult(
        final CliRequest cliRequest,
        final SettingsBuildingResult result,
        final CiOptionContext ciOptContext
    ) {
        // Allow override value of localRepository in settings.xml by user property settings.localRepository.
        // e.g. ./mvnw -Dsettings.localRepository=${HOME}/.m3/repository clean install
        final String settingsLocalRepository = SETTINGS_LOCALREPOSITORY.getValue(ciOptContext).orElse(null);
        if (!isEmpty(settingsLocalRepository)) {
            final String currentValue = result.getEffectiveSettings().getLocalRepository();
            if (logger.isInfoEnabled()) {
                logger.info(String.format(
                    "    Override localRepository [%s] to [%s]", currentValue, settingsLocalRepository));
            }
            result.getEffectiveSettings().setLocalRepository(settingsLocalRepository);
        }
    }

    @Override
    public boolean onMavenExecutionRequest() {
        return true;
    }

    @Override
    public void onMavenExecutionRequest(
        final CliRequest cliRequest,
        final MavenExecutionRequest request,
        final CiOptionContext ciOptContext
    ) {
        if (isEmpty(SETTINGS_LOCALREPOSITORY.getValue(ciOptContext).orElse(null))) { // expose this property to pom
            final String settingsLocalRepository = request.getLocalRepository().getBasedir();
            if (logger.isInfoEnabled()) {
                logger.info(String.format("    Current localRepository [%s]", settingsLocalRepository));
            }
            request.getUserProperties().setProperty(PROP_SETTINGS_LOCALREPOSITORY, settingsLocalRepository);
        }
    }
}
