package top.infra.maven.extension.main;

import static java.util.stream.Collectors.toList;
import static top.infra.maven.shared.extension.VcsProperties.GIT_REF_NAME;
import static top.infra.maven.shared.utils.PropertiesUtils.PATTERN_VARS_ENV_DOT_CI;
import static top.infra.maven.shared.utils.PropertiesUtils.logProperties;
import static top.infra.maven.shared.utils.SupportFunction.componentName;
import static top.infra.maven.shared.utils.SupportFunction.logEnd;
import static top.infra.maven.shared.utils.SupportFunction.logStart;
import static top.infra.maven.shared.utils.SupportFunction.newTuple;
import static top.infra.util.StringUtils.isEmpty;

import java.nio.file.Path;
import java.util.Collections;
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
import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.apache.maven.toolchain.building.ToolchainsBuildingRequest;
import org.apache.maven.toolchain.building.ToolchainsBuildingResult;

import top.infra.logging.Logger;
import top.infra.maven.CiOptionContext;
import top.infra.maven.Ordered;
import top.infra.maven.extension.CiOptionContextFactory;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.extension.OrderedConfigurationProcessor;
import top.infra.maven.shared.extension.Orders;
import top.infra.maven.shared.logging.LoggerPlexusImpl;
import top.infra.maven.shared.utils.MavenUtils;
import top.infra.maven.shared.utils.PropertiesUtils;
import top.infra.maven.shared.utils.SupportFunction;

/**
 * Main entry point. Reads properties and exposes them as user properties.
 * Existing user properties will not be overwritten.
 * <p/>
 * see: https://maven.apache.org/examples/maven-3-lifecycle-extensions.html
 */
// @org.codehaus.plexus.component.annotations.Component(role = org.apache.maven.eventspy.EventSpy.class)
@Named
@Singleton
public class MainEventSpy extends AbstractEventSpy implements OrderedConfigurationProcessor {

    private final Logger logger;

    private final CiOptionContextFactory ciOptContextFactory;

    private final List<MavenEventAware> eventAwares;

    private CiOptionContext ciOptContext;

    private CliRequest cliRequest;

    private Map<String, List<MavenEventAware>> handlerMap;

    /**
     * Constructor.
     *
     * @param logger              inject logger {@link org.codehaus.plexus.logging.Logger}
     * @param ciOptContextFactory ciOptContextFactory
     * @param eventAwares         inject eventAwares
     */
    @Inject
    public MainEventSpy(
        final org.codehaus.plexus.logging.Logger logger,
        final CiOptionContextFactory ciOptContextFactory,
        final List<MavenEventAware> eventAwares
    ) {
        logger.info(logStart(this, "constructor"));

        this.logger = new LoggerPlexusImpl(logger);
        this.ciOptContextFactory = ciOptContextFactory;
        this.eventAwares = Collections.unmodifiableList(eventAwares);

        this.ciOptContext = null;
        this.cliRequest = null;
        this.handlerMap = null;

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
            final Properties systemProperties = MavenUtils.systemProperties(context);
            final Properties userProperties = MavenUtils.userProperties(context);
            final List<MavenEventAware> list = this.eventAwares
                .stream()
                .sorted()
                .filter(it -> {
                    final boolean disabled = SupportFunction.componentDisabled(it.getClass(), systemProperties, userProperties);
                    if (disabled) {
                        logger.info(String.format("    eventAware [%s] disabled", componentName(it.getClass())));
                    }
                    return !disabled;
                })
                .collect(toList());
            final Map<String, List<MavenEventAware>> map = new LinkedHashMap<>();
            predicates()
                .forEach(tuple -> map.put(tuple.getKey(), handlers(list, tuple.getValue())));
            this.handlerMap = map;

            IntStream
                .range(0, list.size())
                .forEach(idx -> {
                    final MavenEventAware it = list.get(idx);
                    logger.info(String.format(
                        "    eventAware index: [%s], order: [%s], name: [%s], from module: [%s]",
                        String.format("%02d ", idx),
                        String.format("%011d ", it.getOrder()),
                        componentName(it.getClass()),
                        SupportFunction.module(it)
                    ));
                    logger.info(String.format("        handles: %s", handles(it)));
                });

            this.handlerMap.forEach((k, v) -> {
                logger.info(String.format("    event [%s]", k));
                v.forEach(it ->
                    logger.info(String.format("        order: [%s], name: [%s], from module: [%s]",
                        it.getOrder(), componentName(it.getClass()), SupportFunction.module(it)))
                );
            });

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
            if (event instanceof SettingsBuildingRequest) {
                final SettingsBuildingRequest request = (SettingsBuildingRequest) event;
                logger.info(logStart(this, "onSettingsBuildingRequest", request));
                this.onSettingsBuildingRequest(this.cliRequest, request, this.ciOptContext);
                logger.info(logEnd(this, "onSettingsBuildingRequest", Void.TYPE, request));
            } else if (event instanceof SettingsBuildingResult) {
                final SettingsBuildingResult result = (SettingsBuildingResult) event;
                logger.info(logStart(this, "onSettingsBuildingResult", result));
                this.onSettingsBuildingResult(this.cliRequest, result, this.ciOptContext);
                logger.info(logEnd(this, "onSettingsBuildingResult", Void.TYPE, result));
            } else if (event instanceof ToolchainsBuildingRequest) {
                final ToolchainsBuildingRequest request = (ToolchainsBuildingRequest) event;
                logger.info(logStart(this, "onToolchainsBuildingRequest", request));
                this.onToolchainsBuildingRequest(this.cliRequest, request, this.ciOptContext);
                logger.info(logEnd(this, "onToolchainsBuildingRequest", Void.TYPE, request));
            } else if (event instanceof ToolchainsBuildingResult) {
                final ToolchainsBuildingResult result = (ToolchainsBuildingResult) event;
                logger.info(logStart(this, "onToolchainsBuildingResult", result));
                this.onToolchainsBuildingResult(this.cliRequest, result, this.ciOptContext);
                logger.info(logEnd(this, "onToolchainsBuildingResult", Void.TYPE, result));
            } else if (event instanceof MavenExecutionRequest) {
                final MavenExecutionRequest request = (MavenExecutionRequest) event;
                logger.info(logStart(this, "onMavenExecutionRequest", request));
                this.onMavenExecutionRequest(this.cliRequest, request, this.ciOptContext);
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

    /**
     * After {@link EventSpy#init(Context)}, before {@link EventSpy#onEvent(Object)}.
     * Actually called by {@link MainConfigurationProcessor#process(CliRequest)}.
     *
     * @param cliRequest      cliRequest
     * @param ciOptionContext ciOptionContext
     */
    public void afterInit(
        final CliRequest cliRequest,
        final CiOptionContext ciOptionContext
    ) {
        final Path rootProjectPath = MavenUtils.executionRootPath(cliRequest).toAbsolutePath();
        logger.info(String.format("    executionRootPath [%s]", rootProjectPath));

        assert Orders.ORDER_SYSTEM_TO_USER_PROPERTIES < Orders.ORDER_GIT_PROPERTIES;
        assert Orders.ORDER_GIT_PROPERTIES < Orders.ORDER_GIT_REPO_FACTORY;
        assert Orders.ORDER_GIT_REPO_FACTORY < Orders.ORDER_CACHE_SETTINGS_RESOURCES_FACTORY;
        assert Orders.ORDER_CACHE_SETTINGS_RESOURCES_FACTORY < Orders.ORDER_CI_OPTION_CONFIG_LOADER;
        assert Orders.ORDER_CI_OPTION_CONFIG_LOADER < Orders.ORDER_CI_OPTION_INIT;
        assert Orders.ORDER_CI_OPTION_INIT < Orders.ORDER_MAVEN_SETTINGS_LOCALREPOSITORY;
        assert Orders.ORDER_MAVEN_SETTINGS_LOCALREPOSITORY < Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_SECURITY_XML;

        this.handlerMap.get("afterInit")
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

        assert Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_FILES < Orders.EVENT_AWARE_ORDER_EXTRA_FILES;
        assert Orders.EVENT_AWARE_ORDER_EXTRA_FILES < Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_SERVERS;

        this.handlerMap.get("onSettingsBuildingRequest")
            .forEach(it -> it.onSettingsBuildingRequest(cliRequest, request, ciOptionContext));
    }

    public void onSettingsBuildingResult(
        final CliRequest cliRequest,
        final SettingsBuildingResult result,
        final CiOptionContext ciOptionContext
    ) {
        // set settings.localRepository (if present) into effectiveSettings
        assert Orders.ORDER_CI_OPTION_INIT < Orders.ORDER_MAVEN_SETTINGS_LOCALREPOSITORY;

        this.handlerMap.get("onSettingsBuildingResult")
            .forEach(it -> it.onSettingsBuildingResult(cliRequest, result, ciOptionContext));
    }

    public void onToolchainsBuildingRequest(
        final CliRequest cliRequest,
        final ToolchainsBuildingRequest request,
        final CiOptionContext ciOptionContext
    ) {
        assert Orders.ORDER_CI_OPTION_INIT < Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_FILES;

        this.handlerMap.get("onToolchainsBuildingRequest")
            .forEach(it -> it.onToolchainsBuildingRequest(cliRequest, request, ciOptionContext));
    }

    public void onToolchainsBuildingResult(
        final CliRequest cliRequest,
        final ToolchainsBuildingResult result,
        final CiOptionContext ciOptionContext
    ) {
        this.handlerMap.get("onToolchainsBuildingResult")
            .forEach(it -> it.onToolchainsBuildingResult(cliRequest, result, ciOptionContext));
    }

    public void onMavenExecutionRequest(
        final CliRequest cliRequest,
        final MavenExecutionRequest request,
        final CiOptionContext ciOptionContext
    ) {
        assert Orders.ORDER_CI_OPTION_INIT < Orders.ORDER_MAVEN_SETTINGS_LOCALREPOSITORY;
        assert Orders.ORDER_MAVEN_SETTINGS_LOCALREPOSITORY < Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_SERVERS;

        this.handlerMap.get("onMavenExecutionRequest")
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

        this.handlerMap.get("onProjectBuildingRequest")
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

        this.handlerMap.get("onInit")
            .forEach(it -> it.onInit(context));

        this.ciOptContext = this.ciOptContextFactory.getObject();
    }

    @Override
    public void process(final CliRequest cliRequest) throws Exception {
        this.cliRequest = cliRequest;

        logger.info(logStart(this, "afterInit"));
        this.afterInit(cliRequest, this.ciOptContext);
        logger.info(logEnd(this, "afterInit", Void.TYPE));
    }

    /**
     * {@link OrderedConfigurationProcessor#getOrder()}.
     *
     * @return order
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
