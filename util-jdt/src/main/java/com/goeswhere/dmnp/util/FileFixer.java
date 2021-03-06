package com.goeswhere.dmnp.util;

import com.google.common.base.Function;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Internal junk for a class which processes compilable files in an {@code String apply(String)} fashion.
 *
 * <p>Internally parallelised.  create()'s return values will not be shared between threads.
 * They must not write to the source directory except by returning new file contents.
 *
 * <p>e.g. <pre>
 * class Foo extends SimpleFileFixer {
 *  public static void main(String[] args) throws InterruptedException {
 *   main(args, new Creator() {
 *    public Function<String, String> create() {
 *     return new Foo();
 *    }});
 *  }
 *
 *  public String apply(final String from) {
 *   CompilationUnit cu = compile(from);
 *   cu.recordModifications();
 *   mutate(cu);
 *   return rewrite(from, cu);
 *  }</pre>
 *
 * <p><b>Overwrites files in-place without back-up</b>.
 */
abstract class FileFixer implements Function<String, String> {

    private static ThreadPoolExecutor pool;
    private static boolean quiet;

    static Iterable<File> fileOrFileList(final File f) {
        return f.isDirectory() ? FileUtils.javaFilesIn(f) : Collections.singletonList(f);
    }

    synchronized static ExecutorService service() {
        if (null != pool)
            throw new IllegalStateException("service() can only be used once");

        final int nThreads = Runtime.getRuntime().availableProcessors() * 4;
        return pool = new ThreadPoolExecutor(nThreads, nThreads,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
    }

    static void err(File file, Exception e) {
        System.err.println("While processing " + file + ": ");
        e.printStackTrace(System.err);
    }

    static void proc(File file) {
        msg(file, "Starting");
    }

    static void term(File file) {
        msg(file, "Finished");
    }

    static void msg(File file, final String t) {
        if (quiet)
            return;

        final int num = queueSizeEstimate();
        final String q = approximately(num);

        System.out.println(Thread.currentThread().getName() + ": " + t + " " + file.getName() + ".  " +
                (pool.isShutdown() ? q + " remaining." :
                        "Still finding files.  " + q + " in queue."));
    }

    private static synchronized int queueSizeEstimate() {
        return pool.getQueue().size() + pool.getActiveCount() - 1;
    }

    private static String approximately(final int num) {
        if (0 == num)
            return "0";
        else
            return "~" + num;
    }

    static void quietIfQ(String[] args, final int qpos) {
        if (args.length > qpos && "-q".equals(args[qpos]))
            quiet = true;
    }

    protected abstract CompilationUnit compile(String src);
}
