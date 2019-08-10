package top.infra.filesafe;

import java.nio.file.Path;

import top.infra.logging.Logger;

public abstract class FileSafe {

    private FileSafe() {
    }

    public static EncryptedFile decryptByGpg(
        final Logger logger,
        final Path path,
        final String gpgExecutable
    ) {
        return new EncryptedFileGpg(logger, path, gpgExecutable);
    }

    public static EncryptedFile decryptByJava(
        final Logger logger,
        final Path path
    ) {
        return new EncryptedFileJava(logger, path);
    }

    public static EncryptedFile decryptByOpenssl(
        final Logger logger,
        final Path path
    ) {
        return new EncryptedFileOpenssl(logger, path);
    }

    public static ClearFile encryptByGpg(
        final Logger logger,
        final Path path,
        final String gpgExecutable
    ) {
        return new ClearFileGpg(logger, path, gpgExecutable);
    }

    public static ClearFile encryptByJava(
        final Logger logger,
        final Path path
    ) {
        return new ClearFileJava(logger, path);
    }

    public static ClearFile encryptByOpenssl(
        final Logger logger,
        final Path path
    ) {
        return new ClearFileOpenssl(logger, path);
    }
}
