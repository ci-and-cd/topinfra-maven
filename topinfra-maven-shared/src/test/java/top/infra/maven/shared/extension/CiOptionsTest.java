package top.infra.maven.shared.extension;

import static org.junit.Assert.assertEquals;
import static top.infra.maven.shared.extension.CiOptions.systemPropertyName;

import org.junit.Test;

public class CiOptionsTest {

    @Test
    public void testSystemPropertyName() {
        assertEquals("env.CI_OPT_OSSRH_NEXUS2_USER", systemPropertyName("CI_OPT_OSSRH_NEXUS2_USER"));
        assertEquals("env.CI_OPT_OSSRH_NEXUS2_USER", systemPropertyName("ossrh.nexus2.user"));
    }
}
