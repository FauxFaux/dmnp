package com.goeswhere.dmnp.util;

import static com.goeswhere.dmnp.util.FileUtils.ABSOLUTE_PATH;
import static com.goeswhere.dmnp.util.FileUtils.createTempDir;
import static com.goeswhere.dmnp.util.FileUtils.filesIn;
import static com.goeswhere.dmnp.util.FileUtils.javaFilesIn;
import static com.goeswhere.dmnp.util.FileUtils.recursivelyDelete;
import static com.goeswhere.dmnp.util.FileUtils.sysSplit;
import static com.goeswhere.dmnp.util.FileUtils.sysSplitWithWildcards;
import static com.goeswhere.dmnp.util.FileUtils.writeFile;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.junit.Test;

import com.google.common.collect.Iterables;


public class FileUtilsTest {
	@Test public void testSplits() {
		assertArrayEquals(new String[] { "a", "b" },
				sysSplit("a" + File.pathSeparator + "b"));
		assertArrayEquals(new String[] { "a*", "b" },
				sysSplit("a*" + File.pathSeparator + "b"));
	}

	@Test public void testSplitStar() throws IOException, FailedException {
		final File td = createTempDir();
		try {
			final File childdir = new File(td + "/s");
			assertTrue(childdir.mkdir());
			final String a = childdir + "/a.tmp";
			final String b = childdir + "/b.tmp";

			writeFile(new File(a), "foo");
			writeFile(new File(b), "bar");

			assertArrayEquals(new String[] { "q", washPath(a), washPath(b) },
					sysSplitWithWildcards("q" + File.pathSeparator + td + "/s/*", "tmp"));
		} finally {
			recursivelyDelete(td);
		}
	}

	@Test public void testAbsolutePath() {
		final File f = new File("");
		assertEquals(f.getAbsolutePath(), ABSOLUTE_PATH.apply(f));
	}

	private String washPath(final String p) {
		return ABSOLUTE_PATH.apply(new File(p));
	}

	final FileFilter EVERYTHING_FILEFILTER = new FileFilter() {
		@Override public boolean accept(File pathname) {
			return true;
		}
	};

	@Test public void testFilesIn() {
		filesIn(new File("/"), EVERYTHING_FILEFILTER).iterator().next();
		Iterables.toString(filesIn(new File("."), EVERYTHING_FILEFILTER));
		javaFilesIn(".").iterator().next().getName().endsWith(".java");
		System.out.println(filesIn(".classpath", "classpath"));
	}
}
