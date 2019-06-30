package top.infra.maven.extension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static top.infra.maven.extension.InfrastructureActivator.profileInfrastructure;

import java.util.Optional;

import org.junit.Test;
import org.slf4j.LoggerFactory;

public class InfrastructureProfileIdTest {

    private static final org.slf4j.Logger slf4jLogger = LoggerFactory.getLogger(InfrastructureProfileIdTest.class);

    @Test
    public void testInfrastructureProfileIds() {
        this.assertProfile("ossrh", "infrastructure_ossrh");
        this.assertProfile("ossrh", "infrastructure_ossrh-github_site");
        this.assertProfile("ossrh", "infrastructure_ossrh-nexus2_staging");
        this.assertProfile("ossrh", "infrastructure_ossrh-site");

        this.assertProfile("private", "infrastructure_private");
        this.assertProfile("private", "infrastructure_private-site");

        this.assertNotInfraProfile("run-on-multi-module-root-and-sub-modules");
        this.assertNotInfraProfile("parent-java-8-profile2");
    }

    private void assertProfile(final String expected, final String id) {
        final Optional<String> profileInfrastructure = profileInfrastructure(id);
        slf4jLogger.info(profileInfrastructure.orElse(null));
        assertTrue(profileInfrastructure.isPresent());
        assertEquals(expected, profileInfrastructure.orElse(null));
    }

    private void assertNotInfraProfile(final String id) {
        final Optional<String> profileInfrastructure = profileInfrastructure(id);
        slf4jLogger.info(profileInfrastructure.orElse(null));
        assertFalse(profileInfrastructure.isPresent());
    }
}
