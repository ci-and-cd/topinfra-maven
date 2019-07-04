package top.infra.maven.extension;

import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import top.infra.maven.cienv.AppveyorVariables;
import top.infra.maven.cienv.GitlabCiVariables;
import top.infra.maven.cienv.TravisCiVariables;
import top.infra.maven.core.CiOption;
import top.infra.maven.core.CiOptionContext;
import top.infra.maven.core.CiOptionNames;

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
    private final String envVariableName;
    private final String propertyName;
    private final String systemPropertyName;

    VcsProperties(final String propertyName) {
        this(propertyName, null);
    }

    VcsProperties(final String propertyName, final String defaultValue) {
        if (!CiOptionNames.name(propertyName).equals(this.name())) {
            throw new IllegalArgumentException(String.format("invalid property name [%s] for enum name [%s]", this.name(), propertyName));
        }

        this.defaultValue = defaultValue;
        this.envVariableName = CiOptionNames.envVariableName(propertyName);
        this.propertyName = propertyName;
        this.systemPropertyName = CiOptionNames.systemPropertyName(propertyName);
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

    public Optional<String> getDefaultValue() {
        return Optional.ofNullable(this.defaultValue);
    }

    public String getEnvVariableName() {
        return this.envVariableName;
    }

    public String getPropertyName() {
        return this.propertyName;
    }

    public String getSystemPropertyName() {
        return this.systemPropertyName;
    }
}
