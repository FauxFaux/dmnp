package com.goeswhere.dmnp.util;

import static com.goeswhere.dmnp.util.FJava.concatMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class FileUtils {

	private static final int BLOCK = 1024 * 8;

	private final static Charset DEFAULT_ENCODING = Charsets.UTF_8;

	public static final Function<File, String> ABSOLUTE_PATH = new Function<File, String>() {
		@Override public String apply(File f) {
			return f.getAbsolutePath();
		}
	};

	public static class RuntimeIOException extends RuntimeException {
		public RuntimeIOException(IOException e) {
			super(e);
		}

		public RuntimeIOException(String msg, IOException e) {
			super(msg, e);
		}
	}

	private FileUtils() {
		throw new AssertionError();
	}

	/** Entire contents of the file in a String */
	public static String consumeFile(final String filename) throws IOException {
		return consumeFile(new FileInputStream(filename));
	}

	public static String consumeFile(File f) throws IOException {
		return consumeFile(new FileInputStream(f));
	}

	public static String consumeFile(final InputStream in) throws IOException {
		return consumeFile(new InputStreamReader(in, DEFAULT_ENCODING));
	}

	/** CLOSES THE FILEREADER */
	public static String consumeFile(final Reader fileReader) throws IOException {
		final StringBuilder fileData = new StringBuilder(BLOCK);
		try {
			final BufferedReader reader = new BufferedReader(fileReader);
			try {
				final char[] buf = new char[BLOCK];
				int numRead;
				while ((numRead = reader.read(buf)) != -1)
					fileData.append(buf, 0, numRead);
			} finally {
				reader.close();
			}
		} finally {
			fileReader.close();
		}
		return fileData.toString();
	}

	public static void recursivelyDelete(final File dir) {
		for (File fi : dir.listFiles())
			if (fi.isDirectory())
				recursivelyDelete(fi);
			else
				logDelete(fi);
		logDelete(dir);
	}

	private static void logDelete(File fi) {
		if (!fi.delete())
			System.err.println("couldn't delete " + fi);
	}

	public static Iterable<File> javaFilesIn(final String path) {
		return filesIn(path, "java");
	}

	public static Iterable<File> javaFilesIn(final File f) {
		return filesIn(f, "java");
	}

	public static Iterable<File> filesIn(String path, String ext) {
		return filesIn(new File(path), ext);
	}

	private static Iterable<File> filesIn(final File root, final String ext) {
		final String dottedex = "." + ext;

		return filesIn(root, new FileFilter() {
			@Override public boolean accept(File pathname) {
				return pathname.getName().endsWith(dottedex);
			}
		});
	}

	/** Silently ignores un-listable directories. */
	public static Iterable<File> filesIn(final File root, final FileFilter ffun) {
		final FileFilter ff = new FileFilter() {
			@Override public boolean accept(File pathname) {
				return !pathname.isHidden()
					&& (pathname.isDirectory() || ffun.accept(pathname));
			}
		};

		final File[] fl = root.listFiles(ff);
		if (null == fl)
			return Collections.emptyList();

		return concatMap(Arrays.asList(fl),
				new Function<File, Iterable<File>>() {
					@Override public Iterable<File> apply(File from) {
						return from.isDirectory() ? filesIn(from, ff) : Arrays.asList(from);
					}});
	}

	public static File createTempDir(final String name) throws IOException, FailedException {
		final File tmpdir = File.createTempFile(name, null);
		if (!tmpdir.delete() || !tmpdir.mkdir())
			throw new FailedException("Couldn't make temporary directory");
		return tmpdir;
	}

	public static File createTempDir() throws IOException, FailedException {
		return createTempDir("dmnp");
	}

	public static File createTempDir(Map<String, String> files) throws IOException, FailedException {
		final File ret = createTempDir();
		try {
			for (Entry<String, String> fs : files.entrySet())
				writeFile(new File(ret + "/" + fs.getKey()), fs.getValue());
		} catch (RuntimeException e) {
			recursivelyDelete(ret);
			throw e;
		}
		return ret;
	}

	public static void writeFile(File file, String string) {
		try {
			final FileOutputStream fos = new FileOutputStream(file);
			try {
				fos.write(string.getBytes(DEFAULT_ENCODING));
			} finally {
				fos.flush();
				fos.close();
			}
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	/** Accept parameters like -classpath does: {@code a;b;c/*;d} (on Windows) */
	public static String[] sysSplitWithWildcards(final String arg, final String starType) {
		return Iterables.toArray(FJava.concatMap(Arrays.asList(sysSplit(arg)),
				new Function<String, Iterable<String>>() {
					@Override public Iterable<String> apply(String from) {
						if (!from.endsWith("*"))
							return ImmutableList.of(from);
						final String withoutStar = from.substring(0, from.length() - 1);
						return Iterables.transform(FileUtils.filesIn(withoutStar, starType), FileUtils.ABSOLUTE_PATH);
					}}), String.class);
	}

	/** Split by the {@link File#separatorChar}. */
	public static String[] sysSplit(final String string) {
		return string.split(Pattern.quote(File.pathSeparator));
	}

	public static void write(File f, InputStream is) throws IOException {
		final FileOutputStream fos = new FileOutputStream(f);
		try {
			final byte[] buf = new byte[BLOCK];
			int numRead;
			while ((numRead = is.read(buf)) != -1)
				fos.write(buf, 0, numRead);
		} finally {
			fos.close();
		}
	}
}
