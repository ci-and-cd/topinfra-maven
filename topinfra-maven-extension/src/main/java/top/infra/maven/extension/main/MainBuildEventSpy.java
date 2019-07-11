package top.infra.maven.extension.main;

import static java.util.stream.Collectors.toList;
import static top.infra.maven.extension.shared.CiOptions.PATTERN_VARS_ENV_DOT_CI;
import static top.infra.maven.extension.shared.VcsProperties.GIT_REF_NAME;
import static top.infra.maven.utils.PropertiesUtils.logProperties;
import static top.infra.maven.utils.SupportFunction.isEmpty;
import static top.infra.maven.utils.SupportFunction.logEnd;
import static top.infra.maven.utils.SupportFunction.logStart;
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

import top.infra.maven.CiOptionContext;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.extension.shared.CiOptionContextBeanFactory;
import top.infra.maven.extension.shared.Orders;
import top.infra.maven.logging.Logger;
import top.infra.maven.logging.LoggerPlexusImpl;
import top.infra.maven.utils.MavenUtils;
import top.infra.maven.utils.PropertiesUtils;
import top.infra.maven.utils.SupportFunction;

/**
 * Main entry point. Reads properties and exposes them as user properties.
 * Existing user properties will not be overwritten.
 * <p/>
 * see: https://maven.apache.org/examples/maven-3-lifecycle-extensions.html
 */
// @org.codehaus.plexus.component.annotations.Component(role = org.apache.maven.eventspy.EventSpy.class)
@Named
@Singleton
public class MainBuildEventSpy extends AbstractEventSpy {

    private final Logger logger;

    private final CiOptionContextBeanFactory ciOptContextFactory;
    private final MainConfigurationProcessor cliRequestFactory;

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
    public MainBuildEventSpy(
        final org.codehaus.plexus.logging.Logger logger,
        final CiOptionContextBeanFactory ciOptContextFactory,
        final MainConfigurationProcessor cliRequestFactory,
        final List<MavenEventAware> eventAwares
    ) {
        logger.info(logStart(this, "constructor"));

        this.logger = new LoggerPlexusImpl(logger);
        this.ciOptContextFactory = ciOptContextFactory;
        this.cliRequestFactory = cliRequestFactory;

        this.ciOptContext = null;

        final List<MavenEventAware> list = eventAwares.stream().sorted().collect(toList());
        final Map<String, List<MavenEventAware>> map = new LinkedHashMap<>();
        predicates()
            .forEach(tuple -> map.put(tuple.getKey(), handlers(list, tuple.getValue())));
        this.eventAwares = map;

        IntStream
            .range(0, list.size())
            .forEach(idx -> {
                final MavenEventAware it = list.get(idx);
                logger.info(String.format(
                    "    eventAware index: [%s], order: [%s], name: [%s], from module: [%s]",
                    String.format("%02d ", idx),
                    String.format("%011d ", it.getOrder()),
                    it.getClass().getSimpleName(),
                    SupportFunction.module(it)
                ));
                logger.info(String.format("        handles: %s", handles(it)));
            });

        this.eventAwares.forEach((k, v) -> {
            logger.info(String.format("    event [%s]", k));
            v.forEach(it ->
                logger.info(String.format("        order: [%s], name: [%s], from module: [%s]",
                    it.getOrder(), it.getClass().getSimpleName(), SupportFunction.module(it)))
            );
        });
        logger.info(logEnd(this, "constructor", Void.TYPE));
    }

    private static List<String> handles(final MavenEventAware eventAware) {
        return predicates()
            .stream()
            .filter(tuple -> tuple.getValue().test(eventAware))
            .map(Entry::getKey)
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
        logger.info(logStart(this, "init", context));
        try {
            this.onInit(context);
        } catch (final Exception ex) {
            logger.error("    Exception on init.", ex);
            System.exit(1);
        }
        logger.info(logEnd(this, "init", Void.TYPE, context));
    }

    @Override
    public void onEvent(final Object event) throws Exception {

        try {
            final CliRequest cliRequest = this.cliRequestFactory.getCliRequest();

            if (event instanceof SettingsBuildingRequest) {
                logger.info(logStart(this, "afterInit"));
                this.afterInit(cliRequest, this.ciOptContext);
                logger.info(logEnd(this, "afterInit", Void.TYPE));

                final SettingsBuildingRequest request = (SettingsBuildingRequest) event;
                logger.info(logStart(this, "onSettingsBuildingRequest", request));
                this.onSettingsBuildingRequest(cliRequest, request, this.ciOptContext);
                logger.info(logEnd(this, "onSettingsBuildingRequest", Void.TYPE, request));
            } else if (event instanceof SettingsBuildingResult) {
                final SettingsBuildingResult result = (SettingsBuildingResult) event;
                logger.info(logStart(this, "onSettingsBuildingResult", result));
                this.onSettingsBuildingResult(cliRequest, result, this.ciOptContext);
                logger.info(logEnd(this, "onSettingsBuildingResult", Void.TYPE, result));
            } else if (event instanceof ToolchainsBuildingRequest) {
                final ToolchainsBuildingRequest request = (ToolchainsBuildingRequest) event;
                logger.info(logStart(this, "onToolchainsBuildingRequest", request));
                this.onToolchainsBuildingRequest(cliRequest, request, this.ciOptContext);
                logger.info(logEnd(this, "onToolchainsBuildingRequest", Void.TYPE, request));
            } else if (event instanceof ToolchainsBuildingResult) {
                final ToolchainsBuildingResult result = (ToolchainsBuildingResult) event;
                logger.info(logStart(this, "onToolchainsBuildingResult", result));
                this.onToolchainsBuildingResult(cliRequest, result, this.ciOptContext);
                logger.info(logEnd(this, "onToolchainsBuildingResult", Void.TYPE, result));
            } else if (event instanceof MavenExecutionRequest) {
                final MavenExecutionRequest request = (MavenExecutionRequest) event;
                logger.info(logStart(this, "onMavenExecutionRequest", request));
                this.onMavenExecutionRequest(cliRequest, request, this.ciOptContext);
                logger.info(logEnd(this, "onMavenExecutionRequest", Void.TYPE, request));
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("    onEvent %s", event));
                }
            }
        } catch (final Exception ex) {
            logger.error(String.format("    Exception on handling event [%s].", event), ex);
            System.exit(1);
        }

        super.onEvent(event);
    }

    public void afterInit(
        final CliRequest cliRequest,
        final CiOptionContext ciOptionContext
    ) {
        final Path rootProjectPath = MavenUtils.executionRootPath(cliRequest, ciOptionContext).toAbsolutePath();
        logger.info(String.format("    executionRootPath [%s]", rootProjectPath));

        assert Orders.ORDER_SYSTEM_TO_USER_PROPERTIES < Orders.ORDER_GIT_PROPERTIES;
        assert Orders.ORDER_GIT_PROPERTIES < Orders.ORDER_CI_OPTION_CONFIG_LOADER;
        assert Orders.ORDER_CI_OPTION_CONFIG_LOADER < Orders.ORDER_CI_OPTION_INIT;
        assert Orders.ORDER_CI_OPTION_INIT < Orders.ORDER_MAVEN_SETTINGS_LOCALREPOSITORY;

        this.eventAwares.get("afterInit")
            .forEach(it -> it.afterInit(cliRequest, ciOptionContext));

        final Optional<String> gitRefName = GIT_REF_NAME.getValue(ciOptionContext);
        if ((!gitRefName.isPresent() || isEmpty(gitRefName.get())) && logger.isWarnEnabled()) {
            logger.warn(String.format(
                "    Can not find value of %s (%s)",
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
            logger.info(String.format("    globalSettingsFile: [%s]", request.getGlobalSettingsFile()));
            logger.info(String.format("    globalSettingsSource: [%s]", request.getGlobalSettingsSource()));
            logger.info(String.format("    userSettingsFile: [%s]", request.getUserSettingsFile()));
            logger.info(String.format("    userSettingsSource: [%s]", request.getUserSettingsSource()));
        }

        assert Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_FILES < Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_SERVERS;

        this.eventAwares.get("onSettingsBuildingRequest")
            .forEach(it -> it.onSettingsBuildingRequest(cliRequest, request, ciOptionContext));
    }

    public void onSettingsBuildingResult(
        final CliRequest cliRequest,
        final SettingsBuildingResult result,
        final CiOptionContext ciOptionContext
    ) {
        // set settings.localRepository (if present) into effectiveSettings
        assert Orders.ORDER_CI_OPTION_INIT < Orders.ORDER_MAVEN_SETTINGS_LOCALREPOSITORY;

        this.eventAwares.get("onSettingsBuildingResult")
            .forEach(it -> it.onSettingsBuildingResult(cliRequest, result, ciOptionContext));
    }

    public void onToolchainsBuildingRequest(
        final CliRequest cliRequest,
        final ToolchainsBuildingRequest request,
        final CiOptionContext ciOptionContext
    ) {
        assert Orders.ORDER_CI_OPTION_INIT < Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_FILES;

        this.eventAwares.get("onToolchainsBuildingRequest")
            .forEach(it -> it.onToolchainsBuildingRequest(cliRequest, request, ciOptionContext));
    }

    public void onToolchainsBuildingResult(
        final CliRequest cliRequest,
        final ToolchainsBuildingResult result,
        final CiOptionContext ciOptionContext
    ) {
        this.eventAwares.get("onToolchainsBuildingResult")
            .forEach(it -> it.onToolchainsBuildingResult(cliRequest, result, ciOptionContext));
    }

    public void onMavenExecutionRequest(
        final CliRequest cliRequest,
        final MavenExecutionRequest request,
        final CiOptionContext ciOptionContext
    ) {
        assert Orders.ORDER_CI_OPTION_INIT < Orders.ORDER_MAVEN_SETTINGS_LOCALREPOSITORY;
        assert Orders.ORDER_MAVEN_SETTINGS_LOCALREPOSITORY < Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_SERVERS;

        this.eventAwares.get("onMavenExecutionRequest")
            .forEach(it -> it.onMavenExecutionRequest(cliRequest, request, ciOptionContext));


        final ProjectBuildingRequest projectBuildingRequest = request.getProjectBuildingRequest();
        if (projectBuildingRequest != null) {
            // To make profile activation conditions work
            PropertiesUtils.merge(request.getSystemProperties(), projectBuildingRequest.getSystemProperties());
            PropertiesUtils.merge(request.getUserProperties(), projectBuildingRequest.getUserProperties());
            if (logger.isInfoEnabled()) {
                logProperties(logger, "    projectBuildingRequest.systemProperties", projectBuildingRequest.getSystemProperties(), PATTERN_VARS_ENV_DOT_CI);
                logProperties(logger, "    projectBuildingRequest.userProperties", projectBuildingRequest.getUserProperties(), null);
            }

            logger.info(logStart(this, "onProjectBuildingRequest", projectBuildingRequest));
            this.onProjectBuildingRequest(cliRequest, request, projectBuildingRequest, ciOptionContext);
            logger.info(logEnd(this, "onProjectBuildingRequest", Void.TYPE, projectBuildingRequest));
        } else {
            if (logger.isInfoEnabled()) {
                logger.info(String.format("    onEvent MavenExecutionRequest %s but projectBuildingRequest is null.", request));
            }
        }
    }

    public void onProjectBuildingRequest(
        final CliRequest cliRequest,
        final MavenExecutionRequest mavenExecution,
        final ProjectBuildingRequest projectBuilding,
        final CiOptionContext ciOptionContext
    ) {
        // Options are not calculated and merged into projectBuildingRequest this time.

        // final File rootProjectDirectory = ((MavenExecutionRequest) request).getMultiModuleProjectDirectory();

        assert Orders.EVENT_AWARE_ORDER_MODEL_RESOLVER < Orders.EVENT_AWARE_ORDER_GPG_KEY;
        assert Orders.EVENT_AWARE_ORDER_GPG_KEY < Orders.EVENT_AWARE_ORDER_MAVEN_PROJECT_INFO;
        assert Orders.EVENT_AWARE_ORDER_MAVEN_PROJECT_INFO < Orders.EVENT_AWARE_ORDER_GOAL_EDITOR;
        assert Orders.EVENT_AWARE_ORDER_GOAL_EDITOR < Orders.ORDER_GIT_FLOW_SEMANTIC_VERSION;
        assert Orders.ORDER_GIT_FLOW_SEMANTIC_VERSION < Orders.EVENT_AWARE_ORDER_DOCKER;

        this.eventAwares.get("onProjectBuildingRequest")
            .forEach(it -> it.onProjectBuildingRequest(cliRequest, mavenExecution, projectBuilding, ciOptionContext));
    }

    public void onInit(final Context context) {
        if (logger.isDebugEnabled()) {
            final Map<String, Object> contextData = context.getData();
            contextData.keySet().stream().sorted().forEach(k -> {
                final Object v = contextData.get(k);
                if (v instanceof Properties) {
                    logger.debug(logProperties(logger, String.format("    context.data.%s", k), (Properties) v, null));
                } else {
                    logger.debug(PropertiesUtils.maskSecrets(String.format("    context.data.%s=%s", k, v)));
                }
            });
        }

        assert Orders.ORDER_INFO_PRINTER < Orders.ORDER_CI_OPTION_CONTEXT_FACTORY;
        assert Orders.ORDER_CI_OPTION_CONTEXT_FACTORY < Orders.ORDER_INFRASTRUCTURE_ACTIVATOR;

        this.eventAwares.get("onInit")
            .forEach(it -> it.onInit(context));

        this.ciOptContext = this.ciOptContextFactory.getCiOpts();
    }
}