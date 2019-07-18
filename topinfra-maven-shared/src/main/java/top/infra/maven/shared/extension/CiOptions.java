package top.infra.maven.shared.extension;

public abstract class CiOptions {

    private CiOptions() {
    }

    public static String systemPropertyName(final String propertyName) {
        return GlobalOption.FAST.systemPropertyName(propertyName);
    }
}
