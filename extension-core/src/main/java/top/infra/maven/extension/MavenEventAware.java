package top.infra.maven.extension;

import java.util.Comparator;

import org.apache.maven.eventspy.EventSpy.Context;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.apache.maven.toolchain.building.ToolchainsBuildingRequest;
import org.apache.maven.toolchain.building.ToolchainsBuildingResult;
import org.jetbrains.annotations.NotNull;

import top.infra.maven.core.CiOptions;

public interface MavenEventAware extends Comparable<MavenEventAware> {

    Comparator<MavenEventAware> MAVEN_EVENT_AWARE_COMPARATOR = (o1, o2) -> {
        final int result;
        if (o1 != null && o2 != null) {
            result = Integer.compare(o1.getOrder(), o2.getOrder());
        } else if (o1 != null) {
            result = -1;
        } else if (o2 != null) {
            result = 1;
        } else {
            result = 0;
        }
        return result;
    };

    @Override
    default int compareTo(@NotNull final MavenEventAware o2) {
        return MAVEN_EVENT_AWARE_COMPARATOR.compare(this, o2);
    }

    /**
     * Smaller one is called more earlier.
     *
     * @return order (default method returns last / biggest value)
     */
    default int getOrder() {
        return Integer.MAX_VALUE;
    }

    default void onInit(final Context context) {
        // no-op
    }

    default void afterInit(final Context context, final CiOptions ciOpts) {
        // no-op
    }

    /**
     * First event on maven execution.
     *
     * @param request SettingsBuildingRequest
     * @param ciOpts  ciOpts
     */
    default void onSettingsBuildingRequest(final SettingsBuildingRequest request, final CiOptions ciOpts) {
        // no-op
    }

    default void onSettingsBuildingResult(final SettingsBuildingResult result, final CiOptions ciOpts) {
        // no-op
    }

    /**
     * After SettingsBuildingRequest.
     *
     * @param request ToolchainsBuildingRequest
     * @param ciOpts  ciOpts
     */
    default void onToolchainsBuildingRequest(final ToolchainsBuildingRequest request, final CiOptions ciOpts) {
        // no-op
    }

    default void onToolchainsBuildingResult(final ToolchainsBuildingResult result, final CiOptions ciOpts) {
        // no-op
    }

    /**
     * After ToolchainsBuildingRequest.
     *
     * @param request MavenExecutionRequest
     * @param ciOpts  ciOpts
     */
    default void onMavenExecutionRequest(final MavenExecutionRequest request, final CiOptions ciOpts) {
        // no-op
    }

    /**
     * After (nested in) MavenExecutionRequest.
     *
     * @param mavenExecution  MavenExecutionRequest
     * @param projectBuilding ProjectBuildingRequest
     * @param ciOpts          ciOpts
     */
    default void onProjectBuildingRequest(
        final MavenExecutionRequest mavenExecution,
        final ProjectBuildingRequest projectBuilding,
        final CiOptions ciOpts
    ) {
        // no-op
    }
}
