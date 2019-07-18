package top.infra.maven.extension;

import org.apache.maven.cli.CliRequest;
import org.apache.maven.eventspy.EventSpy.Context;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.apache.maven.toolchain.building.ToolchainsBuildingRequest;
import org.apache.maven.toolchain.building.ToolchainsBuildingResult;

import top.infra.maven.CiOptionContext;
import top.infra.maven.Ordered;

public interface MavenEventAware extends Ordered {

    default boolean onInit() {
        return false;
    }

    default void onInit(final Context context) {
        // no-op
    }

    default boolean afterInit() {
        return false;
    }

    default void afterInit(
        final CliRequest cliRequest,
        final CiOptionContext ciOptContext
    ) {
        // no-op
    }

    default boolean onSettingsBuildingRequest() {
        return false;
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

    default boolean onSettingsBuildingResult() {
        return false;
    }

    default void onSettingsBuildingResult(
        final CliRequest cliRequest,
        final SettingsBuildingResult result,
        final CiOptionContext ciOptContext
    ) {
        // no-op
    }

    default boolean onToolchainsBuildingRequest() {
        return false;
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

    default boolean onToolchainsBuildingResult() {
        return false;
    }

    default void onToolchainsBuildingResult(
        final CliRequest cliRequest,
        final ToolchainsBuildingResult result,
        final CiOptionContext ciOptContext
    ) {
        // no-op
    }

    default boolean onMavenExecutionRequest() {
        return false;
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

    default boolean onProjectBuildingRequest() {
        return false;
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
