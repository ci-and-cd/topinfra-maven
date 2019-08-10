package top.infra.filesafe;

import java.nio.file.Path;

public interface Resource {

    Path getPath();

    default boolean exists() {
        return this.getPath() != null && this.getPath().toFile().exists();
    }
}
