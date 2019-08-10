package top.infra.maven.shared.utils;

import java.io.File;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public abstract class SystemUtils {

    private SystemUtils() {
    }

    /**
     * Check existence of a program in the path.
     * see: https://stackoverflow.com/questions/934191/how-to-check-existence-of-a-program-in-the-path/23539220
     *
     * @param exec executable name
     * @return exec exists
     */
    public static boolean existsInPath(final String exec) {
        // return exec(String.format("type -p %s", exec)).getKey() == 0;
        return Stream.of(System.getenv("PATH").split(Pattern.quote(File.pathSeparator)))
            .map(Paths::get)
            .anyMatch(path -> path.resolve(exec).toFile().exists());
    }

    public static String os() {
        final String osName = System.getProperty("os.name", "generic").toLowerCase();
        final String result;
        if (osName.contains("mac")) {
            result = "darwin";
        } else if (osName.contains("win")) {
            result = "windows";
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            result = "unix";
        } else {
            result = "generic";
        }
        return result;
    }

    public static boolean isWindows() {
        return "windows".equalsIgnoreCase(SystemUtils.os());
    }

    public static String systemJavaIoTmp() {
        return System.getProperty("java.io.tmpdir");
    }

    public static Optional<Integer> systemJavaVersion() {
        final String systemJavaVersion = System.getProperty("java.version");
        return parseJavaVersion(systemJavaVersion);
    }

    public static Optional<Integer> parseJavaVersion(final String javaVersion) {
        final Optional<Integer> result;

        if (javaVersion.matches("1.\\d+\\.?.*")) {
            result = Optional.of(Integer.parseInt(javaVersion.split("\\.")[1]));
        } else if (javaVersion.matches("\\d+\\.?.*")) {
            result = Optional.of(Integer.parseInt(javaVersion.split("\\.")[0]));
        } else {
            result = Optional.empty();
        }

        return result;
    }

    public static String systemUserHome() {
        return System.getProperty("user.home", systemUserDir());
    }

    public static String systemUserDir() {
        return System.getProperty("user.dir", Paths.get(".").normalize().toString());
    }
}
