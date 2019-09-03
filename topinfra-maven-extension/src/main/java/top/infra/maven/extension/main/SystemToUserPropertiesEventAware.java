package top.infra.maven.extension.main;

import cn.home1.tools.maven.MavenSettingsSecurity;
import org.apache.maven.cli.CliRequest;
import top.infra.logging.Logger;
import top.infra.maven.CiOptionContext;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.shared.extension.Orders;
import top.infra.maven.shared.logging.LoggerPlexusImpl;
import top.infra.maven.shared.utils.MavenUtils;
import top.infra.maven.shared.utils.PropertiesUtils;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static top.infra.maven.shared.extension.GlobalOption.MASTER_PASSWORD;

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
        this.systemToUserProperties(cliRequest, ciOptContext);
    }

    @Override
    public int getOrder() {
        return Orders.ORDER_SYSTEM_TO_USER_PROPERTIES;
    }

    private void systemToUserProperties(
            final CliRequest cliRequest,
            final CiOptionContext ciOptionContext
    ) {
        final Properties systemProperties = ciOptionContext.getSystemProperties();
        final Properties userProperties = ciOptionContext.getUserProperties();

        final Optional<MavenSettingsSecurity> settingsSecurity = MASTER_PASSWORD
                .findInProperties(MASTER_PASSWORD.getPropertyName(), systemProperties, userProperties)
                .map(MavenSettingsSecurity::surroundByBrackets)
                .map(password -> new MavenSettingsSecurity(false, password));
        final Collection<String> decryptedSystemProperties = new HashSet<>();
        if (settingsSecurity.isPresent()) {
            logger.info("    Master password found, decrypt systemProperties.");
            final Collection<Entry<String, String>> decrypted = MavenUtils.decryptProperties(
                    systemProperties,
                    settingsSecurity.orElse(null)
            );
            decrypted.forEach(entry -> {
                logger.info(String.format("    Decrypted systemProperty [%s].", entry.getKey()));
                decryptedSystemProperties.add(entry.getKey());
                systemProperties.put(entry.getKey(), entry.getValue());
            });
        }

        copyOrSetDefaultToUserProps(
                systemProperties,
                userProperties,
                MavenUtils.PROP_MAVEN_MULTIMODULEPROJECTDIRECTORY,
                () -> {
                    final String defaultValue = MavenUtils.executionRootPath(cliRequest).toString();
                    logger.warn(String.format(
                            "    Value of system property [%s] not found, use defaultValue [%s] instead.",
                            MavenUtils.PROP_MAVEN_MULTIMODULEPROJECTDIRECTORY, defaultValue
                    ));
                    return defaultValue;
                }
        );

        final List<String> propsToCopy = propsToCopy(systemProperties, userProperties);

        logger.info(String.format("    propsToCopy: %s", propsToCopy));

        propsToCopy.forEach(name -> {
            final String value = systemProperties.getProperty(name);
            logger.info(PropertiesUtils.maskSecrets(String.format(
                    "    Copy from systemProperties into userProperties [%s=%s]",
                    name, decryptedSystemProperties.contains(name) ? "[secure]" : value)
            ));
            userProperties.setProperty(name, value);
        });

        if (settingsSecurity.isPresent()) {
            logger.info("    Master password found, decrypt userProperties.");
            final Collection<Entry<String, String>> decryptedUserProperties = MavenUtils.decryptProperties(
                    userProperties,
                    settingsSecurity.orElse(null)
            );
            decryptedUserProperties.forEach(entry -> {
                logger.info(String.format("    Decrypted userProperty [%s].", entry.getKey()));
                userProperties.put(entry.getKey(), entry.getValue());
            });
        }
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
