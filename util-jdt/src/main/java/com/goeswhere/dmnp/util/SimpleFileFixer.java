package com.goeswhere.dmnp.util;

import com.google.common.base.Function;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;
import java.util.concurrent.ExecutorService;

import static com.goeswhere.dmnp.util.Containers.awaitTermination;
import static com.goeswhere.dmnp.util.FileUtils.consumeFile;
import static com.goeswhere.dmnp.util.FileUtils.writeFile;

/**
 * {@link #compile(String)} returns a simple CU without resolveBindings support.
 */
public abstract class SimpleFileFixer extends FileFixer {

    public static interface Creator {
        Function<String, String> create();
    }

    private static final ThreadLocal<String> FILE_NAME = new ThreadLocal<>();

    protected SimpleFileFixer() {
        // reducing visibility
    }

    public static void main(String[] args, final Class<? extends Function<String, String>> c) throws InterruptedException {
        main(null, 0, args, () -> {
            try {
                return c.newInstance();
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void main(String[] args, final Creator creator) throws InterruptedException {
        main(null, 0, args, creator);
    }

    public static void main(String extra, int excount, String[] args, final Creator creator) throws InterruptedException {
        if (excount + 1 != args.length && excount + 2 != args.length) {

            System.err.println("Usage: " +
                    (excount != 0 ? extra + " " : "") +
                    "path [-q]");
            return;
        }

        quietIfQ(args, excount + 1);

        loop(args[excount], creator);
    }

    protected static String getFileName() {
        return FILE_NAME.get();
    }

    protected static void loop(final String path, final Creator creator)
            throws InterruptedException {
        final File f = new File(path);
        final ExecutorService es = service();

        for (final File file : fileOrFileList(f)) {
            es.submit(() -> {
                proc(file);
                try {
                    final String thispath = file.getAbsolutePath();
                    final String read = consumeFile(thispath);
                    FILE_NAME.set(path);

                    final String result = creator.create().apply(read);

                    if (!result.equals(read))
                        writeFile(new File(thispath), result);

                } catch (Exception e) {
                    err(file, e);
                } finally {
                    term(file);
                }
            });
        }

        awaitTermination(es);
    }

    @Override
    protected CompilationUnit compile(String src) {
        return ASTWrapper.compile(src);
    }
}
