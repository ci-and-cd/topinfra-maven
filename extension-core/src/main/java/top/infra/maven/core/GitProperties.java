package top.infra.maven.core;

import static org.eclipse.jgit.lib.Repository.shortenRefName;
import static top.infra.maven.utils.SupportFunction.isEmpty;
import static top.infra.maven.utils.SupportFunction.notEmpty;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevWalkUtils;

import top.infra.maven.logging.Logger;

public class GitProperties {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static final String GIT_COMMIT_ID = "git.commit.id";
    private static final String GIT_BRANCH = "git.branch";
    private static final String GIT_BRANCH_FULL = "git.branch.full";
    private static final String GIT_REF_NAME = "git.ref.name";
    private static final String GIT_REF_NAME_FULL = "git.ref.name.full";
    private static final String GIT_REMOTE_ORIGIN_URL = "git.remote.origin.url";
    private static final String GIT_PROPERTIES_LOG_FORMAT = "GitProperties %s='%s'";

    private final Logger logger;

    private final Map<String, String> propertiesMap;

    protected GitProperties(final Logger logger, final Map<String, String> propertiesMap) {
        this.logger = logger;

        this.propertiesMap = Collections.unmodifiableMap(propertiesMap);
    }

    public static Optional<GitProperties> newInstance(final Logger logger) {
        return gitPropertiesMap(logger).map(propertiesMap -> new GitProperties(logger, propertiesMap));
    }

    public static Optional<Map<String, String>> gitPropertiesMap(final Logger logger) {
        try {
            final Map<String, String> map = new LinkedHashMap<>();
            addProperties(logger, map);
            return Optional.of(map);
        } catch (final IOException ex) {
            logger.warn("Exception on gitPropertiesMap.", ex);
            return Optional.empty();
        }
    }

    private static void addProperties(final Logger logger, final Map<String, String> map) throws IOException {
        // final Repository repository = new FileRepositoryBuilder()
        //     .setWorkTree(new File("."))
        //     .readEnvironment()
        //     .findGitDir()
        //     .setMustExist(true)
        //     .build();

        final Repository repository = new RepositoryBuilder()
            .setWorkTree(new File("."))
            .readEnvironment()
            .findGitDir()
            .setMustExist(true)
            .build();

        logger.debug("Using git repository: " + repository.getDirectory());

        final ObjectId head = repository.resolve("HEAD");
        if (head == null) {
            logger.warn("No such revision: HEAD");
            // throw new IllegalStateException("No such revision: HEAD");
            return;
        }

        final String branch = repository.getBranch();
        if (logger.isInfoEnabled()) {
            logger.info(String.format(GIT_PROPERTIES_LOG_FORMAT, GIT_BRANCH, nullToEmpty(branch)));
        }
        map.put(GIT_BRANCH, nullToEmpty(branch));

        final String fullBranch = repository.getFullBranch();
        if (logger.isInfoEnabled()) {
            logger.info(String.format(GIT_PROPERTIES_LOG_FORMAT, GIT_BRANCH_FULL, fullBranch));
        }
        map.put(GIT_BRANCH_FULL, nullToEmpty(fullBranch));

        // `git symbolic-ref -q --short HEAD || git describe --tags --exact-match`
        final String refName;
        final String refNameFull;
        if (fullBranch != null) {
            final Ref fullBranchRef = repository.exactRef(fullBranch);
            if (fullBranchRef != null) {
                refNameFull = fullBranchRef.getName();
                refName = shortenRefName(refNameFull);
            } else {
                refNameFull = "";
                refName = "";
            }
        } else {
            final Optional<String> tag = findTag(repository, head);
            if (tag.isPresent()) {
                refNameFull = tag.get();
                refName = shortenRefName(tag.get());
            } else {
                refNameFull = "";
                refName = "";
            }
        }
        if (logger.isInfoEnabled()) {
            logger.info(String.format(GIT_PROPERTIES_LOG_FORMAT, GIT_REF_NAME, refName));
            logger.info(String.format(GIT_PROPERTIES_LOG_FORMAT, GIT_REF_NAME_FULL, refNameFull));
        }
        map.put(GIT_REF_NAME_FULL, refNameFull);
        map.put(GIT_REF_NAME, refName);

        final String commitId = head.name();
        if (logger.isInfoEnabled()) {
            logger.info(String.format(GIT_PROPERTIES_LOG_FORMAT, GIT_COMMIT_ID, commitId));
        }
        map.put(GIT_COMMIT_ID, commitId);

        try (final ObjectReader objectReader = repository.newObjectReader()) {
            final String commitIdAbbrev = objectReader.abbreviate(head).name();
            map.put("git.commit.id.abbrev", commitIdAbbrev);
        }

        final RevWalk walk = new RevWalk(repository);
        walk.setRetainBody(false);
        final RevCommit headCommit = walk.parseCommit(head);
        final int count = RevWalkUtils.count(walk, headCommit, null);
        map.put("git.count", Integer.toString(count));

        final String color = commitId.substring(0, 6);
        map.put("git.commit.color.value", color);
        map.put("git.build.datetime.simple", getFormattedDate());

        final String remoteOriginUrl = repository.getConfig().getString("remote", "origin", "url");
        if (logger.isInfoEnabled()) {
            logger.info(String.format(GIT_PROPERTIES_LOG_FORMAT, GIT_REMOTE_ORIGIN_URL, remoteOriginUrl));
        }
        map.put(GIT_REMOTE_ORIGIN_URL, remoteOriginUrl);
    }

    private static Optional<String> findTag(final Repository repository, final ObjectId head) {
        Optional<String> result;
        try {
            final List<Ref> tagList = Git.wrap(repository).tagList().call();
            final List<String> tagFound = new LinkedList<>();
            for (final Ref tag : tagList) {
                if (tag.getObjectId().equals(head)) {
                    tagFound.add(tag.getName());
                }
            }
            // tagFound.add(Git.wrap(repository).describe().setTags(true).setTarget(head).call());
            result = Optional.ofNullable(!tagFound.isEmpty() ? tagFound.get(0) : null);
        } catch (final GitAPIException ex) {
            result = Optional.empty();
        }
        return result;
    }

    private static String nullToEmpty(final String str) {
        return (str == null ? "" : str);
    }

    private static String getFormattedDate() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }

    public static GitProperties newBlankInstance(final Logger logger) {
        return new GitProperties(logger, new HashMap<>());
    }

    public Optional<String> commitId() {
        // `git rev-parse HEAD`
        final String value = this.propertiesMap.get(GIT_COMMIT_ID);
        return isEmpty(value) ? Optional.empty() : Optional.of(value);
    }

    public Optional<String> refName() {
        final String value = this.propertiesMap.get(GIT_REF_NAME);
        return notEmpty(value) ? Optional.of(value) : Optional.empty();
    }

    public Optional<String> remoteOriginUrl() {
        final String value = this.propertiesMap.get(GIT_REMOTE_ORIGIN_URL);
        return notEmpty(value) ? Optional.of(value) : Optional.empty();
    }
}
