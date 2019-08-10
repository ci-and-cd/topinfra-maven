package top.infra.maven.shared.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import top.infra.logging.Logger;

public abstract class DownloadUtils {

    private DownloadUtils() {
    }

    public static Entry<Optional<Integer>, Optional<Exception>> download(
        final Logger logger,
        final String fromUrl,
        final Path saveToFile,
        final Map<String, String> headers,
        final int maxTry
    ) {
        // download a file only when it exists
        // see: https://stackoverflow.com/questions/921262/how-to-download-and-save-a-file-from-internet-using-java

        Exception lastException = null;
        Integer lastStatus = null;

        int count = 0;
        while (count < maxTry) {
            count++;

            String newUrl = null;
            InputStream inputStream = null;
            try {
                final URL source = new URL(fromUrl);
                final HttpURLConnection urlConnection = (HttpURLConnection) source.openConnection();
                urlConnection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(10L));
                urlConnection.setReadTimeout((int) TimeUnit.SECONDS.toMillis(20L));

                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("    HttpURLConnection header names: %s", headers != null ? headers.keySet() : null));
                }
                if (headers != null && headers.size() > 0) {
                    headers.forEach(urlConnection::setRequestProperty);
                }

                inputStream = urlConnection.getInputStream();
                final int status = urlConnection.getResponseCode();

                lastException = null;
                lastStatus = status;

                final boolean redirect = lastStatus == HttpURLConnection.HTTP_MOVED_TEMP
                    || lastStatus == HttpURLConnection.HTTP_MOVED_PERM
                    || lastStatus == HttpURLConnection.HTTP_SEE_OTHER;

                if (redirect) {
                    newUrl = urlConnection.getHeaderField("Location");
                    logger.info(String.format("    Download redirect ('%s' to '%s'). %s", fromUrl, newUrl, status));
                } else {
                    newUrl = null;
                    logger.info(String.format("    Download result ('%s' to '%s'). %s", fromUrl, saveToFile, status));
                }
            } catch (final java.io.FileNotFoundException ex) {
                logger.warn(String.format("    Download error ('%s' to '%s'). %s", fromUrl, saveToFile, "Not found"));

                lastException = null;
                lastStatus = 404;
            } catch (final java.net.SocketTimeoutException ex) {
                logger.warn(String.format("    Download timeout ('%s' to '%s'). %s", fromUrl, saveToFile, ex.getMessage()));

                lastException = ex;
                lastStatus = null;
            } catch (final Exception ex) {
                logger.warn(String.format("    Download error ('%s' to '%s'). %s", fromUrl, saveToFile, ex.getMessage()), ex);

                lastException = ex;
                lastStatus = null;
            }

            if (inputStream != null) {
                if (newUrl != null) {
                    return download(logger, newUrl, saveToFile, headers, maxTry);
                } else if (is2xxStatus(lastStatus)) {
                    final File saveToDir = saveToFile.toFile().getParentFile();
                    try {
                        if (!saveToDir.exists()) {
                            Files.createDirectories(Paths.get(saveToDir.toString()));
                        }
                    } catch (final IOException ex) {
                        return SupportFunction.newTupleOptional(lastStatus, ex);
                    }

                    try (final ReadableByteChannel rbc = Channels.newChannel(inputStream)) {
                        try (final FileOutputStream fos = new FileOutputStream(saveToFile.toFile())) {
                            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                        }
                        return SupportFunction.newTupleOptional(lastStatus, null);
                    } catch (final IOException ex) {
                        return SupportFunction.newTupleOptional(lastStatus, ex);
                    }
                } else if (lastStatus != null && !is5xxStatus(lastStatus)) {
                    return SupportFunction.newTupleOptional(lastStatus, null);
                }
            } else if (lastStatus != null && lastStatus == 404) {
                return SupportFunction.newTupleOptional(lastStatus, null);
            }
        }

        return SupportFunction.newTupleOptional(lastStatus, lastException);
    }

    public static boolean is2xxStatus(final Integer status) {
        return status != null && status >= 200 && status < 300;
    }

    public static boolean is404Status(final Integer status) {
        return status != null && status == 404;
    }

    public static boolean is5xxStatus(final Integer status) {
        return status != null && status >= 500 && status < 600;
    }

    public static class DownloadException extends RuntimeException {

        public DownloadException(final String msg) {
            super(msg);
        }

        public DownloadException(final String msg, final Exception cause) {
            super(msg, cause);
        }

        public DownloadException(final Exception cause) {
            super(cause);
        }
    }
}
