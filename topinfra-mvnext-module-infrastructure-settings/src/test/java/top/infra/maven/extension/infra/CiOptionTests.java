package top.infra.maven.extension.infra;

import static java.lang.Boolean.TRUE;
import static org.junit.Assert.assertEquals;
import static top.infra.maven.Constants.BOOL_STRING_TRUE;
import static top.infra.maven.extension.MavenOption.GENERATEREPORTS;
import static top.infra.maven.extension.VcsProperties.GIT_REMOTE_ORIGIN_URL;

import java.util.Properties;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import top.infra.maven.core.CiOptionContext;
import top.infra.maven.extension.OptionCollections;
import top.infra.maven.logging.Logger;
import top.infra.maven.logging.LoggerSlf4jImpl;

public class CiOptionTests {

    private static final org.slf4j.Logger slf4jLogger = LoggerFactory.getLogger(CiOptionTests.class);

    @Test
    public void testGenerateReports() {
        final Properties systemProperties = new Properties();

        final Properties userProperties = new Properties();
        userProperties.setProperty(GENERATEREPORTS.getPropertyName(), BOOL_STRING_TRUE);

        // gitProperties if needed

        final CiOptionContext ciOptContext = new CiOptionContext(
            systemProperties,
            userProperties
        );

        slf4jLogger.info("generateReports {}", GENERATEREPORTS.getValue(ciOptContext).orElse(null));
        assertEquals(TRUE.toString(), GENERATEREPORTS.getValue(ciOptContext).orElse(null));

        final String remoteOriginUrl = GIT_REMOTE_ORIGIN_URL.getValue(ciOptContext).orElse(null);
        OptionFileLoader.ciOptContextFromFile(ciOptContext, logger(), remoteOriginUrl, false, true)
            .ifPresent(ciOptContext::updateSystemProperties);

        slf4jLogger.info("generateReports {}", GENERATEREPORTS.getValue(ciOptContext).orElse(null));
        assertEquals(TRUE.toString(), GENERATEREPORTS.getValue(ciOptContext).orElse(null));

        ciOptContext.setCiOptPropertiesInto(OptionCollections.optionCollections(), userProperties);

        slf4jLogger.info("generateReports {}", GENERATEREPORTS.getValue(ciOptContext).orElse(null));
        assertEquals(TRUE.toString(), GENERATEREPORTS.getValue(ciOptContext).orElse(null));
    }

    private static Logger logger() {
        return new LoggerSlf4jImpl(slf4jLogger);
    }
}
