package top.infra.maven.extension.gpg;

import static top.infra.maven.extension.gpg.Gpg.gpgVersionGreater;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import top.infra.filesafe.GpgUtils;
import top.infra.maven.CiOption;
import top.infra.maven.CiOptionContext;
import top.infra.maven.shared.extension.Constants;
import top.infra.util.cli.CliUtils;

public enum GpgOption implements CiOption {

    GPG_EXECUTABLE("gpg.executable") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            return GpgUtils.gpgExecutable();
        }
    },
    GPG_KEYID("gpg.keyid"),
    GPG_KEYNAME("gpg.keyname"),
    GPG_LOOPBACK("gpg.loopback") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            final Optional<String> gpgExecutable = GPG_EXECUTABLE.getValue(context);

            final Optional<String> result;
            if (gpgExecutable.isPresent()) {
                final List<String> command = Arrays.asList(gpgExecutable.get(), "--batch=true", "--version");
                final Entry<Integer, Entry<String, String>> resultGpgVersion = CliUtils.exec(null, null, command);
                final String gpgVersion = resultGpgVersion.getValue().getKey();
                if (gpgVersionGreater(gpgVersion, "2.1")) {
                    result = Optional.of(Constants.BOOL_STRING_TRUE);
                } else {
                    result = Optional.empty();
                }
            } else {
                result = Optional.empty();
            }
            return result;
        }
    },
    GPG_PASSPHRASE("gpg.passphrase"),
    ;

    private final String defaultValue;
    private final String propertyName;

    GpgOption(final String propertyName) {
        this(propertyName, null);
    }

    GpgOption(final String propertyName, final String defaultValue) {
        this.defaultValue = defaultValue;
        this.propertyName = propertyName;
    }

    @Override
    public Optional<String> getDefaultValue() {
        return Optional.ofNullable(this.defaultValue);
    }

    @Override
    public String getPropertyName() {
        return this.propertyName;
    }
}
