package top.infra.maven.utils;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.junit.Test;
import org.slf4j.LoggerFactory;

public class CommonPrefixTest {

    private static final org.slf4j.Logger slf4jLogger = LoggerFactory.getLogger(CommonPrefixTest.class);

    @Test
    public void testCommonPrefixs() {
        final List<String> names = Arrays.asList(
            "env.CI_COMMIT_REF_NAME",
            "env.CI_OPT_DOCKERFILE_USEMAVENSETTINGSFORAUTH",
            "env.HOME",
            "env.HOMEBREWRC",
            "env.NVM_BIN",
            "env.NVM_CD_FLAGS",
            "env.NVM_DIR",
            "env.PATH",
            "file.encoding",
            "file.encoding.pkg",
            "file.separator",
            "java.home",
            "java.io.tmpdir",
            "java.library.path"
        );

        final List<Entry<String, List<String>>> result = SupportFunction.commonPrefixes(names);
        slf4jLogger.info("result: {}", result);
        assertEquals("env.CI_", result.get(0).getKey());
        assertEquals(2, result.get(0).getValue().size());

        assertEquals("", result.get(1).getKey());
        assertEquals("env.HOME", result.get(1).getValue().get(0));

        assertEquals("", result.get(2).getKey());
        assertEquals("env.HOMEBREWRC", result.get(2).getValue().get(0));

        assertEquals("env.NVM_", result.get(3).getKey());
        assertEquals(3, result.get(3).getValue().size());

        assertEquals("", result.get(4).getKey());
        assertEquals("env.PATH", result.get(4).getValue().get(0));

        assertEquals("file.", result.get(5).getKey());
        assertEquals(3, result.get(5).getValue().size());

        assertEquals("java.", result.get(6).getKey());
        assertEquals(3, result.get(6).getValue().size());
    }
}
