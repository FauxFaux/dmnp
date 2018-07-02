package com.goeswhere.dmnp.util;

import com.google.common.collect.Lists;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.ListIterator;

/**
 * <pre> Closer c = new Closer();
 * try {
 *  A a = c.add(new A());
 *  B b = c.add(new B());
 *  a.whatever();
 *  b.whatever();
 * } finally { c.close(); }</pre>
 */
class Closer implements Closeable {
    private List<Closeable> clo = Lists.newArrayList();
    private final ExceptionHandler eh;

    private static final ExceptionHandler STDERR_EXCEPTION_HANDLER = e -> e.printStackTrace(System.err);

    public <T extends Closeable> T add(T c) {
        clo.add(c);
        return c;
    }

    public <T extends Connection> T add(final T c) {
        clo.add(wrap(c));
        return c;
    }

    public <T extends Statement> T add(final T c) {
        clo.add(wrap(c));
        return c;
    }

    private static class ClosingFailedException extends RuntimeException {
        ClosingFailedException(Throwable cause) {
            super(cause);
        }
    }

    static interface ExceptionHandler {
        void handle(Exception e);
    }

    public Closer() {
        this(STDERR_EXCEPTION_HANDLER);
    }

    private Closer(ExceptionHandler eh) {
        this.eh = eh;
    }

    @Override
    public void close() {
        if (null == clo)
            throw new RuntimeException("Closer already closed");

        final ListIterator<Closeable> it = clo.listIterator(clo.size());
        while (it.hasPrevious()) {
            Closeable c = it.previous();
            try {
                c.close();
            } catch (Exception e) {
                eh.handle(e);
            }
        }

        clo = null;
    }

    private static Closeable wrap(final Connection c) {
        return () -> {
            try {
                c.close();
            } catch (SQLException e) {
                throw new ClosingFailedException(e);
            }
        };
    }

    private static Closeable wrap(final Statement c) {
        return () -> {
            try {
                c.close();
            } catch (SQLException e) {
                throw new ClosingFailedException(e);
            }
        };
    }
}
