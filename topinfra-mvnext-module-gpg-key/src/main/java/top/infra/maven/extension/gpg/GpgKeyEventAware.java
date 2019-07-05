package top.infra.maven.extension.gpg;

import static top.infra.maven.extension.gpg.GpgOption.GPG_EXECUTABLE;
import static top.infra.maven.extension.gpg.GpgOption.GPG_KEYID;
import static top.infra.maven.extension.gpg.GpgOption.GPG_KEYNAME;
import static top.infra.maven.extension.gpg.GpgOption.GPG_PASSPHRASE;

import java.nio.file.Paths;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.cli.CliRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.project.ProjectBuildingRequest;

import top.infra.maven.CiOptionContext;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.extension.shared.Orders;
import top.infra.maven.logging.Logger;
import top.infra.maven.logging.LoggerPlexusImpl;
import top.infra.maven.utils.MavenUtils;
import top.infra.maven.utils.SystemUtils;

@Named
@Singleton
public class GpgKeyEventAware implements MavenEventAware {

    private Logger logger;

    @Inject
    public GpgKeyEventAware(final org.codehaus.plexus.logging.Logger logger) {
        this.logger = new LoggerPlexusImpl(logger);
    }

    @Override
    public boolean onProjectBuildingRequest() {
        return true;
    }

    @Override
    public void onProjectBuildingRequest(
        final CliRequest cliRequest,
        final MavenExecutionRequest mavenExecution,
        final ProjectBuildingRequest projectBuilding,
        final CiOptionContext ciOptContext
    ) {
        logger.info("    >>>>>>>>>> ---------- decrypt files and handle keys ---------- >>>>>>>>>>");
        final Optional<String> executable = GPG_EXECUTABLE.getValue(ciOptContext);
        if (executable.isPresent()) {
            final Optional<String> gpgKeyid = GPG_KEYID.getValue(ciOptContext);
            final String gpgKeyname = GPG_KEYNAME.getValue(ciOptContext).orElse("");
            final Optional<String> gpgPassphrase = GPG_PASSPHRASE.getValue(ciOptContext);
            final Gpg gpg = new Gpg(
                logger,
                Paths.get(SystemUtils.systemUserHome()),
                MavenUtils.executionRootPath(cliRequest, ciOptContext),
                executable.get(),
                gpgKeyid.orElse(null),
                gpgKeyname,
                gpgPassphrase.orElse(null)
            );
            gpg.decryptAndImportKeys();
        } else {
            logger.warn("Both gpg and gpg2 are not found.");
        }
        logger.info("    <<<<<<<<<< ---------- decrypt files and handle keys ---------- <<<<<<<<<<");
    }
    
    @Override
    public int getOrder() {
        return Orders.EVENT_AWARE_ORDER_GPG_KEY;
    }
}
