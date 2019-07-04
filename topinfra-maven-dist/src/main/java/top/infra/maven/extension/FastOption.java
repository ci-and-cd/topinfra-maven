package top.infra.maven.extension;

import java.util.Optional;

import top.infra.maven.core.CiOption;

public enum FastOption implements CiOption {

    FAST("fast");

    private final String defaultValue;
    private final String propertyName;

    FastOption(final String propertyName) {
        this(propertyName, null);
    }

    FastOption(final String propertyName, final String defaultValue) {
        this.defaultValue = defaultValue;
        this.propertyName = propertyName;
    }

    @Override
    public Optional<String> getDefaultValue() {
        return Optional.ofNullable(this.defaultValue);
    }

    @Override
    public String getPropertyName() {
        return this.propertyName;
    }
}
