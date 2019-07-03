package top.infra.maven.extension;

import org.apache.maven.cli.CliRequest;
import org.apache.maven.eventspy.EventSpy.Context;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.apache.maven.toolchain.building.ToolchainsBuildingRequest;
import org.apache.maven.toolchain.building.ToolchainsBuildingResult;

import top.infra.maven.core.CiOptionContext;

public interface MavenEventAware extends Ordered {

    default void onInit(final Context context) {
        // no-op
    }

    default void afterInit(
        final CliRequest cliRequest
    ) {
        // no-op
    }

    /**
     * First event on maven execution.
     *
     * @param cliRequest   cliRequest
     * @param request      SettingsBuildingRequest
     * @param ciOptContext ciOptContext
     */
    default void onSettingsBuildingRequest(
        final CliRequest cliRequest,
        final SettingsBuildingRequest request,
        final CiOptionContext ciOptContext
    ) {
        // no-op
    }

    default void onSettingsBuildingResult(
        final CliRequest cliRequest,
        final SettingsBuildingResult result,
        final CiOptionContext ciOptContext
    ) {
        // no-op
    }

    /**
     * After SettingsBuildingRequest.
     *
     * @param cliRequest   cliRequest
     * @param request      ToolchainsBuildingRequest
     * @param ciOptContext ciOptContext
     */
    default void onToolchainsBuildingRequest(
        final CliRequest cliRequest,
        final ToolchainsBuildingRequest request,
        final CiOptionContext ciOptContext
    ) {
        // no-op
    }

    default void onToolchainsBuildingResult(
        final CliRequest cliRequest,
        final ToolchainsBuildingResult result,
        final CiOptionContext ciOptContext
    ) {
        // no-op
    }

    /**
     * After ToolchainsBuildingRequest.
     *
     * @param cliRequest   cliRequest
     * @param request      MavenExecutionRequest
     * @param ciOptContext ciOptContext
     */
    default void onMavenExecutionRequest(
        final CliRequest cliRequest,
        final MavenExecutionRequest request,
        final CiOptionContext ciOptContext
    ) {
        // no-op
    }

    /**
     * After (nested in) MavenExecutionRequest.
     *
     * @param cliRequest      cliRequest
     * @param mavenExecution  MavenExecutionRequest
     * @param projectBuilding ProjectBuildingRequest
     * @param ciOptContext    ciOptContext
     */
    default void onProjectBuildingRequest(
        final CliRequest cliRequest,
        final MavenExecutionRequest mavenExecution,
        final ProjectBuildingRequest projectBuilding,
        final CiOptionContext ciOptContext
    ) {
        // no-op
    }
}
