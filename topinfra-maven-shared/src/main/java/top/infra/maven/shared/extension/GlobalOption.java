package top.infra.maven.shared.extension;

import java.util.Optional;
import java.util.Properties;
import java.util.function.BiFunction;

import top.infra.maven.CiOption;
import top.infra.maven.CiOptionContext;

/**
 * Global options can be provided by command args only.
 * Global options are handled before options properties files so putting global options in properties files will not work.
 */
public enum GlobalOption implements CiOption {

    FAST("fast"),
    /**
     * Auto detect infrastructure using for this build.<br/>
     * example of gitlab-ci's CI_PROJECT_URL: "https://example.com/gitlab-org/gitlab-ce"<br/>
     * ossrh, private or customized infrastructure name.
     */
    INFRASTRUCTURE("infrastructure"),
    /**
     * Password generated by command 'mvn --encrypt-master-password [text_of_master_password]'.
     * Need to be surrounded by '{' and '}'.
     * see: 'https://maven.apache.org/guides/mini/guide-encryption.html'.
     */
    MASTER_PASSWORD("master.password"),
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
                final String systemPropName = ciOption.systemPropertyName(propName);
                return Optional.ofNullable(context.getUserProperties().getProperty(propName))
                    .orElseGet(() -> context.getSystemProperties().getProperty(systemPropName));
            });
    }

    public static Optional<String> setInfrastructureSpecificValue(
        final CiOption ciOption,
        final BiFunction<CiOptionContext, Properties, Optional<String>> superSetProperties,
        final CiOptionContext context,
        final Properties properties
    ) {
        final Optional<String> result = superSetProperties.apply(context, properties);

        result.ifPresent(value ->
            INFRASTRUCTURE.getValue(context).ifPresent(infra ->
                properties.setProperty(infra + "." + ciOption.getPropertyName(), value))
        );

        return result;
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
