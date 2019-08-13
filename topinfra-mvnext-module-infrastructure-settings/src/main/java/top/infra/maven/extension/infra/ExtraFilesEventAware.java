package top.infra.maven.extension.infra;

import static top.infra.maven.extension.infra.InfraOption.DEPLOYKEY;
import static top.infra.maven.extension.infra.InfraOption.DEPLOYKEY_PASSPHRASE;

import java.nio.file.Path;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.cli.CliRequest;
import org.apache.maven.settings.building.SettingsBuildingRequest;

import top.infra.filesafe.EncryptedFile;
import top.infra.filesafe.FileSafe;
import top.infra.logging.Logger;
import top.infra.maven.CiOptionContext;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.shared.extension.Orders;
import top.infra.maven.shared.logging.LoggerPlexusImpl;
import top.infra.util.StringUtils;

@Named
@Singleton
public class ExtraFilesEventAware implements MavenEventAware {

    private static final String DEPLOY_KEY_ENC = "deploy_key.enc";
    private static final String DEPLOY_KEY_GPG = "deploy_key.gpg";

    private final Logger logger;
    private final CacheSettingsResourcesFactory resourcesFactory;

    private Path deployKey;

    @Inject
    public ExtraFilesEventAware(
        final org.codehaus.plexus.logging.Logger logger,
        final CacheSettingsResourcesFactory resourcesFactory
    ) {
        this.logger = new LoggerPlexusImpl(logger);
        this.resourcesFactory = resourcesFactory;
    }

    @Override
    public int getOrder() {
        return Orders.EVENT_AWARE_ORDER_EXTRA_FILES;
    }

    @Override
    public boolean onSettingsBuildingRequest() {
        return true;
    }

    @Override
    public void onSettingsBuildingRequest(
        final CliRequest cliRequest,
        final SettingsBuildingRequest request,
        final CiOptionContext ciOptContext
    ) {
        final Resources resources = this.resourcesFactory.getObject();
        this.deployKey = resources.findOrDownload(
            cliRequest.getCommandLine(),
            true,
            DEPLOYKEY.getPropertyName(),
            DEPLOY_KEY_GPG, // sourceFile
            DEPLOY_KEY_GPG // filename
        ).orElse(null);

        if (this.deployKey == null) {
            this.deployKey = resources.findOrDownload(
                cliRequest.getCommandLine(),
                true,
                DEPLOYKEY.getPropertyName(),
                DEPLOY_KEY_ENC, // sourceFile
                DEPLOY_KEY_ENC // filename
            ).orElse(null);
        }

        if (this.deployKey != null) {
            final EncryptedFile encryptedFile;
            if (this.deployKey.toString().endsWith(".gpg")) {
                encryptedFile = FileSafe.decryptByBcpg(logger, this.deployKey);
            } else if (this.deployKey.toString().endsWith(".enc")) {
                encryptedFile = FileSafe.decryptByJavaOpenssl(logger, this.deployKey);
            } else {
                encryptedFile = null;
            }

            if (encryptedFile != null) {
                final String passphrase = DEPLOYKEY_PASSPHRASE.getValue(ciOptContext).orElse(null);
                if (StringUtils.isEmpty(passphrase)) {
                    throw new IllegalArgumentException(String.format("file [%s] is encrypted but passphrase absent.", this.deployKey));
                }
                final Path targetPath = encryptedFile.decrypt(passphrase);
                this.deployKey = targetPath;
            }
        }
    }
}
