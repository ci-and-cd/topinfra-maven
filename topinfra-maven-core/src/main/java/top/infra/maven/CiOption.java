package top.infra.maven;

import static java.lang.Boolean.FALSE;

import java.util.Optional;
import java.util.Properties;

public interface CiOption {

    /**
     * Calculate value.
     *
     * @param context context
     * @return Optional value
     */
    default Optional<String> calculateValue(final CiOptionContext context) {
        return Optional.empty();
    }

    default String envVariableName(final String propertyName) {
        final String envVarPrefix = this.getEnvVariablePrefix();
        final String name = name(propertyName);
        final String result;
        if (envVarPrefix == null || envVarPrefix.isEmpty()) {
            result = name;
        } else {
            // result = name.startsWith("CI_OPT_") ? name : "CI_OPT_" + name;
            result = name.startsWith(envVarPrefix) ? name : envVarPrefix + name;
        }
        return result;
    }

    default Optional<String> findInProperties(final CiOptionContext context) {
        return this.findInProperties(this.getPropertyName(), context);
    }

    default Optional<String> findInProperties(
        final String propertyName,
        final CiOptionContext ciOptionContext
    ) {
        return this.findInProperties(propertyName, ciOptionContext.getSystemProperties(), ciOptionContext.getUserProperties());
    }

    default Optional<String> findInProperties(
        final String propertyName,
        final Properties systemProperties,
        final Properties userProperties
    ) {
        final String systemPropertyName = this.systemPropertyName(propertyName);

        // systemProperty first
        final Optional<String> systemProperty = Optional.ofNullable(systemProperties.getProperty(systemPropertyName));
        return systemProperty.isPresent()
            ? systemProperty
            : Optional.ofNullable(userProperties.getProperty(propertyName));

        // // userProperty first
        // final Optional<String> userProperty = Optional.ofNullable(userProperties.getProperty(propertyName));
        // return userProperty.isPresent()
        //     ? userProperty
        //     : Optional.ofNullable(systemProperties.getProperty(systemPropertyName));
    }

    Optional<String> getDefaultValue();

    default String getEnvVariableName() {
        return this.envVariableName(this.getPropertyName());
    }

    default String getEnvVariablePrefix() {
        return "CI_OPT_";
    }

    String getPropertyName();

    default String getSystemPropertyName() {
        return this.systemPropertyName(this.getPropertyName());
    }

    /**
     * Get value.
     *
     * @param context context
     * @return Optional value
     */
    default Optional<String> getValue(final CiOptionContext context) {
        final Optional<String> foundInProperties = this.findInProperties(context);
        final Optional<String> value = foundInProperties.isPresent()
            ? foundInProperties
            : this.calculateValue(context);
        return value.isPresent() ? value : this.getDefaultValue();
    }

    String name();

    default String name(final String propertyName) {
        final boolean propertyNameEmpty = propertyName == null || propertyName.isEmpty();
        if (propertyNameEmpty) {
            throw new IllegalArgumentException("propertyName must not empty");
        }
        return propertyName.replaceAll("-", "").replaceAll("\\.", "_").toUpperCase();
    }

    /**
     * Set value into properties, use defaultValue if value absent.
     *
     * @param context    context
     * @param properties properties to set key/value in
     * @return Optional value
     */
    default Optional<String> setProperties(final CiOptionContext context, final Properties properties) {
        final Optional<String> result;

        final Optional<String> foundInProperties = this.findInProperties(context);
        if (foundInProperties.isPresent()) { // found in properties
            final Optional<String> got = this.getValue(context);
            if (got.map(value -> value.equals(foundInProperties.get())).orElse(FALSE)) {
                properties.setProperty(this.getPropertyName(), foundInProperties.get());
            } else { // getValue is overridden by custom CiOption impl (got present and not equals to value found in properties).
                // final boolean gotDefaultValue = got.map(value -> value.equals(defaultVal)).orElse(FALSE);
                got.ifPresent(value -> properties.setProperty(this.getPropertyName(), value));
            }

            result = foundInProperties;
        } else { // not found in properties
            final Optional<String> calculated = this.calculateValue(context);
            final String propertyValue = calculated.orElseGet(() -> this.getDefaultValue().orElse(null));
            if (propertyValue != null) {
                properties.setProperty(this.getPropertyName(), propertyValue);
            }

            result = Optional.ofNullable(propertyValue);
        }

        return result;
    }

    default String systemPropertyName(final String propertyName) {
        return String.format("env.%s", this.envVariableName(propertyName));
    }
}
