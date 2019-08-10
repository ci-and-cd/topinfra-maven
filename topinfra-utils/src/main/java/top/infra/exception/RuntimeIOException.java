package top.infra.exception;

import java.io.IOException;

public class RuntimeIOException extends RuntimeException {

    public RuntimeIOException(final String msg) {
        super(msg);
    }

    public RuntimeIOException(final String msg, final IOException cause) {
        super(msg, cause);
    }

    public RuntimeIOException(final IOException cause) {
        super(cause);
    }
}
