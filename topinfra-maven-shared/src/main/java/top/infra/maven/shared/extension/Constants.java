package top.infra.maven.shared.extension;

import static org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Constants {

    public static final String BOOL_STRING_FALSE = "false";
    public static final String BOOL_STRING_TRUE = "true";

    public static final String GIT_REF_NAME_DEVELOP = "develop";
    public static final String GIT_REF_NAME_MASTER = "master";

    public static final String GIT_REF_PREFIX_FEATURE = "feature/";
    public static final String GIT_REF_PREFIX_HOTFIX = "hotfix/";
    public static final String GIT_REF_PREFIX_RELEASE = "release/";
    public static final String GIT_REF_PREFIX_SUPPORT = "support/";

    public static final String PHASE_CLEAN = "clean";
    public static final String PHASE_COMPILE = "compile";
    public static final String PHASE_DEPLOY = "deploy";
    public static final String PHASE_INSTALL = "install";
    public static final String PHASE_INTEGRATION_TEST = "integration-test";
    public static final String PHASE_PACKAGE = "package";
    public static final String PHASE_POST_INTEGRATION_TEST = "post-integration-test";
    public static final String PHASE_PRE_INTEGRATION_TEST = "pre-integration-test";
    public static final String PHASE_PROCESS_RESOURCES = "process-resources";
    public static final String PHASE_PROCESS_TEST_RESOURCES = "process-test-resources";
    public static final String PHASE_SITE = "site";
    public static final String PHASE_SITE_DEPLOY = "site-deploy";
    public static final String PHASE_TEST = "test";
    public static final String PHASE_TEST_COMPILE = "test-compile";
    public static final String PHASE_VALIDATE = "validate";
    public static final String PHASE_VERIFY = "verify";

    public static final List<String> PHASES_AFTER_PREPARE_PACKAGE_AND_BEFORE_DEPLOY = Collections.unmodifiableList(Arrays.asList(
        PHASE_PACKAGE,
        PHASE_PRE_INTEGRATION_TEST,
        PHASE_INTEGRATION_TEST,
        PHASE_POST_INTEGRATION_TEST,
        PHASE_VERIFY,
        PHASE_INSTALL
    ));

    public static final String SRC_CI_OPTS_PROPERTIES = "ci_opts.properties";
    public static final String SETTINGS_SECURITY_XML = "settings-security.xml";

    public static final String PROP_SETTINGS = "settings";
    public static final String PROP_SETTINGS_SECURITY = SYSTEM_PROPERTY_SEC_LOCATION;
    public static final String PROP_TOOLCHAINS = "toolchains";

    public static final String PROP_MAVEN_CLEAN_SKIP = "maven.clean.skip";
    public static final String PROP_MAVEN_JAVADOC_SKIP = "maven.javadoc.skip";
    public static final String PROP_MAVEN_INSTALL_SKIP = "maven.install.skip";
    public static final String PROP_MAVEN_PACKAGES_SKIP = "maven.packages.skip";
    public static final String PROP_MAVEN_SOURCE_SKIP = "maven.source.skip";

    public static final String PROP_MVN_MULTI_STAGE_BUILD = "mvn.multi.stage.build";
    public static final String PROP_MVN_MULTI_STAGE_BUILD_GOAL_DEPLOY = "mvn.multi.stage.build.goal.deploy";

    public static final String PROP_NEXUS2_STAGING = "nexus2.staging";

    public static final String PROP_PUBLISH_TO_REPO = "publish.to.repo";

    public static final String PROP_SETTINGS_LOCALREPOSITORY = "settings.localRepository";

    public static final String PROP_SONAR = "sonar";

    private Constants() {
    }
}
