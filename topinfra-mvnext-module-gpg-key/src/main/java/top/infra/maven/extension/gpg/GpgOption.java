package top.infra.maven.extension.gpg;

import static top.infra.maven.extension.gpg.Gpg.gpgVersionGreater;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import top.infra.maven.Constants;
import top.infra.maven.core.CiOption;
import top.infra.maven.core.CiOptionContext;
import top.infra.maven.core.CiOptionNames;
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
    private final String envVariableName;
    private final String propertyName;
    private final String systemPropertyName;

    GpgOption(final String propertyName) {
        this(propertyName, null);
    }

    GpgOption(final String propertyName, final String defaultValue) {
        if (!CiOptionNames.name(propertyName).equals(this.name())) {
            throw new IllegalArgumentException(String.format("invalid property name [%s] for enum name [%s]", this.name(), propertyName));
        }

        this.defaultValue = defaultValue;
        this.envVariableName = CiOptionNames.envVariableName(propertyName);
        this.propertyName = propertyName;
        this.systemPropertyName = CiOptionNames.systemPropertyName(propertyName);
    }

    public Optional<String> getDefaultValue() {
        return Optional.ofNullable(this.defaultValue);
    }

    public String getEnvVariableName() {
        return this.envVariableName;
    }

    public String getPropertyName() {
        return this.propertyName;
    }

    public String getSystemPropertyName() {
        return this.systemPropertyName;
    }
}
