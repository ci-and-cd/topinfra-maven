package top.infra.filesafe;

import java.nio.file.Path;
import java.security.Security;

import top.infra.logging.Logger;

public abstract class FileSafe {

    static {
        // Install the BC provider
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    private FileSafe() {
    }

    public static EncryptedFile decryptByBcpg(
        final Logger logger,
        final Path path
    ) {
        return new EncryptedFileGpgJava(logger, path);
    }

    public static EncryptedFile decryptByNativeGpg(
        final Logger logger,
        final Path path,
        final String gpgExecutable
    ) {
        return new EncryptedFileGpgNative(logger, path, gpgExecutable);
    }

    public static EncryptedFile decryptByJavaOpenssl(
        final Logger logger,
        final Path path
    ) {
        return new EncryptedFileOpensslJava(logger, path);
    }

    public static EncryptedFile decryptByNativeOpenssl(
        final Logger logger,
        final Path path
    ) {
        return new EncryptedFileOpensslNative(logger, path);
    }

    public static ClearFile encryptByBcpg(
        final Logger logger,
        final Path path
    ) {
        return new ClearFileGpgJava(logger, path);
    }

    public static ClearFile encryptByNativeGpg(
        final Logger logger,
        final Path path,
        final String gpgExecutable
    ) {
        return new ClearFileGpgNative(logger, path, gpgExecutable);
    }

    public static ClearFile encryptByJavaOpenssl(
        final Logger logger,
        final Path path
    ) {
        return new ClearFileOpensslJava(logger, path);
    }

    public static ClearFile encryptByNativeOpenssl(
        final Logger logger,
        final Path path
    ) {
        return new ClearFileOpensslNative(logger, path);
    }
}
