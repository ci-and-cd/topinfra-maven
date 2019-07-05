package top.infra.maven;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

import top.infra.maven.extension.shared.CiOptionNames;
import top.infra.maven.utils.PropertiesUtils;

public class CiOptionContext {

    private final Properties systemProperties;

    private final Properties userProperties;

    public CiOptionContext(
        final Properties systemProperties,
        final Properties userProperties
    ) {
        this.systemProperties = systemProperties;
        this.userProperties = userProperties;
    }

    public Properties getSystemProperties() {
        return this.systemProperties;
    }

    public Properties getUserProperties() {
        return this.userProperties;
    }

    public Properties setCiOptPropertiesInto(
        final Collection<List<CiOption>> optionGroups,
        final Properties... targetProperties
    ) {
        final Properties properties = new Properties();

        optionGroups
            .stream()
            .flatMap(group -> group.stream().sorted())
            .forEach(ciOption -> {
                if (!ciOption.name().equals(CiOptionNames.name(ciOption.getPropertyName()))) {
                    throw new IllegalArgumentException(String.format(
                        "invalid property name [%s] for enum name [%s]", ciOption.name(), ciOption.getPropertyName()));
                }

                ciOption.setProperties(this, properties);
            });

        for (final Properties target : targetProperties) {
            PropertiesUtils.merge(properties, target);
        }

        return properties;
    }

    public void updateSystemProperties(final Properties externalProperties) {
        if (externalProperties != null) {
            for (final String name : externalProperties.stringPropertyNames()) {
                final String key = CiOptionNames.systemPropertyName(name);
                final String value = externalProperties.getProperty(name);
                if (value != null && !this.systemProperties.containsKey(key)) {
                    this.systemProperties.setProperty(key, value);
                }
            }
        }
    }
}
