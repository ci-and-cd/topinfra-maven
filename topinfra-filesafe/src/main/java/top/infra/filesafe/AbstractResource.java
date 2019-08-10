package top.infra.filesafe;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import top.infra.logging.Logger;
import top.infra.util.cli.CliUtils;

public abstract class AbstractResource implements Resource {

    protected final Logger logger;
    protected final Path path;

    public AbstractResource(final Logger logger, final Path path) {
        this.logger = logger;
        this.path = path;
    }

    protected Entry<Integer, String> exec(final List<String> command) {
        return CliUtils.exec(this.getEnvironment(), null, command);
    }

    protected Entry<Integer, String> exec(final String stdIn, final List<String> command) {
        return CliUtils.exec(this.getEnvironment(), stdIn, command);
    }

    protected abstract Map<String, String> getEnvironment();

    @Override
    public Path getPath() {
        return this.path;
    }

    @Override
    public String toString() {
        return this.path == null ? "null" : this.path.toString();
    }
}
