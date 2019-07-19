package top.infra.maven.shared.utils;

import static java.lang.Boolean.parseBoolean;
import static top.infra.maven.shared.extension.Constants.BOOL_STRING_FALSE;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import top.infra.maven.shared.exception.RuntimeIOException;

public abstract class SupportFunction {

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    private SupportFunction() {
    }

    public static List<String> asList(final String[] array1, final String... array2) {
        return Arrays.asList(concat(array1, array2));
    }

    public static String componentName(final Class<?> clazz) {
        // see: https://stackoverflow.com/questions/1591132/how-can-i-add-an-underscore-before-each-capital-letter-inside-a-java-string
        return clazz.getSimpleName().replaceAll("(.)(\\p{Lu})", "$1_$2").toUpperCase();
    }

    public static boolean componentDisabled(
        final Class<?> clazz,
        final Properties systemProperties,
        final Properties userProperties
    ) {
        final String name = componentName(clazz);
        final String envVar = "env.CI_OPT_" + name + "_DISABLED";
        final String propName = name.toLowerCase().replace('_', '-') + ".disabled";
        return parseBoolean(systemProperties.getProperty(envVar, userProperties.getProperty(propName, BOOL_STRING_FALSE)));
    }

    public static String[] concat(final String[] array1, final String... array2) {
        return Stream.of(array1, array2).flatMap(Stream::of).toArray(String[]::new);
        // return Stream.concat(Arrays.stream(array1), Arrays.stream(array2)).toArray(String[]::new);
    }

    public static boolean exists(final Path path) {
        return path != null && path.toFile().exists();
    }

    public static boolean isEmpty(final String str) {
        return str == null || str.isEmpty();
    }

    public static List<String> lines(final String cmdOutput) {
        return Arrays.stream(("" + cmdOutput).split("\\r?\\n"))
            .map(line -> line.replaceAll("\\s+", " "))
            .filter(SupportFunction::isNotEmpty)
            .collect(Collectors.toList());
    }

    public static boolean isNotEmpty(final String str) {
        return str != null && !str.isEmpty();
    }

    public static List<Entry<String, List<String>>> commonPrefixes(final List<String> names) {
        final List<Entry<String, List<String>>> result = new LinkedList<>();

        final List<String> randomAccess = new ArrayList<>(names);
        final int size = randomAccess.size();

        String lastCommonPrefix = "";
        List<String> currentGroup = new LinkedList<>();
        for (int idx = 0; idx < size; idx++) {
            final String current = randomAccess.get(idx);
            final String next = idx + 1 != size ? randomAccess.get(idx + 1) : null;

            final String cPrefix = commonPrefix(current, next);
            final boolean hasCommonPrefix;
            final String ccPrefix;
            if (cPrefix.isEmpty()) {
                hasCommonPrefix = false;
                ccPrefix = "";
            } else {
                ccPrefix = lastCommonPrefix.isEmpty() ? cPrefix : commonPrefix(lastCommonPrefix, cPrefix);
                final boolean containsUnderscore = ccPrefix.contains("_");
                hasCommonPrefix = ccPrefix.startsWith("env.") ? containsUnderscore : (containsUnderscore || ccPrefix.contains("."));
            }

            if (hasCommonPrefix) {
                currentGroup.add(current);
            } else {
                currentGroup.add(current);
                result.add(newTuple(lastCommonPrefix, currentGroup));
                currentGroup = new LinkedList<>();
            }
            lastCommonPrefix = hasCommonPrefix ? ccPrefix : "";
        }

        return result;
    }

    private static String commonPrefix(final String o1, final String o2) {
        final String result;
        if (o1 != null && o2 != null) {
            final StringBuilder sb = new StringBuilder();
            for (int idx = 0; idx < o1.length() && idx < o2.length(); idx++) {
                final char charAtIdx = o1.charAt(idx);
                if (charAtIdx == o2.charAt(idx)) {
                    sb.append(charAtIdx);
                } else {
                    break;
                }
            }
            result = sb.toString();
        } else {
            result = "";
        }
        return result;
    }

    public static <F, S> Entry<F, S> newTuple(final F first, final S second) {
        return new AbstractMap.SimpleImmutableEntry<>(first, second);
    }

    public static <F, S> Entry<Optional<F>, Optional<S>> newTupleOptional(final F first, final S second) {
        return new AbstractMap.SimpleImmutableEntry<>(Optional.ofNullable(first), Optional.ofNullable(second));
    }

    public static boolean notEmpty(final String str) {
        return str != null && !str.isEmpty();
    }

    public static String stackTrace(final Exception ex) {
        final StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public static String uniqueKey() {
        try {
            final MessageDigest salt = MessageDigest.getInstance("SHA-256");
            salt.update(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
            return bytesToHex(salt.digest());
        } catch (final NoSuchAlgorithmException ex) {
            throw new UnsupportedOperationException(ex);
        }
    }

    private static String bytesToHex(final byte[] bytes) {
        final char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            final int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String logStart(final Object obj, final String methodName, final Object... args) {
        return String.format(" >>> %s#%s%s --- hashCode: [%s] @ %s >>>",
            obj.getClass().getSimpleName(), methodName, argsToStr(args), obj.hashCode(), module(obj));
    }

    public static String logEnd(final Object obj, final String methodName, final Object returns, final Object... args) {
        return String.format(" <<< %s#%s%s%s --- hashCode: [%s] @ %s <<<",
            obj.getClass().getSimpleName(), methodName, argsToStr(args), returnsToStr(returns), obj.hashCode(), module(obj));
    }

    private static String returnsToStr(final Object returns) {
        return returns != Void.TYPE && returns != Void.class ? String.format(" -> %s", returns) : "";
    }

    private static String argsToStr(final Object... args) {
        return args.length > 0
            ? "(" + Stream.of(args).map(Object::toString).collect(Collectors.joining(", ")) + ")"
            : "";
    }

    public static String module(final Object obj) {
        final Properties moduleInfo = new Properties();
        try {
            moduleInfo.load(obj.getClass().getClassLoader().getResourceAsStream("module-info.properties"));
        } catch (final IOException ex) {
            throw new RuntimeIOException(ex);
        }
        return String.format(
            "%s:%s",
            moduleInfo.getProperty("artifactId", "unknown"),
            moduleInfo.getProperty("version", "unknown"));
    }
}
