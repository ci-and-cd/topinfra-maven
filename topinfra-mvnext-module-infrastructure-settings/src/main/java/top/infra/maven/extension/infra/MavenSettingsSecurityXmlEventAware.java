package top.infra.maven.extension.infra;

import static top.infra.maven.shared.extension.Constants.PROP_SETTINGS_SECURITY;
import static top.infra.maven.shared.extension.Constants.SETTINGS_SECURITY_XML;
import static top.infra.maven.shared.extension.GlobalOption.MASTER_PASSWORD;

import cn.home1.tools.maven.MavenSettingsSecurity;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.cli.CommandLine;
import org.apache.maven.cli.CliRequest;

import top.infra.exception.RuntimeIOException;
import top.infra.logging.Logger;
import top.infra.maven.CiOptionContext;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.shared.extension.Constants;
import top.infra.maven.shared.extension.GlobalOption;
import top.infra.maven.shared.extension.Orders;
import top.infra.maven.shared.logging.LoggerPlexusImpl;
import top.infra.maven.shared.utils.MavenUtils;

@Named
@Singleton
public class MavenSettingsSecurityXmlEventAware implements MavenEventAware {

    static final String SRC_MAIN_MAVEN = "src/main/maven";

    private final Logger logger;
    private final CacheSettingsResourcesFactory resourcesFactory;

    private Path settingsSecurityXml;

    @Inject
    public MavenSettingsSecurityXmlEventAware(
        final org.codehaus.plexus.logging.Logger logger,
        final CacheSettingsResourcesFactory resourcesFactory
    ) {
        this.logger = new LoggerPlexusImpl(logger);
        this.resourcesFactory = resourcesFactory;

        this.settingsSecurityXml = null;
    }

    @Override
    public int getOrder() {
        return Orders.EVENT_AWARE_ORDER_MAVEN_SETTINGS_SECURITY_XML;
    }

    @Override
    public boolean afterInit() {
        return true;
    }

    @Override
    public void afterInit(
        final CliRequest cliRequest,
        final CiOptionContext ciOptContext
    ) {
        final Optional<String> masterPassword = MASTER_PASSWORD
            .findInProperties(MASTER_PASSWORD.getPropertyName(), ciOptContext)
            .map(MavenSettingsSecurity::surroundByBrackets);
        if (masterPassword.isPresent()) {
            try {
                final Path tmpFile = Files.createTempFile("settings-security", ".xml");
                logger.info(String.format("    Master password found, generate temp settings-security.xml [%s]", tmpFile));
                try (PrintWriter writer = new PrintWriter(tmpFile.toFile())) {
                    writer.println("<settingsSecurity>");
                    writer.println(String.format("  <master>%s</master>", masterPassword.get()));
                    writer.println("</settingsSecurity>");
                }
                this.settingsSecurityXml = tmpFile;
            } catch (final IOException ex) {
                throw new RuntimeIOException(ex);
            }
        } else {
            this.findOrDownload(cliRequest.getCommandLine());
        }

        if (this.settingsSecurityXml != null) {
            if (logger.isInfoEnabled()) {
                logger.info(String.format(
                    "    Setting file [%s], using [%s]. (override system property [%s])",
                    SETTINGS_SECURITY_XML,
                    this.settingsSecurityXml,
                    cliRequest.getSystemProperties().getProperty(PROP_SETTINGS_SECURITY)));
            }

            final String xmlPath = this.settingsSecurityXml.toAbsolutePath().toString();
            cliRequest.getSystemProperties().setProperty(PROP_SETTINGS_SECURITY, xmlPath);
            ciOptContext.getSystemProperties().setProperty(PROP_SETTINGS_SECURITY, xmlPath);

            final String systemPropertyName = GlobalOption.FAST.systemPropertyName(PROP_SETTINGS_SECURITY);
            cliRequest.getSystemProperties().setProperty(systemPropertyName, xmlPath);
            ciOptContext.getSystemProperties().setProperty(systemPropertyName, xmlPath);
        }
    }

    private void findOrDownload(final CommandLine commandLine) {
        final Resources resources = this.resourcesFactory.getObject();
        this.settingsSecurityXml = resources.findOrDownload(
            commandLine,
            true,
            Constants.PROP_SETTINGS_SECURITY,
            SRC_MAIN_MAVEN + "/" + SETTINGS_SECURITY_XML,
            SETTINGS_SECURITY_XML,
            userHomeDotM2(SETTINGS_SECURITY_XML),
            SRC_MAIN_MAVEN.replace("/", File.separator) + File.separator + SETTINGS_SECURITY_XML
        ).orElse(null);
    }

    static String userHomeDotM2(final String filename) {
        final Path path = MavenUtils.userHomeDotM2().resolve(filename);
        return path.toFile().exists() ? path.toString() : null;
    }
}
