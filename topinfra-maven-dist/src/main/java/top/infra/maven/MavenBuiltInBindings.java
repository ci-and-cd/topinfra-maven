package top.infra.maven;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static top.infra.maven.utils.SupportFunction.newTuple;

import java.util.List;
import java.util.Map.Entry;

/**
 * See "https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html".
 */
public enum MavenBuiltInBindings {

    EJB("ejb", asList(
        newTuple(MavenPhase.CLEAN, unmodifiableList(singletonList(MavenBuiltInBindings.CLEAN_CLEAN))),
        newTuple(MavenPhase.PROCESS_RESOURCES, unmodifiableList(singletonList(MavenBuiltInBindings.RESOURCES_RESOURCES))),
        newTuple(MavenPhase.COMPILE, unmodifiableList(singletonList(MavenBuiltInBindings.COMPILER_COMPILE))),
        newTuple(MavenPhase.PROCESS_TEST_RESOURCES, unmodifiableList(singletonList(MavenBuiltInBindings.RESOURCES_TESTRESOURCES))),
        newTuple(MavenPhase.TEST_COMPILE, unmodifiableList(singletonList(MavenBuiltInBindings.COMPILER_TESTCOMPILE))),
        newTuple(MavenPhase.TEST, unmodifiableList(singletonList(MavenBuiltInBindings.SUREFIRE_TEST))),
        newTuple(MavenPhase.PACKAGE, unmodifiableList(singletonList("ejb:ejb"))),
        newTuple(MavenPhase.INSTALL, unmodifiableList(singletonList(MavenBuiltInBindings.INSTALL_INSTALL))),
        newTuple(MavenPhase.DEPLOY, unmodifiableList(singletonList(MavenBuiltInBindings.DEPLOY_DEPLOY))),
        newTuple(MavenPhase.SITE, unmodifiableList(singletonList(MavenBuiltInBindings.SITE_SITE))),
        newTuple(MavenPhase.SITE_DEPLOY, unmodifiableList(singletonList(MavenBuiltInBindings.SITE_DEPLOY)))

    )),
    EJB3("ejb3", asList(
        newTuple(MavenPhase.CLEAN, unmodifiableList(singletonList(MavenBuiltInBindings.CLEAN_CLEAN))),
        newTuple(MavenPhase.PROCESS_RESOURCES, unmodifiableList(singletonList(MavenBuiltInBindings.RESOURCES_RESOURCES))),
        newTuple(MavenPhase.COMPILE, unmodifiableList(singletonList(MavenBuiltInBindings.COMPILER_COMPILE))),
        newTuple(MavenPhase.PROCESS_TEST_RESOURCES, unmodifiableList(singletonList(MavenBuiltInBindings.RESOURCES_TESTRESOURCES))),
        newTuple(MavenPhase.TEST_COMPILE, unmodifiableList(singletonList(MavenBuiltInBindings.COMPILER_TESTCOMPILE))),
        newTuple(MavenPhase.TEST, unmodifiableList(singletonList(MavenBuiltInBindings.SUREFIRE_TEST))),
        newTuple(MavenPhase.PACKAGE, unmodifiableList(singletonList("ejb3:ejb3"))),
        newTuple(MavenPhase.INSTALL, unmodifiableList(singletonList(MavenBuiltInBindings.INSTALL_INSTALL))),
        newTuple(MavenPhase.DEPLOY, unmodifiableList(singletonList(MavenBuiltInBindings.DEPLOY_DEPLOY))),
        newTuple(MavenPhase.SITE, unmodifiableList(singletonList(MavenBuiltInBindings.SITE_SITE))),
        newTuple(MavenPhase.SITE_DEPLOY, unmodifiableList(singletonList(MavenBuiltInBindings.SITE_DEPLOY)))
    )),
    JAR("jar", asList(
        newTuple(MavenPhase.CLEAN, unmodifiableList(singletonList(MavenBuiltInBindings.CLEAN_CLEAN))),
        newTuple(MavenPhase.PROCESS_RESOURCES, unmodifiableList(singletonList(MavenBuiltInBindings.RESOURCES_RESOURCES))),
        newTuple(MavenPhase.COMPILE, unmodifiableList(singletonList(MavenBuiltInBindings.COMPILER_COMPILE))),
        newTuple(MavenPhase.PROCESS_TEST_RESOURCES, unmodifiableList(singletonList(MavenBuiltInBindings.RESOURCES_TESTRESOURCES))),
        newTuple(MavenPhase.TEST_COMPILE, unmodifiableList(singletonList(MavenBuiltInBindings.COMPILER_TESTCOMPILE))),
        newTuple(MavenPhase.TEST, unmodifiableList(singletonList(MavenBuiltInBindings.SUREFIRE_TEST))),
        newTuple(MavenPhase.PACKAGE, unmodifiableList(singletonList("jar:jar"))),
        newTuple(MavenPhase.INSTALL, unmodifiableList(singletonList(MavenBuiltInBindings.INSTALL_INSTALL))),
        newTuple(MavenPhase.DEPLOY, unmodifiableList(singletonList(MavenBuiltInBindings.DEPLOY_DEPLOY)))
    )),
    PAR("par", asList(
        newTuple(MavenPhase.CLEAN, unmodifiableList(singletonList(MavenBuiltInBindings.CLEAN_CLEAN))),
        newTuple(MavenPhase.PROCESS_RESOURCES, unmodifiableList(singletonList(MavenBuiltInBindings.RESOURCES_RESOURCES))),
        newTuple(MavenPhase.COMPILE, unmodifiableList(singletonList(MavenBuiltInBindings.COMPILER_COMPILE))),
        newTuple(MavenPhase.PROCESS_TEST_RESOURCES, unmodifiableList(singletonList(MavenBuiltInBindings.RESOURCES_TESTRESOURCES))),
        newTuple(MavenPhase.TEST_COMPILE, unmodifiableList(singletonList(MavenBuiltInBindings.COMPILER_TESTCOMPILE))),
        newTuple(MavenPhase.TEST, unmodifiableList(singletonList(MavenBuiltInBindings.SUREFIRE_TEST))),
        newTuple(MavenPhase.PACKAGE, unmodifiableList(singletonList("par:par"))),
        newTuple(MavenPhase.INSTALL, unmodifiableList(singletonList(MavenBuiltInBindings.INSTALL_INSTALL))),
        newTuple(MavenPhase.DEPLOY, unmodifiableList(singletonList(MavenBuiltInBindings.DEPLOY_DEPLOY))),
        newTuple(MavenPhase.SITE, unmodifiableList(singletonList(MavenBuiltInBindings.SITE_SITE))),
        newTuple(MavenPhase.SITE_DEPLOY, unmodifiableList(singletonList(MavenBuiltInBindings.SITE_DEPLOY)))
    )),
    RAR("rar", asList(
        newTuple(MavenPhase.CLEAN, unmodifiableList(singletonList(MavenBuiltInBindings.CLEAN_CLEAN))),
        newTuple(MavenPhase.PROCESS_RESOURCES, unmodifiableList(singletonList(MavenBuiltInBindings.RESOURCES_RESOURCES))),
        newTuple(MavenPhase.COMPILE, unmodifiableList(singletonList(MavenBuiltInBindings.COMPILER_COMPILE))),
        newTuple(MavenPhase.PROCESS_TEST_RESOURCES, unmodifiableList(singletonList(MavenBuiltInBindings.RESOURCES_TESTRESOURCES))),
        newTuple(MavenPhase.TEST_COMPILE, unmodifiableList(singletonList(MavenBuiltInBindings.COMPILER_TESTCOMPILE))),
        newTuple(MavenPhase.TEST, unmodifiableList(singletonList(MavenBuiltInBindings.SUREFIRE_TEST))),
        newTuple(MavenPhase.PACKAGE, unmodifiableList(singletonList("rar:rar"))),
        newTuple(MavenPhase.INSTALL, unmodifiableList(singletonList(MavenBuiltInBindings.INSTALL_INSTALL))),
        newTuple(MavenPhase.DEPLOY, unmodifiableList(singletonList(MavenBuiltInBindings.DEPLOY_DEPLOY))),
        newTuple(MavenPhase.SITE, unmodifiableList(singletonList(MavenBuiltInBindings.SITE_SITE))),
        newTuple(MavenPhase.SITE_DEPLOY, unmodifiableList(singletonList(MavenBuiltInBindings.SITE_DEPLOY)))
    )),
    WAR("war", asList(
        newTuple(MavenPhase.CLEAN, unmodifiableList(singletonList(MavenBuiltInBindings.CLEAN_CLEAN))),
        newTuple(MavenPhase.PROCESS_RESOURCES, unmodifiableList(singletonList(MavenBuiltInBindings.RESOURCES_RESOURCES))),
        newTuple(MavenPhase.COMPILE, unmodifiableList(singletonList(MavenBuiltInBindings.COMPILER_COMPILE))),
        newTuple(MavenPhase.PROCESS_TEST_RESOURCES, unmodifiableList(singletonList(MavenBuiltInBindings.RESOURCES_TESTRESOURCES))),
        newTuple(MavenPhase.TEST_COMPILE, unmodifiableList(singletonList(MavenBuiltInBindings.COMPILER_TESTCOMPILE))),
        newTuple(MavenPhase.TEST, unmodifiableList(singletonList(MavenBuiltInBindings.SUREFIRE_TEST))),
        newTuple(MavenPhase.PACKAGE, unmodifiableList(singletonList("war:war"))),
        newTuple(MavenPhase.INSTALL, unmodifiableList(singletonList(MavenBuiltInBindings.INSTALL_INSTALL))),
        newTuple(MavenPhase.DEPLOY, unmodifiableList(singletonList(MavenBuiltInBindings.DEPLOY_DEPLOY))),
        newTuple(MavenPhase.SITE, unmodifiableList(singletonList(MavenBuiltInBindings.SITE_SITE))),
        newTuple(MavenPhase.SITE_DEPLOY, unmodifiableList(singletonList(MavenBuiltInBindings.SITE_DEPLOY)))
    )),
    EAR("ear", asList(
        newTuple(MavenPhase.CLEAN, unmodifiableList(singletonList(MavenBuiltInBindings.CLEAN_CLEAN))),
        newTuple(MavenPhase.GENERATE_RESOURCES, unmodifiableList(singletonList("ear:generate-application-xml"))),
        newTuple(MavenPhase.PROCESS_RESOURCES, unmodifiableList(singletonList(MavenBuiltInBindings.RESOURCES_RESOURCES))),
        newTuple(MavenPhase.PACKAGE, unmodifiableList(singletonList("ear:ear"))),
        newTuple(MavenPhase.INSTALL, unmodifiableList(singletonList(MavenBuiltInBindings.INSTALL_INSTALL))),
        newTuple(MavenPhase.DEPLOY, unmodifiableList(singletonList(MavenBuiltInBindings.DEPLOY_DEPLOY))),
        newTuple(MavenPhase.SITE, unmodifiableList(singletonList(MavenBuiltInBindings.SITE_SITE))),
        newTuple(MavenPhase.SITE_DEPLOY, unmodifiableList(singletonList(MavenBuiltInBindings.SITE_DEPLOY)))
    )),
    MAVEN_PLUGIN("maven-plugin", asList(

        newTuple(MavenPhase.CLEAN, unmodifiableList(singletonList(MavenBuiltInBindings.CLEAN_CLEAN))),
        newTuple(MavenPhase.GENERATE_RESOURCES, unmodifiableList(singletonList("plugin:descriptor"))),
        newTuple(MavenPhase.PROCESS_RESOURCES, unmodifiableList(singletonList(MavenBuiltInBindings.RESOURCES_RESOURCES))),
        newTuple(MavenPhase.COMPILE, unmodifiableList(singletonList(MavenBuiltInBindings.COMPILER_COMPILE))),
        newTuple(MavenPhase.PROCESS_TEST_RESOURCES, unmodifiableList(singletonList(MavenBuiltInBindings.RESOURCES_TESTRESOURCES))),
        newTuple(MavenPhase.TEST_COMPILE, unmodifiableList(singletonList(MavenBuiltInBindings.COMPILER_TESTCOMPILE))),
        newTuple(MavenPhase.TEST, unmodifiableList(singletonList(MavenBuiltInBindings.SUREFIRE_TEST))),
        newTuple(MavenPhase.PACKAGE, unmodifiableList(asList("jar:jar", "plugin:addPluginArtifactMetadata"))),
        newTuple(MavenPhase.INSTALL, unmodifiableList(singletonList(MavenBuiltInBindings.INSTALL_INSTALL))),
        newTuple(MavenPhase.DEPLOY, unmodifiableList(singletonList(MavenBuiltInBindings.DEPLOY_DEPLOY))),
        newTuple(MavenPhase.SITE, unmodifiableList(singletonList(MavenBuiltInBindings.SITE_SITE))),
        newTuple(MavenPhase.SITE_DEPLOY, unmodifiableList(singletonList(MavenBuiltInBindings.SITE_DEPLOY)))
    )),
    POM("pom", asList(
        newTuple(MavenPhase.CLEAN, unmodifiableList(singletonList(MavenBuiltInBindings.CLEAN_CLEAN))),
        newTuple(MavenPhase.PACKAGE, unmodifiableList(emptyList())),
        newTuple(MavenPhase.INSTALL, unmodifiableList(singletonList(MavenBuiltInBindings.INSTALL_INSTALL))),
        newTuple(MavenPhase.DEPLOY, unmodifiableList(singletonList(MavenBuiltInBindings.DEPLOY_DEPLOY))),
        newTuple(MavenPhase.SITE, unmodifiableList(singletonList(MavenBuiltInBindings.SITE_SITE))),
        newTuple(MavenPhase.SITE_DEPLOY, unmodifiableList(singletonList(MavenBuiltInBindings.SITE_DEPLOY)))
    ));

    private static final String CLEAN_CLEAN = "clean:clean";
    private static final String RESOURCES_RESOURCES = "resources:resources";
    private static final String COMPILER_COMPILE = "compiler:compile";
    private static final String RESOURCES_TESTRESOURCES = "resources:testResources";
    private static final String COMPILER_TESTCOMPILE = "compiler:testCompile";
    private static final String SUREFIRE_TEST = "surefire:test";
    private static final String INSTALL_INSTALL = "install:install";
    private static final String DEPLOY_DEPLOY = "deploy:deploy";
    private static final String SITE_SITE = "site:site";
    private static final String SITE_DEPLOY = "site:deploy";

    private String packaging;
    private List<Entry<MavenPhase, List<String>>> bindings;

    MavenBuiltInBindings(final String packaging, final List<Entry<MavenPhase, List<String>>> bindings) {
        this.packaging = packaging;
        this.bindings = unmodifiableList(bindings);
    }
}
