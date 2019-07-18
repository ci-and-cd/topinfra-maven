package top.infra.maven.shared;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * See "https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html".
 */
public enum MavenLifecycle {

    CLEAN("clean", Arrays.asList(
        MavenPhase.PRE_CLEAN,
        MavenPhase.CLEAN,
        MavenPhase.POST_CLEAN
    )),
    DEFAULT("default", Arrays.asList(
        MavenPhase.VALIDATE,
        MavenPhase.INITIALIZE,
        MavenPhase.GENERATE_SOURCES,
        MavenPhase.PROCESS_SOURCES,
        MavenPhase.GENERATE_RESOURCES,
        MavenPhase.PROCESS_RESOURCES,
        MavenPhase.COMPILE,
        MavenPhase.PROCESS_CLASSES,
        MavenPhase.GENERATE_TEST_SOURCES,
        MavenPhase.PROCESS_TEST_SOURCES,
        MavenPhase.GENERATE_TEST_RESOURCES,
        MavenPhase.PROCESS_TEST_RESOURCES,
        MavenPhase.TEST_COMPILE,
        MavenPhase.PROCESS_TEST_CLASSES,
        MavenPhase.TEST,
        MavenPhase.PREPARE_PACKAGE,
        MavenPhase.PACKAGE,
        MavenPhase.PRE_INTEGRATION_TEST,
        MavenPhase.INTEGRATION_TEST,
        MavenPhase.POST_INTEGRATION_TEST,
        MavenPhase.VERIFY,
        MavenPhase.INSTALL,
        MavenPhase.DEPLOY
    )),
    SITE("site", Arrays.asList(
        MavenPhase.PRE_SITE,
        MavenPhase.SITE,
        MavenPhase.POST_SITE,
        MavenPhase.SITE_DEPLOY
    ));

    private String name;
    private List<MavenPhase> phases;

    MavenLifecycle(final String name, final List<MavenPhase> phases) {
        this.name = name;
        this.phases = Collections.unmodifiableList(phases);
    }
}
