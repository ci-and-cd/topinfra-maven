package top.infra.maven.core;

import static java.lang.Boolean.FALSE;

import java.util.Optional;
import java.util.Properties;

public interface CiOption {

    String name();

    Optional<String> getDefaultValue();

    String getEnvVariableName();

    String getPropertyName();

    String getSystemPropertyName();

    /**
     * Get value.
     *
     * @param gitProperties    gitProperties
     * @param systemProperties systemProperties
     * @param userProperties   userProperties
     * @return Optional value
     */
    default Optional<String> getValue(
        final GitProperties gitProperties,
        final Properties systemProperties,
        final Properties userProperties
    ) {
        final Optional<String> foundInProperties = this.findInProperties(systemProperties, userProperties);
        final Optional<String> value = foundInProperties.isPresent()
            ? foundInProperties
            : this.calculateValue(gitProperties, systemProperties, userProperties);
        return value.isPresent() ? value : this.getDefaultValue();
    }

    /**
     * Calculate value.
     *
     * @param gitProperties    gitProperties
     * @param systemProperties systemProperties
     * @param userProperties   userProperties
     * @return Optional value
     */
    default Optional<String> calculateValue(
        final GitProperties gitProperties,
        final Properties systemProperties,
        final Properties userProperties
    ) {
        return Optional.empty();
    }

    default Optional<String> findInProperties(
        final Properties systemProperties,
        final Properties userProperties
    ) {
        // systemProperty first
        final Optional<String> systemProperty = Optional.ofNullable(systemProperties.getProperty(this.getSystemPropertyName()));
        return systemProperty.isPresent()
            ? systemProperty
            : Optional.ofNullable(userProperties.getProperty(this.getPropertyName()));

        // // userProperty first
        // final Optional<String> userProperty = Optional.ofNullable(userProperties.getProperty(this.getPropertyName()));
        // return userProperty.isPresent()
        //     ? userProperty
        //     : Optional.ofNullable(systemProperties.getProperty(this.getSystemPropertyName()));
    }

    /**
     * Set value into properties, use defaultValue if value absent.
     *
     * @param gitProperties    gitProperties
     * @param systemProperties systemProperties
     * @param userProperties   userProperties
     * @param properties       properties to set key/value in
     * @return Optional value
     */
    default Optional<String> setProperties(
        final GitProperties gitProperties,
        final Properties systemProperties,
        final Properties userProperties,
        final Properties properties
    ) {
        final Optional<String> result;

        final Optional<String> foundInProperties = this.findInProperties(systemProperties, userProperties);
        if (foundInProperties.isPresent()) { // found in properties
            final Optional<String> got = this.getValue(gitProperties, systemProperties, userProperties);
            if (got.map(value -> value.equals(foundInProperties.get())).orElse(FALSE)) {
                properties.setProperty(this.getPropertyName(), foundInProperties.get());
            } else { // getValue is overridden by custom CiOption impl (got present and not equals to value found in properties).
                // final boolean gotDefaultValue = got.map(value -> value.equals(defaultVal)).orElse(FALSE);
                got.ifPresent(value -> properties.setProperty(this.getPropertyName(), value));
            }

            result = foundInProperties;
        } else { // not found in properties
            final Optional<String> calculated = this.calculateValue(gitProperties, systemProperties, userProperties);
            final String propertyValue = calculated.orElseGet(() -> this.getDefaultValue().orElse(null));
            if (propertyValue != null) {
                properties.setProperty(this.getPropertyName(), propertyValue);
            }

            result = Optional.ofNullable(propertyValue);
        }

        return result;
    }
}
