package top.infra.maven.core;

import java.util.List;
import java.util.Properties;

import top.infra.maven.utils.PropertiesUtils;

public class CiOptionContext {

    private final GitProperties gitProperties;

    private final Properties systemProperties;

    private final Properties userProperties;

    public CiOptionContext(
        final GitProperties gitProperties,
        final Properties systemProperties,
        final Properties userProperties
    ) {
        this.gitProperties = gitProperties;
        this.systemProperties = systemProperties;
        this.userProperties = userProperties;
    }

    public GitProperties getGitProperties() {
        return this.gitProperties;
    }

    public Properties getSystemProperties() {
        return this.systemProperties;
    }

    public Properties getUserProperties() {
        return this.userProperties;
    }

    public Properties setCiOptPropertiesInto(
        final List<List<CiOption>> optionCollections,
        final Properties... targetProperties
    ) {
        final Properties properties = new Properties();

        optionCollections
            .stream()
            .flatMap(collection -> collection.stream().sorted())
            .forEach(ciOption -> ciOption.setProperties(this, properties));

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