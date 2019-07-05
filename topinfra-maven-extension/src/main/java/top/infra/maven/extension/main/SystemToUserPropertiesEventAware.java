package top.infra.maven.extension.main;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.cli.CliRequest;

import top.infra.maven.CiOptionContext;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.extension.shared.Orders;
import top.infra.maven.logging.Logger;
import top.infra.maven.logging.LoggerPlexusImpl;
import top.infra.maven.utils.MavenUtils;
import top.infra.maven.utils.PropertiesUtils;
import top.infra.maven.utils.SystemUtils;

/**
 * Move -Dproperty=value in MAVEN_OPTS from systemProperties into userProperties (maven does not do this automatically).
 * We need this to activate profiles depend on these properties correctly.
 */
@Named
@Singleton
public class SystemToUserPropertiesEventAware implements MavenEventAware {

    private static final String ENV_MAVEN_OPTS = "env.MAVEN_OPTS";

    private final Logger logger;

    @Inject
    public SystemToUserPropertiesEventAware(
        final org.codehaus.plexus.logging.Logger logger
    ) {
        this.logger = new LoggerPlexusImpl(logger);
    }

    @Override
    public boolean afterInit() {
        return true;
    }

    @Override
    public void afterInit(
        final CliRequest cliRequest,
        final CiOptionContext ciOptContext
    ) {
        this.systemToUserProperties(
            ciOptContext.getSystemProperties(),
            ciOptContext.getUserProperties()
        );
    }

    @Override
    public int getOrder() {
        return Orders.ORDER_SYSTEM_TO_USER_PROPERTIES;
    }

    private void systemToUserProperties(
        final Properties systemProperties,
        final Properties userProperties
    ) {
        copyOrSetDefaultToUserProps(
            systemProperties,
            userProperties,
            MavenUtils.PROP_MAVEN_MULTIMODULEPROJECTDIRECTORY,
            () -> {
                final String systemUserDir = SystemUtils.systemUserDir();
                logger.warn(String.format(
                    "    Value of system property [%s] not found, use user.dir [%s] instead.",
                    MavenUtils.PROP_MAVEN_MULTIMODULEPROJECTDIRECTORY, systemUserDir
                ));
                return systemUserDir;
            }
        );

        final List<String> propsToCopy = propsToCopy(systemProperties, userProperties);

        logger.info(String.format("    propsToCopy: %s", propsToCopy));

        propsToCopy.forEach(name -> {
            final String value = systemProperties.getProperty(name);
            logger.info(PropertiesUtils.maskSecrets(String.format(
                "    Copy from systemProperties into userProperties [%s=%s]",
                name, value)
            ));
            userProperties.setProperty(name, value);
        });
    }

    private static List<String> propsToCopy(
        final Properties systemProperties,
        final Properties userProperties
    ) {
        final Optional<String> mavenOptsOptional = Optional.ofNullable(systemProperties.getProperty(ENV_MAVEN_OPTS));
        return mavenOptsOptional
            .map(mavenOpts -> systemProperties.stringPropertyNames()
                .stream()
                .filter(name -> !ENV_MAVEN_OPTS.equals(name))
                .filter(name -> mavenOpts.startsWith(String.format("-D%s ", name))
                    || mavenOpts.startsWith(String.format("-D%s=", name))
                    || mavenOpts.contains(String.format(" -D%s ", name))
                    || mavenOpts.contains(String.format(" -D%s=", name))
                    || mavenOpts.endsWith(String.format(" -D%s", name))
                    || mavenOpts.equals(String.format("-D%s", name)))
                .filter(name -> !userProperties.containsKey(name))
                .collect(Collectors.toList()))
            .orElse(Collections.emptyList());
    }

    public static String copyOrSetDefaultToUserProps(
        final Properties systemProperties,
        final Properties userProperties,
        final String name,
        final Supplier<String> defaultValue
    ) {
        final String result;

        final String foundInUserProperties = userProperties.getProperty(name);
        if (foundInUserProperties == null) {
            final String foundInSystemProperties = systemProperties.getProperty(name);
            if (foundInSystemProperties == null) {
                if (defaultValue != null) {
                    result = defaultValue.get();
                    userProperties.setProperty(name, result);
                } else {
                    result = null;
                }
            } else {
                result = foundInSystemProperties;
                userProperties.setProperty(name, result);
            }
        } else {
            result = foundInUserProperties;
        }

        return result;
    }
}
