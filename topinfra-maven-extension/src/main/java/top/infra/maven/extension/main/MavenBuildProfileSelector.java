package top.infra.maven.extension.main;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static top.infra.maven.shared.utils.SupportFunction.componentName;
import static top.infra.maven.shared.utils.SupportFunction.newTuple;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.stream.IntStream;

import org.apache.maven.model.Activation;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.profile.DefaultProfileSelector;
import org.apache.maven.model.profile.ProfileActivationContext;
import org.apache.maven.model.profile.ProfileSelector;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import top.infra.maven.extension.activator.CustomActivator;
import top.infra.maven.shared.utils.PropertiesUtils;
import top.infra.maven.shared.utils.SupportFunction;

/**
 * Profile selector which combines profiles activated by custom and default
 * activators. Overrides "default" provider.
 * <p/>
 * Must in extension classpath, not work in core classpath.
 */
@Component(role = ProfileSelector.class, hint = "default")
// @Named
// @Singleton
public class MavenBuildProfileSelector extends DefaultProfileSelector {

    @Requirement
    protected org.codehaus.plexus.logging.Logger logger;

    /**
     * Collect only custom activators.
     * Note: keep field name different from super.
     */
    @Requirement(role = CustomActivator.class)
    protected List<CustomActivator> customActivators;

    private List<CustomActivator> availableActivators;

    // @Inject
    // public MavenBuildProfileSelector(
    //     final org.codehaus.plexus.logging.Logger logger,
    //     final List<CustomActivator> customActivators
    // ) {
    //     this.logger = logger;
    //     this.customActivators = customActivators;
    // }

    /**
     * Profiles activated by custom and default activators.
     */
    @Override
    public List<Profile> getActiveProfiles(
        final Collection<Profile> profiles,
        final ProfileActivationContext context,
        final ModelProblemCollector problems
    ) {
        // logger.info(logStart(this, "getActiveProfiles"));
        final Collection<CustomActivator> availableActivators = this.availableActivators(context);

        final Collection<Profile> defaultActivated = new LinkedHashSet<>(super.getActiveProfiles(profiles, context, problems));

        final Map<Profile, List<CustomActivator>> profileActivators = profiles
            .stream()
            .map(p -> newTuple(p, availableActivators.stream().filter(act -> act.supported(p)).collect(toList())))
            .filter(t -> !t.getValue().isEmpty())
            .collect(toMap(Entry::getKey, Entry::getValue));

        final Set<Profile> defaultActiveSupported = defaultActivated
            .stream()
            .filter(profileActivators::containsKey)
            .collect(toCollection(LinkedHashSet::new));

        if (logger.isDebugEnabled() && !defaultActiveSupported.isEmpty()) {
            logger.debug(String.format(
                "profiles default activated: %s", defaultActivated));
            logger.debug(String.format(
                "profiles default activated and custom supported (need to run custom activators against): %s", defaultActiveSupported));
        }

        final Collection<Profile> profilesActivated = defaultActivated
            .stream()
            .filter(profile -> !defaultActiveSupported.contains(profile))
            .collect(toCollection(LinkedHashSet::new));

        if (!profileActivators.keySet().isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format(
                    "profiles customActivate (need to run custom activators against): %s", profileActivators.keySet().size()));
                profileActivators.forEach((profile, activators) ->
                    logger.debug(String.format(
                        "    profile [%s], activators: %s",
                        profile,
                        activators.stream().map(act -> componentName(act.getClass())).collect(toList())
                    )));
            }

            final List<Profile> customActivated = profileActivators.keySet()
                .stream()
                .filter(profile ->
                    profileActivators
                        .get(profile)
                        .stream()
                        .allMatch(activator -> activator.isActive(profile, context, problems))
                )
                .collect(toList());
            profilesActivated.addAll(
                customActivated
                    .stream()
                    .filter(p -> defaultActivated.contains(p) || noAnyCondition(p))
                    .collect(toList())
            );

            if (logger.isDebugEnabled() && !customActivated.isEmpty()) {
                logger.debug(String.format("    custom activated profiles: %s", customActivated));
            }
        }

        if (logger.isDebugEnabled() && !profilesActivated.isEmpty()) {
            logger.debug(String.format("    profiles activated: %s", profilesActivated));
        }

        // logger.info(logEnd(this, "getActiveProfiles", profilesActivated));
        return new ArrayList<>(profilesActivated);
    }

    private Collection<CustomActivator> availableActivators(final ProfileActivationContext context) {
        if (this.availableActivators == null) {
            final Properties systemProperties = PropertiesUtils.toProperties(context.getSystemProperties());
            final Properties userProperties = PropertiesUtils.toProperties(context.getUserProperties());
            this.availableActivators = this.customActivators
                .stream()
                .filter(it -> {
                    final boolean disabled = SupportFunction.componentDisabled(it.getClass(), systemProperties, userProperties);
                    if (disabled) {
                        logger.info(String.format("    eventAware [%s] disabled", componentName(it.getClass())));
                    }
                    return !disabled;
                })
                .collect(toList());

            IntStream
                .range(0, this.availableActivators.size())
                .forEach(idx -> {
                    final CustomActivator it = this.availableActivators.get(idx);
                    logger.info(String.format(
                        "    activator index: [%s], name: [%s]",
                        String.format("%02d ", idx),
                        componentName(it.getClass())
                    ));
                });
        }

        return this.availableActivators;
    }

    static boolean noAnyCondition(final Profile profile) {
        final Activation activation = profile.getActivation();
        return activation == null
            || (activation.getFile() == null
            && activation.getJdk() == null
            && activation.getOs() == null
            && activation.getProperty() == null);
    }

    protected boolean superIsActive(
        final Profile profile,
        final ProfileActivationContext context,
        final ModelProblemCollector problems
    ) {
        try {
            final Method superIsActive = super.getClass().getDeclaredMethod(
                "isActive", Profile.class, ProfileActivationContext.class, ModelProblemCollector.class);
            superIsActive.setAccessible(true);
            return (boolean) superIsActive.invoke(this, profile, context, problems);
        } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            return false;
        }
    }
}
