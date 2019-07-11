package top.infra.maven.extension.shared;

import static java.lang.Boolean.FALSE;
import static top.infra.maven.extension.shared.GlobalOption.FAST;
import static top.infra.maven.extension.shared.MavenProjectInfo.newProjectInfoByBuildProject;
import static top.infra.maven.extension.shared.MavenProjectInfo.newProjectInfoByReadPom;
import static top.infra.maven.utils.SupportFunction.logEnd;
import static top.infra.maven.utils.SupportFunction.logStart;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.cli.CliRequest;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.internal.aether.DefaultRepositorySystemSessionFactory;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.eclipse.aether.RepositorySystemSession;

import top.infra.maven.CiOptionContext;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.logging.Logger;
import top.infra.maven.logging.LoggerPlexusImpl;

@Named
@Singleton
public class MavenProjectInfoEventAware implements MavenEventAware {

    private final Logger logger;

    // @org.codehaus.plexus.component.annotations.Requirement
    private final ProjectBuilder projectBuilder;

    private final DefaultRepositorySystemSessionFactory repositorySessionFactory;

    private MavenExecutionRequest mavenExecutionCopied;

    private MavenProjectInfo rootProjectInfo;

    @Inject
    public MavenProjectInfoEventAware(
        final org.codehaus.plexus.logging.Logger logger,
        final ProjectBuilder projectBuilder,
        final DefaultRepositorySystemSessionFactory repositorySessionFactory
    ) {
        this.logger = new LoggerPlexusImpl(logger);
        this.projectBuilder = projectBuilder;
        this.repositorySessionFactory = repositorySessionFactory;
    }

    @Override
    public int getOrder() {
        return Orders.EVENT_AWARE_ORDER_MAVEN_PROJECT_INFO;
    }

    public MavenProjectInfo getRootProjectInfo() {
        if (this.rootProjectInfo == null && this.mavenExecutionCopied != null) { // Lazy init
            this.rootProjectInfo = this.resolveRootProject();
        }

        return this.rootProjectInfo;
    }

    private MavenProjectInfo resolveRootProject() {
        logger.info(logStart(this, "resolve"));

        final MavenProjectInfo projectInfo = this.resolve(this.mavenExecutionCopied.getPom());

        logger.info(logEnd(this, "resolve", projectInfo));
        return projectInfo;
    }

    public MavenProjectInfo resolve(final File pomFile) {
        final boolean repositorySystemSessionNull = this.createRepositorySystemSessionIfAbsent(this.mavenExecutionCopied);
        final ProjectBuildingRequest projectBuildingRequest = this.mavenExecutionCopied.getProjectBuildingRequest();

        try {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("    getMavenProjectInfo pomFile: [%s]", pomFile));
            }
            return newProjectInfoByReadPom(logger, pomFile)
                .orElseGet(() -> newProjectInfoByBuildProject(logger, this.projectBuilder, pomFile, projectBuildingRequest));
        } finally {
            if (repositorySystemSessionNull) {
                projectBuildingRequest.setRepositorySession(null);
            }
        }
    }

    /**
     * Create repositorySystemSession if absent.
     *
     * @param request mavenExecutionRequest
     * @return repositorySystemSessionNull
     */
    private boolean createRepositorySystemSessionIfAbsent(final MavenExecutionRequest request) {
        /*
        RepositorySystemSession may be null, e.g. maven 3.6.0's MavenExecutionRequest.projectBuildingRequest.repositorySession
        java.lang.NullPointerException
        at org.apache.maven.RepositoryUtils.getWorkspace(RepositoryUtils.java:375)
        at org.apache.maven.plugin.DefaultPluginArtifactsCache$CacheKey.<init>(DefaultPluginArtifactsCache.java:70)
        at org.apache.maven.plugin.DefaultPluginArtifactsCache.createKey(DefaultPluginArtifactsCache.java:135)
        at org.apache.maven.plugin.internal.DefaultMavenPluginManager.setupExtensionsRealm(DefaultMavenPluginManager.java:824)
        at org.apache.maven.project.DefaultProjectBuildingHelper.createProjectRealm(DefaultProjectBuildingHelper.java:197)
        at org.apache.maven.project.DefaultModelBuildingListener.buildExtensionsAssembled(DefaultModelBuildingListener.java:101)
        at org.apache.maven.model.building.ModelBuildingEventCatapult$1.fire(ModelBuildingEventCatapult.java:44)
        at org.apache.maven.model.building.DefaultModelBuilder.fireEvent(DefaultModelBuilder.java:1360)
        at org.apache.maven.model.building.DefaultModelBuilder.build(DefaultModelBuilder.java:452)
        at org.apache.maven.model.building.DefaultModelBuilder.build(DefaultModelBuilder.java:432)
        at org.apache.maven.project.DefaultProjectBuilder.build(DefaultProjectBuilder.java:616)
        at org.apache.maven.project.DefaultProjectBuilder.build(DefaultProjectBuilder.java:385)
        ...
         */
        final ProjectBuildingRequest projectBuildingRequest = request.getProjectBuildingRequest();
        final boolean repositorySystemSessionNull;
        if (projectBuildingRequest != null) {
            repositorySystemSessionNull = projectBuildingRequest.getRepositorySession() == null;
            if (repositorySystemSessionNull) {
                if (logger.isInfoEnabled()) {
                    logger.info(String.format("    repositorySystemSession not found in %s", projectBuildingRequest));
                }
                final RepositorySystemSession repositorySystemSession = this.repositorySessionFactory.newRepositorySession(request);
                projectBuildingRequest.setRepositorySession(repositorySystemSession);
            }
        } else {
            repositorySystemSessionNull = true;
        }
        return repositorySystemSessionNull;
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
        this.mavenExecutionCopied = DefaultMavenExecutionRequest.copy(mavenExecution);

        if (!FAST.getValue(ciOptContext).map(Boolean::parseBoolean).orElse(FALSE)) {
            this.rootProjectInfo = this.resolveRootProject();
        } else {
            // Lazy mode
            logger.info("    Lazy resolving project version under fast mode.");
        }
    }
}
