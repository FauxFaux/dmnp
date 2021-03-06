package com.goeswhere.dmnp.util;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import static com.goeswhere.dmnp.util.FileUtils.*;
import static org.junit.jupiter.api.Assertions.*;


class FileUtilsTest {
    @Test
    void testSplits() {
        assertArrayEquals(new String[]{"a", "b"},
                sysSplit("a" + File.pathSeparator + "b"));
        assertArrayEquals(new String[]{"a*", "b"},
                sysSplit("a*" + File.pathSeparator + "b"));
    }

    @Test
    void testSplitStar() throws IOException, FailedException {
        final File td = createTempDir();
        try {
            final File childdir = new File(td + "/s");
            assertTrue(childdir.mkdir());
            final String a = childdir + "/a.tmp";
            final String b = childdir + "/b.tmp";

            writeFile(new File(a), "foo");
            writeFile(new File(b), "bar");

            assertArrayEquals(new String[]{"q", washPath(a), washPath(b)},
                    sysSplitWithWildcards("q" + File.pathSeparator + td + "/s/*", "tmp"));
        } finally {
            recursivelyDelete(td);
        }
    }

    @Test
    void testAbsolutePath() {
        final File f = new File("");
        assertEquals(f.getAbsolutePath(), ABSOLUTE_PATH.apply(f));
    }

    private String washPath(final String p) {
        return ABSOLUTE_PATH.apply(new File(p));
    }

    private final FileFilter EVERYTHING_FILEFILTER = pathname -> true;

    @Test
    void testFilesIn() {
        filesIn(new File("/"), EVERYTHING_FILEFILTER).iterator().next();
        Iterables.toString(filesIn(new File("."), EVERYTHING_FILEFILTER));
        javaFilesIn(".").iterator().next().getName().endsWith(".java");
        System.out.println(filesIn(".classpath", "classpath"));
    }
}
