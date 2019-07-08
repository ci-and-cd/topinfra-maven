package top.infra.maven.extension.shared;

import java.util.Optional;

import top.infra.maven.CiOption;
import top.infra.maven.CiOptionContext;

public enum GlobalOption implements CiOption {

    FAST("fast"),
    /**
     * Auto detect infrastructure using for this build.<br/>
     * example of gitlab-ci's CI_PROJECT_URL: "https://example.com/gitlab-org/gitlab-ce"<br/>
     * ossrh, private or customized infrastructure name.
     */
    INFRASTRUCTURE("infrastructure"),
    ;

    private final String defaultValue;
    private final String propertyName;

    GlobalOption(final String propertyName) {
        this(propertyName, null);
    }

    GlobalOption(final String propertyName, final String defaultValue) {
        this.defaultValue = defaultValue;
        this.propertyName = propertyName;
    }

    public static Optional<String> getInfrastructureSpecificValue(
        final CiOption ciOption,
        final CiOptionContext context
    ) {
        return INFRASTRUCTURE.getValue(context)
            .map(infra -> {
                final String propName = infra + "." + ciOption.getPropertyName();
                final String systemPropName = CiOptions.systemPropertyName(propName);
                return Optional.ofNullable(context.getUserProperties().getProperty(propName))
                    .orElseGet(() -> context.getSystemProperties().getProperty(systemPropName));
            });
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
