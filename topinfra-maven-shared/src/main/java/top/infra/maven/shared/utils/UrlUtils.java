package top.infra.maven.shared.utils;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class UrlUtils {

    private static final Pattern PATTERN_URL = Pattern.compile("^(.+://|git@)([^/\\:]+(:\\d+)?).*$");

    private UrlUtils() {
    }

    public static Optional<String> domainOrHostFromUrl(final String url) {
        final Optional<String> result;
        final Matcher matcher = PATTERN_URL.matcher(url);
        if (matcher.matches()) {
            result = Optional.ofNullable(matcher.group(2));
        } else {
            result = Optional.empty();
        }
        return result;
    }

    public static Optional<String> urlWithoutPath(final String url) {
        final Optional<String> result;
        final Matcher matcher = PATTERN_URL.matcher(url);
        if (matcher.matches()) {
            result = Optional.of(matcher.group(1) + matcher.group(2));
        } else {
            result = Optional.empty();
        }
        return result;
    }
}
