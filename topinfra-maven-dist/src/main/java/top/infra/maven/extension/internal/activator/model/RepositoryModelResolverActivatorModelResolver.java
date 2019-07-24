package top.infra.maven.extension.internal.activator.model;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.profile.ProfileActivationContext;

import top.infra.maven.shared.extension.activator.model.AbstractActivatorModelResolver;

@Deprecated
public class RepositoryModelResolverActivatorModelResolver extends AbstractActivatorModelResolver {

    private final RepositoryModelResolver modelResolver;

    protected RepositoryModelResolverActivatorModelResolver(
        final org.codehaus.plexus.logging.Logger logger,
        final ModelBuilder modelBuilder,
        final RepositoryModelResolver modelResolver
    ) {
        super(logger, modelBuilder);

        this.modelResolver = modelResolver;
    }

    /**
     * Default model resolution request.
     */
    @Override
    protected ModelBuildingRequest modelBuildingRequest(final ProfileActivationContext context, final File pomFile) {
        final ModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setLocationTracking(false);
        // Got "java.lang.NullPointerException: request.modelResolver cannot be null" on core extension triggered project resolution.
        request.setModelResolver(this.modelResolver.newCopy());
        request.setPomFile(pomFile);
        request.setSystemProperties(propertiesToMap(context.getSystemProperties()));
        request.setUserProperties(propertiesToMap(context.getUserProperties()));
        request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        return request;
    }

    /**
     * Change properties format.
     */
    private static Properties propertiesToMap(final Map<String, String> map) {
        final Properties props = new Properties();
        props.putAll(map);
        return props;
    }
}
