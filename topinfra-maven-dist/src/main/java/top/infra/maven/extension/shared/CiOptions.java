package top.infra.maven.extension.shared;

import static top.infra.maven.utils.SupportFunction.isEmpty;

import java.util.regex.Pattern;

public class CiOptions {

    public static final Pattern PATTERN_VARS_ENV_DOT_CI = Pattern.compile("^env\\.CI_.+");

    public static String systemPropertyName(final String propertyName) {
        return String.format("env.%s", envVariableName(propertyName));
    }

    public static String envVariableName(final String propertyName) {
        final String name = name(propertyName);
        return name.startsWith("CI_OPT_") ? name : "CI_OPT_" + name;
    }

    public static String name(final String propertyName) {
        if (isEmpty(propertyName)) {
            throw new IllegalArgumentException("propertyName must not empty");
        }
        return propertyName.replaceAll("-", "").replaceAll("\\.", "_").toUpperCase();
    }
}
