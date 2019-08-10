package top.infra.maven.extension.main;

import static top.infra.maven.shared.utils.PropertiesUtils.logProperties;
import static top.infra.maven.shared.utils.SupportFunction.logEnd;
import static top.infra.maven.shared.utils.SupportFunction.logStart;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.cli.CliRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.project.ProjectBuildingRequest;

import top.infra.logging.Logger;
import top.infra.maven.CiOptionContext;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.shared.extension.Orders;
import top.infra.maven.shared.extension.VcsProperties;
import top.infra.maven.shared.logging.LoggerPlexusImpl;
import top.infra.maven.shared.utils.PropertiesUtils;

@Named
@Singleton
public class MavenGoalEditorEventAware implements MavenEventAware {

    private Logger logger;

    @Inject
    public MavenGoalEditorEventAware(
        final org.codehaus.plexus.logging.Logger logger
    ) {
        this.logger = new LoggerPlexusImpl(logger);
    }

    @Override
    public int getOrder() {
        return Orders.EVENT_AWARE_ORDER_GOAL_EDITOR;
    }

    @Override
    public boolean onProjectBuildingRequest() {
        return true;
    }

    @Override
    public void onProjectBuildingRequest(
        final CliRequest cliRequest,
        final MavenExecutionRequest mavenExecution,
        final ProjectBuildingRequest projectBuilding,
        final CiOptionContext ciOptContext
    ) {
        final Entry<List<String>, Properties> goalsAndProps = this.editGoals(logger, mavenExecution, ciOptContext);

        if (goalsAndProps.getKey().isEmpty() && !mavenExecution.getGoals().isEmpty()) {
            logger.warn(String.format("    No goal to run, all goals requested (%s) were removed.", mavenExecution.getGoals()));
            // request.setGoals(Collections.singletonList("help:active-profiles"));
            mavenExecution.setGoals(Collections.singletonList("validate"));
        } else {
            mavenExecution.setGoals(goalsAndProps.getKey());
        }
        PropertiesUtils.merge(goalsAndProps.getValue(), mavenExecution.getUserProperties());
        PropertiesUtils.merge(goalsAndProps.getValue(), projectBuilding.getUserProperties());
    }

    private Entry<List<String>, Properties> editGoals(
        final Logger logger,
        final MavenExecutionRequest request,
        final CiOptionContext ciOptContext
    ) {
        logger.info(logStart(this, "editGoals", request.getGoals()));
        VcsProperties.info(logger, ciOptContext);

        final MavenGoalEditor goalEditor = MavenGoalEditor.newMavenGoalEditor(logger, ciOptContext);
        final Entry<List<String>, Properties> goalsAndProps = goalEditor.goalsAndUserProperties(ciOptContext, request.getGoals());

        logProperties(logger, "    goalEditor.userProperties", goalsAndProps.getValue(), null);
        logger.info(logEnd(this, "editGoals", goalsAndProps, request.getGoals()));
        return goalsAndProps;
    }
}
