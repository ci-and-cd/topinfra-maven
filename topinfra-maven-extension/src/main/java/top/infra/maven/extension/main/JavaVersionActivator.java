package top.infra.maven.extension.main;

import static java.lang.Integer.parseInt;
import static top.infra.maven.utils.MavenUtils.profileId;
import static top.infra.maven.utils.MavenUtils.projectName;
import static top.infra.maven.utils.PropertiesUtils.mapFromProperties;
import static top.infra.maven.utils.SystemUtils.parseJavaVersion;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.profile.ProfileActivationContext;

import top.infra.maven.extension.activator.AbstractCustomActivator;
import top.infra.maven.extension.activator.model.ProjectBuilderActivatorModelResolver;

// @Component(role = CustomActivator.class, hint = "JavaVersionActivator")
@Named
@Singleton
public class JavaVersionActivator extends AbstractCustomActivator {

    private static final Pattern PATTERN_JAVA_PROFILE = Pattern.compile(".*java[-]?(\\d+)[-]?.*");

    @Inject
    public JavaVersionActivator(
        final org.codehaus.plexus.logging.Logger logger,
        final ProjectBuilderActivatorModelResolver resolver
    ) {
        super(logger, resolver);
    }

    static boolean isJavaVersionRelatedProfile(final String id) {
        return PATTERN_JAVA_PROFILE.matcher(id).matches();
    }

    @Override
    protected boolean cacheResult() {
        return true;
    }

    @Override
    protected String getName() {
        return "JavaVersionActivator";
    }

    @Override
    protected boolean isActive(
        final Model model,
        final Profile profile,
        final ProfileActivationContext context,
        final ModelProblemCollector problems
    ) {
        final boolean result;
        if (this.supported(profile)) {
            final Optional<Integer> profileJavaVersion = profileJavaVersion(profile.getId());

            // if (logger.isInfoEnabled()) {
            //     logger.info(String.format("%s project='%s' profile='%s' is java version related profile (java %s)",
            //         this.getName(), projectName(context), profileId(profile), profileJavaVersion.orElse(null)));
            // }

            final Map<String, Object> projectContext = projectContext(model, context);

            Optional<Integer> javaVersionForce;
            try {
                javaVersionForce = Optional.of(parseInt(String.format("%s", projectContext.get("javaVersionForce"))));
            } catch (final Exception ex) {
                javaVersionForce = Optional.empty();
            }

            final boolean javaVersionActive;
            if (javaVersionForce.isPresent()) {
                javaVersionActive = javaVersionForce.get().equals(profileJavaVersion.orElse(null));
            } else {
                // final String javaDotVersion = String.format("%s", projectContext.get("java.version"));
                final String javaDotVersion = context.getSystemProperties().get("java.version");
                final Optional<Integer> projectJavaVersion = parseJavaVersion(javaDotVersion);
                javaVersionActive = projectJavaVersion
                    .map(integer -> integer.equals(profileJavaVersion.orElse(null))).orElse(false);
            }

            result = javaVersionActive;
        } else {
            result = false;

            if (logger.isDebugEnabled() || profile.getId().contains("java")) {
                logger.info(String.format("%s project='%s' profile='%s' is not java version related profile",
                    this.getName(), projectName(context), profileId(profile)));
            }
        }

        return result;
    }

    @Override
    public boolean supported(final Profile profile) {
        return profileJavaVersion(profile.getId()).isPresent();
    }

    static Optional<Integer> profileJavaVersion(final String id) {
        final Optional<Integer> result;

        final Matcher matcher = PATTERN_JAVA_PROFILE.matcher(id);
        if (matcher.matches()) {
            result = Optional.of(parseInt(matcher.group(1)));
        } else {
            result = Optional.empty();
        }

        return result;
    }

    /**
     * Provide script execution context variables.
     */
    private static Map<String, Object> projectContext(
        final Model project,
        final ProfileActivationContext context
    ) {
        // Note: keep order.
        final Map<String, Object> bindings = new LinkedHashMap<>();
        // Inject project props: override defaults
        bindings.putAll(context.getProjectProperties());

        bindings.putAll(mapFromProperties(project.getProperties()));
        // Inject system props, override previous.
        bindings.putAll(context.getSystemProperties());
        // Inject user props, override previous.
        bindings.putAll(context.getUserProperties());
        // Expose default variable context.
        bindings.put("value", bindings);
        // Expose resolved pom.xml model.
        bindings.put("project", project);
        return bindings;
    }
}
