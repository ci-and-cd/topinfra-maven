package top.infra.maven.extension.mavenbuild;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static top.infra.maven.extension.mavenbuild.JavaVersionActivator.isJavaVersionRelatedProfile;
import static top.infra.maven.extension.mavenbuild.JavaVersionActivator.profileJavaVersion;
import static top.infra.maven.utils.SystemUtils.parseJavaVersion;

import org.junit.Test;

public class JavaVersionAndProfileIdTest {

    @Test
    public void testIsJavaVersionRelatedProfile() {
        assertTrue(isJavaVersionRelatedProfile("java8"));
        assertTrue(isJavaVersionRelatedProfile("java-8"));
        assertTrue(isJavaVersionRelatedProfile("java9"));
        assertTrue(isJavaVersionRelatedProfile("java-9"));
        assertTrue(isJavaVersionRelatedProfile("java10"));
        assertTrue(isJavaVersionRelatedProfile("java-10"));
        assertTrue(isJavaVersionRelatedProfile("java-10-without-groovy"));
        assertTrue(isJavaVersionRelatedProfile("java-10without-groovy"));
        assertTrue(isJavaVersionRelatedProfile("java10-without-groovy"));
        assertTrue(isJavaVersionRelatedProfile("java10without-groovy"));
    }

    @Test
    public void testProfileJavaVersion() {
        assertEquals(Integer.valueOf(8), profileJavaVersion("java8").orElse(null));
        assertEquals(Integer.valueOf(8), profileJavaVersion("java-8").orElse(null));
        assertEquals(Integer.valueOf(10), profileJavaVersion("java-10-without-groovy").orElse(null));
        assertEquals(Integer.valueOf(10), profileJavaVersion("java-10without-groovy").orElse(null));
        assertEquals(Integer.valueOf(10), profileJavaVersion("java10-without-groovy").orElse(null));
        assertEquals(Integer.valueOf(10), profileJavaVersion("java10without-groovy").orElse(null));
    }

    @Test
    public void testProjectJavaVersion() {
        assertEquals(Integer.valueOf(8), parseJavaVersion("1.8.0_201").orElse(null));
        assertEquals(Integer.valueOf(10), parseJavaVersion("10.0.1").orElse(null));
    }
}
