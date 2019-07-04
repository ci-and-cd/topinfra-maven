package top.infra.maven.extension.main;

import static org.eclipse.jgit.lib.Repository.shortenRefName;
import static top.infra.maven.extension.VcsProperties.GIT_COMMIT_ID;
import static top.infra.maven.extension.VcsProperties.GIT_REF_NAME;
import static top.infra.maven.extension.VcsProperties.GIT_REMOTE_ORIGIN_URL;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.cli.CliRequest;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;

import top.infra.maven.core.CiOptionContext;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.extension.Orders;
import top.infra.maven.extension.VcsProperties;
import top.infra.maven.logging.Logger;
import top.infra.maven.logging.LoggerPlexusImpl;
import top.infra.maven.utils.PropertiesUtils;

@Named
@Singleton
public class GitPropertiesEventAware implements MavenEventAware {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // private static final String GIT_BRANCH = "git.branch";
    private static final String GIT_BRANCH_FULL = "git.branch.full";
    // private static final String GIT_REF_NAME_FULL = "git.ref.name.full";

    private static final String GIT_PROPERTIES_LOG_FORMAT = "GitProperties %s='%s'";

    private Logger logger;

    @Inject
    public GitPropertiesEventAware(
        final org.codehaus.plexus.logging.Logger logger
    ) {
        this(new LoggerPlexusImpl(logger));
    }

    public GitPropertiesEventAware(
        final Logger logger
    ) {
        this.logger = logger;
    }

    private static String nullToEmpty(final String str) {
        return (str == null ? "" : str);
    }

    private static String getFormattedDate() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }

    @Override
    public int getOrder() {
        return Orders.ORDER_GIT_PROPERTIES;
    }

    @Override
    public void afterInit(
        final CliRequest cliRequest,
        final CiOptionContext ciOptContext
    ) {
        // TODO heck if .git file (git submodules) or .git directory exists.
        final Optional<Properties> gitProperties = newJgitProperties(this.logger);
        gitProperties.ifPresent(props -> {
            final Properties copied = new Properties();
            Stream.of(VcsProperties.values()).forEach(opt -> {
                final String name = opt.getPropertyName();
                final String value = props.getProperty(name);
                if (value != null) {
                    copied.setProperty(name, value);
                }
            });
            PropertiesUtils.merge(copied, ciOptContext.getSystemProperties());
            PropertiesUtils.merge(copied, ciOptContext.getUserProperties());
        });
    }

    private static Optional<Properties> newJgitProperties(final Logger logger) {
        Optional<Properties> result;
        try {
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
                result = Optional.empty();
            } else {
                final Map<String, String> map = new LinkedHashMap<>();

                // final String branch = repository.getBranch();
                // map.put(GIT_BRANCH, nullToEmpty(branch));
                final String fullBranch = repository.getFullBranch();
                // map.put(GIT_BRANCH_FULL, nullToEmpty(fullBranch));
                if (logger.isInfoEnabled()) {
                    // logger.info(String.format(GIT_PROPERTIES_LOG_FORMAT, GIT_BRANCH, nullToEmpty(branch)));
                    logger.info(String.format(GIT_PROPERTIES_LOG_FORMAT, GIT_BRANCH_FULL, fullBranch));
                }

                // `git symbolic-ref -q --short HEAD || git describe --tags --exact-match`
                final String refName;
                final String refNameFull;
                if (fullBranch != null) {
                    final Ref fullBranchRef = repository.exactRef(fullBranch);
                    if (fullBranchRef != null) {
                        refNameFull = fullBranchRef.getName();
                        refName = shortenRefName(refNameFull);
                    } else {
                        // refNameFull = "";
                        refName = "";
                    }
                } else {
                    final Optional<String> tag = findTag(repository, head);
                    if (tag.isPresent()) {
                        // refNameFull = tag.get();
                        refName = shortenRefName(tag.get());
                    } else {
                        // refNameFull = "";
                        refName = "";
                    }
                }
                map.put(GIT_REF_NAME.getPropertyName(), refName);
                // map.put(GIT_REF_NAME_FULL, refNameFull);
                if (logger.isInfoEnabled()) {
                    logger.info(String.format(GIT_PROPERTIES_LOG_FORMAT, GIT_REF_NAME.getPropertyName(), refName));
                    // logger.info(String.format(GIT_PROPERTIES_LOG_FORMAT, GIT_REF_NAME_FULL, refNameFull));
                }

                final String commitId = head.name();
                if (logger.isInfoEnabled()) {
                    logger.info(String.format(GIT_PROPERTIES_LOG_FORMAT, GIT_COMMIT_ID.getPropertyName(), commitId));
                }
                map.put(GIT_COMMIT_ID.getPropertyName(), commitId);

                // try (final org.eclipse.jgit.lib.ObjectReader objectReader = repository.newObjectReader()) {
                //     final String commitIdAbbrev = objectReader.abbreviate(head).name();
                //     map.put("git.commit.id.abbrev", commitIdAbbrev);
                // }

                // final org.eclipse.jgit.revwalk.RevWalk walk = new org.eclipse.jgit.revwalk.RevWalk(repository);
                // walk.setRetainBody(false);
                // final org.eclipse.jgit.revwalk.RevCommit headCommit = walk.parseCommit(head);
                // final int count = org.eclipse.jgit.revwalk.RevWalkUtils.count(walk, headCommit, null);
                // map.put("git.count", Integer.toString(count));

                // final String color = commitId.substring(0, 6);
                // map.put("git.commit.color.value", color);
                // map.put("git.build.datetime.simple", getFormattedDate());

                final String remoteOriginUrl = repository.getConfig().getString("remote", "origin", "url");
                if (logger.isInfoEnabled()) {
                    logger.info(String.format(GIT_PROPERTIES_LOG_FORMAT, GIT_REMOTE_ORIGIN_URL.getPropertyName(), remoteOriginUrl));
                }
                map.put(GIT_REMOTE_ORIGIN_URL.getPropertyName(), remoteOriginUrl);

                final Properties properties = new Properties();
                properties.putAll(map);
                result = Optional.of(properties);
            }
        } catch (final IOException ex) {
            logger.warn("Exception on newGitProperties.", ex);
            result = Optional.empty();
        }

        return result;
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
}
