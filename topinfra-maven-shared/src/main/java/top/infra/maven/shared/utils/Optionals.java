package top.infra.maven.shared.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class Optionals {

    private Optionals() {
    }

    /**
     * Returns first optional that presents.
     *
     * @param optionals optionals
     * @param <T>       value type
     * @return first optional that presents
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> or(final Supplier<Optional<T>>... optionals) {
        return Arrays.stream(optionals)
            .map(Supplier::get)
            .filter(Optional::isPresent)
            .findFirst()
            .orElseGet(Optional::empty);
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T> Optional<T> or(final Optional<T>... optionals) {
        return Arrays.stream(optionals)
            .filter(Objects::nonNull)
            .filter(Optional::isPresent)
            .findFirst()
            .orElseGet(Optional::empty);
    }

    /**
     * Returns first optional that presents.
     *
     * @param optionals optionals
     * @param <T>       value type
     * @return first optional that presents
     */
    public static <T> Optional<T> or(final Collection<Optional<T>> optionals) {
        return optionals.stream()
            .filter(Optional::isPresent)
            .findFirst()
            .orElseGet(Optional::empty);
    }
}
