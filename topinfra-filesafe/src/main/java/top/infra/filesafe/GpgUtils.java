package top.infra.filesafe;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.MULTILINE;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import top.infra.util.StringUtils;
import top.infra.util.cli.CliUtils;

public abstract class GpgUtils {

    private GpgUtils() {
    }

    public static List<String> cmdGpgBatch(final String gpgExecutable, final String... command) {
        final String[] cmd = GpgUtils.gpgCommand(gpgExecutable);
        return StringUtils.asList(StringUtils.concat(cmd, "--batch=true"), command);
    }

    public static List<String> cmdGpgBatchYes(final String gpgExecutable, final String... command) {
        final String[] cmd = GpgUtils.gpgCommand(gpgExecutable);
        return StringUtils.asList(StringUtils.concat(cmd, "--batch=true", "--yes"), command);
    }

    public static String[] gpgCommand(final String gpgExecutable) {
        return "gpg2".equals(gpgExecutable) ? new String[]{"gpg2", "--use-agent"} : new String[]{"gpg"};
    }

    public static Map<String, String> gpgEnvironment() {
        final Map<String, String> env = new LinkedHashMap<>();
        env.put("LC_CTYPE", "UTF-8");
        final Optional<String> tty = GpgUtils.tty();
        tty.ifPresent(s -> env.put("GPG_TTY", s));
        return Collections.unmodifiableMap(env);
    }

    public static Optional<String> gpgExecutable() {
        final String gpg = CliUtils.exec("which gpg").getKey() == 0 ? "gpg" : null;
        final String gpgExecutable = CliUtils.exec("which gpg2").getKey() == 0 ? "gpg2" : gpg;

        return Optional.ofNullable(gpgExecutable);
    }

    public static Optional<String> gpgVersion(final String gpgExecutable) {
        // use --batch=true to avoid 'gpg tty not a tty' error
        final Map<String, String> environment = GpgUtils.gpgEnvironment();
        final List<String> command = cmdGpgBatch(gpgExecutable, "--version");
        final Entry<Integer, Entry<String, String>> result = CliUtils.exec(environment, null, command);

        final String stdOut = result.getValue().getKey();

        // e.g. "gpg (GnuPG) 2.2.14\nlibgcrypt 1.8.4\nCopyright ( ... pressed, ZIP, ZLIB, BZIP2"
        final Matcher matcher = Pattern
            .compile("[^0-9]*([0-9]+\\.[0-9]+\\.[0-9]+).*", DOTALL | MULTILINE)
            .matcher(stdOut);

        return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    public static boolean gpgVersionGreater(
        final String version,
        final String thanVersion
    ) {
        return !StringUtils.isEmpty(version)
            && new DefaultArtifactVersion(version).compareTo(new DefaultArtifactVersion(thanVersion)) > 0;
    }

    public static Optional<String> tty() {
        final Entry<Integer, Entry<String, String>> tty = CliUtils.exec("tty");
        return Optional.ofNullable(tty.getKey() == 0 ? tty.getValue().getKey() : null);
    }
}
