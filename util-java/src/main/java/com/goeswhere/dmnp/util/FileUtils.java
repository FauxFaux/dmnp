package com.goeswhere.dmnp.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.regex.Pattern;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class FileUtils {

	private final static Charset DEFAULT_ENCODING = Charset.forName("UTF-8");

	public static final Function<File, String> ABSOLUTE_PATH = new Function<File, String>() {
		@Override public String apply(File f) {
			return f.getAbsolutePath();
		}
	};

	private FileUtils() {
		throw new AssertionError();
	}

	/** Entire contents of the file in a String */
	public static String consumeFile(final String filename) throws IOException {
		return consumeFile(new FileReader(filename));
	}

	/** CLOSES THE FILEREADER */
	public static String consumeFile(final FileReader fileReader) throws IOException {
		final int block = 1024 * 10;
		final StringBuilder fileData = new StringBuilder(block);
		try {
			final BufferedReader reader = new BufferedReader(fileReader);
			try {
				char[] buf = new char[block];
				int numRead = 0;
				while ((numRead = reader.read(buf)) != -1) {
					fileData.append(buf, 0, numRead);
				}
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

	public static Iterable<File> filesIn(final String path, final String ext) {
		return new Iterable<File>() {
			@SuppressWarnings("unchecked")
			@Override
			public java.util.Iterator<File> iterator() {
				return org.apache.commons.io.FileUtils.iterateFiles(
						new File(path),
						new String[] { ext },
						true);
			}
		};
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

	public static void writeFile(File file, String string) throws IOException {
		final FileOutputStream fos = new FileOutputStream(file);
		try {
			fos.write(string.getBytes(DEFAULT_ENCODING));
		} finally {
			fos.flush();
			fos.close();
		}
	}

	public static String consumeFile(File f) throws IOException {
		return consumeFile(new FileReader(f));
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
}
