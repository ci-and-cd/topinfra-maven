package top.infra.maven.shared.extension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
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

        logger.info("url: [{}], slug: [{}]", mavenBuildInfraTopSsh, VcsProperties.gitRepoSlugFromUrl(mavenBuildInfraTopSsh).orElse(null));
        assertTrue(VcsProperties.PATTERN_GIT_REPO_SLUG.matcher(mavenBuildInfraTopSsh).matches());
        Assert.assertEquals("ci-and-cd/maven-build", VcsProperties.gitRepoSlugFromUrl(mavenBuildInfraTopSsh).orElse(null));

        logger.info("url: [{}], slug: [{}]", mavenBuildGitHubSsh, VcsProperties.gitRepoSlugFromUrl(mavenBuildGitHubSsh).orElse(null));
        assertTrue(VcsProperties.PATTERN_GIT_REPO_SLUG.matcher(mavenBuildGitHubSsh).matches());
        Assert.assertEquals("ci-and-cd/maven-build", VcsProperties.gitRepoSlugFromUrl(mavenBuildGitHubSsh).orElse(null));

        logger.info("url: [{}], slug: [{}]", mavenBuildGitLabSsh, VcsProperties.gitRepoSlugFromUrl(mavenBuildGitLabSsh).orElse(null));
        assertTrue(VcsProperties.PATTERN_GIT_REPO_SLUG.matcher(mavenBuildGitLabSsh).matches());
        Assert.assertEquals("ci-and-cd/maven-build", VcsProperties.gitRepoSlugFromUrl(mavenBuildGitLabSsh).orElse(null));

        logger.info("url: [{}], slug: [{}]", jenkinsPipelineUnitHttps, VcsProperties.gitRepoSlugFromUrl(jenkinsPipelineUnitHttps).orElse(null));
        assertTrue(VcsProperties.PATTERN_GIT_REPO_SLUG.matcher(jenkinsPipelineUnitHttps).matches());
        Assert.assertEquals("jenkinsci/JenkinsPipelineUnit", VcsProperties.gitRepoSlugFromUrl(jenkinsPipelineUnitHttps).orElse(null));

        logger.info("url: [{}], slug: [{}]", repoInGitLabSubGroupSsh, VcsProperties.gitRepoSlugFromUrl(repoInGitLabSubGroupSsh).orElse(null));
        assertTrue(VcsProperties.PATTERN_GIT_REPO_SLUG.matcher(repoInGitLabSubGroupSsh).matches());
        Assert.assertEquals("sub-group/internal-repo", VcsProperties.gitRepoSlugFromUrl(repoInGitLabSubGroupSsh).orElse(null));

        logger.info("url: [{}], slug: [{}]", repoInGitLabSubGroupHttps, VcsProperties.gitRepoSlugFromUrl(repoInGitLabSubGroupHttps).orElse(null));
        assertTrue(VcsProperties.PATTERN_GIT_REPO_SLUG.matcher(repoInGitLabSubGroupHttps).matches());
        Assert.assertEquals("sub-group/internal-repo", VcsProperties.gitRepoSlugFromUrl(repoInGitLabSubGroupHttps).orElse(null));
    }
}
