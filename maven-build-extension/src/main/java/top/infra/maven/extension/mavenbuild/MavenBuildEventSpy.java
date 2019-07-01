package top.infra.maven.extension.mavenbuild;

import static top.infra.maven.core.CiOptionNames.PATTERN_VARS_ENV_DOT_CI;
import static top.infra.maven.extension.MavenBuildExtensionOption.GIT_REF_NAME;
import static top.infra.maven.utils.SupportFunction.isEmpty;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.apache.maven.toolchain.building.ToolchainsBuildingRequest;
import org.apache.maven.toolchain.building.ToolchainsBuildingResult;

import top.infra.maven.core.CiOptionContext;
import top.infra.maven.core.CiOptionContextFactoryBean;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.extension.Orders;
import top.infra.maven.logging.Logger;
import top.infra.maven.logging.LoggerPlexusImpl;
import top.infra.maven.utils.PropertiesUtils;

/**
 * Main entry point. Reads properties and exposes them as user properties.
 * Existing user properties will not be overwritten.
 * <p/>
 * see: https://maven.apache.org/examples/maven-3-lifecycle-extensions.html
 */
// @org.codehaus.plexus.component.annotations.Component(role = org.apache.maven.eventspy.EventSpy.class)
@Named
@Singleton
public class MavenBuildEventSpy extends AbstractEventSpy {

    private final Logger logger;

    private final CiOptionContextFactoryBean ciOptContextFactory;

    private final List<MavenEventAware> eventAwares;

    private CiOptionContext ciOptContext;

    /**
     * Constructor.
     *
     * @param logger              inject logger {@link org.codehaus.plexus.logging.Logger}
     * @param ciOptContextFactory ciOptContextFactory
     * @param eventAwares         inject eventAwares
     */
    @Inject
    public MavenBuildEventSpy(
        final org.codehaus.plexus.logging.Logger logger,
        final CiOptionContextFactoryBean ciOptContextFactory,
        final List<MavenEventAware> eventAwares
    ) {
        this.logger = new LoggerPlexusImpl(logger);
        this.ciOptContextFactory = ciOptContextFactory;
        this.eventAwares = eventAwares.stream().sorted().collect(Collectors.toList());

        this.ciOptContext = null;

        logger.info(String.format("MavenBuildEventSpy [%s]", this));
        IntStream
            .range(0, this.eventAwares.size())
            .forEach(idx -> {
                final MavenEventAware it = this.eventAwares.get(idx);
                logger.info(String.format(
                    "eventAware index: [%s], order: [%s], name: [%s]",
                    String.format("%02d ", idx),
                    String.format("%011d ", it.getOrder()),
                    it.getClass().getSimpleName()
                ));
            });
    }

    @Override
    public void init(final Context context) throws Exception {
        try {
            if (logger.isInfoEnabled()) {
                logger.info(String.format("init with context [%s]", context));

                final Map<String, Object> contextData = context.getData();
                contextData.keySet().stream().sorted().forEach(k -> {
                    final Object v = contextData.get(k);
                    if (v instanceof Properties) {
                        logger.info(String.format("contextData found properties %s => ", k));
                        logger.info(PropertiesUtils.toString((Properties) v, null));
                    } else {
                        logger.info(String.format("contextData found property   %s => %s", k, v));
                    }
                });
            }

            this.onInit(context);
        } catch (final Exception ex) {
            logger.error("Exception on init.", ex);
            System.exit(1);
        }
    }

    @Override
    public void onEvent(final Object event) throws Exception {
        try {
            if (event instanceof SettingsBuildingRequest) {
                final SettingsBuildingRequest request = (SettingsBuildingRequest) event;
                this.onSettingsBuildingRequest(request, this.ciOptContext);
            } else if (event instanceof SettingsBuildingResult) {
                final SettingsBuildingResult result = (SettingsBuildingResult) event;
                this.onSettingsBuildingResult(result, this.ciOptContext);
            } else if (event instanceof ToolchainsBuildingRequest) {
                final ToolchainsBuildingRequest request = (ToolchainsBuildingRequest) event;
                this.onToolchainsBuildingRequest(request, this.ciOptContext);
            } else if (event instanceof ToolchainsBuildingResult) {
                final ToolchainsBuildingResult result = (ToolchainsBuildingResult) event;
                this.onToolchainsBuildingResult(result, this.ciOptContext);
            } else if (event instanceof MavenExecutionRequest) {
                final MavenExecutionRequest request = (MavenExecutionRequest) event;
                this.onMavenExecutionRequest(request, this.ciOptContext);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("onEvent %s", event));
                }
            }
        } catch (final Exception ex) {
            logger.error(String.format("Exception on handling event [%s].", event), ex);
            System.exit(1);
        }

        super.onEvent(event);
    }

    public void onInit(final Context context) {
        // print info
        assert Orders.ORDER_INFO_PRINTER < Orders.ORDER_SYSTEM_TO_USER_PROPERTIES;
        // move -Dproperty=value in MAVEN_OPTS from systemProperties into userProperties (maven does not do this automatically)
        assert Orders.ORDER_SYSTEM_TO_USER_PROPERTIES < Orders.EVENT_AWARE_ORDER_CI_OPTION;
        // init ci options

        this.eventAwares.forEach(it -> it.onInit(context));

        this.ciOptContext = this.ciOptContextFactory.getCiOpts();
        final Optional<String> gitRefName = GIT_REF_NAME.getValue(this.ciOptContext);
        if ((!gitRefName.isPresent() || isEmpty(gitRefName.get())) && logger.isWarnEnabled()) {
            logger.warn(String.format(
                "Can not find value of %s (%s)",
                GIT_REF_NAME.getEnvVariableName(), GIT_REF_NAME.getPropertyName()
            ));
        }

        this.afterInit(context, this.ciOptContext);
    }

    public void afterInit(final Context context, final CiOptionContext ciOptContext) {
        // try to read settings.localRepository from request.userProperties
        assert Orders.EVENT_AWARE_ORDER_CI_OPTION < Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_LOCALREPOSITORY;
        // download maven settings.xml, settings-security.xml (optional) and toolchains.xml
        assert Orders.EVENT_AWARE_ORDER_CI_OPTION < Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_FILES;
        assert Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_LOCALREPOSITORY < Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_FILES;
        // warn about absent env.VARIABLEs in settings.xml's server tags
        assert Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_FILES < Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_SERVERS;

        this.eventAwares.forEach(it -> it.afterInit(context, ciOptContext));
    }

    public void onSettingsBuildingRequest(
        final SettingsBuildingRequest request,
        final CiOptionContext ciOptContext
    ) {
        if (logger.isInfoEnabled()) {
            logger.info(String.format("onEvent SettingsBuildingRequest %s", request));
            logger.info(String.format("onEvent SettingsBuildingRequest. globalSettingsFile: [%s]", request.getGlobalSettingsFile()));
            logger.info(String.format("onEvent SettingsBuildingRequest. globalSettingsSource: [%s]", request.getGlobalSettingsSource()));
            logger.info(String.format("onEvent SettingsBuildingRequest. userSettingsFile: [%s]", request.getUserSettingsFile()));
            logger.info(String.format("onEvent SettingsBuildingRequest. userSettingsSource: [%s]", request.getUserSettingsSource()));
        }

        // set custom settings file (if present) into request.userSettingsFile
        assert Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_LOCALREPOSITORY < Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_FILES;

        this.eventAwares.forEach(it -> it.onSettingsBuildingRequest(request, ciOptContext));
    }

    public void onSettingsBuildingResult(
        final SettingsBuildingResult result,
        final CiOptionContext ciOptContext
    ) {
        if (logger.isInfoEnabled()) {
            logger.info(String.format("onEvent SettingsBuildingResult %s", result));
        }

        // set settings.localRepository (if present) into effectiveSettings
        assert Orders.EVENT_AWARE_ORDER_CI_OPTION < Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_LOCALREPOSITORY;

        this.eventAwares.forEach(it -> it.onSettingsBuildingResult(result, ciOptContext));
    }

    public void onToolchainsBuildingRequest(
        final ToolchainsBuildingRequest request,
        final CiOptionContext ciOptContext
    ) {
        if (logger.isInfoEnabled()) {
            logger.info(String.format("onEvent ToolchainsBuildingRequest %s", request));
        }

        this.eventAwares.forEach(it -> it.onToolchainsBuildingRequest(request, ciOptContext));
    }

    public void onToolchainsBuildingResult(
        final ToolchainsBuildingResult result,
        final CiOptionContext ciOptContext
    ) {
        if (logger.isInfoEnabled()) {
            logger.info(String.format("onEvent ToolchainsBuildingResult %s", result));
        }

        this.eventAwares.forEach(it -> it.onToolchainsBuildingResult(result, ciOptContext));
    }

    public void onMavenExecutionRequest(
        final MavenExecutionRequest request,
        final CiOptionContext ciOptContext
    ) {
        if (logger.isInfoEnabled()) {
            logger.info(String.format("onEvent MavenExecutionRequest %s", request));
        }

        // if settings.localRepository absent, set mavenExecutionRequest.ocalRepository into userProperties
        assert Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_LOCALREPOSITORY < Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_SERVERS;
        // check empty or blank property values in settings.servers

        this.eventAwares.forEach(it -> it.onMavenExecutionRequest(request, ciOptContext));


        final ProjectBuildingRequest projectBuildingRequest = request.getProjectBuildingRequest();
        if (projectBuildingRequest != null) {
            // To make profile activation conditions work
            PropertiesUtils.merge(request.getSystemProperties(), projectBuildingRequest.getSystemProperties());
            PropertiesUtils.merge(request.getUserProperties(), projectBuildingRequest.getUserProperties());
            if (logger.isInfoEnabled()) {
                logger.info("     >>>>> projectBuildingRequest (ProfileActivationContext) systemProperties >>>>>");
                logger.info(PropertiesUtils.toString(projectBuildingRequest.getSystemProperties(), PATTERN_VARS_ENV_DOT_CI));
                logger.info("     <<<<< projectBuildingRequest (ProfileActivationContext) systemProperties <<<<<");

                logger.info("     >>>>> projectBuildingRequest (ProfileActivationContext) userProperties >>>>>");
                logger.info(PropertiesUtils.toString(projectBuildingRequest.getUserProperties(), null));
                logger.info("     <<<<< projectBuildingRequest (ProfileActivationContext) userProperties <<<<<");
            }

            this.onProjectBuildingRequest(request, projectBuildingRequest, ciOptContext);
        } else {
            if (logger.isInfoEnabled()) {
                logger.info(String.format("onEvent MavenExecutionRequest %s but projectBuildingRequest is null.", request));
            }
        }
    }

    public void onProjectBuildingRequest(
        final MavenExecutionRequest mavenExecution,
        final ProjectBuildingRequest projectBuilding,
        final CiOptionContext ciOptContext
    ) {
        if (logger.isInfoEnabled()) {
            logger.info(String.format("onEvent ProjectBuildingRequest %s", projectBuilding));
        }

        // Options are not calculated and merged into projectBuildingRequest this time.

        // final File rootProjectDirectory = ((MavenExecutionRequest) request).getMultiModuleProjectDirectory();

        // set projectBuildingRequest into project model resolver
        assert Orders.EVENT_AWARE_ORDER_MODEL_RESOLVER < Orders.EVENT_AWARE_ORDER_GPG;
        // decrypt gpg keys
        assert Orders.EVENT_AWARE_ORDER_GPG < Orders.EVENT_AWARE_ORDER_MAVEN_PROJECT_INFO;
        // check project version and assert it is valid
        assert Orders.EVENT_AWARE_ORDER_MAVEN_PROJECT_INFO < Orders.EVENT_AWARE_ORDER_GOAL_EDITOR;
        // edit goals
        assert Orders.EVENT_AWARE_ORDER_GOAL_EDITOR < Orders.EVENT_AWARE_ORDER_DOCKER;
        // prepare docker

        this.eventAwares.forEach(it -> it.onProjectBuildingRequest(mavenExecution, projectBuilding, ciOptContext));
    }
}
