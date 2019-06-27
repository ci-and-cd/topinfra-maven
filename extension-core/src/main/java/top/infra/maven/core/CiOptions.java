package top.infra.maven.core;

import java.util.Optional;
import java.util.Properties;

import top.infra.maven.core.CiOption;
import top.infra.maven.core.CiOptionNames;
import top.infra.maven.core.GitProperties;

public class CiOptions {

    private final GitProperties gitProperties;

    private final Properties systemProperties;

    private final Properties userProperties;

    public CiOptions(
        final GitProperties gitProperties,
        final Properties systemProperties,
        final Properties userProperties
    ) {
        this.gitProperties = gitProperties;
        this.systemProperties = systemProperties;
        this.userProperties = userProperties;
    }

    public Optional<String> getOption(final CiOption ciOption) {
        return ciOption.getValue(this.gitProperties, this.systemProperties, this.userProperties);
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

    public void updateSystemProperties(final Properties properties) {
        for (final String name : properties.stringPropertyNames()) {
            final String key = CiOptionNames.systemPropertyName(name);
            final String value = properties.getProperty(name);
            if (value != null && !this.systemProperties.containsKey(key)) {
                this.systemProperties.setProperty(key, value);
            }
        }
    }
}
