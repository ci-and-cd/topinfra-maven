package top.infra.maven.extension.gpg;

import static top.infra.maven.extension.gpg.Gpg.gpgVersionGreater;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import top.infra.maven.extension.shared.Constants;
import top.infra.maven.CiOption;
import top.infra.maven.CiOptionContext;
import top.infra.maven.utils.SystemUtils;

public enum GpgOption implements CiOption {

    GPG_EXECUTABLE("gpg.executable") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            final String gpg = SystemUtils.exec("which gpg").getKey() == 0 ? "gpg" : null;
            final String gpgExecutable = SystemUtils.exec("which gpg2").getKey() == 0 ? "gpg2" : gpg;

            return Optional.ofNullable(gpgExecutable);
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
                final List<String> gpgVersion = Arrays.asList(gpgExecutable.get(), "--batch=true", "--version");
                final Map.Entry<Integer, String> resultGpgVersion = SystemUtils.exec(null, null, gpgVersion);
                if (gpgVersionGreater(resultGpgVersion.getValue(), "2.1")) {
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
