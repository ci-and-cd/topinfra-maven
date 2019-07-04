package top.infra.maven;

public final class Constants {

    public static final String BOOL_STRING_FALSE = "false";
    public static final String BOOL_STRING_TRUE = "true";

    public static final String GIT_REF_NAME_DEVELOP = "develop";
    public static final String GIT_REF_NAME_MASTER = "master";

    public static final String GIT_REF_PREFIX_FEATURE = "feature/";
    public static final String GIT_REF_PREFIX_HOTFIX = "hotfix/";
    public static final String GIT_REF_PREFIX_RELEASE = "release/";
    public static final String GIT_REF_PREFIX_SUPPORT = "support/";

    public static final String PHASE_DEPLOY = "deploy";
    public static final String PHASE_INSTALL = "install";
    public static final String PHASE_PACKAGE = "package";
    public static final String PHASE_SITE = "site";

    public static final String SRC_CI_OPTS_PROPERTIES = "src/main/ci-script/ci_opts.properties"; // TODO rename
    public static final String SETTINGS_SECURITY_XML = "settings-security.xml";

    public static final String PUBLISH_CHANNEL_RELEASE = "release";
    public static final String PUBLISH_CHANNEL_SNAPSHOT = "snapshot";

    private Constants() {
    }
}
