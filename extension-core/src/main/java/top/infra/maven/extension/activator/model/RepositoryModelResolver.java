package top.infra.maven.extension.activator.model;

import static java.lang.Boolean.FALSE;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryPolicy;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;

import top.infra.maven.logging.Logger;
import top.infra.maven.logging.LoggerPlexusImpl;
import top.infra.maven.utils.DownloadUtils;
import top.infra.maven.utils.DownloadUtils.DownloadException;

/**
 * This class allows to resolve Maven artifact in order to build a Maven model
 * Inspired by code from:
 * https://github.com/rickardoberg/neomvn/
 * <p/>
 * https://github.com/Spirals-Team/repairnator/blob/master/repairnator/repairnator-pipeline/src/main/java/fr/inria/spirals/repairnator/process/maven/RepositoryModelResolver.java
 */
@Deprecated
// @Named
// @Singleton
public class RepositoryModelResolver implements ModelResolver {

    private static final String MAVEN_CENTRAL_URL = "https://repo1.maven.org/maven2";
    private static final String MAVEN_CENTRAL_URL_MIRROR_1 = "http://repo.maven.apache.org/maven2";

    private final Logger logger;

    private File localRepository;

    private Collection<Repository> repositories = new LinkedHashSet<>();

    // @Inject
    public RepositoryModelResolver(
        final org.codehaus.plexus.logging.Logger logger
    ) {
        this.logger = new LoggerPlexusImpl(logger);

        final Repository central = new Repository();
        central.setId("central");
        central.setUrl(MAVEN_CENTRAL_URL);
        this.repositories.add(central);

        final Repository mirror1 = new Repository();
        mirror1.setId("mirror1");
        mirror1.setUrl(MAVEN_CENTRAL_URL_MIRROR_1);
        this.repositories.add(mirror1);
    }

    private RepositoryModelResolver(
        final Logger logger,
        final File localRepository,
        final Collection<Repository> repositories
    ) {
        this.logger = logger;
        this.localRepository = localRepository;
        this.repositories = new LinkedHashSet<>();
        this.repositories.addAll(repositories.stream().map(Repository::clone).collect(toList()));
    }

    public void addRepositories(final List<ArtifactRepository> artifactRepositories) throws InvalidRepositoryException {
        for (final ArtifactRepository artifactRepository : artifactRepositories) {
            if (logger.isInfoEnabled()) {
                logger.info(String.format("artifactRepository %s %s",
                    artifactRepository.getId(), artifactRepository.getUrl()));
            }

            final Repository repository = new Repository();
            repository.setId(artifactRepository.getId());
            repository.setUrl(artifactRepository.getUrl());

            final RepositoryPolicy releases = new RepositoryPolicy();
            if (artifactRepository.getReleases() != null) {
                releases.setChecksumPolicy(artifactRepository.getReleases().getChecksumPolicy());
                releases.setEnabled(artifactRepository.getReleases().isEnabled());
                releases.setUpdatePolicy(artifactRepository.getReleases().getUpdatePolicy());
                repository.setReleases(releases);
            }

            final RepositoryPolicy snapshots = new RepositoryPolicy();
            if (artifactRepository.getSnapshots() != null) {
                snapshots.setChecksumPolicy(artifactRepository.getSnapshots().getChecksumPolicy());
                snapshots.setEnabled(artifactRepository.getSnapshots().isEnabled());
                snapshots.setUpdatePolicy(artifactRepository.getSnapshots().getUpdatePolicy());
                repository.setSnapshots(snapshots);
            }

            this.addRepository(repository, true);
        }
    }

    @Override
    public void addRepository(Repository repository) throws InvalidRepositoryException {
        this.addRepository(repository, false);
    }

    public void setLocalRepository(final String localRepository) {
        this.localRepository = new File(localRepository);
    }

    @Override
    public void addRepository(final Repository repository, boolean replace) throws InvalidRepositoryException {
        for (final Repository existingRepository : this.repositories) {
            if (existingRepository.getId().equals(repository.getId()) && !replace) {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("addRepository [%s] skip [%s]", this, repository));
                }
                return;
            }
        }

        if (logger.isInfoEnabled()) {
            logger.info(String.format("addRepository [%s] add [%s]", this, repository));
        }
        this.repositories.add(repository);
    }


    @Override
    public ModelResolver newCopy() {
        return new RepositoryModelResolver(logger, this.localRepository, this.repositories);
    }

    @Override
    public ModelSource resolveModel(final Dependency dependency) throws UnresolvableModelException {
        return resolveModel(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
    }

    @Override
    public ModelSource resolveModel(
        final String groupId, final String artifactId, final String version
    ) throws UnresolvableModelException {
        File pom = getLocalFile(groupId, artifactId, version);

        if (!pom.exists()) {
            try {
                this.download(pom);
            } catch (final Exception e) {
                throw new UnresolvableModelException("Could not download POM", groupId, artifactId, version, e);
            }
        }

        return new FileModelSource(pom);
    }

    @Override
    public ModelSource resolveModel(final Parent parent) throws UnresolvableModelException {
        return resolveModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
    }


    private File getLocalFile(final String groupId, final String artifactId, final String version) {
        File pom = this.localRepository;

        final String[] groupIds = groupId.split("\\.");
        // go through subdirectories
        for (final String id : groupIds) {
            pom = new File(pom, id);
        }

        return new File(new File(new File(pom, artifactId), version), artifactId + "-" + version + ".pom");
    }

    private void download(final File localRepoFile) {
        for (final Repository repo : this.repositories) {
            final String repoUrl = repo.getUrl().endsWith("/")
                ? repo.getUrl().substring(0, repo.getUrl().length() - 1)
                : repo.getUrl();

            final String filePath = localRepoFile.getAbsolutePath().substring(this.localRepository.getAbsolutePath().length());
            final String sourceUrl = repoUrl + filePath;

            final Path path = localRepoFile.toPath().normalize();
            final Entry<Optional<Integer>, Optional<Exception>> result = DownloadUtils.download(
                logger, sourceUrl, path, emptyMap(), 3);
            final Optional<Integer> status = result.getKey();
            final Optional<Exception> error = result.getValue();
            final boolean is2xxStatus = status.map(DownloadUtils::is2xxStatus).orElse(FALSE);
            if (error.isPresent()) {
                throw new DownloadException(error.get());
            } else if (!is2xxStatus) {
                throw new DownloadException(String.format("url [%s], status [%s]", sourceUrl, status.orElse(null)));
            }

            // this.httpDownload(sourceUrl, pathname(localRepoFile));
        }
    }

    // @Deprecated
    // private void httpDownload(final String sourceUrl, final String targetLocalFile) {
    //     try {
    //         final URL source = new URL(sourceUrl);
    //         final File target = new File(targetLocalFile);
    //
    //         logger.debug("Downloading " + source);
    //
    //         final okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
    //         final okhttp3.Request request = new okhttp3.Request.Builder()
    //             .url(source)
    //             .build();
    //
    //         final okhttp3.Response response = client.newCall(request).execute();
    //         if (response.code() == 200 && response.body() != null) {
    //             target.getParentFile().mkdirs();
    //             try (final FileWriter out = new FileWriter(target)) {
    //                 out.write(response.body().string());
    //                 out.flush();
    //             }
    //         }
    //     } catch (final IOException ex) {
    //         throw new RuntimeIOException(ex);
    //     }
    // }
}
