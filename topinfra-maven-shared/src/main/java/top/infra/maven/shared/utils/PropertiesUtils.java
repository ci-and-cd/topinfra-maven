package top.infra.maven.shared.utils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import top.infra.logging.Logger;
import top.infra.maven.shared.extension.CiOptions;

public class PropertiesUtils {

    public static Properties toProperties(final Map<String, String> map) {
        final Properties result;
        if (map != null) {
            result = new Properties();
            result.putAll(map);
        } else {
            result = null;
        }
        return result;
    }

    public static Properties merge(final Properties properties, final Properties intoProperties) {
        properties.stringPropertyNames().forEach(name -> intoProperties.setProperty(name, properties.getProperty(name)));
        return intoProperties;
    }

    /**
     * Provide script execution context variables.
     */
    public static Map<String, Object> mapFromProperties(final Properties properties) {
        final Map<String, Object> result = new LinkedHashMap<>();
        properties.forEach((key, value) -> result.put(key.toString(), value.toString()));
        return result;
    }

    public static final Pattern PATTERN_VARS_ENV_DOT_CI = Pattern.compile("^env\\.CI_.+");

    public static String logProperties(final Logger logger, final String title, final Properties properties, final Pattern pattern) {
        final StringBuilder sb = new StringBuilder(pattern != null ? pattern.pattern() : "");

        final String[] propNames = properties.stringPropertyNames()
            .stream()
            .filter(propName -> pattern == null || pattern.matcher(propName).matches())
            .sorted()
            .toArray(String[]::new);

        IntStream
            .range(0, propNames.length)
            .forEach(idx -> {
                final String name = propNames[idx];
                final String value = properties.getProperty(name);
                final String line = PropertiesUtils.maskSecrets(String.format("%s[%03d] %s=%s", title, idx, name, value));
                sb.append(System.lineSeparator());
                sb.append(line);
                if (logger != null) {
                    logger.info(line);
                }
            });

        return maskSecrets(sb.toString());
    }

    public static String maskSecrets(final String text) {
        // see: https://stackoverflow.com/questions/406230/regular-expression-to-match-a-line-that-doesnt-contain-a-word
        return "" + text
            .replaceAll("KEY=(?!null).*", "KEY=[secure]")
            .replaceAll("key=(?!null).*", "key=[secure]")
            .replaceAll("KEYNAME=(?!null).*", "KEYNAME=[secure]")
            .replaceAll("keyname=(?!null).*", "keyname=[secure]")
            .replaceAll("LOGIN=(?!null).*", "LOGIN=[secure]")
            .replaceAll("login=(?!null).*", "login=[secure]")
            .replaceAll("ORGANIZATION=(?!null).*", "ORGANIZATION=[secure]")
            .replaceAll("organization=(?!null).*", "organization=[secure]")
            .replaceAll("PASS=(?!null).*", "PASS=[secure]")
            .replaceAll("pass=(?!null).*", "pass=[secure]")
            .replaceAll("PASSWORD=(?!null).*", "PASSWORD=[secure]")
            .replaceAll("password=(?!null).*", "password=[secure]")
            .replaceAll("PASSPHRASE=(?!null).*", "PASSPHRASE=[secure]")
            .replaceAll("passphrase=(?!null).*", "passphrase=[secure]")
            .replaceAll("TOKEN=(?!null).*", "TOKEN=[secure]")
            .replaceAll("token=(?!null).*", "token=[secure]")
            .replaceAll("USER=(?!null).*", "USER=[secure]")
            .replaceAll("user=(?!null).*", "user=[secure]")
            .replaceAll("USERNAME=(?!null).*", "USERNAME=[secure]")
            .replaceAll("username=(?!null).*", "username=[secure]");
    }

    public static void setSystemPropertiesIfAbsent(
        final Properties systemProperties,
        final Properties input
    ) {
        if (input != null) {
            for (final String name : input.stringPropertyNames()) {
                final String key = CiOptions.systemPropertyName(name);
                final String value = input.getProperty(name);
                if (value != null && !systemProperties.containsKey(key)) {
                    systemProperties.setProperty(key, value);
                }
            }
        }
    }

    public static String toString(final Map<String, String> properties, final Pattern pattern) {
        final StringBuilder sb = new StringBuilder(pattern != null ? pattern.pattern() : "").append(System.lineSeparator());

        final String[] names = properties.keySet()
            .stream()
            .filter(name -> pattern == null || pattern.matcher(name).matches())
            .sorted()
            .toArray(String[]::new);

        IntStream
            .range(0, names.length)
            .forEach(idx -> {
                if (idx > 0) {
                    sb.append(System.lineSeparator());
                }
                sb.append(String.format("%03d ", idx));
                final String name = names[idx];
                sb.append(name).append("=").append(properties.get(name));
            });

        return maskSecrets(sb.toString());
    }
}
