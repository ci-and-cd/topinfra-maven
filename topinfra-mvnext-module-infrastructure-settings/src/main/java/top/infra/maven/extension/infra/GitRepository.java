package top.infra.maven.extension.infra;

import static java.lang.Boolean.FALSE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.SYNC;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static top.infra.maven.shared.extension.Constants.GIT_REF_NAME_MASTER;
import static top.infra.maven.shared.utils.FileUtils.readFile;
import static top.infra.maven.shared.utils.FileUtils.writeFile;
import static top.infra.maven.shared.utils.SupportFunction.newTuple;
import static top.infra.maven.shared.utils.SupportFunction.newTupleOptional;
import static top.infra.util.StringUtils.isEmpty;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import top.infra.logging.Logger;
import top.infra.maven.shared.utils.DownloadUtils;
import top.infra.maven.shared.utils.DownloadUtils.DownloadException;
import top.infra.maven.shared.utils.Optionals;
import top.infra.maven.shared.utils.UrlUtils;

public class GitRepository {

    private static final Pattern PATTERN_GITLAB_URL = Pattern.compile("^.+/api/v4/projects/[0-9]+/repository/.+$");

    private final Logger logger;

    private final String repo;
    private final String repoRef;
    private final String token;

    public GitRepository(
        final Logger logger,
        final String repo,
        final String repoRef,
        final String token
    ) {
        this.logger = logger;

        this.repo = repo;
        this.repoRef = repoRef != null ? repoRef : GIT_REF_NAME_MASTER;
        this.token = token;
    }

    private static Optional<String> gitRepo(
        final Properties systemProperties,
        @Nullable final String infrastructure,
        @Nullable final String remoteOriginUrl
    ) {
        // TODO urls like 'gitProperties git.remote.origin.url='https://gitlab-ci-token:token@gitlab.com/gitlab-cloud-ready/cloud-ready-parent.git''
        final Optional<String> gitPrefix;
        final Optional<String> ciProjectUrl = Optional.ofNullable(systemProperties.getProperty("env.CI_PROJECT_URL"));
        if (ciProjectUrl.isPresent()) {
            gitPrefix = ciProjectUrl.map(url -> UrlUtils.urlWithoutPath(url).orElse(null));
        } else {
            gitPrefix = Optional.ofNullable(remoteOriginUrl)
                .map(url -> url.startsWith("http")
                    ? UrlUtils.urlWithoutPath(url).orElse(null)
                    : UrlUtils.domainOrHostFromUrl(url).map(value -> "http://" + value).orElse(null));
        }

        return Optional.ofNullable(infrastructure).map(infra -> {
            final String repoOwner = "ci-and-cd";
            final String repoName = String.format("maven-build-opts-%s", infra);
            return gitPrefix.map(prefix -> String.format("%s/%s/%s", prefix, repoOwner, repoName)).orElse(null);
        });
    }

    public static Optional<GitRepository> newGitRepository(
        final Logger logger,
        final Properties systemProperties,
        @Nullable final String infrastructure,
        @Nullable final String repo,
        @Nullable final String repoRef,
        @Nullable final String remoteOriginUrl,
        @Nullable final String token
    ) {
        // prefix of git service url (infrastructure specific), i.e. https://github.com
        return Optionals.or(Arrays.asList(
            Optional.ofNullable(repo),
            gitRepo(systemProperties, infrastructure, remoteOriginUrl)
        ))
            .map(value -> new GitRepository(
                logger,
                value,
                repoRef,
                token
            ));
    }

    public boolean download(
        final String sourceFile,
        final Path targetFile,
        final boolean exceptionOnError,
        final boolean offline,
        final boolean update
    ) {
        final boolean doDownload;

        final boolean targetFileExists = targetFile.toFile().exists();
        if (update) {
            doDownload = true;
        } else {
            if (!targetFileExists) {
                doDownload = !offline;
            } else {
                doDownload = false;
            }
        }

        final boolean ok;
        if (doDownload) {
            ok = this.download(sourceFile, targetFile, exceptionOnError);
        } else {
            if (targetFileExists) {
                logger.info(String.format(
                    "    Local target file [%s] already exists, skip download unless option '-U' is used.", targetFile));
            } else {
                final String errorMsg = String.format(
                    "    Local target file [%s] does not exists and option '-o' (offline mode) is used.", targetFile);
                logger.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }
            ok = true;
        }
        return ok;
    }

    /**
     * Download sourceFile from git repository.
     * Throws RuntimeException on error.
     *
     * @param sourceFile       relative path in git repository
     * @param targetFile       target local file
     * @param exceptionOnError throw exception on download error or not found
     * @return ok
     */
    public boolean download(
        final String sourceFile,
        final Path targetFile,
        final boolean exceptionOnError
    ) {
        if (logger.isInfoEnabled()) {
            logger.info(String.format("    Download from [%s] to [%s]", sourceFile, targetFile));
        }

        final Entry<Optional<String>, Entry<Optional<Integer>, Optional<Exception>>> result = this.downloadAndDecode(
            sourceFile, targetFile);

        final Optional<Integer> status = result.getValue().getKey();
        final Optional<Exception> error = result.getValue().getValue();
        final boolean is2xxStatus = status.map(DownloadUtils::is2xxStatus).orElse(FALSE);
        final boolean is404Status = status.map(DownloadUtils::is404Status).orElse(FALSE);

        final boolean ok;
        if (error.isPresent() || !is2xxStatus) {
            final String errorMsg;
            final RuntimeException ex;
            if (is404Status) {
                errorMsg = String.format("    Resources [%s] not found.", result.getKey().orElse(null));
                ex = new DownloadException(errorMsg);
            } else {
                errorMsg = String.format(
                    "    Download error. From [%s], to [%s], error [%s].",
                    result.getKey().orElse(null),
                    targetFile,
                    error.map(Throwable::getMessage).orElseGet(() -> status.map(Object::toString).orElse(null))
                );
                ex = new DownloadException(errorMsg, error.orElse(null));
            }

            if (exceptionOnError) {
                logger.error(errorMsg);
                throw ex;
            } else {
                logger.warn(errorMsg);
            }
            ok = false;
        } else {
            ok = true;
        }
        return ok;
    }

    /**
     * Download sourceFile from git repository.
     *
     * @param sourceFile relative path in git repository
     * @param targetFile target local file
     * @return tuple(url, tuple ( status, exception))
     */
    private Entry<Optional<String>, Entry<Optional<Integer>, Optional<Exception>>> downloadAndDecode(
        final String sourceFile,
        final Path targetFile
    ) {
        final Entry<Optional<Integer>, Optional<Exception>> statusOrException;

        final String fromUrl;

        if (!isEmpty(this.repo)) {
            final Map<String, String> headers = new LinkedHashMap<>();
            if (!isEmpty(this.token)) {
                if (this.repo.contains("raw.githubusercontent.com") || this.repo.contains("github.com")) {
                    headers.put("Authorization", "token " + this.token); // github
                    logger.info("    token send in header 'Authorization'.");
                } else {
                    headers.put("PRIVATE-TOKEN", this.token); // gitlab
                    logger.info("    token send in header 'PRIVATE-TOKEN'.");
                }
            } else {
                logger.info("    token absent.");
            }

            final String sourceFilePath = sourceFile.startsWith("/") ? sourceFile.substring(1) : sourceFile;

            final String urlPrefix = this.repo.endsWith("/") ? this.repo : this.repo + "/";
            final Optional<Integer> status;
            if (PATTERN_GITLAB_URL.matcher(this.repo).matches()) {
                fromUrl = urlPrefix + sourceFilePath.replaceAll("/", "%2F") + "?ref=" + this.repoRef;
                final Path saveToFile = targetFile.resolveSibling(targetFile.getFileName() + ".json");
                statusOrException = DownloadUtils.download(logger, fromUrl, saveToFile, headers, 3);
                status = statusOrException.getKey();

                final boolean is2xxStatus = status.map(DownloadUtils::is2xxStatus).orElse(FALSE);
                if (is2xxStatus) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format("    decode [%s]", saveToFile));
                    }
                    // cat "${target_file}.json" | jq -r ".content" | base64 --decode | tee "${target_file}"
                    final JSONObject jsonFile = new JSONObject(readFile(saveToFile, UTF_8).orElse("{\"content\": \"\"}"));
                    final String content = jsonFile.getString("content");
                    if (!isEmpty(content)) {
                        final byte[] bytes = Base64.getDecoder().decode(content);
                        if (logger.isDebugEnabled()) {
                            logger.debug(String.format("    Write content into targetFile [%s] (%s bytes)", targetFile, bytes.length));
                        }
                        writeFile(targetFile, bytes, CREATE, SYNC, TRUNCATE_EXISTING);
                    } else {
                        if (logger.isDebugEnabled()) {
                            logger.debug(String.format("    Content is empty. Skip write content into targetFile [%s]", targetFile));
                        }
                    }
                }
            } else {
                final String path = this.repo.contains("raw.githubusercontent.com")
                    ? this.repoRef + "/" + sourceFilePath
                    : "raw/" + this.repoRef + "/" + sourceFilePath;
                fromUrl = urlPrefix + path;
                statusOrException = DownloadUtils.download(logger, fromUrl, targetFile, headers, 3);
                status = statusOrException.getKey();
            }

            final boolean hasError = statusOrException.getValue().isPresent();
            if (hasError) {
                if (status.isPresent()) {
                    logger.warn(String.format("    Error download %s.", targetFile), statusOrException.getValue().orElse(null));
                } else {
                    if (logger.isWarnEnabled()) {
                        logger.warn(String.format("    Can not download %s.", targetFile));
                        logger.warn(String.format(
                            "    Please make sure: 1. Resources exists 2. You have permission to access resources and %s is set.",
                            InfraOption.GIT_AUTH_TOKEN.getEnvVariableName())
                        );
                    }
                }
            }
        } else {
            fromUrl = null;
            statusOrException = newTupleOptional(null, null);
        }

        return newTuple(Optional.ofNullable(fromUrl), statusOrException);
    }
}
