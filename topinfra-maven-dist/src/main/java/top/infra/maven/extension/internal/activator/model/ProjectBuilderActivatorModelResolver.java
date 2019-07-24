package top.infra.maven.extension.internal.activator.model;

import java.io.File;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.repository.LegacyLocalRepositoryManager;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelCache;
import org.apache.maven.model.profile.ProfileActivationContext;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectModelResolver;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;

import top.infra.maven.shared.extension.activator.model.AbstractActivatorModelResolver;

@Named
@Singleton
public class ProjectBuilderActivatorModelResolver extends AbstractActivatorModelResolver {

    private final ModelCache modelCache;

    private final RemoteRepositoryManager repositoryManager;

    private final org.eclipse.aether.RepositorySystem repositorySystem;

    private ProjectBuildingRequest projectBuildingRequest;

    @Inject
    public ProjectBuilderActivatorModelResolver(
        final org.codehaus.plexus.logging.Logger logger,
        final ModelBuilder modelBuilder,
        final RemoteRepositoryManager repositoryManager,
        final org.eclipse.aether.RepositorySystem repositorySystem
    ) {
        super(logger, modelBuilder);

        this.modelCache = new ReactorModelCache();

        this.repositoryManager = repositoryManager;
        this.repositorySystem = repositorySystem;
    }

    @Override
    protected ModelBuildingRequest modelBuildingRequest(final ProfileActivationContext context, final File pomFile) {
        final RepositorySystemSession session = LegacyLocalRepositoryManager.overlay(
            this.projectBuildingRequest.getLocalRepository(), this.projectBuildingRequest.getRepositorySession(), this.repositorySystem);

        final ModelBuildingRequest modelBuildingRequest = new DefaultModelBuildingRequest();

        final RequestTrace trace = RequestTrace.newChild(null, this.projectBuildingRequest).newChild(modelBuildingRequest);
        final List<RemoteRepository> repositories = RepositoryUtils.toRepos(this.projectBuildingRequest.getRemoteRepositories());
        final ModelResolver modelResolver = new ProjectModelResolver(session, trace, this.repositorySystem,
            this.repositoryManager, repositories,
            this.projectBuildingRequest.getRepositoryMerging(),
            null);

        // modelBuildingRequest.setActiveProfileIds(this.projectBuildingRequest.getActiveProfileIds());
        modelBuildingRequest.setBuildStartTime(this.projectBuildingRequest.getBuildStartTime());
        // modelBuildingRequest.setInactiveProfileIds(this.projectBuildingRequest.getInactiveProfileIds());
        modelBuildingRequest.setLocationTracking(false);
        modelBuildingRequest.setModelResolver(modelResolver);
        modelBuildingRequest.setPomFile(pomFile);
        // modelBuildingRequest.setProcessPlugins(this.projectBuildingRequest.isProcessPlugins());
        // modelBuildingRequest.setProfiles(this.projectBuildingRequest.getProfiles());
        modelBuildingRequest.setSystemProperties(this.projectBuildingRequest.getSystemProperties());
        modelBuildingRequest.setUserProperties(this.projectBuildingRequest.getUserProperties());
        modelBuildingRequest.setValidationLevel(this.projectBuildingRequest.getValidationLevel());

        // java.lang.IllegalAccessError: tried to access class org.apache.maven.project.ReactorModelCache from class
        modelBuildingRequest.setModelCache(this.modelCache);

        return modelBuildingRequest;
    }

    public void setProjectBuildingRequest(final ProjectBuildingRequest projectBuildingRequest) {
        this.projectBuildingRequest = projectBuildingRequest;
    }
}
