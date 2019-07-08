package top.infra.maven;

import static java.lang.Boolean.FALSE;

import java.util.Optional;
import java.util.Properties;

import top.infra.maven.extension.shared.CiOptions;
import top.infra.maven.utils.MavenUtils;

public interface CiOption {

    String name();

    Optional<String> getDefaultValue();

    default String getEnvVariableName() {
        return CiOptions.envVariableName(this.getPropertyName());
    }

    String getPropertyName();

    default String getSystemPropertyName() {
        return CiOptions.systemPropertyName(this.getPropertyName());
    }

    /**
     * Get value.
     *
     * @param context context
     * @return Optional value
     */
    default Optional<String> getValue(final CiOptionContext context) {
        final Optional<String> foundInProperties = this.findInProperties(context.getSystemProperties(), context.getUserProperties());
        final Optional<String> value = foundInProperties.isPresent()
            ? foundInProperties
            : this.calculateValue(context);
        return value.isPresent() ? value : this.getDefaultValue();
    }

    /**
     * Calculate value.
     *
     * @param context context
     * @return Optional value
     */
    default Optional<String> calculateValue(final CiOptionContext context) {
        return Optional.empty();
    }

    default Optional<String> findInProperties(
        final Properties systemProperties,
        final Properties userProperties
    ) {
        return MavenUtils.findInProperties(this.getPropertyName(), systemProperties, userProperties);
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

        final Optional<String> foundInProperties = this.findInProperties(context.getSystemProperties(), context.getUserProperties());
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
}
