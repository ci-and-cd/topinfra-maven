package top.infra.maven.extension.main;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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

    private boolean printed = false;

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
        this.info();

        final List<Profile> defaultActivated = super.getActiveProfiles(profiles, context, problems);
        final Set<String> customSupported = supportedProfiles(defaultActivated, this.customActivators)
            .stream()
            .map(Profile::toString)
            .collect(Collectors.toSet());

        if (!customSupported.isEmpty()) {
            logger.debug(String.format("profiles default activated: %s", defaultActivated));
            logger.debug(String.format(
                "profiles default activated and custom supported (need to run custom activators against these): %s",
                customSupported));
        }

        final List<Profile> defaultActiveNotSupported = defaultActivated
            .stream()
            .filter(profile -> !customSupported.contains(profile.toString()))
            .collect(toList());

        final List<Profile> customActivate = supportedProfiles(profiles, this.customActivators)
            .stream()
            .filter(profile -> customSupported.contains(profile.toString()) || noAnyCondition(profile))
            .collect(toList());
        final Map<Profile, List<CustomActivator>> profileActivators = customActivate
            .stream()
            .collect(toMap(
                Function.identity(),
                profile -> this.customActivators.stream().filter(activator -> activator.supported(profile)).collect(toList())));

        final Collection<Profile> profilesActivated = new LinkedHashSet<>(defaultActiveNotSupported);

        if (!customActivate.isEmpty()) {
            logger.debug(String.format("profiles customActivate (need to run custom activators against these): %s",
                customActivate));
            profileActivators.forEach((profile, activators) -> logger.debug(String.format(
                "profile [%s], activators: %s",
                profile,
                activators.stream().map(activator -> activator.getClass().getSimpleName()).collect(toList())
            )));

            final List<Profile> customActivated = customActivate
                .stream()
                .filter(profile ->
                    profileActivators.get(profile).stream().allMatch(activator -> activator.isActive(profile, context, problems)))
                .collect(toList());
            profilesActivated.addAll(customActivated);

            if (!customActivated.isEmpty()) {
                logger.debug(String.format("custom activated profiles: %s", customActivated));
            }
        }

        if (!profilesActivated.isEmpty()) {
            logger.debug(String.format("profiles activated: %s", Arrays.toString(profilesActivated.toArray())));
        }

        return new ArrayList<>(profilesActivated);
    }

    private void info() {
        if (!this.printed) {
            this.printed = true;
            logger.info(String.format("MavenBuildProfileSelector [%s]", this));
            IntStream
                .range(0, this.customActivators.size())
                .forEach(idx -> {
                    final CustomActivator it = this.customActivators.get(idx);
                    logger.info(String.format(
                        "activator index: [%s], name: [%s]",
                        String.format("%02d ", idx),
                        it.getClass().getSimpleName()
                    ));
                });
        }
    }

    static boolean noAnyCondition(final Profile profile) {
        final Activation activation = profile.getActivation();
        return activation == null
            || (activation.getFile() == null
            && activation.getJdk() == null
            && activation.getOs() == null
            && activation.getProperty() == null);
    }

    static List<Profile> supportedProfiles(final Collection<Profile> profiles, final Collection<CustomActivator> customActivators) {
        return profiles.stream()
            .filter(profile -> customActivators.stream().anyMatch(activator -> activator.supported(profile)))
            .collect(toList());
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
