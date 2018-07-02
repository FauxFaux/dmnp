package com.goeswhere.dmnp.ue;

import com.goeswhere.dmnp.util.ASTWrapper;
import com.goeswhere.dmnp.util.FileUtils;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class UsefulExceptionChecker {

    static final class ProcessFolder implements Runnable {
        final List<String> output;
        final String folder;

        /**
         * Process a folder, directly placing the results in the output parameter.
         */
        ProcessFolder(String folder, List<String> output) {
            this.output = output;
            this.folder = folder;
        }

        @Override
        public void run() {
            try {
                processDir(new File(folder), r -> {
                    output.add(r);
                    System.out.println(r + " in " + folder);
                });
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }
    }

    private static interface ResultAccumulator {
        void accumulate(String r);
    }

    public static void main(String[] args) throws InterruptedException {
        if (args.length == 0) {
            System.out.println("Usage: dir1 [dir2...]");
            System.exit(-1);
        } else {
            final ExecutorService pool = Executors.newCachedThreadPool();
            final List<List<String>> reses = new ArrayList<>(args.length);
            for (String arg1 : args) reses.add(Collections.synchronizedList(new ArrayList<>()));

            for (int i = 0; i < args.length; ++i) {
                final String arg = args[i];
                final List<String> res = reses.get(i);
                pool.submit(new ProcessFolder(arg, res));
            }
            pool.shutdown();
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

            System.out.println();
            int count = 0;
            for (int i = 0; i < args.length; ++i) {
                final int thisSize = reses.get(i).size();
                count += thisSize;
                System.out.println(args[i] + ": " + thisSize);
            }

            System.out.println();
            System.out.println(count + " instances total");
        }
    }

    static void processDir(File dir, ResultAccumulator ra) throws IOException {
        if (null == dir || null == dir.listFiles()) {
            System.err.println("Invalid dir: " + dir);
            return;
        }

        for (File child : dir.listFiles())
            if (!child.getName().equals(".") && !child.getName().equals(".."))
                if (child.isDirectory())
                    processDir(child, ra);
                else if (child.getName().endsWith(".java"))
                    processFile(child.getPath(), ra);
    }

    static void processFile(final String filename, ResultAccumulator ra) throws IOException {
        processString(new File(filename).getName(), FileUtils.consumeFile(filename), ra);
    }

    private static void processString(final String filename, final String contents, final ResultAccumulator ra) {
        final CompilationUnit cu = ASTWrapper.compile(contents);
        final Reporter rep = cc -> ra.accumulate(cc.getException()
                + " unused at (" + filename + ":" + cu.getLineNumber(cc.getStartPosition())
                + ") in " + ASTWrapper.methodName(cc));
        VisitCatchClauses.accept(cu, rep);
    }
}
