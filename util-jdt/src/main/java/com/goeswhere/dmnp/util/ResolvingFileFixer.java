package com.goeswhere.dmnp.util;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.goeswhere.dmnp.util.Containers.awaitTermination;
import static com.goeswhere.dmnp.util.FileUtils.*;

/**
 * The results of {@link #compile(String)} support the various resolveBindings() methods.
 *
 * <p>This is harder to set-up, significantly slower than, and less parallelisable than {@link SimpleFileFixer}.
 *
 * <p>It has internal locking to prevent files from changing while a compile is running.
 */
public abstract class ResolvingFileFixer extends FileFixer {

    protected static interface Creator {
        Function<String, String> create(String[] cp, String[] sourcePath, String unitName, Lock compileLock);
    }

    private static final ThreadLocal<String> FILE_NAME = new ThreadLocal<String>() {
        /** Mostly for testing. */
        @Override
        protected String initialValue() {
            return "unknown";
        }
    };

    private final String[] classpath;
    private final String[] sourcepath;
    private final String unitName;
    private final Lock compilerLock;

    /**
     * compile() resolves bindings.
     */
    protected ResolvingFileFixer(String[] classpath, String[] sourcepath, String unitName, Lock compilerLock) {
        this.classpath = classpath;
        this.sourcepath = sourcepath;
        this.unitName = unitName;
        this.compilerLock = compilerLock;
    }

    /**
     * Process parameters for main, then do work.
     */
    protected static void main(String[] args, Creator creator) throws InterruptedException {
        main(null, 0, args, creator);
    }

    protected static void main(String extra, int excount, String[] args, Creator creator) throws InterruptedException {
        if (excount + 3 != args.length && excount + 4 != args.length) {
            System.err.println("Usage: " +
                    (excount != 0 ? extra + " " : "") +
                    "classpath sourcepath file [-q]");
            return;
        }

        final String[] cp = sysSplitWithWildcards(args[excount + 0], "jar");
        final String[] sourcePath = sysSplit(args[excount + 1]);
        final String path = args[excount + 2];
        quietIfQ(args, excount + 3);

        loop(cp, sourcePath, path, creator);
    }

    private static String getFileName() {
        return FILE_NAME.get();
    }

    protected static String fileLocation(final CompilationUnit cu, ASTNode s) {
        return new File(getFileName()).getName() + ":" + cu.getLineNumber(s.getStartPosition());
    }

    private static void loop(final String[] cp, final String[] sourcePath, final String path, final Creator creator)
            throws InterruptedException {
        final File f = new File(path);
        final ExecutorService es = service();
        final ReadWriteLock r = new ReentrantReadWriteLock(true);
        final Lock wl = r.writeLock();

        try (BlockDoer writer = BlockDoer.start(wl)) {
            for (final File file : fileOrFileList(f)) {
                es.submit(() -> {
                    proc(file);
                    try {
                        final String thispath = file.getAbsolutePath();
                        FILE_NAME.set(thispath);
                        final String read;

                        final Lock rl = r.readLock();
                        rl.lock();
                        try {
                            read = consumeFile(thispath);
                        } finally {
                            rl.unlock();
                        }

                        final String result = creator.create(cp, sourcePath, unitName(thispath), rl).apply(read);

                        if (!result.equals(read))
                            writer.offer(() -> {
                                msg(file, "Writing");
                                writeFile(new File(thispath), result);
                            });
                    } catch (Exception e) {
                        err(file, e);
                    } finally {
                        term(file);
                    }
                });
            }

            awaitTermination(es);
        }
    }

    private static String unitName(String path) {
        final String f = new File(path).getName();
        final String ext = ".java";
        Preconditions.checkArgument(f.endsWith(ext));
        return f.substring(0, f.length() - ext.length());
    }

    @Override
    protected CompilationUnit compile(String src) {
        compilerLock.lock();

        try {
            return ASTWrapper.compile(src, unitName, classpath, sourcepath);
        } finally {
            compilerLock.unlock();
        }
    }
}
