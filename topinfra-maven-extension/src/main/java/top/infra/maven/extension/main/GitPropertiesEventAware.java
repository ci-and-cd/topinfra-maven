package top.infra.maven.extension.main;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import org.apache.maven.cli.CliRequest;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import top.infra.logging.Logger;
import top.infra.maven.CiOptionContext;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.shared.extension.Orders;
import top.infra.maven.shared.extension.VcsProperties;
import top.infra.maven.shared.logging.LoggerPlexusImpl;
import top.infra.maven.shared.utils.MavenUtils;
import top.infra.maven.shared.utils.PropertiesUtils;

import static top.infra.maven.shared.extension.VcsProperties.*;
import static top.infra.maven.shared.utils.PropertiesUtils.logProperties;

@Named
@Singleton
public class GitPropertiesEventAware implements MavenEventAware {

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

    @Override
    public boolean afterInit() {
        return true;
    }

    @Override
    public void afterInit(
        final CliRequest cliRequest,
        final CiOptionContext ciOptContext
    ) {
        final Path executionRootPath = MavenUtils.executionRootPath(cliRequest);
        final Path dotGit = executionRootPath.resolve(".git");
        if (dotGit.toFile().exists()) {
            final Optional<Properties> gitProperties = newJgitProperties(this.logger);
            gitProperties.ifPresent(props -> {
                final Properties merged = new Properties();
                Stream.of(VcsProperties.values()).forEach(opt -> {
                    final String name = opt.getPropertyName();
                    final String value = MavenUtils.findInProperties(name, ciOptContext)
                        .orElseGet(() -> props.getProperty(name));
                    if (value != null) {
                        merged.setProperty(name, value);
                    }
                });

                logProperties(logger, "    gitProperties", merged, null);

                PropertiesUtils.merge(merged, ciOptContext.getSystemProperties());
                PropertiesUtils.merge(merged, ciOptContext.getUserProperties());
            });
        } else {
            logger.info(String.format("    Git repo [%s] not found. Skip initializing gitProperties.", dotGit));
        }
    }

    @Override
    public int getOrder() {
        return Orders.ORDER_GIT_PROPERTIES;
    }

    private static Optional<Properties> newJgitProperties(final Logger logger) {
        Optional<Properties> result;
        try {
            final Repository repository = new RepositoryBuilder()
                .setWorkTree(new File("."))
                .readEnvironment()
                .findGitDir()
                .setMustExist(true)
                .build();

            logger.debug("    Using git repository: " + repository.getDirectory());

            final ObjectId head = repository.resolve("HEAD");
            if (head == null) {
                logger.warn("    No such revision: HEAD");
                result = Optional.empty();
            } else {
                final Map<String, String> map = new LinkedHashMap<>();
                String refName;
                Git git = new Git(repository);
                // .git/HEAD
                // if branch: ref: refs/heads/master
                // if tag: 2d2118b814c11f509e1aa76cb07110f7231668dc
                refName = repository.getBranch();
                if (refName.equals(head.getName())) {
                    Map<ObjectId, String> namedCommits = git.nameRev().addPrefix("refs/tags/").add(head).call();
                    if (namedCommits.containsKey(head.toObjectId())) {
                        refName = namedCommits.get(head.toObjectId());
                    }
                }
                map.put(GIT_REF_NAME.getPropertyName(), refName);

                final String commitId = head.name();
                map.put(GIT_COMMIT_ID.getPropertyName(), commitId);

                final String remoteOriginUrl = repository.getConfig().getString("remote", "origin", "url");
                map.put(GIT_REMOTE_ORIGIN_URL.getPropertyName(), remoteOriginUrl);

                final Properties properties = new Properties();
                properties.putAll(map);
                result = Optional.of(properties);
            }
        } catch (final IOException | GitAPIException ex) {
            logger.warn("    Exception on newGitProperties.", ex);
            result = Optional.empty();
        }

        return result;
    }

}
