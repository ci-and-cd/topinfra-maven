package top.infra.maven.extension.infra;

import static java.lang.Boolean.FALSE;
import static top.infra.maven.shared.utils.MavenUtils.profileId;
import static top.infra.maven.shared.utils.MavenUtils.projectName;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.eventspy.EventSpy.Context;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.profile.ProfileActivationContext;
import org.apache.maven.model.profile.activation.ProfileActivator;

import top.infra.maven.CiOptionContext;
import top.infra.maven.extension.CiOptionContextFactory;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.shared.extension.activator.AbstractCustomActivator;
import top.infra.maven.extension.activator.CustomActivator;
import top.infra.maven.extension.activator.model.ActivatorModelResolver;
import top.infra.maven.shared.extension.GlobalOption;
import top.infra.maven.shared.extension.Orders;

// @Component(role = CustomActivator.class, hint = "InfrastructureActivator") // This instance has multiple roles
@Named
@Singleton
public class InfrastructureActivator extends AbstractCustomActivator implements MavenEventAware {

    private static final Pattern PATTERN_INFRASTRUCTURE_PROFILE = Pattern.compile(".*infrastructure_(\\w+)[-]?.*");

    private final CiOptionContextFactory ciOptContextFactory;

    private CiOptionContext ciOptContext;

    @Inject
    public InfrastructureActivator(
        final org.codehaus.plexus.logging.Logger logger,
        final ActivatorModelResolver resolver,
        final CiOptionContextFactory ciOptContextFactory
    ) {
        super(logger, resolver);

        this.ciOptContextFactory = ciOptContextFactory;
    }

    /**
     * Method used by {@link AbstractCustomActivator#isActive(Profile, ProfileActivationContext, ModelProblemCollector)}.
     *
     * @return whether this activator caches the result for profiles
     */
    @Override
    protected boolean cacheResult() {
        return true;
    }

    /**
     * A {@link AbstractCustomActivator} method.
     * <p/>
     * Returns {@link Class#getSimpleName()} by default.
     *
     * @return activator name in log
     */
    @Override
    protected String getName() {
        return "InfrastructureActivator";
    }

    /**
     * A abstract method declared in {@link AbstractCustomActivator}.
     * <p/>
     * Used by method {@link AbstractCustomActivator#isActive(Profile, ProfileActivationContext, ModelProblemCollector)}
     * which implements {@link ProfileActivator#isActive(Profile, ProfileActivationContext, ModelProblemCollector)}.
     *
     * @param model    model
     * @param profile  profile
     * @param context  context
     * @param problems problems
     * @return is profile active
     */
    @Override
    protected boolean isActive(
        final Model model,
        final Profile profile,
        final ProfileActivationContext context,
        final ModelProblemCollector problems
    ) {
        final boolean result;
        if (this.supported(profile)) {
            final Optional<String> profileInfrastructure = profileInfrastructure(profile.getId());

            // if (logger.isInfoEnabled()) {
            //     logger.info(String.format("%s project='%s' profile='%s' is infrastructure related profile (infrastructure %s)",
            //         this.getName(), projectName(context), profileId(profile), profileInfrastructure.orElse(null)));
            // }

            final Optional<String> infrastructure = GlobalOption.INFRASTRUCTURE.getValue(this.ciOptContext);

            result = infrastructure
                .map(value -> value.equals(profileInfrastructure.orElse(null)))
                .orElse(FALSE);
        } else {
            result = false;

            if (logger.isDebugEnabled() || profile.getId().contains("infrastructure")) {
                logger.info(String.format("%s project='%s' profile='%s' is not infrastructure related profile",
                    this.getName(), projectName(context), profileId(profile)));
            }
        }

        return result;
    }

    /**
     * A {@link CustomActivator} method used by {@link AbstractCustomActivator}.
     *
     * @param profile profile
     * @return is profile supported
     */
    @Override
    public boolean supported(final Profile profile) {
        return profileInfrastructure(profile.getId()).isPresent();
    }

    static Optional<String> profileInfrastructure(final String id) {
        final Optional<String> result;

        final Matcher matcher = PATTERN_INFRASTRUCTURE_PROFILE.matcher(id);
        if (matcher.matches()) {
            result = Optional.of(matcher.group(1));
        } else {
            result = Optional.empty();
        }

        return result;
    }

    @Override
    public boolean onInit() {
        return true;
    }

    /**
     * A {@link MavenEventAware} method.
     *
     * @param context context of current maven session.
     */
    @Override
    public void onInit(final Context context) {
        this.ciOptContext = this.ciOptContextFactory.getObject();
    }

    /**
     * A {@link MavenEventAware} method.
     *
     * @return order of this {@link MavenEventAware} instance.
     */
    @Override
    public int getOrder() {
        return Orders.ORDER_INFRASTRUCTURE_ACTIVATOR;
    }
}
