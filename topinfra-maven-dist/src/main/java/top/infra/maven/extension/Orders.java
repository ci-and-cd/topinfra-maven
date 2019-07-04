package top.infra.maven.extension;

public abstract class Orders {

    public static final int CI_OPTION_FAST = Ordered.HIGHEST_PRECEDENCE;
    public static final int CI_OPTION_INFRA = CI_OPTION_FAST + 1;
    public static final int CI_OPTION_MAVEN_BUILD_EXTENSION = CI_OPTION_INFRA + 1;
    public static final int CI_OPTION_MAVEN = CI_OPTION_MAVEN_BUILD_EXTENSION + 1;
    public static final int CI_OPTION_DOCKER = CI_OPTION_MAVEN + 1;
    public static final int CI_OPTION_GPG = CI_OPTION_DOCKER + 1;
    public static final int CI_OPTION_MAVEN_BUILD_POM = CI_OPTION_GPG + 1;

    public static final int ORDER_INFO_PRINTER = Ordered.HIGHEST_PRECEDENCE;
    public static final int ORDER_SYSTEM_TO_USER_PROPERTIES = ORDER_INFO_PRINTER + 1;
    public static final int ORDER_GIT_PROPERTIES = ORDER_SYSTEM_TO_USER_PROPERTIES + 1;
    public static final int ORDER_OPTIONS_FACTORY = ORDER_GIT_PROPERTIES + 1;
    public static final int ORDER_OPTION_FILE_LOADER = ORDER_OPTIONS_FACTORY + 1; //

    public static final int EVENT_AWARE_ORDER_CI_OPTION = ORDER_OPTION_FILE_LOADER + 1;
    public static final int EVENT_AWARE_ORDER_MAVEN_SETTINGS_LOCALREPOSITORY = EVENT_AWARE_ORDER_CI_OPTION + 1;
    public static final int EVENT_AWARE_ORDER_MAVEN_SETTINGS_FILES = EVENT_AWARE_ORDER_MAVEN_SETTINGS_LOCALREPOSITORY + 1;
    public static final int EVENT_AWARE_ORDER_MAVEN_SETTINGS_SERVERS = EVENT_AWARE_ORDER_MAVEN_SETTINGS_FILES + 1;
    public static final int EVENT_AWARE_ORDER_MODEL_RESOLVER = EVENT_AWARE_ORDER_MAVEN_SETTINGS_SERVERS + 1; //
    public static final int EVENT_AWARE_ORDER_GPG = EVENT_AWARE_ORDER_MODEL_RESOLVER + 1;
    public static final int EVENT_AWARE_ORDER_MAVEN_PROJECT_INFO = EVENT_AWARE_ORDER_GPG + 1; //
    public static final int EVENT_AWARE_ORDER_GOAL_EDITOR = EVENT_AWARE_ORDER_MAVEN_PROJECT_INFO + 1;
    public static final int ORDER_GITFLOW_SEMANTIC_VERSION = EVENT_AWARE_ORDER_GOAL_EDITOR + 1;
    public static final int EVENT_AWARE_ORDER_DOCKER = ORDER_GITFLOW_SEMANTIC_VERSION + 1; //

    public static final int ORDER_SETTINGS_SECURITY = EVENT_AWARE_ORDER_DOCKER + 1;

    private Orders() {
    }
}
