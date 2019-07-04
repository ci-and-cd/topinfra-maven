package top.infra.maven.extension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static top.infra.maven.extension.VcsProperties.PATTERN_GIT_REPO_SLUG;
import static top.infra.maven.extension.VcsProperties.gitRepoSlugFromUrl;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitRepoSlugTest {

    private static final Logger logger = LoggerFactory.getLogger(GitRepoSlugTest.class);

    @Test
    public void testRepoSlug() {
        final String mavenBuildInfraTopSsh = "git@repo.infra.top:ci-and-cd/maven-build.git";
        final String mavenBuildGitHubSsh = "git@github.com:ci-and-cd/maven-build";
        final String mavenBuildGitLabSsh = "git@gitlab.com:ci-and-cd/maven-build.git";

        final String jenkinsPipelineUnitHttps = "https://github.com/jenkinsci/JenkinsPipelineUnit.git";

        final String repoInGitLabSubGroupSsh = "git@repo.infra.top:group/sub-group/internal-repo.git";
        final String repoInGitLabSubGroupHttps = "https://repo.infra.top/path/group/sub-group/internal-repo.git";

        logger.info("url: [{}], slug: [{}]", mavenBuildInfraTopSsh, gitRepoSlugFromUrl(mavenBuildInfraTopSsh).orElse(null));
        assertTrue(PATTERN_GIT_REPO_SLUG.matcher(mavenBuildInfraTopSsh).matches());
        assertEquals("ci-and-cd/maven-build", gitRepoSlugFromUrl(mavenBuildInfraTopSsh).orElse(null));

        logger.info("url: [{}], slug: [{}]", mavenBuildGitHubSsh, gitRepoSlugFromUrl(mavenBuildGitHubSsh).orElse(null));
        assertTrue(PATTERN_GIT_REPO_SLUG.matcher(mavenBuildGitHubSsh).matches());
        assertEquals("ci-and-cd/maven-build", gitRepoSlugFromUrl(mavenBuildGitHubSsh).orElse(null));

        logger.info("url: [{}], slug: [{}]", mavenBuildGitLabSsh, gitRepoSlugFromUrl(mavenBuildGitLabSsh).orElse(null));
        assertTrue(PATTERN_GIT_REPO_SLUG.matcher(mavenBuildGitLabSsh).matches());
        assertEquals("ci-and-cd/maven-build", gitRepoSlugFromUrl(mavenBuildGitLabSsh).orElse(null));

        logger.info("url: [{}], slug: [{}]", jenkinsPipelineUnitHttps, gitRepoSlugFromUrl(jenkinsPipelineUnitHttps).orElse(null));
        assertTrue(PATTERN_GIT_REPO_SLUG.matcher(jenkinsPipelineUnitHttps).matches());
        assertEquals("jenkinsci/JenkinsPipelineUnit", gitRepoSlugFromUrl(jenkinsPipelineUnitHttps).orElse(null));

        logger.info("url: [{}], slug: [{}]", repoInGitLabSubGroupSsh, gitRepoSlugFromUrl(repoInGitLabSubGroupSsh).orElse(null));
        assertTrue(PATTERN_GIT_REPO_SLUG.matcher(repoInGitLabSubGroupSsh).matches());
        assertEquals("sub-group/internal-repo", gitRepoSlugFromUrl(repoInGitLabSubGroupSsh).orElse(null));

        logger.info("url: [{}], slug: [{}]", repoInGitLabSubGroupHttps, gitRepoSlugFromUrl(repoInGitLabSubGroupHttps).orElse(null));
        assertTrue(PATTERN_GIT_REPO_SLUG.matcher(repoInGitLabSubGroupHttps).matches());
        assertEquals("sub-group/internal-repo", gitRepoSlugFromUrl(repoInGitLabSubGroupHttps).orElse(null));
    }
}
