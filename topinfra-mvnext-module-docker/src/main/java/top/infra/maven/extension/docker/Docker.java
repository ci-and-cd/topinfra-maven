package top.infra.maven.extension.docker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import top.infra.maven.utils.SupportFunction;
import top.infra.maven.utils.SystemUtils;

/**
 * TODO use a java docker client ?
 */
public class Docker {

    private static final Pattern PATTERN_BASE_IMAGE = Pattern.compile("^FROM[ ]+.+$");

    private static final Pattern PATTERN_FILE_WITH_EXT = Pattern.compile(".+/.+\\..+");

    private final Map<String, String> environment;
    private final String homeDir;
    private final String registry;
    private final String registryPass;
    private final String registryUrl;
    private final String registryUser;

    public Docker(
        final String dockerHost,
        final String homeDir,
        final String registry,
        final String registryPass,
        final String registryUrl,
        final String registryUser
    ) {
        this.environment = environment(dockerHost, registry);
        this.homeDir = homeDir;

        this.registry = registry;
        this.registryPass = registryPass;
        this.registryUrl = registryUrl;
        this.registryUser = registryUser;
    }

    static Map<String, String> environment(final String dockerHost, final String registry) {
        final Map<String, String> result = new LinkedHashMap<>();

        if (SupportFunction.notEmpty(dockerHost)) {
            result.put("DOCKER_HOST", dockerHost);
        }

        if (SupportFunction.notEmpty(registry) && !registry.startsWith("https://")) {
            result.put("DOCKER_OPTS", String.format("–insecure-registry %s", registry));
        }

        return result;
    }

    public static Optional<String> dockerHost(final Properties systemProperties) {
        return Optional.ofNullable(systemProperties.getProperty("env.DOCKER_HOST"));
    }

    public static List<String> dockerfiles() {
        return org.unix4j.Unix4j.find(".", "*Docker*")
            .toStringList()
            .stream()
            .filter(line -> !line.contains("/target/classes/")) // TODO windows ?
            .filter(line -> !PATTERN_FILE_WITH_EXT.matcher(line).matches())
            .collect(Collectors.toList());
    }

    public List<String> imageIdsToClean() {
        final Entry<Integer, String> returnCodeAndStdout = this.docker("images");

        final List<String> imageIds;
        if (returnCodeAndStdout.getKey() == 0) {
            imageIds = imagesToClean(SupportFunction.lines(returnCodeAndStdout.getValue()));
        } else {
            imageIds = Collections.emptyList();
        }
        return imageIds;
    }

    private Entry<Integer, String> docker(final String... options) {
        return SystemUtils.exec(this.environment, null, dockerCommand(options));
    }

    static List<String> dockerCommand(final String... options) {
        return SupportFunction.asList(new String[]{"docker"}, options);
    }

    static List<String> imagesToClean(final List<String> dockerImages) {
        return dockerImages
            .stream()
            .filter(line -> line.contains("<none>"))
            .map(line -> line.split("\\s+"))
            .filter(value -> value.length > 2)
            .map(value -> value[2])
            .filter(value -> !"IMAGE".equals(value))
            .collect(Collectors.toList());
    }

    /**
     * Delete images.
     *
     * @param imageIds imageIds
     * @return imageId, return code map
     */
    public Map<String, Integer> deleteImages(final List<String> imageIds) {
        return imageIds.stream().collect(Collectors.toMap(id -> id, id -> this.docker("rmi", id).getKey()));
    }

    public void initConfigFile() {
        this.docker("version");

        // TODO config docker log rotation here ?
        final File dockerConfigDir = Paths.get(this.homeDir, ".docker").toFile();
        if (!dockerConfigDir.exists()) {
            dockerConfigDir.mkdirs();
        }
    }

    /**
     * Pull base images found in Dockerfiles.
     *
     * @param dockerfiles Dockerfiles to find base images in.
     * @return base images found / pulled
     */
    public List<String> pullBaseImages(final List<String> dockerfiles) {
        final List<String> baseImages = baseImages(dockerfiles);
        baseImages.forEach(image -> this.docker("pull", image));
        return baseImages;
    }

    static List<String> baseImages(final List<String> dockerfiles) {
        return dockerfiles
            .stream()
            .map(Docker::baseImageOf)
            .filter(SupportFunction::isNotEmpty)
            .map(line -> line.replaceAll("\\s+", " "))
            .filter(SupportFunction::isNotEmpty)
            .map(line -> line.split("\\s+"))
            .filter(value -> value.length > 1)
            .map(value -> value[1])
            .distinct()
            .collect(Collectors.toList());
    }

    static String baseImageOf(final String dockerfile) {
        try (final Stream<String> stream = Files.lines(Paths.get(dockerfile))) {
            return stream.filter(line -> PATTERN_BASE_IMAGE.matcher(line).matches()).findFirst().orElse(null);
        } catch (final IOException ex) {
            return null;
        }
    }

    public String getLoginTarget() {
        return SupportFunction.isNotEmpty(this.registry) ? this.registry : this.registryUrl;
    }

    public Optional<Integer> login(final String target) {
        final Optional<Integer> result;
        if (SupportFunction.isNotEmpty(target) && !shouldSkipDockerLogin()) {
            final List<String> command = dockerCommand("login", "--password-stdin", "-u=" + this.registryUser, target);
            result = Optional.of(SystemUtils.exec(this.environment, this.registryPass, command).getKey());
        } else {
            result = Optional.empty();
        }
        return result;
    }

    public boolean shouldSkipDockerLogin() {
        return SupportFunction.isEmpty(this.registryPass) || SupportFunction.isEmpty(this.registryUser);
    }
}
