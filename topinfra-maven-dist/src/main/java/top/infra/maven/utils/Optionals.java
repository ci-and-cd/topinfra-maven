package top.infra.maven.utils;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class Optionals {

    private Optionals() {
    }

    public static <T> Optional<T> or(final Supplier<Optional<T>>... optionals) {
        return Arrays.stream(optionals)
            .map(Supplier::get)
            .filter(Optional::isPresent)
            .findFirst()
            .orElseGet(Optional::empty);
    }
}
