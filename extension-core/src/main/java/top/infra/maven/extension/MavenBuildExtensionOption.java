package top.infra.maven.extension;

import static java.lang.Boolean.FALSE;
import static top.infra.maven.Constants.BOOL_STRING_FALSE;
import static top.infra.maven.Constants.BOOL_STRING_TRUE;
import static top.infra.maven.Constants.BRANCH_PREFIX_FEATURE;
import static top.infra.maven.Constants.BRANCH_PREFIX_HOTFIX;
import static top.infra.maven.Constants.BRANCH_PREFIX_RELEASE;
import static top.infra.maven.Constants.BRANCH_PREFIX_SUPPORT;
import static top.infra.maven.Constants.GIT_REF_NAME_DEVELOP;
import static top.infra.maven.utils.SystemUtils.systemUserHome;

import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import top.infra.maven.core.CiOption;
import top.infra.maven.core.CiOptionContext;
import top.infra.maven.core.CiOptionNames;
import top.infra.maven.core.GitProperties;
import top.infra.maven.cienv.AppveyorVariables;
import top.infra.maven.cienv.GitlabCiVariables;
import top.infra.maven.cienv.TravisCiVariables;
import top.infra.maven.utils.FileUtils;
import top.infra.maven.utils.SupportFunction;

public enum MavenBuildExtensionOption implements CiOption {
    CACHE_SESSION_PATH("cache.session.path") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            // We need a stable path prefix for cache
            // java.io.tmp changed on every time java process start.
            final String prefix = Paths.get(systemUserHome(), ".ci-and-cd", "sessions").toString();
            final String commitId = context.getGitProperties().commitId().map(value -> value.substring(0, 8)).orElse("unknown-commit");
            final String pathname = Paths.get(prefix, SupportFunction.uniqueKey(), commitId).toString();
            return Optional.of(pathname);
        }

        @Override
        public Optional<String> getValue(final CiOptionContext context) {
            final Optional<String> result = super.getValue(context);
            result.ifPresent(FileUtils::createDirectories);
            return result;
        }
    },
    // GIT_AUTH_TOKEN("git.auth.token"),
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
            } else if (travisBranch.isPresent()) {
                result = travisBranch;
            } else {
                result = context.getGitProperties().refName();
            }

            return result;
        }
    },
    MVN_DEPLOY_PUBLISH_SEGREGATION("mvn.deploy.publish.segregation"),
    /**
     * Determine current is origin (original) or forked.
     */
    ORIGIN_REPO("origin.repo") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            final AppveyorVariables appveyor = new AppveyorVariables(context.getSystemProperties());
            final Optional<String> gitRepoSlug = gitRepoSlug(context.getGitProperties(), context.getSystemProperties());
            final Optional<String> originRepoSlug = ORIGIN_REPO_SLUG.getValue(context);
            final TravisCiVariables travisCi = new TravisCiVariables(context.getSystemProperties());

            return Optional.of(
                originRepoSlug.isPresent() && gitRepoSlug.isPresent() && gitRepoSlug.get().equals(originRepoSlug.get())
                    && !travisCi.isPullRequestEvent()
                    && !appveyor.isPullRequest()
                    ? BOOL_STRING_TRUE
                    : BOOL_STRING_FALSE
            );
        }
    },
    ORIGIN_REPO_SLUG("origin.repo.slug", "unknown/unknown"),
    PUBLISH_TO_REPO("publish.to.repo") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            final String result;

            final String refName = GIT_REF_NAME.getValue(context).orElse("");
            final boolean originRepo = ORIGIN_REPO.getValue(context)
                .map(Boolean::parseBoolean).orElse(FALSE);

            if (originRepo) {
                if (GIT_REF_NAME_DEVELOP.equals(refName)
                    || refName.startsWith(BRANCH_PREFIX_FEATURE)
                    || refName.startsWith(BRANCH_PREFIX_HOTFIX)
                    || refName.startsWith(BRANCH_PREFIX_RELEASE)
                    || refName.startsWith(BRANCH_PREFIX_SUPPORT)
                ) {
                    result = BOOL_STRING_TRUE;
                } else {
                    result = BOOL_STRING_FALSE;
                }
            } else {
                result = refName.startsWith(BRANCH_PREFIX_FEATURE) ? BOOL_STRING_TRUE : BOOL_STRING_FALSE;
            }

            return Optional.of(result);
        }
    },
    ;

    private final String defaultValue;
    private final String envVariableName;
    private final String propertyName;
    private final String systemPropertyName;

    MavenBuildExtensionOption(final String propertyName) {
        this(propertyName, null);
    }

    MavenBuildExtensionOption(final String propertyName, final String defaultValue) {
        if (!CiOptionNames.name(propertyName).equals(this.name())) {
            throw new IllegalArgumentException(String.format("invalid property name [%s] for enum name [%s]", this.name(), propertyName));
        }

        this.defaultValue = defaultValue;
        this.envVariableName = CiOptionNames.envVariableName(propertyName);
        this.propertyName = propertyName;
        this.systemPropertyName = CiOptionNames.systemPropertyName(propertyName);
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

    /**
     * Get slug info of current repository (directory).
     *
     * @param gitProperties    gitProperties
     * @param systemProperties systemProperties
     * @return 'group/project' or 'owner/project'
     */
    public static Optional<String> gitRepoSlug(
        final GitProperties gitProperties,
        final Properties systemProperties
    ) {
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
            final Optional<String> gitRemoteOriginUrl = gitProperties.remoteOriginUrl();
            if (gitRemoteOriginUrl.isPresent()) {
                result = gitRepoSlugFromUrl(gitRemoteOriginUrl.get());
            } else {
                result = Optional.empty();
            }
        }

        return result;
    }

    static final Pattern PATTERN_GIT_REPO_SLUG = Pattern.compile(".*[:/]([^/]+(/[^/.]+))(\\.git)?");

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
}
