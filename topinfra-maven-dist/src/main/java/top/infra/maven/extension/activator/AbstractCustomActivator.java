package top.infra.maven.extension.activator;

import static top.infra.maven.utils.MavenUtils.profileId;
import static top.infra.maven.utils.MavenUtils.projectName;

import java.io.File;
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

            if (!this.presentInConfig(profile, context, problems)) {
                result = false;

                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("    %s profile '%s' not presentInConfig", this.getName(), profileId(profile)));
                    logger.debug(String.format("    %s project='%s' profile='%s' result='false'",
                        this.getName(), projectName(context), profileId(profile)));
                }
            } else {
                // Required project.
                final Optional<Model> project = this.resolver.resolveModel(profile, context);

                if (project.isPresent()) {
                    final String mementoKey = mementoKey(profile, project.get().getPomFile());
                    final Boolean found = this.profileMemento.get(mementoKey);
                    if (found == null) {
                        result = this.isActive(project.get(), profile, context, problems);

                        if (this.cacheResult()) {
                            this.profileMemento.put(mementoKey, result);
                        }

                        if (result || this.cacheResult()) {
                            logger.info(String.format("    %s project='%s' profile='%s' result='%s'",
                                this.getName(), projectName(context), profileId(profile), result));
                        } else if (logger.isDebugEnabled()) {
                            logger.debug(String.format("    %s project='%s' profile='%s' result='false'",
                                this.getName(), projectName(context), profileId(profile)));
                        }
                    } else {
                        result = found;
                    }
                } else {
                    // reportProblem("Failed to resolve model", new Exception("Invalid Project"), profile, context, problems);
                    result = false;
                }
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

    private static String mementoKey(final Profile profile, final File pomFile) {
        // TODO remove duplicate codes
        final String pom = pomFile != null ? pomFile.getPath() : "";
        return String.format("%s@%s", profile, pom);
    }
}
