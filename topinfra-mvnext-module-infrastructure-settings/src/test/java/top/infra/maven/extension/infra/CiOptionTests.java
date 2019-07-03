package top.infra.maven.extension.infra;

import static java.lang.Boolean.TRUE;
import static org.junit.Assert.assertEquals;
import static top.infra.maven.Constants.BOOL_STRING_TRUE;
import static top.infra.maven.extension.MavenOption.GENERATEREPORTS;

import java.util.Properties;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import top.infra.maven.core.CiOptionContext;
import top.infra.maven.core.GitProperties;
import top.infra.maven.core.GitPropertiesBeanFactory;
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

        final CiOptionContext ciOptContext = new CiOptionContext(
            gitProperties(),
            systemProperties,
            userProperties
        );

        slf4jLogger.info("generateReports {}", GENERATEREPORTS.getValue(ciOptContext).orElse(null));
        assertEquals(TRUE.toString(), GENERATEREPORTS.getValue(ciOptContext).orElse(null));

        final String remoteOriginUrl = gitProperties().remoteOriginUrl().orElse(null);
        OptionFileLoader.ciOptContextFromFile(ciOptContext, logger(), remoteOriginUrl, false, true)
            .ifPresent(ciOptContext::updateSystemProperties);

        slf4jLogger.info("generateReports {}", GENERATEREPORTS.getValue(ciOptContext).orElse(null));
        assertEquals(TRUE.toString(), GENERATEREPORTS.getValue(ciOptContext).orElse(null));

        ciOptContext.setCiOptPropertiesInto(OptionCollections.optionCollections(), userProperties);

        slf4jLogger.info("generateReports {}", GENERATEREPORTS.getValue(ciOptContext).orElse(null));
        assertEquals(TRUE.toString(), GENERATEREPORTS.getValue(ciOptContext).orElse(null));
    }

    private static GitProperties gitProperties() {
        return new GitPropertiesBeanFactory(logger()).getObject();
    }

    private static Logger logger() {
        return new LoggerSlf4jImpl(slf4jLogger);
    }
}
