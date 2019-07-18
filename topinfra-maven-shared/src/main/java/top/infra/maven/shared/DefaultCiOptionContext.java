package top.infra.maven.shared;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

import top.infra.maven.CiOption;
import top.infra.maven.CiOptionContext;
import top.infra.maven.shared.utils.PropertiesUtils;

public class DefaultCiOptionContext implements CiOptionContext {

    private final Properties systemProperties;

    private final Properties userProperties;

    public DefaultCiOptionContext(
        final Properties systemProperties,
        final Properties userProperties
    ) {
        this.systemProperties = systemProperties;
        this.userProperties = userProperties;
    }

    @Override
    public Properties getSystemProperties() {
        return this.systemProperties;
    }

    @Override
    public Properties getUserProperties() {
        return this.userProperties;
    }

    @Override
    public Properties setCiOptPropertiesInto(
        final Collection<List<CiOption>> optionGroups,
        final Properties... targetProperties
    ) {
        final Properties properties = new Properties();

        optionGroups
            .stream()
            .flatMap(group -> group.stream().sorted())
            .forEach(ciOption -> {
                if (!ciOption.name().equals(ciOption.name(ciOption.getPropertyName()))) {
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
}
