package top.infra.maven.extension.main;

import static java.lang.Boolean.FALSE;
import static top.infra.maven.Constants.BOOL_STRING_FALSE;
import static top.infra.maven.Constants.BOOL_STRING_TRUE;
import static top.infra.maven.Constants.GIT_REF_NAME_DEVELOP;
import static top.infra.maven.Constants.GIT_REF_PREFIX_FEATURE;
import static top.infra.maven.Constants.GIT_REF_PREFIX_HOTFIX;
import static top.infra.maven.Constants.GIT_REF_PREFIX_RELEASE;
import static top.infra.maven.Constants.GIT_REF_PREFIX_SUPPORT;
import static top.infra.maven.utils.SystemUtils.systemUserHome;

import java.nio.file.Paths;
import java.util.Optional;

import top.infra.maven.cienv.AppveyorVariables;
import top.infra.maven.cienv.TravisCiVariables;
import top.infra.maven.core.CiOption;
import top.infra.maven.core.CiOptionContext;
import top.infra.maven.extension.VcsProperties;
import top.infra.maven.utils.FileUtils;
import top.infra.maven.utils.SupportFunction;

public enum MavenBuildExtensionOption implements CiOption {
    CACHE_SESSION_PATH("cache.session.path") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            // We need a stable path prefix for cache
            // java.io.tmp changed on every time java process start.
            final String prefix = Paths.get(systemUserHome(), ".ci-and-cd", "sessions").toString();
            final String commitId = VcsProperties.GIT_COMMIT_ID.getValue(context).map(value -> value.substring(0, 8)).orElse("unknown-commit");
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
    MVN_DEPLOY_PUBLISH_SEGREGATION("mvn.deploy.publish.segregation"),
    /**
     * Determine current is origin (original) or forked.
     */
    ORIGIN_REPO("origin.repo") {
        @Override
        public Optional<String> calculateValue(final CiOptionContext context) {
            final AppveyorVariables appveyor = new AppveyorVariables(context.getSystemProperties());
            final Optional<String> gitRepoSlug = VcsProperties.gitRepoSlug(context);
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

            final String refName = VcsProperties.GIT_REF_NAME.getValue(context).orElse("");
            final boolean originRepo = ORIGIN_REPO.getValue(context)
                .map(Boolean::parseBoolean).orElse(FALSE);

            if (originRepo) {
                if (GIT_REF_NAME_DEVELOP.equals(refName)
                    || refName.startsWith(GIT_REF_PREFIX_FEATURE)
                    || refName.startsWith(GIT_REF_PREFIX_HOTFIX)
                    || refName.startsWith(GIT_REF_PREFIX_RELEASE)
                    || refName.startsWith(GIT_REF_PREFIX_SUPPORT)
                ) {
                    result = BOOL_STRING_TRUE;
                } else {
                    result = BOOL_STRING_FALSE;
                }
            } else {
                result = refName.startsWith(GIT_REF_PREFIX_FEATURE) ? BOOL_STRING_TRUE : BOOL_STRING_FALSE;
            }

            return Optional.of(result);
        }
    },
    ;

    private final String defaultValue;
    private final String propertyName;

    MavenBuildExtensionOption(final String propertyName) {
        this(propertyName, null);
    }

    MavenBuildExtensionOption(final String propertyName, final String defaultValue) {
        this.defaultValue = defaultValue;
        this.propertyName = propertyName;
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
