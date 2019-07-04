package top.infra.maven.extension.main;

import static java.util.stream.Collectors.toList;
import static top.infra.maven.core.CiOptionNames.PATTERN_VARS_ENV_DOT_CI;
import static top.infra.maven.extension.VcsProperties.GIT_REF_NAME;
import static top.infra.maven.utils.SupportFunction.isEmpty;
import static top.infra.maven.utils.SupportFunction.newTuple;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.cli.CliRequest;
import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.apache.maven.toolchain.building.ToolchainsBuildingRequest;
import org.apache.maven.toolchain.building.ToolchainsBuildingResult;

import top.infra.maven.core.CiOptionContext;
import top.infra.maven.core.CiOptionContextBeanFactory;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.extension.Orders;
import top.infra.maven.logging.Logger;
import top.infra.maven.logging.LoggerPlexusImpl;
import top.infra.maven.utils.MavenUtils;
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

    private final CiOptionContextBeanFactory ciOptContextFactory;
    private final MavenBuildConfigurationProcessor cliRequestFactory;

    private final Map<String, List<MavenEventAware>> eventAwares;

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
        final CiOptionContextBeanFactory ciOptContextFactory,
        final MavenBuildConfigurationProcessor cliRequestFactory,
        final List<MavenEventAware> eventAwares
    ) {
        this.logger = new LoggerPlexusImpl(logger);
        this.ciOptContextFactory = ciOptContextFactory;
        this.cliRequestFactory = cliRequestFactory;

        this.ciOptContext = null;

        final List<MavenEventAware> list = eventAwares.stream().sorted().collect(toList());
        logger.info(String.format("MavenBuildEventSpy [%s]", this));
        IntStream
            .range(0, list.size())
            .forEach(idx -> {
                final MavenEventAware it = list.get(idx);
                logger.info(String.format(
                    "eventAware index: [%s], order: [%s], name: [%s]",
                    String.format("%02d ", idx),
                    String.format("%011d ", it.getOrder()),
                    it.getClass().getSimpleName()
                ));
                logger.info(String.format("    handles: %s", handles(it)));
            });

        final Map<String, List<MavenEventAware>> map = new LinkedHashMap<>();
        predicates()
            .forEach(tuple -> map.put(tuple.getKey(), handlers(list, tuple.getValue())));
        this.eventAwares = map;

        this.eventAwares.forEach((k, v) -> {
            logger.info(String.format("[%s]", k));
            v.forEach(it -> {
                logger.info(String.format("    [%s] (order [%s])", it.getClass().getSimpleName(), it.getOrder()));
            });
        });
    }

    private static List<String> handles(final MavenEventAware eventAware) {
        return predicates()
            .stream()
            .filter(tuple -> tuple.getValue().test(eventAware))
            .map(tuple -> tuple.getKey())
            .collect(toList());
    }

    private static List<MavenEventAware> handlers(
        final List<MavenEventAware> eventAwares,
        final Predicate<MavenEventAware> predicate
    ) {
        return eventAwares.stream().filter(predicate).sorted().collect(toList());
    }

    private static List<Entry<String, Predicate<MavenEventAware>>> predicates() {
        return Stream.<Entry<String, Predicate<MavenEventAware>>>of(
            newTuple("onInit", MavenEventAware::onInit),
            newTuple("afterInit", MavenEventAware::afterInit),
            newTuple("onSettingsBuildingRequest", MavenEventAware::onSettingsBuildingRequest),
            newTuple("onSettingsBuildingResult", MavenEventAware::onSettingsBuildingResult),
            newTuple("onToolchainsBuildingRequest", MavenEventAware::onToolchainsBuildingRequest),
            newTuple("onToolchainsBuildingResult", MavenEventAware::onToolchainsBuildingResult),
            newTuple("onMavenExecutionRequest", MavenEventAware::onMavenExecutionRequest),
            newTuple("onProjectBuildingRequest", MavenEventAware::onProjectBuildingRequest)
        ).collect(toList());
    }

    @Override
    public void init(final Context context) throws Exception {
        try {
            this.onInit(context);
        } catch (final Exception ex) {
            logger.error("Exception on init.", ex);
            System.exit(1);
        }
    }

    @Override
    public void onEvent(final Object event) throws Exception {
        try {
            final CliRequest cliRequest = this.cliRequestFactory.getCliRequest();

            if (event instanceof SettingsBuildingRequest) {
                this.afterInit(cliRequest, this.ciOptContext);

                final SettingsBuildingRequest request = (SettingsBuildingRequest) event;
                this.onSettingsBuildingRequest(cliRequest, request, this.ciOptContext);
            } else if (event instanceof SettingsBuildingResult) {
                final SettingsBuildingResult result = (SettingsBuildingResult) event;
                this.onSettingsBuildingResult(cliRequest, result, this.ciOptContext);
            } else if (event instanceof ToolchainsBuildingRequest) {
                final ToolchainsBuildingRequest request = (ToolchainsBuildingRequest) event;
                this.onToolchainsBuildingRequest(cliRequest, request, this.ciOptContext);
            } else if (event instanceof ToolchainsBuildingResult) {
                final ToolchainsBuildingResult result = (ToolchainsBuildingResult) event;
                this.onToolchainsBuildingResult(cliRequest, result, this.ciOptContext);
            } else if (event instanceof MavenExecutionRequest) {
                final MavenExecutionRequest request = (MavenExecutionRequest) event;
                this.onMavenExecutionRequest(cliRequest, request, this.ciOptContext);
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

    public void afterInit(
        final CliRequest cliRequest,
        final CiOptionContext ciOptionContext
    ) {
        final Path rootProjectPath = MavenUtils.executionRootPath(cliRequest, ciOptionContext).toAbsolutePath();
        logger.info(String.format("executionRootPath [%s]", rootProjectPath));

        assert Orders.ORDER_OPTIONS_FACTORY < Orders.ORDER_OPTION_FILE_LOADER;
        // move -Dproperty=value in MAVEN_OPTS from systemProperties into userProperties (maven does not do this automatically)
        assert Orders.ORDER_SYSTEM_TO_USER_PROPERTIES < Orders.EVENT_AWARE_ORDER_CI_OPTION;
        // init ci options
        // try to read settings.localRepository from request.userProperties
        assert Orders.EVENT_AWARE_ORDER_CI_OPTION < Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_LOCALREPOSITORY;

        this.eventAwares.get("afterInit")
            .forEach(it -> it.afterInit(cliRequest, ciOptionContext));

        final Optional<String> gitRefName = GIT_REF_NAME.getValue(ciOptionContext);
        if ((!gitRefName.isPresent() || isEmpty(gitRefName.get())) && logger.isWarnEnabled()) {
            logger.warn(String.format(
                "Can not find value of %s (%s)",
                GIT_REF_NAME.getEnvVariableName(), GIT_REF_NAME.getPropertyName()
            ));
        }
    }

    /**
     * After org.apache.maven.cli.configuration.ConfigurationProcessor.
     *
     * @param cliRequest      cliRequest
     * @param request         {@link SettingsBuildingRequest}
     * @param ciOptionContext context
     */
    public void onSettingsBuildingRequest(
        final CliRequest cliRequest,
        final SettingsBuildingRequest request,
        final CiOptionContext ciOptionContext
    ) {
        if (logger.isInfoEnabled()) {
            logger.info(String.format("onEvent SettingsBuildingRequest %s", request));
            logger.info(String.format("onEvent SettingsBuildingRequest. globalSettingsFile: [%s]", request.getGlobalSettingsFile()));
            logger.info(String.format("onEvent SettingsBuildingRequest. globalSettingsSource: [%s]", request.getGlobalSettingsSource()));
            logger.info(String.format("onEvent SettingsBuildingRequest. userSettingsFile: [%s]", request.getUserSettingsFile()));
            logger.info(String.format("onEvent SettingsBuildingRequest. userSettingsSource: [%s]", request.getUserSettingsSource()));
        }

        // download maven settings.xml, settings-security.xml (optional) and toolchains.xml
        assert Orders.EVENT_AWARE_ORDER_CI_OPTION < Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_FILES;
        // set custom settings file (if present) into request.userSettingsFile
        assert Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_LOCALREPOSITORY < Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_FILES;
        // warn about absent env.VARIABLEs in settings.xml's server tags
        assert Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_FILES < Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_SERVERS;

        this.eventAwares.get("onSettingsBuildingRequest")
            .forEach(it -> it.onSettingsBuildingRequest(cliRequest, request, ciOptionContext));
    }

    public void onSettingsBuildingResult(
        final CliRequest cliRequest,
        final SettingsBuildingResult result,
        final CiOptionContext ciOptionContext
    ) {
        if (logger.isInfoEnabled()) {
            logger.info(String.format("onEvent SettingsBuildingResult %s", result));
        }

        // set settings.localRepository (if present) into effectiveSettings
        assert Orders.EVENT_AWARE_ORDER_CI_OPTION < Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_LOCALREPOSITORY;

        this.eventAwares.get("onSettingsBuildingResult")
            .forEach(it -> it.onSettingsBuildingResult(cliRequest, result, ciOptionContext));
    }

    public void onToolchainsBuildingRequest(
        final CliRequest cliRequest,
        final ToolchainsBuildingRequest request,
        final CiOptionContext ciOptionContext
    ) {
        if (logger.isInfoEnabled()) {
            logger.info(String.format("onEvent ToolchainsBuildingRequest %s", request));
        }

        this.eventAwares.get("onToolchainsBuildingRequest")
            .forEach(it -> it.onToolchainsBuildingRequest(cliRequest, request, ciOptionContext));
    }

    public void onToolchainsBuildingResult(
        final CliRequest cliRequest,
        final ToolchainsBuildingResult result,
        final CiOptionContext ciOptionContext
    ) {
        if (logger.isInfoEnabled()) {
            logger.info(String.format("onEvent ToolchainsBuildingResult %s", result));
        }

        this.eventAwares.get("onToolchainsBuildingResult")
            .forEach(it -> it.onToolchainsBuildingResult(cliRequest, result, ciOptionContext));
    }

    public void onMavenExecutionRequest(
        final CliRequest cliRequest,
        final MavenExecutionRequest request,
        final CiOptionContext ciOptionContext
    ) {
        if (logger.isInfoEnabled()) {
            logger.info(String.format("onEvent MavenExecutionRequest %s", request));
        }

        // if settings.localRepository absent, set mavenExecutionRequest.ocalRepository into userProperties
        assert Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_LOCALREPOSITORY < Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_SERVERS;
        // check empty or blank property values in settings.servers

        this.eventAwares.get("onMavenExecutionRequest")
            .forEach(it -> it.onMavenExecutionRequest(cliRequest, request, ciOptionContext));


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

            this.onProjectBuildingRequest(cliRequest, request, projectBuildingRequest, ciOptionContext);
        } else {
            if (logger.isInfoEnabled()) {
                logger.info(String.format("onEvent MavenExecutionRequest %s but projectBuildingRequest is null.", request));
            }
        }
    }

    public void onProjectBuildingRequest(
        final CliRequest cliRequest,
        final MavenExecutionRequest mavenExecution,
        final ProjectBuildingRequest projectBuilding,
        final CiOptionContext ciOptionContext
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

        this.eventAwares.get("onProjectBuildingRequest")
            .forEach(it -> it.onProjectBuildingRequest(cliRequest, mavenExecution, projectBuilding, ciOptionContext));
    }

    public void onInit(final Context context) {
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

        // print info
        assert Orders.ORDER_INFO_PRINTER < Orders.ORDER_SYSTEM_TO_USER_PROPERTIES;
        // ciOptionContext
        assert Orders.ORDER_SYSTEM_TO_USER_PROPERTIES < Orders.ORDER_OPTIONS_FACTORY;

        this.eventAwares.get("onInit")
            .forEach(it -> it.onInit(context));

        this.ciOptContext = this.ciOptContextFactory.getCiOpts();
    }
}
