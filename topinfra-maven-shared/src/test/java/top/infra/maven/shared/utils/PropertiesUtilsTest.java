package top.infra.maven.shared.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PropertiesUtilsTest {

    @Test
    public void testMaskSecrets() {
        assertEquals("pass=null", PropertiesUtils.maskSecrets("pass=null"));
        assertEquals("pass=[secure]", PropertiesUtils.maskSecrets("pass=pass"));

        assertEquals("ssh.pass=null", PropertiesUtils.maskSecrets("ssh.pass=null"));
        assertEquals("ssh_pass=[secure]", PropertiesUtils.maskSecrets("ssh_pass=pass"));

        assertEquals("keyname=null", PropertiesUtils.maskSecrets("keyname=null"));
        assertEquals("keyname=[secure]", PropertiesUtils.maskSecrets("keyname=pass"));

        assertEquals("username=null", PropertiesUtils.maskSecrets("username=null"));
        assertEquals("username=[secure]", PropertiesUtils.maskSecrets("username=pass"));
    }
}
