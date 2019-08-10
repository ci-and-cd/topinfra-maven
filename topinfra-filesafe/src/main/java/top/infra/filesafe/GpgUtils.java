package top.infra.filesafe;

import java.util.List;
import java.util.Optional;

import top.infra.util.StringUtils;
import top.infra.util.cli.CliUtils;

public abstract class GpgUtils {

    private GpgUtils() {
    }

    public static List<String> cmdGpgBatchYes(final String gpgExecutable, final String... command) {
        final String[] cmd = GpgUtils.gpgCommand(gpgExecutable);
        return StringUtils.asList(StringUtils.concat(cmd, "--batch=true", "--yes"), command);
    }

    public static String[] gpgCommand(final String gpgExecutable) {
        return "gpg2".equals(gpgExecutable) ? new String[]{"gpg2", "--use-agent"} : new String[]{"gpg"};
    }

    public static Optional<String> gpgExecutable() {
        final String gpg = CliUtils.exec("which gpg").getKey() == 0 ? "gpg" : null;
        final String gpgExecutable = CliUtils.exec("which gpg2").getKey() == 0 ? "gpg2" : gpg;

        return Optional.ofNullable(gpgExecutable);
    }
}
