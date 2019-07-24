package top.infra.maven.shared.extension;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.file.Paths;
import java.util.Optional;

import top.infra.maven.CiOption;
import top.infra.maven.CiOptionContext;
import top.infra.maven.shared.utils.SystemUtils;

/**
 * Maven official (include official plugin) options.
 */
public enum MavenOption implements CiOption {
    /**
     * maven-failsafe-plugin and maven-surefire-plugin's configuration argLine.
     */
    ARGLINE("argLine", "") {
        // private Optional<String> additionalArgs(final String argLine, final CiOptionContext context) {
        //     final Optional<String> result;
        //
        //     final Optional<Integer> javaVersion = systemJavaVersion();
        //     if (javaVersion.map(version -> version >= 9).orElse(FALSE)) {
        //         // TODO calculate addModules in poms by maven plugins ?
        //         final Optional<String> addModules = JAVA_ADDMODULES.getValue(context);
        //         final Optional<String> argLineWithModules;
        //         if (addModules.isPresent() && (argLine == null || !argLine.contains("--add-modules"))) {
        //             argLineWithModules = Optional.of(String.format("--add-modules %s", addModules.get()));
        //         } else {
        //             argLineWithModules = Optional.empty();
        //         }
        //
        //         if (javaVersion.map(version -> version >= 11).orElse(FALSE)) {
        //             final String addExports = " --add-exports java.base/jdk.internal.loader=ALL-UNNAMED"
        //                 + " --add-exports java.base/sun.security.ssl=ALL-UNNAMED"
        //                 + " --add-opens java.base/jdk.internal.loader=ALL-UNNAMED"
        //                 + " --add-opens java.base/sun.security.ssl=ALL-UNNAMED";
        //             result = Optional.of(String.format("%s --illegal-access=permit %s", addExports, argLineWithModules.orElse("")));
        //         } else {
        //             result = argLineWithModules;
        //         }
        //     } else {
        //         result = Optional.empty();
        //     }
        //
        //     return result;
        // }

        // @Override
        // public Optional<String> calculateValue(final CiOptionContext context) {
        //     return this.additionalArgs(null, context);
        // }

        // @Override
        // public Optional<String> getValue(final CiOptionContext context) {
        //     final String argLine = this.findInProperties(context.getSystemProperties(), context.getUserProperties())
        //         .orElse(null);
        //     return this.additionalArgs(argLine, context);
        // }
    },
    ENFORCER_SKIP("enforcer.skip") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            return GlobalOption.FAST.getValue(context);
        }
    },
    // /**
    //  * Got warning on maven-surefire-plugin's test goal.
    //  * [WARNING] file.encoding cannot be set as system property, use &lt;argLine&gt;-Dfile.encoding=...&lt;/argLine&gt; instead
    //  */
    // @Deprecated
    // FILE_ENCODING("file.encoding", UTF_8.name()),
    // JAVA_ADDMODULES("java.addModules") {
    //     @Override
    //     public Optional<String> calculateValue(final CiOptionContext context) {
    //         final Optional<Integer> javaVersion = systemJavaVersion();
    //         return javaVersion
    //             .map(version -> {
    //                 final String defaultModules;
    //                 if (version == 9) {
    //                     defaultModules = "java.xml.bind,java.xml.ws,java.xml.ws.annotation";
    //                 } else {
    //                     defaultModules = null;
    //                 }
    //                 return defaultModules;
    //             });
    //     }
    // },
    MAVEN_CLEAN_SKIP(Constants.PROP_MAVEN_CLEAN_SKIP),
    MAVEN_COMPILER_ENCODING("maven.compiler.encoding", UTF_8.name()),
    /**
     * See "https://github.com/aleksandr-m/gitflow-maven-plugin".
     */
    MVNEXECUTABLE("mvnExecutable") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            final String mvn = SystemUtils.isWindows() ? "mvn.cmd" : "mvn";
            return Optional.ofNullable(context.getSystemProperties().getProperty("maven.home"))
                .map(mvnHome -> Paths.get(mvnHome, "bin", mvn).toString());
        }
    },
    MAVEN_INSTALL_SKIP(Constants.PROP_MAVEN_INSTALL_SKIP),
    MAVEN_JAVADOC_SKIP(Constants.PROP_MAVEN_JAVADOC_SKIP) {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            return Optional.of(GlobalOption.FAST.getValue(context)
                .map(Boolean::parseBoolean)
                .filter(fast -> fast)
                .map(fast -> Constants.BOOL_STRING_TRUE)
                .orElse(Constants.BOOL_STRING_FALSE));
        }
    },
    /**
     * See: "https://maven.apache.org/plugins/maven-site-plugin/site-mojo.html".
     * <p/>
     * Convenience parameter that allows you to disable report generation.
     */
    GENERATEREPORTS("generateReports") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            return GlobalOption.FAST.getValue(context)
                .map(Boolean::parseBoolean)
                .filter(fast -> fast)
                .map(fast -> Constants.BOOL_STRING_FALSE);
        }
    },
    JIRA_PROJECTKEY("jira.projectKey"),
    JIRA_USER("jira.user") {
        @Override
        public Optional<String> getValue(final CiOptionContext context) {
            final Optional<String> jiraProjectKey = JIRA_PROJECTKEY.getValue(context);
            return jiraProjectKey.isPresent() ? super.getValue(context) : Optional.empty();
        }
    },
    JIRA_PASSWORD("jira.password") {
        @Override
        public Optional<String> getValue(final CiOptionContext context) {
            final Optional<String> jiraProjectKey = JIRA_PROJECTKEY.getValue(context);
            return jiraProjectKey.isPresent() ? super.getValue(context) : Optional.empty();
        }
    },
    LINKXREF("linkXRef") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            return Optional.ofNullable(GlobalOption.FAST.getValue(context)
                .map(Boolean::parseBoolean).orElse(FALSE) ? Constants.BOOL_STRING_FALSE : null);
        }
    },
    /**
     * See: "https://maven.apache.org/plugins/maven-site-plugin/site-mojo.html".
     * <p/>
     * Set this to 'true' to skip site generation and staging.
     */
    MAVEN_SITE_SKIP("maven.site.skip") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            return Optional.ofNullable(GENERATEREPORTS.getValue(context)
                .map(Boolean::parseBoolean).orElse(TRUE) ? null : Constants.BOOL_STRING_TRUE);
        }
    },
    MAVEN_SITE_DEPLOY_SKIP("maven.site.deploy.skip") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            return MAVEN_SITE_SKIP.calculateValue(context);
        }
    },
    MAVEN_SOURCE_SKIP(Constants.PROP_MAVEN_SOURCE_SKIP) {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            return MAVEN_JAVADOC_SKIP.calculateValue(context);
        }
    },
    MAVEN_TEST_FAILURE_IGNORE("maven.test.failure.ignore", Constants.BOOL_STRING_FALSE) {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            return GlobalOption.FAST.getValue(context);
        }
    },

    /**
     * Skip test-compile and skipTests and skipITs.
     * <p/>
     * maven.test.skip property skips compiling the tests. maven.test.skip is honored by Surefire, Failsafe and the Compiler Plugin.
     */
    @Deprecated
    MAVEN_TEST_SKIP("maven.test.skip"),

    PMD_SKIP("pmd.skip") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            return Optional.ofNullable(GENERATEREPORTS.calculateValue(context)
                .map(Boolean::parseBoolean).orElse(TRUE) ? null : Constants.BOOL_STRING_TRUE);
        }
    },

    @Deprecated
    PROJECT_BUILD_SOURCEENCODING("project.build.sourceEncoding"),
    @Deprecated
    PROJECT_REPORTING_OUTPUTENCODING("project.reporting.outputEncoding"),

    /**
     * Since skipTests is also supported by the Surefire Plugin, this will have the effect of not running any tests.
     * If, instead, you want to skip only the integration tests being run by the Failsafe Plugin,
     * you would use the skipITs property instead.
     * see: https://maven.apache.org/surefire/maven-failsafe-plugin/examples/skipping-tests.html
     */
    SKIPITS("skipITs") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            return GlobalOption.FAST.getValue(context);
        }
    },
    /**
     * Skip tests when `./mvnw gitflow:release-finish`.
     * see: https://github.com/aleksandr-m/gitflow-maven-plugin
     */
    SKIPTESTPROJECT("skipTestProject") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            return GlobalOption.FAST.getValue(context);
        }
    },
    /**
     * Keep test-compile but do not run tests.
     * <p/>
     * see: https://maven.apache.org/surefire/maven-surefire-plugin/examples/skipping-tests.html
     */
    SKIPTESTS("skipTests", Constants.BOOL_STRING_FALSE) {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            return GlobalOption.FAST.getValue(context);
        }
    },
    SONAR_BUILDBREAKER_SKIP("sonar.buildbreaker.skip") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            return Optional.ofNullable(GlobalOption.FAST.getValue(context)
                .map(Boolean::parseBoolean).orElse(FALSE) ? Constants.BOOL_STRING_TRUE : null);
        }
    },
    SPOTBUGS_SKIP("spotbugs.skip") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            return Optional.ofNullable(GENERATEREPORTS.calculateValue(context)
                .map(Boolean::parseBoolean).orElse(TRUE) ? null : Constants.BOOL_STRING_TRUE);
        }
    },
    ;

    private final String defaultValue;
    private final String propertyName;

    MavenOption(final String propertyName) {
        this(propertyName, null);
    }

    MavenOption(final String propertyName, final String defaultValue) {
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
