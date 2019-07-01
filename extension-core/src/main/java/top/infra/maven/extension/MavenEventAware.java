package top.infra.maven.extension;

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

    default void afterInit(final Context context, final CiOptionContext ciOptContext) {
        // no-op
    }

    /**
     * First event on maven execution.
     *
     * @param request SettingsBuildingRequest
     * @param ciOptContext  ciOptContext
     */
    default void onSettingsBuildingRequest(final SettingsBuildingRequest request, final CiOptionContext ciOptContext) {
        // no-op
    }

    default void onSettingsBuildingResult(final SettingsBuildingResult result, final CiOptionContext ciOptContext) {
        // no-op
    }

    /**
     * After SettingsBuildingRequest.
     *
     * @param request ToolchainsBuildingRequest
     * @param ciOptContext  ciOptContext
     */
    default void onToolchainsBuildingRequest(final ToolchainsBuildingRequest request, final CiOptionContext ciOptContext) {
        // no-op
    }

    default void onToolchainsBuildingResult(final ToolchainsBuildingResult result, final CiOptionContext ciOptContext) {
        // no-op
    }

    /**
     * After ToolchainsBuildingRequest.
     *
     * @param request MavenExecutionRequest
     * @param ciOptContext  ciOptContext
     */
    default void onMavenExecutionRequest(final MavenExecutionRequest request, final CiOptionContext ciOptContext) {
        // no-op
    }

    /**
     * After (nested in) MavenExecutionRequest.
     *
     * @param mavenExecution  MavenExecutionRequest
     * @param projectBuilding ProjectBuildingRequest
     * @param ciOptContext          ciOptContext
     */
    default void onProjectBuildingRequest(
        final MavenExecutionRequest mavenExecution,
        final ProjectBuildingRequest projectBuilding,
        final CiOptionContext ciOptContext
    ) {
        // no-op
    }
}
