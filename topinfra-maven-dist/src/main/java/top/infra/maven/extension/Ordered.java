package top.infra.maven.extension;

import java.util.Comparator;

import org.jetbrains.annotations.NotNull;

public interface Ordered extends Comparable<Ordered> {

    Comparator<Ordered> ORDERED_COMPARATOR = (o1, o2) -> {
        final int result;
        if (o1 != null && o2 != null) {
            result = Integer.compare(o1.getOrder(), o2.getOrder());
        } else if (o1 != null) {
            result = -1;
        } else if (o2 != null) {
            result = 1;
        } else {
            result = 0;
        }
        return result;
    };

    @Override
    default int compareTo(@NotNull final Ordered o2) {
        return ORDERED_COMPARATOR.compare(this, o2);
    }

    /**
     * Useful constant for the highest precedence value.
     *
     * @see java.lang.Integer#MIN_VALUE
     */
    int HIGHEST_PRECEDENCE = Integer.MIN_VALUE;

    /**
     * Useful constant for the lowest precedence value.
     *
     * @see java.lang.Integer#MAX_VALUE
     */
    int LOWEST_PRECEDENCE = Integer.MAX_VALUE;

    /**
     * Get the order value of this object.
     * </p>
     * Higher values are interpreted as lower priority. As a consequence,
     * the object with the lowest value has the highest priority (somewhat
     * analogous to Servlet {@code load-on-startup} values).
     * </p>
     * Same order values will result in arbitrary sort positions for the
     * affected objects.
     *
     * @return the order value (default method returns {@link #LOWEST_PRECEDENCE})
     * @see #HIGHEST_PRECEDENCE
     * @see #LOWEST_PRECEDENCE
     */
    default int getOrder() {
        return LOWEST_PRECEDENCE;
    }
}
