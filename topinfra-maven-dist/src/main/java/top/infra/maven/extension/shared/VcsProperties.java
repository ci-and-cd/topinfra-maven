package top.infra.maven.extension.shared;

import static top.infra.maven.extension.shared.Constants.GIT_REF_NAME_DEVELOP;
import static top.infra.maven.extension.shared.Constants.GIT_REF_PREFIX_FEATURE;
import static top.infra.maven.extension.shared.Constants.GIT_REF_PREFIX_HOTFIX;
import static top.infra.maven.extension.shared.Constants.GIT_REF_PREFIX_RELEASE;
import static top.infra.maven.extension.shared.Constants.GIT_REF_PREFIX_SUPPORT;

import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import top.infra.maven.CiOption;
import top.infra.maven.CiOptionContext;
import top.infra.maven.cienv.AppveyorVariables;
import top.infra.maven.cienv.GitlabCiVariables;
import top.infra.maven.cienv.TravisCiVariables;

public enum VcsProperties implements CiOption {

    /**
     * `git rev-parse HEAD`.
     */
    GIT_COMMIT_ID("git.commit.id"),

    /**
     * Auto detect current build ref name by CI environment variables or local git info.
     * Current build ref name, i.e. develop, release ...
     * <p>
     * travis-ci<br/>
     * TRAVIS_BRANCH for travis-ci, see: https://docs.travis-ci.com/user/environment-variables/<br/>
     * for builds triggered by a tag, this is the same as the name of the tag (TRAVIS_TAG).<br/>
     * </p>
     * <p>
     * appveyor<br/>
     * APPVEYOR_REPO_BRANCH - build branch. For Pull Request commits it is base branch PR is merging into<br/>
     * APPVEYOR_REPO_TAG - true if build has started by pushed tag; otherwise false<br/>
     * APPVEYOR_REPO_TAG_NAME - contains tag name for builds started by tag; otherwise this variable is<br/>
     * </p>
     */
    GIT_REF_NAME("git.ref.name") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            final Optional<String> result;

            final Optional<String> appveyorRefName = new AppveyorVariables(context.getSystemProperties()).refName();
            final Optional<String> gitlabCiRefName = new GitlabCiVariables(context.getSystemProperties()).refName();
            final Optional<String> travisBranch = new TravisCiVariables(context.getSystemProperties()).branch();

            if (appveyorRefName.isPresent()) {
                result = appveyorRefName;
            } else if (gitlabCiRefName.isPresent()) {
                result = gitlabCiRefName;
            } else {
                result = travisBranch;
            }

            return result;
        }
    },
    GIT_REMOTE_ORIGIN_URL("git.remote.origin.url"),
    ;

    static final Pattern PATTERN_GIT_REPO_SLUG = Pattern.compile(".*[:/]([^/]+(/[^/.]+))(\\.git)?");

    private final String defaultValue;
    private final String propertyName;

    VcsProperties(final String propertyName) {
        this(propertyName, null);
    }

    VcsProperties(final String propertyName, final String defaultValue) {
        this.defaultValue = defaultValue;
        this.propertyName = propertyName;
    }

    public static boolean isSnapshotRef(final String gitRef) {
        return gitRef != null && !gitRef.isEmpty()
            && (GIT_REF_NAME_DEVELOP.equals(gitRef)
            || gitRef.startsWith(GIT_REF_PREFIX_FEATURE));
    }

    public static boolean isReleaseRef(final String gitRef) {
        return gitRef != null && !gitRef.isEmpty()
            && (gitRef.startsWith(GIT_REF_PREFIX_HOTFIX)
            || gitRef.startsWith(GIT_REF_PREFIX_RELEASE)
            || gitRef.startsWith(GIT_REF_PREFIX_SUPPORT));
    }

    /**
     * Get slug info of current repository (directory).
     *
     * @param ciOptionContext ciOptionContext
     * @return 'group/project' or 'owner/project'
     */
    public static Optional<String> gitRepoSlug(
        final CiOptionContext ciOptionContext
    ) {
        final Properties systemProperties = ciOptionContext.getSystemProperties();
        final Optional<String> appveyorRepoSlug = new AppveyorVariables(systemProperties).repoSlug();
        final Optional<String> gitlabCiRepoSlug = new GitlabCiVariables(systemProperties).repoSlug();
        final Optional<String> travisRepoSlug = new TravisCiVariables(systemProperties).repoSlug();

        final Optional<String> result;
        if (appveyorRepoSlug.isPresent()) {
            result = appveyorRepoSlug;
        } else if (gitlabCiRepoSlug.isPresent()) {
            result = gitlabCiRepoSlug;
        } else if (travisRepoSlug.isPresent()) {
            result = travisRepoSlug;
        } else {
            final Optional<String> gitRemoteOriginUrl = GIT_REMOTE_ORIGIN_URL.getValue(ciOptionContext);
            if (gitRemoteOriginUrl.isPresent()) {
                result = gitRepoSlugFromUrl(gitRemoteOriginUrl.get());
            } else {
                result = Optional.empty();
            }
        }

        return result;
    }

    /**
     * Gitlab's sub group is not supported intentionally.
     *
     * @param url git remote origin url
     * @return repo slug
     */
    static Optional<String> gitRepoSlugFromUrl(final String url) {
        final Optional<String> result;

        final Matcher matcher = PATTERN_GIT_REPO_SLUG.matcher(url);
        if (matcher.matches()) {
            result = Optional.ofNullable(matcher.group(1));
        } else {
            result = Optional.empty();
        }

        return result;
    }

    @Override
    public Optional<String> getDefaultValue() {
        return Optional.ofNullable(this.defaultValue);
    }

    @Override
    public String getPropertyName() {
        return this.propertyName;
    }
}
