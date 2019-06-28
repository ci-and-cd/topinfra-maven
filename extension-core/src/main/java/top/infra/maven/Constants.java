package top.infra.maven;

public final class Constants {

    public static final String BOOL_STRING_FALSE = "false";
    public static final String BOOL_STRING_TRUE = "true";

    public static final String BRANCH_PREFIX_FEATURE = "feature/";
    public static final String BRANCH_PREFIX_HOTFIX = "hotfix/";
    public static final String BRANCH_PREFIX_RELEASE = "release/";
    public static final String BRANCH_PREFIX_SUPPORT = "support/";

    public static final String GIT_REF_NAME_DEVELOP = "develop";
    public static final String GIT_REF_NAME_MASTER = "master";

    public static final String SRC_CI_OPTS_PROPERTIES = "src/main/ci-script/ci_opts.properties";
    public static final String SRC_MAVEN_SETTINGS_XML = "src/main/maven/settings.xml";
    public static final String SRC_MAVEN_SETTINGS_SECURITY_XML = "src/main/maven/settings-security.xml";

    public static final String PUBLISH_CHANNEL_RELEASE = "release";
    public static final String PUBLISH_CHANNEL_SNAPSHOT = "snapshot";

    private Constants() {
    }
}
