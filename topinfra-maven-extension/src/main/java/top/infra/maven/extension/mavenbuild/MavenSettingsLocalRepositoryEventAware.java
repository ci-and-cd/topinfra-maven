package top.infra.maven.extension.mavenbuild;

import static top.infra.maven.extension.mavenbuild.SystemToUserPropertiesEventAware.copyOrSetDefaultToUserProps;
import static top.infra.maven.utils.SupportFunction.isEmpty;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.eventspy.EventSpy.Context;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;

import top.infra.maven.core.CiOptionContext;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.extension.Orders;
import top.infra.maven.logging.Logger;
import top.infra.maven.logging.LoggerPlexusImpl;
import top.infra.maven.utils.MavenUtils;

/**
 * Support specify maven local repository by user property settings.localRepository.
 */
@Named
@Singleton
public class MavenSettingsLocalRepositoryEventAware implements MavenEventAware {

    private static final String USER_PROPERTY_SETTINGS_LOCALREPOSITORY = "settings.localRepository";

    private final Logger logger;

    private String settingsLocalRepository;

    @Inject
    public MavenSettingsLocalRepositoryEventAware(
        final org.codehaus.plexus.logging.Logger logger
    ) {
        this.logger = new LoggerPlexusImpl(logger);

        this.settingsLocalRepository = null;
    }

    @Override
    public int getOrder() {
        return Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_LOCALREPOSITORY;
    }

    @Override
    public void onInit(final Context context) {
        copyOrSetDefaultToUserProps(
            MavenUtils.systemProperties(context),
            MavenUtils.userProperties(context),
            USER_PROPERTY_SETTINGS_LOCALREPOSITORY,
            null
        );
    }

    @Override
    public void onSettingsBuildingRequest(final SettingsBuildingRequest request, final CiOptionContext ciOptContext) {
        // no-op
    }

    @Override
    public void onSettingsBuildingResult(final SettingsBuildingResult result, final CiOptionContext ciOptContext) {
        // Allow override value of localRepository in settings.xml by user property settings.localRepository.
        // e.g. ./mvnw -Dsettings.localRepository=${HOME}/.m3/repository clean install
        if (!isEmpty(this.settingsLocalRepository)) {
            final String currentValue = result.getEffectiveSettings().getLocalRepository();
            if (logger.isInfoEnabled()) {
                logger.info(String.format(
                    "Override localRepository [%s] to [%s]", currentValue, this.settingsLocalRepository));
            }
            result.getEffectiveSettings().setLocalRepository(this.settingsLocalRepository);
        }
    }

    @Override
    public void onMavenExecutionRequest(final MavenExecutionRequest request, final CiOptionContext ciOptContext) {
        if (isEmpty(this.settingsLocalRepository)) {
            this.settingsLocalRepository = request.getLocalRepository().getBasedir();
            if (logger.isInfoEnabled()) {
                logger.info(String.format("Current localRepository [%s]", this.settingsLocalRepository));
            }
            request.getUserProperties().setProperty(USER_PROPERTY_SETTINGS_LOCALREPOSITORY, this.settingsLocalRepository);
        }
    }
}