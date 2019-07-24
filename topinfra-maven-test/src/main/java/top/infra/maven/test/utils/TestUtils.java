package top.infra.maven.test.utils;

import java.util.Properties;

import top.infra.maven.CiOptionContext;
import top.infra.maven.shared.DefaultCiOptionContext;

public abstract class TestUtils {

    private TestUtils() {
    }

    public static CiOptionContext blankCiOptCtx() {
        final Properties systemProperties = new Properties();
        final Properties userProperties = new Properties();
        return new DefaultCiOptionContext(systemProperties, userProperties);
    }
}
