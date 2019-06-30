package top.infra.maven.extension;

import static java.lang.Boolean.TRUE;
import static org.junit.Assert.assertEquals;
import static top.infra.maven.Constants.BOOL_STRING_TRUE;
import static top.infra.maven.extension.MavenOption.GENERATEREPORTS;

import java.util.Properties;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import top.infra.maven.core.CiOptions;
import top.infra.maven.core.GitProperties;
import top.infra.maven.core.GitPropertiesFactoryBean;
import top.infra.maven.logging.Logger;
import top.infra.maven.logging.LoggerSlf4jImpl;

public class CiOptionTests {

    private static final org.slf4j.Logger slf4jLogger = LoggerFactory.getLogger(CiOptionTests.class);

    private static Logger logger() {
        return new LoggerSlf4jImpl(slf4jLogger);
    }

    private static GitProperties gitProperties() {
        final Logger logger = logger();
        return new GitPropertiesFactoryBean(logger)
            .getObjct()
            .orElseGet(GitProperties::newBlankGitProperties);
    }

    @Test
    public void testGenerateReports() {
        final Properties systemProperties = new Properties();

        final Properties userProperties = new Properties();
        userProperties.setProperty(GENERATEREPORTS.getPropertyName(), BOOL_STRING_TRUE);

        final CiOptions ciOpts = new CiOptions(
            gitProperties(),
            systemProperties,
            userProperties
        );

        slf4jLogger.info("generateReports {}", ciOpts.getOption(GENERATEREPORTS).orElse(null));
        assertEquals(TRUE.toString(), ciOpts.getOption(GENERATEREPORTS).orElse(null));

        OptionFileLoader.ciOptsFromFile(ciOpts, logger()).ifPresent(ciOpts::updateSystemProperties);

        slf4jLogger.info("generateReports {}", ciOpts.getOption(GENERATEREPORTS).orElse(null));
        assertEquals(TRUE.toString(), ciOpts.getOption(GENERATEREPORTS).orElse(null));

        ciOpts.setCiOptPropertiesInto(OptionCollections.optionCollections(), userProperties);

        slf4jLogger.info("generateReports {}", ciOpts.getOption(GENERATEREPORTS).orElse(null));
        assertEquals(TRUE.toString(), ciOpts.getOption(GENERATEREPORTS).orElse(null));
    }
}
