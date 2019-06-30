package top.infra.maven.extension.activator;

import static top.infra.maven.utils.MavenUtils.profileId;
import static top.infra.maven.utils.MavenUtils.projectName;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.profile.ProfileActivationContext;

import top.infra.maven.extension.activator.model.ActivatorModelResolver;
import top.infra.maven.extension.activator.model.ProjectBuilderActivatorModelResolver;
import top.infra.maven.logging.Logger;
import top.infra.maven.logging.LoggerPlexusImpl;
import top.infra.maven.utils.MavenUtils;

public abstract class AbstractCustomActivator implements CustomActivator {

    /**
     * Logger provided by Maven runtime.
     */
    // @org.codehaus.plexus.component.annotations.Requirement
    protected final Logger logger;

    protected final ActivatorModelResolver resolver;

    private final Map<String, Boolean> profileMemento;

    protected AbstractCustomActivator(
        final org.codehaus.plexus.logging.Logger logger,
        final ProjectBuilderActivatorModelResolver resolver
    ) {
        this.logger = new LoggerPlexusImpl(logger);
        this.resolver = resolver;

        this.profileMemento = new LinkedHashMap<>();
    }

    @Override
    public boolean isActive(
        final Profile profile,
        final ProfileActivationContext context,
        final ModelProblemCollector problems
    ) {
        try {
            final Boolean result;

            final Boolean found = this.profileMemento.get(profile.toString());

            if (found == null) {
                if (!this.presentInConfig(profile, context, problems)) {
                    result = false;

                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format("%s profile '%s' not presentInConfig", this.getName(), profileId(profile)));
                        logger.debug(String.format("%s project='%s' profile='%s' result='false'",
                            this.getName(), projectName(context), profileId(profile)));
                    }
                } else {
                    // Required project.
                    final Optional<Model> project = this.resolver.resolveModel(profile, context);
                    if (project.isPresent()) {
                        result = this.isActive(project.get(), profile, context, problems);
                    } else {
                        // reportProblem("Failed to resolve model", new Exception("Invalid Project"), profile, context, problems);
                        result = false;
                    }

                    if (this.cacheResult()) {
                        this.profileMemento.put(profile.toString(), result);
                    }

                    if (result || this.cacheResult()) {
                        logger.info(String.format("%s project='%s' profile='%s' result='%s'",
                            this.getName(), projectName(context), profileId(profile), result));
                    } else if (logger.isDebugEnabled()) {
                        logger.debug(String.format("%s project='%s' profile='%s' result='false'",
                            this.getName(), projectName(context), profileId(profile)));
                    }
                }
            } else {
                result = found;
            }

            return result;
        } catch (final Exception ex) {
            logger.error(ex.getMessage(), ex);
            MavenUtils.reportProblem(ex.getMessage(), ex, profile, context, problems);
            throw ex;
        }
    }

    protected boolean cacheResult() {
        return false;
    }

    @Override
    public boolean presentInConfig(
        final Profile profile,
        final ProfileActivationContext context,
        final ModelProblemCollector problems
    ) {
        return this.supported(profile) && this.resolver.resolveModel(profile, context).isPresent();
    }

    protected String getName() {
        return this.getClass().getSimpleName();
    }

    protected abstract boolean isActive(
        Model model,
        Profile profile,
        ProfileActivationContext context,
        ModelProblemCollector problems
    );
}
